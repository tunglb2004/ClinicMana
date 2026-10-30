package g2.be.controller;

import g2.be.dto.request.ChangePasswordRequest;
import g2.be.dto.request.UpdateProfileRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.UserInfoResponse;
import g2.be.security.UserDetailsImpl;
import g2.be.service.UserService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getProfileReturnsCurrentUserProfile() {
        setAuthenticatedUser(7L);
        UserInfoResponse userInfoResponse = mock(UserInfoResponse.class);

        when(userService.getUserInfo(7L)).thenReturn(userInfoResponse);

        ResponseEntity<UserInfoResponse> response = userController.getProfile();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(userInfoResponse, response.getBody());
    }

    @Test
    void updateProfileDelegatesToUserService() {
        setAuthenticatedUser(7L);
        UpdateProfileRequest request = new UpdateProfileRequest();
        UserInfoResponse userInfoResponse = mock(UserInfoResponse.class);

        when(userService.updateProfile(7L, request)).thenReturn(userInfoResponse);

        ResponseEntity<UserInfoResponse> response = userController.updateProfile(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(userInfoResponse, response.getBody());
    }

    @Test
    void changePasswordDelegatesToUserService() {
        setAuthenticatedUser(7L);
        ChangePasswordRequest request = new ChangePasswordRequest();
        MessageResponse messageResponse = MessageResponse.builder().message("changed").build();

        when(userService.changePassword(7L, request)).thenReturn(messageResponse);

        ResponseEntity<MessageResponse> response = userController.changePassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(messageResponse, response.getBody());
        verify(userService).changePassword(7L, request);
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