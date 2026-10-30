package g2.be.service;

import g2.be.entity.VerificationToken;
import g2.be.repository.VerificationTokenRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private VerificationTokenService verificationTokenService;

    private VerificationToken validToken;
    private VerificationToken expiredToken;

    @BeforeEach
    void setUp() {
        validToken = VerificationToken.builder()
                .email("test@example.com")
                .otpCode("123456")
                .type(VerificationToken.TokenType.REGISTER)
                .isUsed(false)
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .build();

        expiredToken = VerificationToken.builder()
                .email("test@example.com")
                .otpCode("654321")
                .type(VerificationToken.TokenType.REGISTER)
                .isUsed(false)
                .expiryDate(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    @Test
    void testCreateOtpToken_Success() {
        // Arrange
        String email = "test@example.com";
        VerificationToken.TokenType type = VerificationToken.TokenType.REGISTER;

        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenReturn(validToken);

        // Act
        VerificationToken result = verificationTokenService.createOtpToken(email, type);

        // Assert
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(type, result.getType());
        assertFalse(result.getIsUsed());
        assertNotNull(result.getOtpCode());
        assertEquals(6, result.getOtpCode().length());
        assertTrue(result.getExpiryDate().isAfter(LocalDateTime.now()));

        verify(verificationTokenRepository).save(any(VerificationToken.class));
    }

    @Test
    void testVerifyOtp_Success() {
        // Arrange
        String email = "test@example.com";
        String otpCode = "123456";
        VerificationToken.TokenType type = VerificationToken.TokenType.REGISTER;

        when(verificationTokenRepository.findByEmailAndOtpCodeAndTypeAndIsUsed(email, otpCode, type, false))
                .thenReturn(Optional.of(validToken));

        // Act
        VerificationToken result = verificationTokenService.verifyOtp(email, otpCode, type);

        // Assert
        assertNotNull(result);
        assertEquals(validToken, result);

        verify(verificationTokenRepository).findByEmailAndOtpCodeAndTypeAndIsUsed(email, otpCode, type, false);
    }

    @Test
    void testVerifyOtp_InvalidOtp() {
        // Arrange
        String email = "test@example.com";
        String otpCode = "999999";
        VerificationToken.TokenType type = VerificationToken.TokenType.REGISTER;

        when(verificationTokenRepository.findByEmailAndOtpCodeAndTypeAndIsUsed(email, otpCode, type, false))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            verificationTokenService.verifyOtp(email, otpCode, type);
        });

        assertEquals("OTP không hợp lệ hoặc đã được sử dụng", exception.getMessage());
    }

    @Test
    void testVerifyOtp_ExpiredOtp() {
        // Arrange
        String email = "test@example.com";
        String otpCode = "654321";
        VerificationToken.TokenType type = VerificationToken.TokenType.REGISTER;

        when(verificationTokenRepository.findByEmailAndOtpCodeAndTypeAndIsUsed(email, otpCode, type, false))
                .thenReturn(Optional.of(expiredToken));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            verificationTokenService.verifyOtp(email, otpCode, type);
        });

        assertEquals("OTP đã hết hạn. Vui lòng yêu cầu mã mới.", exception.getMessage());
    }

    @Test
    void testVerifyAndMarkAsUsed_Success() {
        // Arrange
        String email = "test@example.com";
        String otpCode = "123456";
        VerificationToken.TokenType type = VerificationToken.TokenType.REGISTER;

        when(verificationTokenRepository.findByEmailAndOtpCodeAndTypeAndIsUsed(email, otpCode, type, false))
                .thenReturn(Optional.of(validToken));
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenReturn(validToken);

        // Act
        VerificationToken result = verificationTokenService.verifyAndMarkAsUsed(email, otpCode, type);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsUsed());

        verify(verificationTokenRepository).findByEmailAndOtpCodeAndTypeAndIsUsed(email, otpCode, type, false);
        verify(verificationTokenRepository).save(validToken);
    }

    @Test
    void testVerifyAndMarkAsUsed_InvalidOtp() {
        // Arrange
        String email = "test@example.com";
        String otpCode = "999999";
        VerificationToken.TokenType type = VerificationToken.TokenType.REGISTER;

        when(verificationTokenRepository.findByEmailAndOtpCodeAndTypeAndIsUsed(email, otpCode, type, false))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            verificationTokenService.verifyAndMarkAsUsed(email, otpCode, type);
        });

        assertEquals("OTP không hợp lệ hoặc đã được sử dụng", exception.getMessage());
        verify(verificationTokenRepository, never()).save(any());
    }

    @Test
    void testVerifyAndMarkAsUsed_ExpiredOtp() {
        // Arrange
        String email = "test@example.com";
        String otpCode = "654321";
        VerificationToken.TokenType type = VerificationToken.TokenType.REGISTER;

        when(verificationTokenRepository.findByEmailAndOtpCodeAndTypeAndIsUsed(email, otpCode, type, false))
                .thenReturn(Optional.of(expiredToken));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            verificationTokenService.verifyAndMarkAsUsed(email, otpCode, type);
        });

        assertEquals("OTP đã hết hạn. Vui lòng yêu cầu mã mới.", exception.getMessage());
        verify(verificationTokenRepository, never()).save(any());
    }

    @Test
    void testCreateOtpToken_GeneratesUniqueCode() {
        // Arrange
        String email = "test@example.com";
        VerificationToken.TokenType type = VerificationToken.TokenType.RESET_PASSWORD;

        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VerificationToken token1 = verificationTokenService.createOtpToken(email, type);
        VerificationToken token2 = verificationTokenService.createOtpToken(email, type);

        // Assert
        assertNotEquals(token1.getOtpCode(), token2.getOtpCode(),
                "OTP codes should be unique");
        assertTrue(token1.getOtpCode().matches("\\d{6}"),
                "OTP should be 6 digits");
        assertTrue(token2.getOtpCode().matches("\\d{6}"),
                "OTP should be 6 digits");
    }
}
