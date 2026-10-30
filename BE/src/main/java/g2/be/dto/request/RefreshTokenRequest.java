package g2.be.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    
    private String refreshToken;
}
