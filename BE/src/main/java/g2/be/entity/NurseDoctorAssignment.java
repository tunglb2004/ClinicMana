package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import g2.be.util.ClinicTime;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bảng gán y tá theo dõi bác sĩ.
 * Mỗi y tá có thể theo dõi 1 hoặc nhiều bác sĩ.
 * 
 * Ví dụ: Y tá Trần Thị B theo dõi Bác sĩ Nguyễn Văn A từ 01/03/2026
 * 
 * @author Nam
 */
@Entity
@Table(name = "nurse_doctor_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "nurse_id", "doctor_id",
                "effective_date" }, name = "uk_nurse_doctor_effective")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseDoctorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    // Y tá được gán
    @ManyToOne
    @JoinColumn(name = "nurse_id", nullable = false, columnDefinition = "varchar(20)")
    private Nurse nurse;

    // Bác sĩ mà y tá theo dõi
    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false, columnDefinition = "varchar(20)")
    private Doctor doctor;

    // Ngày bắt đầu hiệu lực
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    // Ngày kết thúc (null = vô thời hạn)
    @Column(name = "end_date")
    private LocalDate endDate;

    // Trạng thái
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
        updatedAt = ClinicTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ClinicTime.now();
    }
}
