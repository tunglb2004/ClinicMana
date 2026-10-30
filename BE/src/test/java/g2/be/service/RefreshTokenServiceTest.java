package g2.be.service;

import g2.be.entity.RefreshToken;
import g2.be.entity.User;
import g2.be.repository.RefreshTokenRepository;
import g2.be.repository.UserRepository;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken validRefreshToken;
    private RefreshToken expiredRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        validRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("valid-refresh-token")
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        expiredRefreshToken = RefreshToken.builder()
                .id(2L)
                .token("expired-refresh-token")
                .user(testUser)
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void testCreateRefreshToken_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(validRefreshToken);

        // Act
        RefreshToken result = refreshTokenService.createRefreshToken(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertEquals(testUser, result.getUser());
        assertTrue(result.getExpiryDate().isAfter(LocalDateTime.now()));

        verify(userRepository).findById(1L);
        verify(refreshTokenRepository).deleteByUser(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    // Added imports for Exception handling
    @Test
    void testCreateRefreshToken_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            refreshTokenService.createRefreshToken(999L);
        });

        assertEquals(ErrorCode.USER_NOT_EXIST, exception.getErrorCode());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testFindByToken_Success() {
        // Arrange
        String token = "valid-refresh-token";
        when(refreshTokenRepository.findByToken(token))
                .thenReturn(Optional.of(validRefreshToken));

        // Act
        Optional<RefreshToken> result = refreshTokenService.findByToken(token);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(validRefreshToken, result.get());

        verify(refreshTokenRepository).findByToken(token);
    }

    @Test
    void testFindByToken_NotFound() {
        // Arrange
        String token = "non-existent-token";
        when(refreshTokenRepository.findByToken(token))
                .thenReturn(Optional.empty());

        // Act
        Optional<RefreshToken> result = refreshTokenService.findByToken(token);

        // Assert
        assertFalse(result.isPresent());

        verify(refreshTokenRepository).findByToken(token);
    }

    @Test
    void testVerifyExpiration_ValidToken() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            refreshTokenService.verifyExpiration(validRefreshToken);
        });

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void testVerifyExpiration_ExpiredToken() {
        // Arrange & Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            refreshTokenService.verifyExpiration(expiredRefreshToken);
        });

        assertEquals(ErrorCode.REFRESH_TOKEN_EXPIRED, exception.getErrorCode());
        verify(refreshTokenRepository).delete(expiredRefreshToken);
    }

    @Test
    void testDeleteByToken_Success() {
        // Arrange
        String token = "token-to-delete";
        RefreshToken tokenToDelete = RefreshToken.builder()
                .token(token)
                .user(testUser)
                .build();

        when(refreshTokenRepository.findByToken(token))
                .thenReturn(Optional.of(tokenToDelete));

        // Act
        refreshTokenService.deleteByToken(token);

        // Assert
        verify(refreshTokenRepository).findByToken(token);
        verify(refreshTokenRepository).delete(tokenToDelete);
    }

    @Test
    void testCreateRefreshToken_DeletesOldToken() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(validRefreshToken);

        // Act
        RefreshToken result = refreshTokenService.createRefreshToken(1L);

        // Assert
        assertNotNull(result);
        verify(refreshTokenRepository).deleteByUser(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testCreateRefreshToken_GeneratesUniqueToken() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RefreshToken token1 = refreshTokenService.createRefreshToken(1L);
        RefreshToken token2 = refreshTokenService.createRefreshToken(1L);

        // Assert
        assertNotNull(token1.getToken());
        assertNotNull(token2.getToken());
        assertNotEquals(token1.getToken(), token2.getToken(),
                "Refresh tokens should be unique");
    }
}
