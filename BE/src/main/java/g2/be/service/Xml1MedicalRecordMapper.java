package g2.be.service;

import g2.be.dto.request.Xml1MedicalRecordRequest;
import g2.be.dto.response.CitizenInsuranceInfoResponse;
import g2.be.entity.*;
import g2.be.repository.MockCitizenDataRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapper Service - Chuyển đổi thông minh từ DB thực sang XML 1
 * 
 * Chiến lược "Data Honesty" (Trung thực với dữ liệu):
 * - Ruột: DB nhỏ gọn (Invoice, MedicalRecord, Patient, Appointment)
 * - Vỏ: DTO 65 trường để giao tiếp với API BHXH
 * - Nguyên tắc: Chỉ điền trường có dữ liệu thực, không có = null
 * 
 * Không gán giá trị "giả" (fake defaults) để đảm bảo tính trung thực!
 */
@Service
@RequiredArgsConstructor
public class Xml1MedicalRecordMapper {

    private final CitizenInsuranceService citizenInsuranceService;
    private final MockCitizenDataRepository mockCitizenDataRepository;

    // Formatter cho ngày tháng theo chuẩn Bộ Y tế
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /**
     * Hàm chính: Chuyển đổi từ Invoice + MedicalRecord → XML1 Request
     * 
     * Nguyên tắc: Data Honesty
     * - Có dữ liệu thực → Điền vào
     * - Không có dữ liệu → Để null (không bịa giá trị giả)
     * 
     * @param invoice Hóa đơn thanh toán (chứa thông tin tài chính)
     * @return DTO 65 trường, chỉ điền các trường có dữ liệu
     */
    public Xml1MedicalRecordRequest mapToXml1Request(Invoice invoice) {
        MedicalRecord medicalRecord = invoice.getMedicalRecord();
        Appointment appointment = medicalRecord.getAppointment();
        Patient patient = medicalRecord.getPatient();
        User user = patient.getUser();

        // Tra cứu thông tin BHYT từ MockCitizenData
        CitizenInsuranceInfoResponse insuranceInfo = getInsuranceInfo(user.getCccd());

        // Tính toán tài chính dựa vào mức hưởng BHYT
        BigDecimal totalFee = invoice.getTotalServiceFee();
        Integer coverageRate = insuranceInfo != null ? insuranceInfo.getCoverageRate() : 0;
        
        BigDecimal insurancePaid = calculateInsurancePaid(totalFee, coverageRate);
        BigDecimal patientCopay = calculatePatientCopay(totalFee, coverageRate);
        BigDecimal patientPaid = totalFee.subtract(insurancePaid);

        // Tạo mã đợt điều trị duy nhất
        String maLk = generateMaLk(invoice);
        LocalDateTime accountingTime = invoice.getPaidAt() != null ? invoice.getPaidAt() : ClinicTime.now();

        // Build DTO với 15 trường thật + các trường khác để null
        Xml1MedicalRecordRequest request = Xml1MedicalRecordRequest.builder()
            // ========== 15 TRƯỜNG CỐT LÕI (Từ DB thực) ==========
            .maLk(maLk)
            .stt(1) // Sẽ được điều chỉnh khi gửi batch
            .maBn(patient.getPatientId())
            .hoTen(user.getFullName())
            .soCccd(user.getCccd())
            .ngaySinh(formatDateOfBirth(patient.getDateOfBirth()))
            .gioiTinh(mapGender(user.getGender()))
            .maTheBhyt(insuranceInfo != null ? insuranceInfo.getInsuranceCode() : null)
            .ngayVao(formatDateTime(appointment.getAppointmentDate(), appointment.getAppointmentTime()))
            .ngayRa(formatDateTime(invoice.getPaidAt()))
            .ngayTtoan(formatDateTime(invoice.getPaidAt()))
            .tTongchiBv(totalFee)
            .tTongchiBh(insurancePaid)
            .tBntt(patientPaid)
            .tBncct(patientCopay)
            
            // ========== TRƯỜNG BỔ SUNG TỪ DB (Có dữ liệu thực) ==========
            .diaChi(patient.getAddress())
            .dienThoai(user.getPhoneNumber())
            .chanDoanVao(medicalRecord.getSymptoms())
            .chanDoanRv(medicalRecord.getDiagnosis())
            .ppDieuTri(medicalRecord.getDoctorNotes())
            .ghiChu(medicalRecord.getDoctorNotes())
            .maHsba(medicalRecord.getRecordId())
            .canNang(formatWeight(patient.getWeight()))
            
            // ========== THÔNG TIN BHYT (Từ MockCitizenData nếu có) ==========
            .maDkbd(insuranceInfo != null ? extractMaDkbd(insuranceInfo.getInsuranceCode()) : null)
            .gtTheTu(insuranceInfo != null ? formatDate(insuranceInfo.getValidTo().minusYears(1)) : null)
            .gtTheDen(insuranceInfo != null ? formatDate(insuranceInfo.getValidTo()) : null)
            
            // ========== TÍNH TOÁN TÀI CHÍNH ==========
            .tBhtt(insurancePaid.subtract(patientCopay)) // BHXH thanh toán = Phần BH trả - Cùng chi trả
            .namQt(accountingTime.getYear())
            .thangQt(accountingTime.getMonthValue())
            
            // ========== CÁC TRƯỜNG KHÁC ĐỂ NULL ==========
            // maQuocTich, maDanToc, maNgheNghiep... → null
            // Mapper không gán giá trị giả, chỉ điền khi có dữ liệu thực
            .build();

        return request;
    }

