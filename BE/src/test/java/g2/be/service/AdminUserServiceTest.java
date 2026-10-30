package g2.be.service;

import g2.be.dto.request.CreateUserRequest;
import g2.be.dto.request.UpdateUserRequest;
import g2.be.dto.request.UpdateUserStatusRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.UserDetailResponse;
import g2.be.dto.response.UserListResponse;
import g2.be.entity.Role;
import g2.be.entity.User;
import g2.be.repository.RoleRepository;
import g2.be.repository.UserRepository;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests cho AdminUserService.
 * Sử dụng Mockito để mock các dependencies (Repository, PasswordEncoder).
 * 
 * Cấu trúc test theo pattern: Given - When - Then
 * - Given (Arrange): Chuẩn bị dữ liệu và mock
 * - When (Act): Gọi method cần test
 * - Then (Assert): Kiểm tra kết quả
 * 
 * @author Nam
 */
@ExtendWith(MockitoExtension.class) // Kích hoạt Mockito
class AdminUserServiceTest {

        // ==================== MOCK DEPENDENCIES ====================

        @Mock // Tạo mock object cho UserRepository
        private UserRepository userRepository;

        @Mock // Tạo mock object cho RoleRepository
        private RoleRepository roleRepository;

        @Mock // Tạo mock object cho PasswordEncoder
        private PasswordEncoder passwordEncoder;

        @Mock // Tạo mock object cho EmailService
        private EmailService emailService;

        @InjectMocks // Inject các mock vào AdminUserService
        private AdminUserService adminUserService;

        // ==================== TEST DATA ====================

        private User testUser;
        private Role testRole;

        /**
         * Chạy trước mỗi test case.
         * Khởi tạo dữ liệu test mẫu.
         */
        @BeforeEach
        void setUp() {
                // Tạo Role mẫu
                testRole = Role.builder()
                                .roleId(1)
                                .roleName("ROLE_DOCTOR")
                                .description("Bác sĩ")
                                .build();

                // Tạo User mẫu
                Set<Role> roles = new HashSet<>();
                roles.add(testRole);

                testUser = User.builder()
                                .userId(1L)
                                .username("bacsi_test")
                                .email("bacsi@test.com")
                                .passwordHash("hashedPassword123")
                                .fullName("Bác Sĩ Test")
                                .cccd("012345678901")
                                .gender(User.Gender.MALE)
                                .phoneNumber("0901234567")
                                .isActive(true)
                                .isDeleted(false)
                                .roles(roles)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
        }

        // ==================== TEST: getAllUsers ====================

        @Nested // Nhóm các test cho method getAllUsers
        @DisplayName("Test getAllUsers()")
        class GetAllUsersTests {

                @Test
                @DisplayName("Lấy danh sách users thành công với pagination")
                void getAllUsers_Success_ReturnsUserList() {
                        // GIVEN (Chuẩn bị)
                        int page = 0;
                        int size = 10;
                        List<User> users = List.of(testUser);
                        Page<User> userPage = new PageImpl<>(users);

                        // Mock: khi gọi searchUsers thì trả về userPage
                        when(userRepository.searchUsers(isNull(), isNull(), any(Pageable.class)))
                                        .thenReturn(userPage);

                        // WHEN (Thực hiện)
                        UserListResponse response = adminUserService.getAllUsers(page, size);

                        // THEN (Kiểm tra)
                        assertNotNull(response); // Response không null
                        assertEquals(1, response.getUsers().size()); // Có 1 user
                        assertEquals(1, response.getTotalElements()); // Tổng 1 phần tử
                        assertEquals("bacsi_test", response.getUsers().get(0).getUsername());

                        // Verify: đảm bảo repository được gọi đúng 1 lần
                        verify(userRepository, times(1)).searchUsers(isNull(), isNull(), any(Pageable.class));
                }

                @Test
                @DisplayName("Lấy danh sách users - trả về empty khi không có data")
                void getAllUsers_EmptyList_ReturnsEmptyResponse() {
                        // GIVEN
                        Page<User> emptyPage = new PageImpl<>(List.of());
                        when(userRepository.searchUsers(isNull(), isNull(), any(Pageable.class)))
                                        .thenReturn(emptyPage);

                        // WHEN
                        UserListResponse response = adminUserService.getAllUsers(0, 10);

                        // THEN
                        assertNotNull(response);
                        assertTrue(response.getUsers().isEmpty());
                        assertEquals(0, response.getTotalElements());
                }
        }

        // ==================== TEST: getUserById ====================

        @Nested
        @DisplayName("Test getUserById()")
        class GetUserByIdTests {

                @Test
                @DisplayName("Lấy chi tiết user thành công")
                void getUserById_Success_ReturnsUserDetail() {
                        // GIVEN
                        when(userRepository.findByUserIdAndIsDeleted(1L, false))
                                        .thenReturn(Optional.of(testUser));

                        // WHEN
                        UserDetailResponse response = adminUserService.getUserById(1L);

                        // THEN
                        assertNotNull(response);
                        assertEquals(1L, response.getUserId());
                        assertEquals("bacsi_test", response.getUsername());
                        assertEquals("bacsi@test.com", response.getEmail());
                        assertTrue(response.getRoles().contains("ROLE_DOCTOR"));
                }

                // Added imports for Exception handling
                // ... imports are added at top of file separately

                @Test
                @DisplayName("Lấy chi tiết user - ném exception khi user không tồn tại")
                void getUserById_UserNotFound_ThrowsException() {
                        // GIVEN
                        when(userRepository.findByUserIdAndIsDeleted(999L, false))
                                        .thenReturn(Optional.empty());

                        // WHEN & THEN
                        AppException exception = assertThrows(
                                        AppException.class,
                                        () -> adminUserService.getUserById(999L));

                        assertEquals(ErrorCode.USER_NOT_EXIST, exception.getErrorCode());
                }

