package g2.be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

/**
 * Request DTO để gán nhân viên vào ca làm việc.
 * 
 * @author Nam
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateStaffScheduleRequest {

    @NotNull(message = "userId không được để trống")
    private Long userId;

    @NotNull(message = "slotId không được để trống")
    private Long slotId;

    @NotNull(message = "workDate không được để trống")
    private LocalDate workDate;

    private String roomId;

    private String note;
}
