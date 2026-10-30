package g2.be.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRoomRequest {

    @NotEmpty(message = "Danh sách serviceId không được trống")
    private List<String> serviceIds;

    private Boolean confirmReplaceAll;
}
