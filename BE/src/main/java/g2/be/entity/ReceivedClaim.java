package g2.be.entity;

import g2.be.util.ClinicTime;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ hồ sơ BHYT nhận được từ các phòng khám (Mock BHXH Server)
 * Mục đích: Mock server BHXH lưu trữ để đối soát và xử lý
 */
@Entity
@Table(name = "received_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivedClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã hồ sơ từ phòng khám gửi lên
     */
    @Column(name = "submission_code", unique = true, nullable = false)
    private String submissionCode;

    /**
     * Mã phòng khám gửi hồ sơ (MA_CSKCB)
     */
    @Column(name = "clinic_code", length = 5)
    private String clinicCode;

    /**
     * Số CCCD bệnh nhân
     */
    @Column(name = "cccd_number", length = 12)
    private String cccdNumber;

    /**
     * Mã thẻ BHYT
     */
    @Column(name = "insurance_code", length = 15)
    private String insuranceCode;

    /**
     * Họ tên bệnh nhân
     */
    @Column(name = "patient_name")
    private String patientName;

    /**
     * Mã đăng ký ban đầu của bệnh nhân
     */
    @Column(name = "ma_dkbd", length = 5)
    private String maDkbd;

    /**
     * Đúng tuyến hay không
     */
    @Column(name = "is_dung_tuyen")
    private Boolean isDungTuyen;

    /**
     * Tổng chi phí phòng khám yêu cầu
     */
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Số tiền BHYT yêu cầu thanh toán
     */
    @Column(name = "insurance_claim_amount", precision = 15, scale = 2)
    private BigDecimal insuranceClaimAmount;

    /**
     * Số tiền BHYT đã duyệt thanh toán
     */
    @Column(name = "insurance_approved_amount", precision = 15, scale = 2)
    private BigDecimal insuranceApprovedAmount;

    /**
     * Bệnh nhân đã thanh toán
     */
    @Column(name = "patient_paid", precision = 15, scale = 2)
    private BigDecimal patientPaid;

    /**
     * Đường dẫn file XML/JSON nhận được
     */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /**
     * Nội dung XML/JSON nhận được
     */
    @Column(name = "xml_content", columnDefinition = "LONGTEXT")
    private String xmlContent;

    /**
     * Trạng thái xử lý
     * - RECEIVED: Đã nhận
     * - PROCESSING: Đang xử lý
     * - APPROVED: Đã duyệt
     * - REJECTED: Từ chối
     * - PAID: Đã thanh toán
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * Lý do từ chối (nếu có)
     */
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    /**
     * Ghi chú của BHXH
     */
    @Column(name = "notes", length = 2000)
    private String notes;

    /**
     * Ngày giờ nhận hồ sơ
     */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /**
     * Ngày giờ xử lý xong
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Ngày giờ thanh toán
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        receivedAt = ClinicTime.now();
        if (status == null) {
            status = "RECEIVED";
        }
    }
}
