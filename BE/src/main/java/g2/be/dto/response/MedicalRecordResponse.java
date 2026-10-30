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
public class MedicalRecordResponse {
    private String recordId;
    private LocalDateTime examinationDate; // Mapping from createdAt
    private String patientId;
    private String patientName;
    private String doctorName;
    private String diagnosisSummary; // Mapping from diagnosis
    private String status;
    private String visitType;
    private String parentRecordId;
    private String rootRecordId;
    private Boolean canFollowUp;
    private String clinicalServiceId;
    private String clinicalServiceName;
    private String departmentName;
}
