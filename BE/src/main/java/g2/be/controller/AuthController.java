package g2.be.controller;

import g2.be.dto.request.*;
import g2.be.dto.response.*;
import g2.be.entity.VerificationToken;
import g2.be.security.UserDetailsImpl;
import g2.be.service.AuthService;
import g2.be.service.EmailService;
import g2.be.service.UserService;
import g2.be.service.VerificationTokenService;
import g2.be.exception.ErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs xác thực & bảo mật")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;

    /**
     * POST /api/auth/login
     * Đăng nhập bằng email hoặc số điện thoại
     */
    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Xác thực bằng email/số điện thoại và password, trả về JWT Token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/register/send-otp
     * Bước 1: Gửi OTP để xác thực email trước khi đăng ký
     */
    @PostMapping("/register/send-otp")
    @Operation(summary = "Gửi OTP đăng ký", description = "Gửi mã OTP 6 số đến email để xác thực trước khi đăng ký tài khoản")
    public ResponseEntity<OtpResponse> sendRegisterOtp(@Valid @RequestBody SendOtpRequest request) {
        VerificationToken token = verificationTokenService.createOtpToken(
                request.getEmail(),
                VerificationToken.TokenType.REGISTER);

        // Gửi email OTP qua Gmail
        emailService.sendOtpEmail(request.getEmail(), token.getOtpCode(), "REGISTER");

        OtpResponse response = OtpResponse.builder()
                .message(ErrorCode.FORGOT_PASSWORD_OTP_SENT.getMessage())
                .email(request.getEmail())
                .expiresInMinutes(5L)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/register
     * Bước 2: Đăng ký tài khoản sau khi xác thực OTP
     */
    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản mới cho bệnh nhân (yêu cầu OTP đã được xác thực)")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new MessageResponse(ErrorCode.REGISTER_SUCCESS.getMessage()));
    }

    /**
     * POST /api/auth/refresh-token
     * Làm mới Access Token bằng Refresh Token
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới token", description = "Cấp lại Access Token mới khi token cũ hết hạn")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Đăng xuất và xóa Refresh Token
     */
    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Xóa Refresh Token khỏi hệ thống")
    public ResponseEntity<MessageResponse> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Logged out"));
    }

    /**
     * GET /api/auth/me
     * Lấy thông tin người dùng hiện tại
     */
    @GetMapping("/me")
    @Operation(summary = "Thông tin người dùng", description = "Lấy thông tin chi tiết của người đang đăng nhập")
    public ResponseEntity<UserInfoResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        UserInfoResponse response = authService.getCurrentUserInfo(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/forgot-password
     * Quên mật khẩu - Gửi email đặt lại mật khẩu
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Quên mật khẩu", description = "Gửi email hướng dẫn đặt lại mật khẩu đến người dùng")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(ErrorCode.FORGOT_PASSWORD_OTP_SENT.getMessage()));
    }

    /**
     * POST /api/auth/reset-password
     * Đặt lại mật khẩu qua email
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu", description = "Cập nhật mật khẩu mới cho người dùng sau khi đã xác thực qua email")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse(ErrorCode.CHANGE_PASSWORD_SUCCESS.getMessage()));
    }
}
