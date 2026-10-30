package g2.be.service;

import g2.be.dto.request.Xml1MedicalRecordRequest;
import g2.be.dto.request.Xml4ParaclinicalRequest;
import g2.be.dto.response.CombinedXmlResponse;
import g2.be.entity.Invoice;
import g2.be.entity.MedicalRecord;
import g2.be.entity.ServiceOrder;
import g2.be.entity.XmlCombinedFlatRecord;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.MedicalRecordRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.repository.XmlCombinedFlatRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BhytSyncService {

    private static final String APPOINTMENT_STATUS_COMPLETED = "COMPLETED";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String BHYT_SYNC_STATUS_PENDING = "PENDING";
    private static final String BHYT_SYNC_STATUS_PROCESSING = "PROCESSING";
    private static final String BHYT_SYNC_STATUS_SUCCESS = "SUCCESS";
    private static final String BHYT_SYNC_STATUS_FAILED = "FAILED";

    private final InvoiceRepository invoiceRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final Xml1MedicalRecordMapper xml1Mapper;
    private final Xml4ParaclinicalMapper xml4Mapper;
    private final XmlCombinedFlatRecordRepository flatRecordRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.mock-bhxh.url:http://localhost:8080/api/mock-bhxh/receive}")
    private String mockBhxhUrl;

    @Value("${clinic.bhyt-sync.async.batch-size:10}")
    private int batchSize;

    /**
     * Build XML1 + XML4 tổng hợp từ tất cả hóa đơn PAID của appointmentId
     */
    public CombinedXmlResponse buildCombinedXml(String appointmentId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointment_AppointmentId(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_FOUND));

        // Lấy tất cả invoices PAID
        List<Invoice> paidInvoices = invoiceRepository.findByMedicalRecord_RecordId(medicalRecord.getRecordId())
                .stream().filter(inv -> "PAID".equals(inv.getPaymentStatus())).toList();

        if (paidInvoices.isEmpty()) {
            throw new AppException(ErrorCode.INVOICE_NOT_FOUND);
        }

        // Dùng invoice đầu tiên để map XML1 (thông tin bệnh nhân, bệnh án)
        Invoice primaryInvoice = paidInvoices.get(0);
        Xml1MedicalRecordRequest xml1 = xml1Mapper.mapToXml1Request(primaryInvoice);

        // Tổng hợp tài chính từ TẤT CẢ invoices
        BigDecimal totalServiceFee = BigDecimal.ZERO;
        BigDecimal totalInsurancePaid = BigDecimal.ZERO;
        BigDecimal totalPatientPaid = BigDecimal.ZERO;
        for (Invoice inv : paidInvoices) {
            if (inv.getTotalServiceFee() != null) totalServiceFee = totalServiceFee.add(inv.getTotalServiceFee());
            if (inv.getInsurancePaid() != null) totalInsurancePaid = totalInsurancePaid.add(inv.getInsurancePaid());
            if (inv.getCustomerPaid() != null) totalPatientPaid = totalPatientPaid.add(inv.getCustomerPaid());
        }
        xml1.setTTongchiBv(totalServiceFee);
        xml1.setTTongchiBh(totalInsurancePaid);
        xml1.setTBntt(totalPatientPaid);

        // Lấy TẤT CẢ service orders (từ mọi invoice)
        List<ServiceOrder> allServiceOrders = serviceOrderRepository
                .findByMedicalRecord_RecordId(medicalRecord.getRecordId());

        List<Xml4ParaclinicalRequest> xml4List = xml4Mapper.mapToXml4List(allServiceOrders, xml1.getMaLk());
        xml4Mapper.assignStt(xml4List);

        // Metadata
        long completedTests = allServiceOrders.stream()
                .filter(so -> "COMPLETED".equals(so.getStatus()) || "PAID".equals(so.getStatus()))
                .count();
        long pendingTests = allServiceOrders.size() - completedTests;

        return CombinedXmlResponse.builder()
                .xml1(xml1)
                .xml4(xml4List)
                .metadata(CombinedXmlResponse.MetaData.builder()
                        .maLk(xml1.getMaLk())
                        .patientName(xml1.getHoTen())
                        .totalParaclinicalTests(allServiceOrders.size())
                        .completedTests((int) completedTests)
                        .pendingTests((int) pendingTests)
                        .message(pendingTests > 0
                                ? "Có " + pendingTests + " dịch vụ chưa hoàn thành."
                                : "Tất cả dịch vụ đã hoàn thành.")
                        .build())
                .build();
    }

    /**
     * Kết thúc khám — Gửi đồng bộ toàn bộ hồ sơ sang Mock API BHXH
     */
    @Transactional
    public CombinedXmlResponse syncByAppointment(String appointmentId) {
        return syncAppointmentInternal(appointmentId);
    }

    @Async
    public void queueAppointmentSyncAsync(String appointmentId) {
        try {
            runInRequiresNewTransaction(() -> {
                syncQueuedAppointment(appointmentId);
                return null;
            });
        } catch (RuntimeException ex) {
            log.error("[BhytSync] Async queue failed for appointment {}: {}", appointmentId, ex.getMessage(), ex);
        }
    }

    public void processCompletedAppointmentSyncs() {
        List<String> appointmentIds = invoiceRepository.findAppointmentIdsEligibleForBhytSync(
                APPOINTMENT_STATUS_COMPLETED,
                PAYMENT_STATUS_PAID,
                List.of(BHYT_SYNC_STATUS_PENDING, BHYT_SYNC_STATUS_FAILED),
                PageRequest.of(0, batchSize));

        for (String appointmentId : appointmentIds) {
            try {
                runInRequiresNewTransaction(() -> {
                    syncQueuedAppointment(appointmentId);
                    return null;
                });
            } catch (RuntimeException ex) {
                log.error("[BhytSync] Scheduled processing failed for appointment {}: {}", appointmentId, ex.getMessage(), ex);
            }
        }
    }

    private void syncQueuedAppointment(String appointmentId) {
        if (!tryClaimAppointmentSync(appointmentId)) {
            log.debug("[BhytSync] Skip appointment {} because no claimable invoices remain", appointmentId);
            return;
        }

        try {
            syncAppointmentInternal(appointmentId);
        } catch (RuntimeException ex) {
            updateAppointmentSyncStatus(appointmentId, BHYT_SYNC_STATUS_FAILED, ex.getMessage());
            throw ex;
        }
    }

    private boolean tryClaimAppointmentSync(String appointmentId) {
        return invoiceRepository.claimBhytSyncProcessingByAppointment(
                appointmentId,
                PAYMENT_STATUS_PAID,
                BHYT_SYNC_STATUS_PENDING,
                BHYT_SYNC_STATUS_FAILED,
                BHYT_SYNC_STATUS_PROCESSING) > 0;
    }

    private CombinedXmlResponse syncAppointmentInternal(String appointmentId) {
        log.info("=== [BhytSync] Starting sync for appointmentId: {} ===", appointmentId);

        CombinedXmlResponse combined = buildCombinedXml(appointmentId);

        // Save to flat table
        try {
            saveToFlatTable(combined.getXml1(), combined.getXml4());
            log.info("[BhytSync] Saved to xml_combined_flat_record OK");
        } catch (Exception e) {
            log.error("[BhytSync] Error saving flat table: {}", e.getMessage(), e);
        }

        // Send to Mock BHXH Server
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointment_AppointmentId(appointmentId)
                .orElse(null);

        sendToMockBhxhServer(appointmentId, combined);

        // Update medical record status
        if (medicalRecord != null) {
            medicalRecord.setStatus("COMPLETED");
            medicalRecordRepository.save(medicalRecord);
        }

        return combined;
    }

    private <T> T runInRequiresNewTransaction(java.util.function.Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> action.get());
    }

    private void updateAppointmentSyncStatus(String appointmentId, String status, String errorMessage) {
        invoiceRepository.updateBhytSyncStatusByAppointment(
                appointmentId,
                PAYMENT_STATUS_PAID,
                status,
                trimErrorMessage(errorMessage));
    }

    private String trimErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        return errorMessage.substring(0, Math.min(errorMessage.length(), 2000));
    }

    private void saveToFlatTable(Xml1MedicalRecordRequest xml1, List<Xml4ParaclinicalRequest> xml4List) {
        List<XmlCombinedFlatRecord> recordsToSave = new ArrayList<>();

        if (xml4List == null || xml4List.isEmpty()) {
            // Case no paraclinical tests, save just the XML1 data with dummy XML4 stt
            recordsToSave.add(mapToEntity(xml1, null, 1));
        } else {
            // For each test, create a flat record combining XML1 + XML4
            for (Xml4ParaclinicalRequest xml4 : xml4List) {
                recordsToSave.add(mapToEntity(xml1, xml4, xml4.getStt()));
            }
        }

        flatRecordRepository.saveAll(recordsToSave);
    }

    private XmlCombinedFlatRecord mapToEntity(Xml1MedicalRecordRequest xml1, Xml4ParaclinicalRequest xml4,
            int sttXml4) {
        XmlCombinedFlatRecord record = new XmlCombinedFlatRecord();

        // --- Map XML1 ---
        record.setMaLk(xml1.getMaLk());
        record.setSttXml1(xml1.getStt());
        record.setMaBn(xml1.getMaBn());
        record.setHoTen(xml1.getHoTen());
        record.setSoCccd(xml1.getSoCccd());
        record.setNgaySinh(xml1.getNgaySinh());
        record.setGioiTinh(xml1.getGioiTinh());
        record.setMaQuocTich(xml1.getMaQuocTich());
        record.setMaDanToc(xml1.getMaDanToc());
        record.setMaNgheNghiep(xml1.getMaNgheNghiep());
        record.setDiaChi(xml1.getDiaChi());
        record.setMaTinhCuTru(xml1.getMaTinhCuTru());
        record.setMaHuyenCuTru(xml1.getMaHuyenCuTru());
        record.setMaXaCuTru(xml1.getMaXaCuTru());
        record.setDienThoai(xml1.getDienThoai());
        record.setMaTheBhyt(xml1.getMaTheBhyt());
        record.setMaDkbd(xml1.getMaDkbd());
        record.setGtTheTu(xml1.getGtTheTu());
        record.setGtTheDen(xml1.getGtTheDen());
        record.setNgayMienCct(xml1.getNgayMienCct());
        record.setLyDoVv(xml1.getLyDoVv());
        record.setLyDoVnt(xml1.getLyDoVnt());
        record.setMaLyDoVnt(xml1.getMaLyDoVnt());
        record.setChanDoanVao(xml1.getChanDoanVao());
        record.setChanDoanRv(xml1.getChanDoanRv());
        record.setMaBenhChinh(xml1.getMaBenhChinh());
        record.setMaBenhKt(xml1.getMaBenhKt());
        record.setMaBenhYhct(xml1.getMaBenhYhct());
        record.setMaPtttQt(xml1.getMaPtttQt());
        record.setMaDoiTuongKcb(xml1.getMaDoiTuongKcb());
        record.setMaNoiDi(xml1.getMaNoiDi());
        record.setMaNoiDen(xml1.getMaNoiDen());
        record.setMaTaiNan(xml1.getMaTaiNan());
        record.setNgayVao(xml1.getNgayVao());
        record.setNgayVaoNoiTru(xml1.getNgayVaoNoiTru());
        record.setNgayRa(xml1.getNgayRa());
        record.setGiayChuyenTuyen(xml1.getGiayChuyenTuyen());
        record.setSoNgayDtri(xml1.getSoNgayDtri());
        record.setPpDieuTri(xml1.getPpDieuTri());
        record.setKetQuaDtri(xml1.getKetQuaDtri());
        record.setMaLoaiRv(xml1.getMaLoaiRv());
        record.setGhiChu(xml1.getGhiChu());
        record.setNgayTtoan(xml1.getNgayTtoan());
        record.setTThuoc(xml1.getTThuoc());
        record.setTVtyt(xml1.getTVtyt());
        record.setTTongchiBv(xml1.getTTongchiBv());
        record.setTTongchiBh(xml1.getTTongchiBh());
        record.setTBntt(xml1.getTBntt());
        record.setTBncct(xml1.getTBncct());
        record.setTBhtt(xml1.getTBhtt());
        record.setTNguonkhac(xml1.getTNguonkhac());
        record.setTBhttGdv(xml1.getTBhttGdv());
        record.setNamQt(xml1.getNamQt());
        record.setThangQt(xml1.getThangQt());
        record.setMaLoaiKcb(xml1.getMaLoaiKcb());
        record.setMaKhoa(xml1.getMaKhoa());
        record.setMaCskcb(xml1.getMaCskcb());
        record.setMaKhuvuc(xml1.getMaKhuvuc());
        record.setCanNang(xml1.getCanNang());
        record.setCanNangCon(xml1.getCanNangCon());
        record.setNamNamLienTuc(xml1.getNamNamLienTuc());
        record.setNgayTaiKham(xml1.getNgayTaiKham());
        record.setMaHsba(xml1.getMaHsba());
        record.setMaTtdv(xml1.getMaTtdv());
        record.setDuPhongXml1(xml1.getDuPhong());

        // --- Map XML4 ---
        record.setSttXml4(sttXml4);
        if (xml4 != null) {
            record.setMaDichVu(xml4.getMaDichVu());
            record.setMaChiSo(xml4.getMaChiSo());
            record.setTenChiSo(xml4.getTenChiSo());
            record.setGiaTri(xml4.getGiaTri());
            record.setDonViDo(xml4.getDonViDo());
            record.setMoTa(xml4.getMoTa());
            record.setKetLuan(xml4.getKetLuan());
            record.setNgayKq(xml4.getNgayKq());
            record.setMaBsDocKq(xml4.getMaBsDocKq());
            record.setDuPhongXml4(xml4.getDuPhong());
        }

        return record;
    }

    private void sendToMockBhxhServer(String appointmentId, CombinedXmlResponse payload) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CombinedXmlResponse> request = new HttpEntity<>(payload, headers);

        try {
            log.info("[BhytSync] Sending to Mock BHXH Server: {}", mockBhxhUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(mockBhxhUrl, request, String.class);

            String status = response.getStatusCode().is2xxSuccessful() ? BHYT_SYNC_STATUS_SUCCESS : BHYT_SYNC_STATUS_FAILED;
            String errorMsg = response.getStatusCode().is2xxSuccessful() ? null
                    : "Mock Server returned: " + response.getStatusCode();

            updateAppointmentSyncStatus(appointmentId, status, errorMsg);
            log.info("[BhytSync] {} - Updated paid invoices for appointment {}", status, appointmentId);
        } catch (Exception e) {
            String errDetail = "Connection Timeout: Không thể kết nối tới giả lập BHYT. " + e.getMessage();
            log.error("[BhytSync] FAILED - {}", errDetail);
            updateAppointmentSyncStatus(appointmentId, BHYT_SYNC_STATUS_FAILED, errDetail);
        }
    }
}
