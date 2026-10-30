package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import g2.be.util.ClinicTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Bảng gán nhân viên (Doctor/Nurse) vào ca làm việc theo ngày cụ thể.
 * Liên kết bảng users với bảng working_time_slots.
 * 
 * Ví dụ: Bác sĩ Nguyễn Văn A làm ca sáng (07:00-11:30) ngày 05/03/2026
 * 
 * @author Nam
 */
@Entity
@Table(name = "staff_schedules", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "slot_id",
                "work_date" }, name = "uk_staff_schedule_user_slot_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    // Nhân viên được gán (Doctor hoặc Nurse)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Ca làm việc
    @ManyToOne
    @JoinColumn(name = "slot_id", nullable = false)
    private WorkingTimeSlot slot;

    // Ngày làm việc cụ thể
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    // Phòng được gán trong ca trực (nullable — bản ghi cũ có thể chưa có)
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    /**
     * Cột legacy trong một số DB cũ vẫn để NOT NULL.
     * Giữ mapping để tương thích ngược khi ghi dữ liệu lịch.
     */
    @Column(name = "date")
    private LocalDate legacyDate;

    /**
     * Cột legacy trong một số DB cũ vẫn yêu cầu start_time/end_time trực tiếp
     * trên bảng staff_schedules.
     */
    @Column(name = "start_time")
    private LocalTime legacyStartTime;

    @Column(name = "end_time")
    private LocalTime legacyEndTime;

    /**
     * Cột legacy trong một số DB cũ yêu cầu dept_id không null.
     */
    @Column(name = "dept_id")
    private Integer legacyDeptId;

    // Ghi chú
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (workDate == null && legacyDate != null) {
            workDate = legacyDate;
        }
        if (legacyDate == null && workDate != null) {
            legacyDate = workDate;
        }
        if (slot != null) {
            if (legacyStartTime == null) {
                legacyStartTime = slot.getStartTime();
            }
            if (legacyEndTime == null) {
                legacyEndTime = slot.getEndTime();
            }
        }
        if (legacyDeptId == null && user != null) {
            if (user.getDoctor() != null && user.getDoctor().getDepartment() != null) {
                legacyDeptId = user.getDoctor().getDepartment().getDeptId();
            } else if (user.getNurse() != null && user.getNurse().getDepartment() != null) {
                legacyDeptId = user.getNurse().getDepartment().getDeptId();
            }
        }
        createdAt = ClinicTime.now();
        updatedAt = ClinicTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (workDate == null && legacyDate != null) {
            workDate = legacyDate;
        }
        if (legacyDate == null && workDate != null) {
            legacyDate = workDate;
        }
        if (slot != null) {
            if (legacyStartTime == null) {
                legacyStartTime = slot.getStartTime();
            }
            if (legacyEndTime == null) {
                legacyEndTime = slot.getEndTime();
            }
        }
        if (legacyDeptId == null && user != null) {
            if (user.getDoctor() != null && user.getDoctor().getDepartment() != null) {
                legacyDeptId = user.getDoctor().getDepartment().getDeptId();
            } else if (user.getNurse() != null && user.getNurse().getDepartment() != null) {
                legacyDeptId = user.getNurse().getDepartment().getDeptId();
            }
        }
        updatedAt = ClinicTime.now();
    }
}
