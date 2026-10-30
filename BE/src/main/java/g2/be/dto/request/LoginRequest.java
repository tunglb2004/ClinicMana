package g2.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email hoac so dien thoai khong duoc de trong")
    private String identifier;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(max = 100, message = "Mat khau khong duoc vuot qua 100 ky tu")
    private String password;
}