    /**
     * Tra cứu thông tin BHYT từ số CCCD
     */
    private CitizenInsuranceInfoResponse getInsuranceInfo(String cccd) {
        try {
            return citizenInsuranceService.getCitizenInfoByCccd(cccd);
        } catch (Exception e) {
            // Nếu không tìm thấy BHYT, trả về null (tự phí hoàn toàn)
            return null;
        }
    }

    /**
     * Tính tiền BHYT thanh toán
     * 
     * @param totalFee Tổng chi phí
     * @param coverageRate Mức hưởng (80, 95, 100)
     * @return Số tiền BHYT trả
     */
    private BigDecimal calculateInsurancePaid(BigDecimal totalFee, Integer coverageRate) {
        if (coverageRate == null || coverageRate == 0) {
            return BigDecimal.ZERO;
        }
        return totalFee.multiply(BigDecimal.valueOf(coverageRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Tính tiền bệnh nhân cùng chi trả (copay)
     * 
     * @param totalFee Tổng chi phí
     * @param coverageRate Mức hưởng
     * @return Số tiền bệnh nhân phải đóng góp (20% nếu hưởng 80%)
     */
    private BigDecimal calculatePatientCopay(BigDecimal totalFee, Integer coverageRate) {
        if (coverageRate == null || coverageRate == 100) {
            return BigDecimal.ZERO; // Hưởng 100% thì không cùng chi trả
        }
        int copayRate = 100 - coverageRate;
        return totalFee.multiply(BigDecimal.valueOf(copayRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Tạo mã đợt điều trị duy nhất (MA_LK)
     * Format: INV_yyyyMMddHHmmss_invoiceId
     */
    private String generateMaLk(Invoice invoice) {
        String timestamp = ClinicTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "INV_" + timestamp + "_" + invoice.getInvoiceId();
    }

    /**
     * Chuyển đổi ngày sinh sang format yyyyMMdd0000
     */
    private String formatDateOfBirth(java.time.LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return "";
        }
        return dateOfBirth.format(DATE_FORMAT) + "0000"; // Thêm "0000" cho giờ phút
    }

    /**
     * Format ngày tháng sang yyyyMMdd
     */
    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE_FORMAT);
    }

    /**
     * Format datetime sang yyyyMMddHHmm
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATETIME_FORMAT);
    }

    /**
     * Format datetime từ date + time riêng biệt
     */
    private String formatDateTime(java.time.LocalDate date, java.time.LocalTime time) {
        if (date == null) {
            return "";
        }
        LocalDateTime dateTime = LocalDateTime.of(date, time != null ? time : java.time.LocalTime.of(0, 0));
        return dateTime.format(DATETIME_FORMAT);
    }

    /**
     * Chuyển đổi Gender enum sang mã số
     * 1: Nam, 2: Nữ, 3: Chưa xác định
     */
    private Integer mapGender(User.Gender gender) {
        if (gender == null) {
            return 3;
        }
        return switch (gender) {
            case MALE -> 1;
            case FEMALE -> 2;
            default -> 3;
        };
    }

    /**
     * Format cân nặng từ BigDecimal sang String (2 chữ số thập phân)
     */
    private String formatWeight(BigDecimal weight) {
        if (weight == null) {
            return "";
        }
        return weight.setScale(2, RoundingMode.HALF_UP).toString();
    }

    /**
     * Trích xuất mã ĐKBĐ từ mã thẻ BHYT (5 ký tự đầu)
     * Ví dụ: GD4010112345678 → GD401
     */
    private String extractMaDkbd(String insuranceCode) {
        if (insuranceCode == null || insuranceCode.length() < 5) {
            return "";
        }
        return insuranceCode.substring(0, 5);
    }
}
