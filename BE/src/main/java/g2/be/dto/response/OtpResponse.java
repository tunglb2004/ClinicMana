package g2.be.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpResponse {

    private String message;
    private String email;
    private Long expiresInMinutes;
}
