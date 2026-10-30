package g2.be.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    private String email;
    private String otpCode;
    private String type; // "REGISTER" hoặc "RESET_PASSWORD"
}
