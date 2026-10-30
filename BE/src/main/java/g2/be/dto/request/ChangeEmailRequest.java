package g2.be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChangeEmailRequest {

    @NotBlank(message = "Email mới không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String newEmail;

    @NotBlank(message = "Mã OTP không được để trống")
    private String otpCode;
}
