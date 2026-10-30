package g2.be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDepartmentStatusRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
