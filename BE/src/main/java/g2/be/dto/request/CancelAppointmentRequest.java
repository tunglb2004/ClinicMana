package g2.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CancelAppointmentRequest {

    @NotBlank(message = "Ly do huy lich khong duoc de trong")
    @Size(max = 500, message = "Ly do huy lich khong duoc vuot qua 500 ky tu")
    private String cancelReason;
}
