package g2.be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {
    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong hop le")
    private String email;

    @NotBlank(message = "OTP khong duoc de trong")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP phai gom 6 chu so")
    private String otpCode;

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    @Size(min = 6, max = 100, message = "Mat khau moi phai tu 6 den 100 ky tu")
    private String newPassword;
}
