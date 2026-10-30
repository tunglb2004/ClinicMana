package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDetailResponse {
    private Long id;
    private String medicineName;
    private String dosage;
    private String quantity;
    // Note: Based on existing entity PrescriptionDetail, frequency, duration, unit, note are additional fields in Request.
    // I will include them here to match the Request requirement if I update the entity, 
    // but for now I'll stick to what the entity has or what is requested for the response.
    // The user request said: "PrescriptionResponse (with all detail fields)"
    private String frequency;
    private String duration;
    private String unit;
    private String note;
}
