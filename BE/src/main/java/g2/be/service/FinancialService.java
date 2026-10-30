package g2.be.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import g2.be.entity.Appointment;
import g2.be.repository.AppointmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g2.be.dto.response.FinancialDetailResponse;
import g2.be.dto.response.FinancialResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.ReceiptResponse;
import g2.be.entity.Invoice;
import g2.be.entity.ServiceOrder;
import g2.be.entity.StaffSchedule;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialService {

    private final InvoiceRepository invoiceRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final StaffScheduleRepository staffScheduleRepository;
        private final ReceiptIssuanceService receiptIssuanceService;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Danh sách hóa đơn (có tìm kiếm, lọc theo trạng thái, phân trang)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getInvoices(int page, int size, String keyword, String paymentStatus) {
        return getInvoices(page, size, keyword, paymentStatus, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getInvoices(int page, int size, String keyword, String paymentStatus, String cashierId) {
        return getInvoices(page, size, keyword, paymentStatus, cashierId, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getInvoices(int page, int size, String keyword, String paymentStatus, String cashierId, String cashierStaffId) {
        return getInvoices(page, size, keyword, paymentStatus, cashierId, cashierStaffId, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getInvoices(int page, int size, String keyword, String paymentStatus, String cashierId, String cashierStaffId, boolean todayOnly) {
        return getInvoices(page, size, keyword, paymentStatus, cashierId, cashierStaffId, todayOnly, 0);
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getInvoices(int page, int size, String keyword, String paymentStatus,
            String cashierId, String cashierStaffId, boolean todayOnly, int recentDays) {
        Pageable pageable = PageRequest.of(page, size);
        String searchKeyword = normalizeKeyword(keyword);
        int days = todayOnly ? 1 : Math.max(recentDays, 0);
        LocalDateTime from = days > 0 ? ClinicTime.today().minusDays(days - 1L).atStartOfDay() : null;
        LocalDateTime to = days > 0 ? ClinicTime.today().plusDays(1).atStartOfDay() : null;
        Page<Invoice> invoicePage = invoiceRepository.findByFilters(searchKeyword, paymentStatus, cashierId, cashierStaffId, from, to, pageable);

        return PageResponse.<FinancialResponse>builder()
                .content(invoicePage.getContent().stream()
                        .map(invoice -> {
                            FinancialResponse resp = FinancialResponse.fromEntity(invoice);
                                                        List<ServiceOrder> orders = getActiveOrdersForInvoice(invoice);
                            if (orders != null && !orders.isEmpty()) {
                                String summary = orders.stream()
                                        .map(o -> o.getService().getServiceName())
                                        .distinct()
                                        .collect(java.util.stream.Collectors.joining(", "));
                                resp.setServiceSummary(summary);
                            }
                            return resp;
                        })
                        .toList())
                .pageNumber(invoicePage.getNumber() + 1)
                .pageSize(invoicePage.getSize())
                .totalElements(invoicePage.getTotalElements())
                .totalPages(invoicePage.getTotalPages())
                .last(invoicePage.isLast())
                .first(invoicePage.isFirst())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1b. Polling trạng thái hóa đơn (cho FE check sau khi hiển thị QR)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public java.util.Map<String, String> getInvoiceStatus(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));
        return java.util.Map.of(
                "invoiceId", invoice.getInvoiceId(),
                "paymentStatus", invoice.getPaymentStatus()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Chi tiết hóa đơn (thông tin bệnh nhân, lịch hẹn, dịch vụ, thanh toán)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public FinancialDetailResponse getInvoiceDetail(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

                List<ServiceOrder> serviceOrders = getActiveOrdersForInvoice(invoice);

        return FinancialDetailResponse.fromEntity(invoice, serviceOrders);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Xuất phiếu thu (Issue Receipt) — sinh mã phiếu thu + link hóa đơn điện tử
    // ─────────────────────────────────────────────────────────────────────────
    public ReceiptResponse issueReceipt(String invoiceId) {
                return receiptIssuanceService.issueReceiptNow(invoiceId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Xem chi tiết phiếu thu (Receipt detail)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptDetail(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

                List<ServiceOrder> serviceOrders = getActiveOrdersForInvoice(invoice);

        return ReceiptResponse.fromEntity(invoice, serviceOrders);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Tổng quan tài chính
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getFinancialSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", invoiceRepository.getTotalRevenue());
        summary.put("totalInsurancePaid", invoiceRepository.getTotalInsurancePaid());
        summary.put("totalInvoices", invoiceRepository.count());
        summary.put("paidCount", invoiceRepository.countByPaymentStatus("PAID"));
        summary.put("unpaidCount", invoiceRepository.countByPaymentStatus("UNPAID"));
        summary.put("pendingCount", invoiceRepository.countByPaymentStatus("PENDING"));
        return summary;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCashierDashboardSummary(String cashierId, String cashierStaffId) {
        LocalDate today = ClinicTime.today();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        boolean hasCashierScope = cashierId != null && !cashierId.isBlank();

        Map<String, Object> summary = new HashMap<>();
        summary.put("pendingInitial", safeLong(appointmentRepository.countUnpaidAppointments(null, today)));
        summary.put("pendingSupplement", safeLong(serviceOrderRepository.countSupplementPendingRecords(null, today)));
        summary.put("paidToday", hasCashierScope
                ? safeLong(invoiceRepository.countPaidByCashierBetween(cashierId, cashierStaffId, from, to))
                : safeLong(invoiceRepository.countPaidBetween(from, to)));
        summary.put("revenueToday", hasCashierScope
                ? safeAmount(invoiceRepository.sumPaidCustomerAmountByCashierBetween(cashierId, cashierStaffId, from, to))
                : safeAmount(invoiceRepository.sumPaidCustomerAmountBetween(from, to)));
        return summary;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Danh sách lượt khám chưa thanh toán
    //    Query các Appointment có services mà chưa có Invoice PAID.
    //    invoiceId = appointmentId (dùng để navigate đến trang thanh toán)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getUnpaidRecords(int page, int size, String keyword) {
        return getUnpaidRecords(page, size, keyword, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getUnpaidRecords(int page, int size, String keyword, boolean todayOnly) {
        Pageable pageable = PageRequest.of(page, size);
        String searchKeyword = normalizeKeyword(keyword);
        LocalDate appointmentDate = todayOnly ? ClinicTime.today() : null;
        Page<Appointment> resultPage = appointmentRepository.findUnpaidAppointments(searchKeyword, appointmentDate, pageable);

        List<FinancialResponse> mapped = resultPage.getContent().stream()
                .map(a -> {
                    BigDecimal totalServiceFee = a.getServices().stream()
                            .map(g2.be.entity.Service::getPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    String patientId = a.getPatient() != null ? a.getPatient().getPatientId() : null;
                    String patientName = (a.getPatient() != null && a.getPatient().getUser() != null)
                            ? a.getPatient().getUser().getFullName() : null;
                    String healthInsurance = a.getPatient() != null ? a.getPatient().getHealthInsuranceNumber() : null;
                    String deptName = (a.getDoctor() != null && a.getDoctor().getDepartment() != null)
                            ? a.getDoctor().getDepartment().getDeptName() : null;

                    String serviceSummary = a.getServices().stream()
                            .map(g2.be.entity.Service::getServiceName)
                            .filter(java.util.Objects::nonNull)
                            .sorted()
                            .distinct()
                            .collect(java.util.stream.Collectors.joining(", "));

                    return FinancialResponse.builder()
                            .invoiceId(a.getAppointmentId())
                            .appointmentId(a.getAppointmentId())
                            .patientId(patientId)
                            .patientName(patientName)
                            .healthInsuranceNumber(healthInsurance)
                            .departmentName(deptName)
                            .totalServiceFee(totalServiceFee)
                            .paymentStatus("PENDING")
                            .receiptStatus("NONE")
                            .createdAt(a.getCreatedAt())
                            .serviceSummary(serviceSummary)
                            .build();
                })
                .toList();

        return PageResponse.<FinancialResponse>builder()
                .content(mapped)
                .pageNumber(resultPage.getNumber() + 1)
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .last(resultPage.isLast())
                .first(resultPage.isFirst())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Danh sách lượt khám có chỉ định bổ sung chưa thanh toán
    //    GROUP BY medical_record → SUM(SUPPLEMENT service price)
    //    Response có recordId để FE truyền vào POST /payments/supplement-payment
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getSupplementPendingRecords(int page, int size, String keyword) {
        return getSupplementPendingRecords(page, size, keyword, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getSupplementPendingRecords(int page, int size, String keyword, boolean todayOnly) {
        Pageable pageable = PageRequest.of(page, size);
        String searchKeyword = normalizeKeyword(keyword);
        LocalDate queryDate = todayOnly ? ClinicTime.today() : null;
        Page<g2.be.dto.response.UnpaidRecordProjection> resultPage =
                serviceOrderRepository.findSupplementPendingRecords(searchKeyword, queryDate, pageable);

        List<FinancialResponse> mapped = resultPage.getContent().stream()
                .map(r -> FinancialResponse.builder()
                        .recordId(r.getRecordId())
                        .appointmentId(r.getAppointmentId())
                        .invoiceId(null)
                        .patientId(r.getPatientId())
                        .patientName(r.getPatientName())
                        .healthInsuranceNumber(r.getHealthInsuranceNumber())
                        .departmentName(r.getDepartmentName())
                        .totalServiceFee(r.getTotalServiceFee())
                        .paymentStatus("PENDING")
                        .receiptStatus("NONE")
                        .createdAt(r.getCreatedAt())
                        .serviceSummary(r.getServiceSummary())
                        .build())
                .toList();

        return PageResponse.<FinancialResponse>builder()
                .content(mapped)
                .pageNumber(resultPage.getNumber() + 1)
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .last(resultPage.isLast())
                .first(resultPage.isFirst())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CUSTOMER-FACING METHODS (chỉ xem hóa đơn của chính mình)
    // ═══════════════════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────────────────
    // C1. Danh sách hóa đơn của customer (theo userId đang đăng nhập)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<FinancialResponse> getMyInvoices(Long userId, String paymentStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Invoice> invoicePage = invoiceRepository.findByPatientUserId(userId, paymentStatus, pageable);

        return PageResponse.<FinancialResponse>builder()
                .content(invoicePage.getContent().stream()
                        .map(FinancialResponse::fromEntity)
                        .toList())
                .pageNumber(invoicePage.getNumber() + 1)
                .pageSize(invoicePage.getSize())
                .totalElements(invoicePage.getTotalElements())
                .totalPages(invoicePage.getTotalPages())
                .last(invoicePage.isLast())
                .first(invoicePage.isFirst())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // C2. Chi tiết hóa đơn & phiếu thu (customer chỉ xem của mình)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ReceiptResponse getMyInvoiceDetail(Long userId, String invoiceId) {
        // Kiểm tra hóa đơn có thuộc về user đang đăng nhập không
        if (!invoiceRepository.existsByInvoiceIdAndPatientUserId(invoiceId, userId)) {
            throw new AppException(ErrorCode.INVOICE_ACCESS_DENIED);
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

                List<ServiceOrder> serviceOrders = getActiveOrdersForInvoice(invoice);

        return ReceiptResponse.fromEntity(invoice, serviceOrders);
    }

        private List<ServiceOrder> getActiveOrdersForInvoice(Invoice invoice) {
                if (invoice == null) {
                        return List.of();
                }

                if (invoice.getInvoiceId() != null && !invoice.getInvoiceId().isBlank()) {
                        return serviceOrderRepository.findByInvoiceId(invoice.getInvoiceId()).stream()
                                        .filter(order -> !"CANCELLED".equals(order.getStatus()))
                                        .sorted(Comparator.comparing(ServiceOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                                        .toList();
                }

                if (invoice.getMedicalRecord() == null || invoice.getMedicalRecord().getRecordId() == null) {
                        return List.of();
                }

                Set<String> paidInvoiceIds = invoiceRepository.findByMedicalRecord_RecordId(invoice.getMedicalRecord().getRecordId()).stream()
                                .filter(relatedInvoice -> "PAID".equals(relatedInvoice.getPaymentStatus()))
                                .map(Invoice::getInvoiceId)
                                .filter(Objects::nonNull)
                                .collect(java.util.stream.Collectors.toSet());

                if (paidInvoiceIds.isEmpty()) {
                        return List.of();
                }

                return serviceOrderRepository.findByMedicalRecord_RecordId(invoice.getMedicalRecord().getRecordId()).stream()
                                .filter(order -> !"CANCELLED".equals(order.getStatus()))
                                .filter(order -> order.getInvoiceId() != null && paidInvoiceIds.contains(order.getInvoiceId()))
                                .sorted(Comparator.comparing(ServiceOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList();
        }

    // ─────────────────────────────────────────────────────────────────────────
    // C3. Xuất phiếu thu (customer yêu cầu xuất cho hóa đơn của mình)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public ReceiptResponse issueMyReceipt(Long userId, String invoiceId) {
        // Kiểm tra hóa đơn có thuộc về user đang đăng nhập không
        if (!invoiceRepository.existsByInvoiceIdAndPatientUserId(invoiceId, userId)) {
            throw new AppException(ErrorCode.INVOICE_ACCESS_DENIED);
        }

        // Delegate sang logic xuất phiếu thu chung
        return issueReceipt(invoiceId);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

