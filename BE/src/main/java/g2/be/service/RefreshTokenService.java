package g2.be.service;

import g2.be.entity.RefreshToken;
import g2.be.entity.User;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.RefreshTokenRepository;
import g2.be.repository.UserRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(ClinicTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(ClinicTime.now())) {
            refreshTokenRepository.delete(token);
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        return token;
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(ClinicTime.now());
    }
}
