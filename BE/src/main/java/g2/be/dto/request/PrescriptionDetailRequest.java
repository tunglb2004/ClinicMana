package g2.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDetailRequest {
    @NotBlank(message = "Medicine name is required")
    private String medicineName;
    
    private String dosage;
    
    private String frequency;
    
    private String duration;
    
    @NotBlank(message = "Quantity is required")
    private String quantity;
    
    private String unit;
    
    private String note;
}
