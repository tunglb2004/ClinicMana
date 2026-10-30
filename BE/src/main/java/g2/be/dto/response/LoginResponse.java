package g2.be.dto.response;

import lombok.*;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String fullName;
        private Set<String> roles;
        private String avatar;
        private Boolean mustChangePassword;
    }
}
