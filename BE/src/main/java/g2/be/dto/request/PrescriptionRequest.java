package g2.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequest {
    @NotBlank(message = "Medical Record ID is required")
    private String medicalRecordId;
    
    private String notes;
    
    @NotEmpty(message = "Prescription details cannot be empty")
    private List<PrescriptionDetailRequest> details;
}
