package g2.be.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import g2.be.dto.request.ChangeEmailRequest;
import g2.be.dto.request.ChangePasswordRequest;
import g2.be.dto.request.UpdateProfileRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.UserInfoResponse;
import g2.be.security.UserDetailsImpl;
import g2.be.service.CloudinaryService;
import g2.be.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs quản lý thông tin người dùng")
public class UserController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    /**
     * GET /api/user/profile
     * Lấy thông tin profile người dùng hiện tại
     */
    @GetMapping("/profile")
    @Operation(summary = "Xem profile", description = "Lấy thông tin chi tiết profile của người dùng đang đăng nhập")
    public ResponseEntity<UserInfoResponse> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        UserInfoResponse response = userService.getUserInfo(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/user/profile
     * Cập nhật thông tin profile
     */
    @PutMapping("/profile")
    @Operation(summary = "Cập nhật profile", description = "Cập nhật thông tin cá nhân: họ tên, CCCD, giới tính, số điện thoại, avatar")
    public ResponseEntity<UserInfoResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        UserInfoResponse response = userService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật ảnh đại diện", description = "Tải ảnh đại diện lên Cloudinary và lưu URL vào tài khoản")
    public ResponseEntity<UserInfoResponse> updateAvatar(@RequestPart("file") MultipartFile file) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String avatarUrl = cloudinaryService.uploadImageFile(file, "clinic_avatars");
        UserInfoResponse response = userService.updateAvatar(userDetails.getId(), avatarUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/profile/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Táº£i file há»“ sÆ¡", description = "Táº£i file há»“ sÆ¡ nghá» nghiá»‡p lÃªn Cloudinary vÃ  tráº£ vá» URL")
    public ResponseEntity<Map<String, String>> uploadProfileFile(@RequestPart("file") MultipartFile file)
            throws IOException {
        String fileUrl = cloudinaryService.uploadPdfFile(file, "clinic_profile_files");
        return ResponseEntity.ok(Map.of("fileUrl", fileUrl));
    }

    /**
     * POST /api/user/change-password
     * Đổi mật khẩu
     */
    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu", description = "Thay đổi mật khẩu (yêu cầu nhập mật khẩu cũ)")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        MessageResponse response = userService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/user/change-email
     * Đổi email
     */
    @PostMapping("/change-email")
    @Operation(summary = "Đổi email", description = "Thay đổi email (yêu cầu mật khẩu để xác thực)")
    public ResponseEntity<MessageResponse> changeEmail(@Valid @RequestBody ChangeEmailRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        MessageResponse response = userService.changeEmail(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }
}
