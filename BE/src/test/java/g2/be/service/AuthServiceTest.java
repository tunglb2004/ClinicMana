package g2.be.service;

import g2.be.dto.request.LoginRequest;
import g2.be.dto.request.RegisterRequest;
import g2.be.dto.response.LoginResponse;
import g2.be.dto.response.RefreshTokenResponse;
import g2.be.entity.*;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.PatientRepository;
import g2.be.repository.RoleRepository;
import g2.be.repository.UserRepository;
import g2.be.security.JwtUtil;
import g2.be.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role patientRole;

    @BeforeEach
    void setUp() {
        // Setup test user
        patientRole = Role.builder()
                .roleId(1)
                .roleName("ROLE_PATIENT")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(patientRole);

        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .phoneNumber("0123456789")
                .fullName("Test User")
                .passwordHash("hashedPassword")
                .gender(User.Gender.MALE)
                .isActive(true)
                .roles(roles)
                .build();
    }

    @Test
    void testLogin_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@example.com");
        request.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "testuser", "test@example.com", "hashedPassword",
                Set.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-123")
                .build();

        when(userRepository.findByEmailOrPhoneNumber("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(authentication)).thenReturn("access-token-123");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("access-token-123", response.getAccessToken());
        assertEquals("refresh-token-123", response.getRefreshToken());
        assertEquals(1L, response.getUser().getId());
        assertEquals("Test User", response.getUser().getFullName());
        assertTrue(response.getUser().getRoles().contains("ROLE_PATIENT"));

        verify(userRepository).findByEmailOrPhoneNumber("test@example.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setIdentifier("notfound@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmailOrPhoneNumber("notfound@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
                AppException exception = assertThrows(AppException.class, () -> {
            authService.login(request);
        });

                assertEquals(ErrorCode.LOGIN_INVALID_CREDENTIALS, exception.getErrorCode());
                assertEquals(ErrorCode.LOGIN_INVALID_CREDENTIALS.getMessage(), exception.getMessage());
    }

    @Test
    void testLogin_InactiveUserBlocked() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@example.com");
        request.setPassword("password123");
        testUser.setIsActive(false);

        when(userRepository.findByEmailOrPhoneNumber("test@example.com"))
                .thenReturn(Optional.of(testUser));

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(ErrorCode.LOGIN_ACCOUNT_LOCKED, exception.getErrorCode());
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testRegister_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setFullName("New User");
        request.setPhone("0987654321");
        request.setCccd("123456789012");
        request.setGender("MALE");
        request.setOtpCode("123456");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByCccd(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(roleRepository.findByRoleName("ROLE_PATIENT")).thenReturn(Optional.of(patientRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(patientRepository.existsByPatientId(anyString())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(new Patient());

        // Act
        assertDoesNotThrow(() -> {
            authService.register(request);
        });

        // Assert
        verify(verificationTokenService).verifyAndMarkAsUsed(
                "newuser@example.com",
                "123456",
                VerificationToken.TokenType.REGISTER
        );
        verify(userRepository).save(any(User.class));
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void testRefreshToken_Success() {
        // Arrange
        RefreshToken refreshToken = RefreshToken.builder()
                .token("old-refresh-token")
                .user(testUser)
                .build();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token("new-refresh-token")
                .build();

        when(refreshTokenService.findByToken("old-refresh-token"))
                .thenReturn(Optional.of(refreshToken));
        when(jwtUtil.generateAccessTokenFromUsername(anyString(), anyLong(), anyString()))
                .thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(newRefreshToken);

        // Act
        RefreshTokenResponse response = authService.refreshToken("old-refresh-token");

        // Assert
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        verify(refreshTokenService).verifyExpiration(refreshToken);
    }

    @Test
    void testRefreshToken_InvalidToken() {
        // Arrange
        when(refreshTokenService.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.refreshToken("invalid-token");
        });

        assertEquals("Refresh token không hợp lệ", exception.getMessage());
    }

    @Test
    void testLogout_Success() {
        // Arrange
        String refreshToken = "refresh-token-123";

        // Act
        assertDoesNotThrow(() -> {
            authService.logout(refreshToken);
        });

        // Assert
        verify(refreshTokenService).deleteByToken(refreshToken);
    }
}
