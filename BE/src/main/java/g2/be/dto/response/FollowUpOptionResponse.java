package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpOptionResponse {
    private String recordId;
    private LocalDateTime examinationDate;
    private String status;
    private String visitType;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String departmentName;
    private String clinicalServiceId;
    private String clinicalServiceName;
    private String diagnosisSummary;
}
