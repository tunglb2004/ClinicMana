package g2.be.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO cho lịch làm việc nhân viên.
 * Hiển thị trong danh sách lịch.
 * 
 * @author Nam
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffScheduleResponse {

    private Long scheduleId;

    // Thông tin nhân viên
    private Long userId;
    private String fullName;
    private String staffId;
    private String role; // DOCTOR hoặc NURSE

    // Thông tin ca làm việc
    private Long slotId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;

    // Ngày làm việc
    private LocalDate workDate;

    // Phòng
    private String roomId;
    private String roomName;

    // Ghi chú
    private String note;

    // Thông tin khoa
    private String deptName;
}
