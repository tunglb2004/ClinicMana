package g2.be.controller;

import g2.be.dto.request.ForgotPasswordRequest;
import g2.be.dto.request.LoginRequest;
import g2.be.dto.request.RefreshTokenRequest;
import g2.be.dto.request.RegisterRequest;
import g2.be.dto.request.ResetPasswordRequest;
import g2.be.dto.request.SendOtpRequest;
import g2.be.dto.response.LoginResponse;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.OtpResponse;
import g2.be.dto.response.RefreshTokenResponse;
import g2.be.dto.response.UserInfoResponse;
import g2.be.entity.VerificationToken;
import g2.be.security.UserDetailsImpl;
import g2.be.service.AuthService;
import g2.be.service.EmailService;
import g2.be.service.UserService;
import g2.be.service.VerificationTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthController authController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginReturnsOkResponse() {
        LoginRequest request = new LoginRequest();
        LoginResponse loginResponse = mock(LoginResponse.class);

        when(authService.login(request)).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(loginResponse, response.getBody());
    }

    @Test
    void sendRegisterOtpSendsOtpEmail() {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail("patient@test.com");
        VerificationToken token = mock(VerificationToken.class);

        when(token.getOtpCode()).thenReturn("123456");
        when(verificationTokenService.createOtpToken("patient@test.com", VerificationToken.TokenType.REGISTER))
                .thenReturn(token);

        ResponseEntity<OtpResponse> response = authController.sendRegisterOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("patient@test.com", response.getBody().getEmail());
        verify(emailService).sendOtpEmail("patient@test.com", "123456", "REGISTER");
    }

    @Test
    void registerDelegatesToAuthService() {
        RegisterRequest request = new RegisterRequest();

        ResponseEntity<MessageResponse> response = authController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Đăng ký"));
        verify(authService).register(request);
    }

    @Test
    void refreshTokenReturnsOkResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        RefreshTokenResponse refreshTokenResponse = mock(RefreshTokenResponse.class);

        when(authService.refreshToken("refresh-token")).thenReturn(refreshTokenResponse);

        ResponseEntity<RefreshTokenResponse> response = authController.refreshToken(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(refreshTokenResponse, response.getBody());
    }

    @Test
    void forgotPasswordDelegatesToUserService() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("patient@test.com");

        ResponseEntity<MessageResponse> response = authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).forgotPassword("patient@test.com");
    }

    @Test
    void resetPasswordDelegatesToUserService() {
        ResetPasswordRequest request = new ResetPasswordRequest();

        ResponseEntity<MessageResponse> response = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).resetPassword(request);
    }

    @Test
    void getCurrentUserReturnsCurrentUserInfo() {
        setAuthenticatedUser(99L);
        UserInfoResponse userInfoResponse = mock(UserInfoResponse.class);

        when(authService.getCurrentUserInfo(99L)).thenReturn(userInfoResponse);

        ResponseEntity<UserInfoResponse> response = authController.getCurrentUser();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(userInfoResponse, response.getBody());
    }

    private void setAuthenticatedUser(long userId) {
        UserDetailsImpl userDetails = new UserDetailsImpl(userId, "tester", "tester@test.com", "secret", List.of());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}