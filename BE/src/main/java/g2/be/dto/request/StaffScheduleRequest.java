package g2.be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffScheduleRequest {
    private Long userId;
    private Integer deptId;
    private String date; // YYYY-MM-DD
    private String startTime; // HH:mm
    private String endTime; // HH:mm
    private String status; // PRESENT, ABSENT, SCHEDULED
    private String note;
}
