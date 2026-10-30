package g2.be.entity;

import g2.be.util.ClinicTime;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ hồ sơ BHYT đã gửi lên cơ quan BHXH
 * Mục đích: Lưu vết, có bằng chứng đã gửi và đối soát sau này
 */
@Entity
@Table(name = "insurance_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã hồ sơ (UUID hoặc mã tự generate)
     */
    @Column(name = "submission_code", unique = true, nullable = false)
    private String submissionCode;

    /**
     * Mã phiếu thanh toán trong hệ thống (ServiceOrder ID)
     */
    @Column(name = "service_order_id")
    private Long serviceOrderId;

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
     * Tổng chi phí
     */
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /**
     * BHYT thanh toán
     */
    @Column(name = "insurance_paid", precision = 15, scale = 2)
    private BigDecimal insurancePaid;

    /**
     * Bệnh nhân thanh toán
     */
    @Column(name = "patient_paid", precision = 15, scale = 2)
    private BigDecimal patientPaid;

    /**
     * Đường dẫn file XML hoặc JSON
     * VD: /storage/insurance/submissions/2026-03-03/SUBM_20260303_001.xml
     */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /**
     * Nội dung XML/JSON (nếu lưu trực tiếp vào DB)
     */
    @Column(name = "xml_content", columnDefinition = "LONGTEXT")
    private String xmlContent;

    /**
     * Trạng thái gửi
     * - PENDING: Chưa gửi
     * - SENT: Đã gửi
     * - ACCEPTED: BHXH chấp nhận
     * - REJECTED: BHXH từ chối
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * Thông báo phản hồi từ BHXH
     */
    @Column(name = "response_message", length = 1000)
    private String responseMessage;

    /**
     * Ngày giờ tạo hồ sơ
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Ngày giờ gửi lên BHXH
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /**
     * Ngày giờ nhận phản hồi từ BHXH
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}
