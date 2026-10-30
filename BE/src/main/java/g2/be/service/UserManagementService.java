package g2.be.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g2.be.dto.request.CreateUserRequest;
import g2.be.dto.request.UpdateUserRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.UserDetailResponse;
import g2.be.entity.Role;
import g2.be.entity.User;
import g2.be.repository.RoleRepository;
import g2.be.repository.UserRepository;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy danh sách tất cả người dùng với phân trang và tìm kiếm
     */
    @Transactional(readOnly = true)
    public PageResponse<UserDetailResponse> getAllUsers(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String keyword,
            Boolean isActive) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage;

        if (keyword != null && !keyword.isEmpty()) {
            // Tìm kiếm theo username, email, fullName, phoneNumber
            userPage = userRepository.findByKeywordAndIsDeleted(keyword, false, pageable);
        } else if (isActive != null) {
            // Lọc theo trạng thái active
            userPage = userRepository.findByIsActiveAndIsDeleted(isActive, false, pageable);
        } else {
            // Lấy tất cả user chưa bị xóa
            userPage = userRepository.findByIsDeleted(false, pageable);
        }

        List<UserDetailResponse> content = userPage.getContent().stream()
                .map(UserDetailResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<UserDetailResponse>builder()
                .content(content)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .first(userPage.isFirst())
                .build();
    }

    /**
     * Lấy thông tin chi tiết một người dùng
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        if (user.getIsDeleted()) {
            throw new AppException(ErrorCode.USER_DELETED);
        }

        return UserDetailResponse.fromEntity(user);
    }

    /**
     * Tạo mới tài khoản người dùng
     */
    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        log.info("Creating new user with username: {}", request.getUsername());

        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTS);
        }

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.REGISTER_EMAIL_EXISTS);
        }

        // Kiểm tra số điện thoại đã tồn tại (nếu có)
        if (request.getPhoneNumber() != null &&
                userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.REGISTER_PHONE_EXISTS);
        }

        // Lấy roles
        Set<Role> roles = new HashSet<>();
        if (request.getRoleNames() != null && !request.getRoleNames().isEmpty()) {
            for (String roleName : request.getRoleNames()) {
                Role role = roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
                roles.add(role);
            }
        } else {
            // Mặc định gán role PATIENT
            Role defaultRole = roleRepository.findByRoleName("ROLE_PATIENT")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            roles.add(defaultRole);
        }

        // Tạo mật khẩu random (không lấy từ request)
        String rawPassword = generateRandomPassword();

        // Tạo mã nhân viên
        String staffId = generateUniqueStaffId();

        // Tạo user mới
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(request.getFullName())
                .cccd(request.getCccd())
                .gender(request.getGender())
                .phoneNumber(request.getPhoneNumber())
                .avatarUrl(request.getAvatarUrl())
                .staffId(staffId)
                .isActive(true)
                .isDeleted(false)
                .mustChangePassword(true) // Bắt buộc đổi MK lần đầu
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}, StaffId: {}", savedUser.getUserId(), staffId);

        // TODO: Gửi email với mật khẩu rawPassword cho user (tích hợp EmailService)

        return UserDetailResponse.fromEntity(savedUser);
    }

    /**
     * Cập nhật thông tin người dùng
     */
    @Transactional
    public UserDetailResponse updateUser(Long userId, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        if (user.getIsDeleted()) {
            throw new AppException(ErrorCode.USER_DELETED);
        }

        // Cập nhật email (kiểm tra trùng)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AppException(ErrorCode.REGISTER_EMAIL_EXISTS);
            }
            user.setEmail(request.getEmail());
        }

        // Cập nhật số điện thoại (kiểm tra trùng)
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new AppException(ErrorCode.REGISTER_PHONE_EXISTS);
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        // Cập nhật các trường khác
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getCccd() != null) {
            user.setCccd(request.getCccd());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Cập nhật roles nếu có
        if (request.getRoleNames() != null && !request.getRoleNames().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoleNames()) {
                Role role = roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        // Cập nhật avatar URL nếu có
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", updatedUser.getUserId());

        return UserDetailResponse.fromEntity(updatedUser);
    }

    /**
     * Xóa người dùng (soft delete)
     */
    @Transactional
    public MessageResponse deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        if (user.getIsDeleted()) {
            throw new AppException(ErrorCode.USER_ALREADY_DELETED);
        }

        // Soft delete: đánh dấu isDeleted = true
        user.setIsDeleted(true);
        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deleted successfully with ID: {}", userId);

        return new MessageResponse(ErrorCode.DELETE_USER_SUCCESS.getMessage());
    }

    /**
     * Khóa tài khoản người dùng
     */
    @Transactional
    public MessageResponse lockUser(Long userId) {
        log.info("Locking user account with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        if (user.getIsDeleted()) {
            throw new AppException(ErrorCode.USER_DELETED);
        }

        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.USER_ALREADY_LOCKED);
        }

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User account locked successfully with ID: {}", userId);

        return new MessageResponse(ErrorCode.LOCK_USER_SUCCESS.getMessage());
    }

    /**
     * Mở khóa tài khoản người dùng
     */
    @Transactional
    public MessageResponse unlockUser(Long userId) {
        log.info("Unlocking user account with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        if (user.getIsDeleted()) {
            throw new AppException(ErrorCode.USER_DELETED);
        }

        if (user.getIsActive()) {
            throw new AppException(ErrorCode.USER_NOT_LOCKED);
        }

        user.setIsActive(true);
        userRepository.save(user);

        log.info("User account unlocked successfully with ID: {}", userId);

        return new MessageResponse(ErrorCode.UNLOCK_USER_SUCCESS.getMessage());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Tự động tạo mã nhân viên duy nhất.
     * Format: NV-XXXXXXXX (8 ký tự hex viết hoa)
     */
    private String generateUniqueStaffId() {
        String staffId;
        do {
            staffId = "NV-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.existsByStaffId(staffId));
        return staffId;
    }

    /**
     * Tự động tạo mật khẩu ngẫu nhiên an toàn.
     * Format: 12 ký tự bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.
     */
    private String generateRandomPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%&*";
        String allChars = upperCase + lowerCase + digits + special;

        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder();

        // Đảm bảo có ít nhất 1 ký tự mỗi loại
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Thêm 8 ký tự ngẫu nhiên nữa (tổng 12 ký tự)
        for (int i = 0; i < 8; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Xáo trộn các ký tự
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }
}
