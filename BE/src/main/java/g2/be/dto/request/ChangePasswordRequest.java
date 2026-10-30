package g2.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {
    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])\\S{8,100}$";
    private static final String PASSWORD_MESSAGE =
            "Mat khau moi phai co 8-100 ky tu, gom chu hoa, chu thuong, so va ky tu dac biet";

    @NotBlank(message = "Mat khau hien tai khong duoc de trong")
    private String currentPassword;

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
    private String newPassword;

    @NotBlank(message = "Xac nhan mat khau khong duoc de trong")
    private String confirmationPassword;
}