                // ...

                @Test
                @DisplayName("Tạo user - ném exception khi username đã tồn tại")
                void createUser_UsernameExists_ThrowsException() {
                        // GIVEN
                        CreateUserRequest request = CreateUserRequest.builder()
                                        .username("existinguser")
                                        .email("new@test.com")
                                        .fullName("Test")
                                        .roleNames(Set.of("ROLE_DOCTOR"))
                                        .build();

                        when(userRepository.existsByUsername("existinguser")).thenReturn(true); // Username đã tồn tại

                        // WHEN & THEN
                        AppException exception = assertThrows(
                                        AppException.class,
                                        () -> adminUserService.createUser(request));

                        assertEquals(ErrorCode.USERNAME_EXISTS, exception.getErrorCode());
                        verify(userRepository, never()).save(any()); // Không save
                }

                @Test
                @DisplayName("Tạo user - ném exception khi email đã tồn tại")
                void createUser_EmailExists_ThrowsException() {
                        // GIVEN
                        CreateUserRequest request = CreateUserRequest.builder()
                                        .username("newuser")
                                        .email("existing@test.com")
                                        .fullName("Test")
                                        .roleNames(Set.of("ROLE_DOCTOR"))
                                        .build();

                        when(userRepository.existsByUsername("newuser")).thenReturn(false);
                        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true); // Email đã tồn tại

                        // WHEN & THEN
                        AppException exception = assertThrows(
                                        AppException.class,
                                        () -> adminUserService.createUser(request));

                        assertEquals(ErrorCode.REGISTER_EMAIL_EXISTS, exception.getErrorCode());
                }

                @Test
                @DisplayName("Tạo user - ném exception khi role không tồn tại")
                void createUser_RoleNotFound_ThrowsException() {
                        // GIVEN
                        CreateUserRequest request = CreateUserRequest.builder()
                                        .username("newuser")
                                        .email("new@test.com")
                                        .fullName("Test")
                                        .roleNames(Set.of("ROLE_INVALID"))
                                        .build();

                        when(userRepository.existsByUsername("newuser")).thenReturn(false);
                        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
                        when(roleRepository.findByRoleName("ROLE_INVALID")).thenReturn(Optional.empty());

                        // WHEN & THEN
                        AppException exception = assertThrows(
                                        AppException.class,
                                        () -> adminUserService.createUser(request));

                        assertEquals(ErrorCode.ROLE_NOT_FOUND, exception.getErrorCode());
                }

                // ...

                @Test
                @DisplayName("Cập nhật user - ném exception khi phone đã được dùng")
                void updateUser_PhoneExists_ThrowsException() {
                        // GIVEN
                        UpdateUserRequest request = UpdateUserRequest.builder()
                                        .phoneNumber("0909999999")
                                        .build();

                        when(userRepository.findByUserIdAndIsDeleted(1L, false))
                                        .thenReturn(Optional.of(testUser));
                        when(userRepository.existsByPhoneNumberAndUserIdNot("0909999999", 1L))
                                        .thenReturn(true); // Phone đã tồn tại

                        // WHEN & THEN
                        AppException exception = assertThrows(
                                        AppException.class,
                                        () -> adminUserService.updateUser(1L, request));

                        assertEquals(ErrorCode.REGISTER_PHONE_EXISTS, exception.getErrorCode());
                }

                // ...

                @Test
                @DisplayName("Xóa user - ném exception khi user không tồn tại")
                void deleteUser_UserNotFound_ThrowsException() {
                        // GIVEN
                        when(userRepository.findByUserIdAndIsDeleted(999L, false))
                                        .thenReturn(Optional.empty());

                        // WHEN & THEN
                        AppException exception = assertThrows(
                                        AppException.class,
                                        () -> adminUserService.deleteUser(999L));

                        assertEquals(ErrorCode.USER_NOT_EXIST, exception.getErrorCode());
                }

                // ==================== TEST: updateUserStatus ====================

                @Nested
                @DisplayName("Test updateUserStatus()")
                class UpdateUserStatusTests {

                        @Test
                        @DisplayName("Khóa tài khoản thành công")
                        void updateUserStatus_LockAccount_Success() {
                                // GIVEN
                                UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                                                .isActive(false) // Khóa
                                                .build();

                                when(userRepository.findByUserIdAndIsDeleted(1L, false))
                                                .thenReturn(Optional.of(testUser));
                                when(userRepository.save(any(User.class))).thenReturn(testUser);

                                // WHEN
                                UserDetailResponse response = adminUserService.updateUserStatus(1L, request);

                                // THEN
                                assertNotNull(response);
                                assertFalse(testUser.getIsActive()); // Đã bị khóa
                                verify(userRepository).save(testUser);
                        }

                        @Test
                        @DisplayName("Mở khóa tài khoản thành công")
                        void updateUserStatus_UnlockAccount_Success() {
                                // GIVEN
                                testUser.setIsActive(false); // Đang bị khóa

                                UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                                                .isActive(true) // Mở khóa
                                                .build();

                                when(userRepository.findByUserIdAndIsDeleted(1L, false))
                                                .thenReturn(Optional.of(testUser));
                                when(userRepository.save(any(User.class))).thenReturn(testUser);

                                // WHEN
                                UserDetailResponse response = adminUserService.updateUserStatus(1L, request);

                                // THEN
                                assertNotNull(response);
                                assertTrue(testUser.getIsActive()); // Đã mở khóa
                        }
                }
        }
}
