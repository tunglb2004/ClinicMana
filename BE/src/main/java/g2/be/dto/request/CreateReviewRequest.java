package g2.be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequest {
    private String patientId;
    private String doctorId;
    private String appointmentId;
    private String serviceId;
    private Integer rating;
    private String comment;
}
