package g2.be.dto.request;

import lombok.*;
import java.time.LocalDate;

/**
 * Request DTO để cập nhật lịch làm việc nhân viên.
 * 
 * @author Nam
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStaffScheduleRequest {

    private Long userId;

    private Long slotId;

    private LocalDate workDate;

    private String roomId;

    private String note;
}
