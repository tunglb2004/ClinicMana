package g2.be.controller;

import g2.be.dto.request.SendOtpRequest;
import g2.be.dto.request.VerifyOtpRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.OtpResponse;
import g2.be.entity.VerificationToken;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.service.EmailService;
import g2.be.service.VerificationTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
@Tag(name = "OTP Verification", description = "APIs xác thực OTP qua email")
public class OtpController {

    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;

    /**
     * POST /api/otp/send
     * Gửi mã OTP qua email
     */
    @PostMapping("/send")
    @Operation(summary = "Gửi mã OTP", description = "Tạo và gửi mã OTP 6 chữ số đến email của người dùng")
    public ResponseEntity<OtpResponse> sendOtp(@RequestBody SendOtpRequest request) {
        try {
            VerificationToken.TokenType type = VerificationToken.TokenType.valueOf(request.getType().toUpperCase());
            VerificationToken token = verificationTokenService.createOtpToken(request.getEmail(), type);

            // Gửi email OTP thực tế qua Gmail
            emailService.sendOtpEmail(request.getEmail(), token.getOtpCode(), request.getType());

            OtpResponse response = OtpResponse.builder()
                    .message(ErrorCode.FORGOT_PASSWORD_OTP_SENT.getMessage())
                    .email(request.getEmail())
                    .expiresInMinutes(5L)
                    .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_OTP_TYPE);
        }
    }

    /**
     * POST /api/otp/verify
     * Xác thực mã OTP
     */
    @PostMapping("/verify")
    @Operation(summary = "Xác thực OTP", description = "Kiểm tra mã OTP có hợp lệ không và đánh dấu đã sử dụng")
    public ResponseEntity<MessageResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            VerificationToken.TokenType type = VerificationToken.TokenType.valueOf(request.getType().toUpperCase());
            verificationTokenService.verifyAndMarkAsUsed(request.getEmail(), request.getOtpCode(), type);

            return ResponseEntity.ok(new MessageResponse(ErrorCode.VERIFY_OTP_SUCCESS.getMessage()));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_OTP_TYPE);
        }
    }

    /**
     * POST /api/otp/resend
     * Gửi lại mã OTP mới
     */
    @PostMapping("/resend")
    @Operation(summary = "Gửi lại OTP", description = "Tạo và gửi mã OTP mới, thay thế mã cũ")
    public ResponseEntity<OtpResponse> resendOtp(@RequestBody SendOtpRequest request) {
        // Giống như sendOtp
        return sendOtp(request);
    }
}
