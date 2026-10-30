package g2.be.service;

import g2.be.entity.VerificationToken;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.VerificationTokenRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRATION_MINUTES = 5; // OTP hết hạn sau 5 phút

    /**
     * Tạo mã OTP ngẫu nhiên 6 chữ số
     */
    public String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Tạo và lưu OTP token mới
     */
    @Transactional
    public VerificationToken createOtpToken(String email, VerificationToken.TokenType type) {
        // Xóa các OTP cũ của email này với cùng loại
        verificationTokenRepository.deleteByEmailAndType(email, type);

        String otpCode = generateOtpCode();
        LocalDateTime expiryDate = ClinicTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);

        VerificationToken token = VerificationToken.builder()
                .email(email)
                .otpCode(otpCode)
                .expiryDate(expiryDate)
                .type(type)
                .isUsed(false)
                .build();

        return verificationTokenRepository.save(token);
    }

    /**
     * Xác thực OTP
     */
    public VerificationToken verifyOtp(String email, String otpCode, VerificationToken.TokenType type) {
        VerificationToken token = verificationTokenRepository
                .findByEmailAndOtpCodeAndTypeAndIsUsed(email, otpCode, type, false)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID_OR_USED));

        if (token.getExpiryDate().isBefore(ClinicTime.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        return token;
    }

    /**
     * Đánh dấu OTP đã được sử dụng
     */
    @Transactional
    public void markAsUsed(VerificationToken token) {
        token.setIsUsed(true);
        verificationTokenRepository.save(token);
    }

    /**
     * Xác thực và đánh dấu OTP đã sử dụng (kết hợp)
     */
    @Transactional
    public VerificationToken verifyAndMarkAsUsed(String email, String otpCode, VerificationToken.TokenType type) {
        VerificationToken token = verifyOtp(email, otpCode, type);
        markAsUsed(token);
        return token;
    }

}
