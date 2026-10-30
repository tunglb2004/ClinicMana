package g2.be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong hop le")
    private String email;
    private String type; // "REGISTER" hoặc "RESET_PASSWORD"
}
