package g2.be.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenResponse {
    
    private String accessToken;
    private String refreshToken;
}
