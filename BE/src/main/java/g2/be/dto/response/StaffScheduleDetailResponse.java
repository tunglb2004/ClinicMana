package g2.be.dto.response;

import lombok.*;
import java.util.List;

/**
 * Response DTO chi tiết lịch làm việc của 1 nhân viên.
 * Bao gồm thông tin cá nhân + danh sách các ca trong khoảng ngày.
 * 
 * @author Nam
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffScheduleDetailResponse {

    // Thông tin nhân viên
    private Long userId;
    private String fullName;
    private String staffId;
    private String role; // DOCTOR hoặc NURSE
    private String deptName;

    // Danh sách ca làm việc
    private List<StaffScheduleResponse> schedules;
}
