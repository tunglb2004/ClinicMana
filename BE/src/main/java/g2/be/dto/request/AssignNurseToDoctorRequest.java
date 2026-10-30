package g2.be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

/**
 * Request DTO để gán y tá theo dõi bác sĩ.
 * 
 * @author Nam
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignNurseToDoctorRequest {

    @NotNull(message = "nurseUserId không được để trống")
    private Long nurseUserId;

    @NotNull(message = "doctorUserId không được để trống")
    private Long doctorUserId;

    private LocalDate effectiveDate;

    private LocalDate endDate;
}
