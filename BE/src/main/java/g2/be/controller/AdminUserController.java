package g2.be.controller;

import g2.be.dto.request.CreateUserRequest;
import g2.be.dto.request.UpdateUserRequest;
import g2.be.dto.request.UpdateUserStatusRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.UserDetailResponse;
import g2.be.dto.response.UserInfoResponse;
import g2.be.dto.response.UserListResponse;
import g2.be.service.AdminUserService;
import g2.be.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xử lý các API quản lý tài khoản nhân viên dành cho Admin.
 * Tất cả APIs yêu cầu quyền ADMIN, MANAGER, RECEPTIONIST, CASHIER.
 *
 * Base URL: /api/users
 *
 * @author Nam
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER', 'ROLE_RECEPTIONIST', 'ROLE_CASHIER')")
@Tag(name = "User Management", description = "APIs quản lý tài khoản nhân viên")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserService userService;

    // ==================== 1. DANH SÁCH USERS ====================

    /**
     * API lấy danh sách tất cả tài khoản nhân viên (có phân trang).
     * GET /api/users?page=0&size=10
     */
    @GetMapping
    @Operation(summary = "Danh sách tài khoản nhân viên",
               description = "Lấy danh sách tất cả tài khoản nhân viên với phân trang. Sắp xếp theo thời gian tạo mới nhất.")
    public ResponseEntity<UserListResponse> getAllUsers(
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng users mỗi trang") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Từ khóa tìm kiếm (tên, username, mã)") @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc theo vai trò cụ thể. Giá trị hợp lệ: ROLE_RECEPTIONIST, ROLE_SPECIALISTDOCTOR, ROLE_CASHIER, ROLE_PATIENT, ROLE_CLINIC_MANAGER, ROLE_DOCTOR, ROLE_MANAGER, ROLE_ADMIN, ROLE_SYSTEM_ADMIN, ROLE_NURSE. Để trống hoặc truyền 'all' để lấy tất cả.") @RequestParam(required = false) String role) {
        UserListResponse response = adminUserService.getAllUsers(page, size, keyword, role);
        return ResponseEntity.ok(response);
    }

    // ==================== 2. CHI TIẾT 1 USER ====================

    /**
     * API lấy thông tin chi tiết của 1 tài khoản.
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Chi tiết tài khoản", description = "Xem thông tin chi tiết của 1 tài khoản nhân viên theo ID")
    public ResponseEntity<UserDetailResponse> getUserDetail(
            @Parameter(description = "ID của user") @PathVariable Long userId) {
        UserDetailResponse response = adminUserService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    // ==================== 3. TẠO USER MỚI ====================

    /**
     * API tạo tài khoản nhân viên mới.
     * POST /api/users
     */
    @PostMapping
    @Operation(summary = "Thêm tài khoản nhân viên",
               description = "Tạo tài khoản nhân viên mới. Yêu cầu: username, email, password, fullName, roleNames")
    public ResponseEntity<UserDetailResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserDetailResponse response = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== 4. CẬP NHẬT USER ====================

    /**
     * API cập nhật thông tin tài khoản nhân viên.
     * PUT /api/users/{userId}
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Sửa tài khoản nhân viên",
               description = "Cập nhật thông tin tài khoản. Chỉ cần gửi các field muốn thay đổi.")
    public ResponseEntity<UserDetailResponse> updateUser(
            @Parameter(description = "ID của user cần cập nhật") @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserDetailResponse response = adminUserService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    // ==================== 5. XÓA USER ====================

    /**
     * API xóa tài khoản nhân viên (soft delete).
     * DELETE /api/users/{userId}
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Xóa tài khoản nhân viên",
               description = "Xóa tài khoản (soft delete - có thể khôi phục). Tài khoản sẽ bị khóa và đánh dấu đã xóa.")
    public ResponseEntity<MessageResponse> deleteUser(
            @Parameter(description = "ID của user cần xóa") @PathVariable Long userId) {
        MessageResponse response = adminUserService.deleteUser(userId);
        return ResponseEntity.ok(response);
    }

    // ==================== 6. KHÓA TÀI KHOẢN ====================

    /**
     * API khóa tài khoản nhân viên.
     * PATCH /api/users/{userId}/lock
     */
    @PatchMapping("/{userId}/lock")
    @Operation(summary = "Khóa tài khoản", description = "Khóa tài khoản nhân viên (isActive = false)")
    public ResponseEntity<UserDetailResponse> lockUser(
            @Parameter(description = "ID của user") @PathVariable Long userId) {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setIsActive(false);
        UserDetailResponse response = adminUserService.updateUserStatus(userId, request);
        return ResponseEntity.ok(response);
    }

    // ==================== 7. MỞ KHÓA TÀI KHOẢN ====================

    /**
     * API mở khóa tài khoản nhân viên.
     * PATCH /api/users/{userId}/unlock
     */
    @PatchMapping("/{userId}/unlock")
    @Operation(summary = "Mở khóa tài khoản", description = "Mở khóa tài khoản nhân viên (isActive = true)")
    public ResponseEntity<UserDetailResponse> unlockUser(
            @Parameter(description = "ID của user") @PathVariable Long userId) {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setIsActive(true);
        UserDetailResponse response = adminUserService.updateUserStatus(userId, request);
        return ResponseEntity.ok(response);
    }

    // ==================== 8. ĐỒNG BỘ USER CŨ THEO ROLE ====================

    /**
     * API đồng bộ user cũ:
     * - Role -> tạo hồ sơ doctor/nurse nếu thiếu
     * - Hồ sơ doctor/nurse -> bổ sung role nếu thiếu
     */
    @PostMapping("/sync-existing-staff-profiles")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Đồng bộ user cũ theo role", description = "Backfill hồ sơ doctor/nurse và role tương ứng cho dữ liệu user cũ")
    public ResponseEntity<Map<String, Object>> synchronizeExistingStaffProfiles() {
        Map<String, Object> result = adminUserService.synchronizeExistingUsersByRoles();
        return ResponseEntity.ok(result);
    }
    @GetMapping("/{userId}/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Xem ho so nhan vien", description = "Quan ly xem ho so chi tiet cua bac si/nhan vien theo ID")
    public ResponseEntity<UserInfoResponse> getManagedUserProfile(
            @Parameter(description = "ID cua user") @PathVariable Long userId) {
        UserInfoResponse response = userService.getUserInfo(userId);
        return ResponseEntity.ok(response);
    }
}
