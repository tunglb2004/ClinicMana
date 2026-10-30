package g2.be.dto.response;

import lombok.*;
import java.time.LocalDate;

/**
 * Response DTO cho quan hệ y tá theo dõi bác sĩ.
 * 
 * @author Nam
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseDoctorAssignmentResponse {

    private Long assignmentId;

    // Thông tin y tá
    private Long nurseUserId;
    private String nurseFullName;
    private String nurseStaffId;

    // Thông tin bác sĩ
    private Long doctorUserId;
    private String doctorFullName;
    private String doctorStaffId;
    private String doctorSpecialization;

    // Thời gian hiệu lực
    private LocalDate effectiveDate;
    private LocalDate endDate;
    private Boolean isActive;
}
