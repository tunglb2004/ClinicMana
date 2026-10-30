package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {
    private String prescriptionId;
    private String medicalRecordId;
    private String notes;
    private LocalDateTime createdAt;
    private List<PrescriptionDetailResponse> details;
}
