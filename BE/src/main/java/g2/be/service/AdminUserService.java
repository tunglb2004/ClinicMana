package g2.be.service;

import g2.be.dto.request.CreateUserRequest;
import g2.be.dto.request.UpdateUserRequest;
import g2.be.dto.request.UpdateUserStatusRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.UserDetailResponse;
import g2.be.dto.response.UserListResponse;
import g2.be.dto.response.UserListResponse.UserSummary;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.DoctorType;
import g2.be.entity.Nurse;
import g2.be.entity.Role;
import g2.be.entity.User;
import g2.be.repository.DoctorRepository;
import g2.be.repository.NurseRepository;
import g2.be.repository.RoleRepository;
import g2.be.repository.UserRepository;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service xử lý logic nghiệp vụ cho Admin quản lý tài khoản nhân viên.
 * Bao gồm: Danh sách, Thêm, Sửa, Xóa, Khóa/Mở khóa tài khoản.
 * 
 * @author Nam
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private static final Set<String> DOCTOR_ROLE_NAMES = Set.of(
        "ROLE_DOCTOR",
        "ROLE_CLINICALDOCTOR",
        "ROLE_CLINICAL_DOCTOR",
        "ROLE_SPECIALISTDOCTOR",
        "ROLE_SPECIALIST_DOCTOR");

    private static final Set<String> SPECIALIST_DOCTOR_ROLE_NAMES = Set.of(
        "ROLE_SPECIALISTDOCTOR",
        "ROLE_SPECIALIST_DOCTOR");

    private static final String NURSE_ROLE_NAME = "ROLE_NURSE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DoctorRepository doctorRepository;
    private final NurseRepository nurseRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ==================== 1. LẤY DANH SÁCH USERS ====================

    /**
     * Lấy danh sách tất cả users (chưa bị xóa) với phân trang.
     * 
     * @param page - Số trang (bắt đầu từ 0)
     * @param size - Số lượng users mỗi trang
     * @return UserListResponse - Danh sách users kèm thông tin phân trang
     * 
     *         Luồng xử lý:
     *         1. Tạo Pageable với page, size và sắp xếp theo createdAt giảm dần
     *         2. Query database lấy users chưa bị xóa (isDeleted = false)
     *         3. Convert từ Entity sang DTO
     *         4. Trả về response kèm thông tin phân trang
     */
    @Transactional(readOnly = true)
    public UserListResponse getAllUsers(int page, int size) {
        return getAllUsers(page, size, null, null);
    }

    /**
     * Lấy danh sách tất cả users (chưa bị xóa) với phân trang, tìm kiếm và lọc.
     * 
     * @param page    - Số trang
     * @param size    - Số lượng mỗi trang
     * @param keyword - Từ khóa tìm kiếm
     * @param role    - Vai trò cần lọc
     * @return UserListResponse
     */
    @Transactional(readOnly = true)
    public UserListResponse getAllUsers(int page, int size, String keyword, String role) {
        // Bước 1: Tạo Pageable - sắp xếp theo thời gian tạo mới nhất
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Bước 2: Query database - tìm kiếm và filter
        Page<User> userPage = userRepository.searchUsers(keyword, role, pageable);

        // Bước 3: Convert Entity -> DTO (thông tin rút gọn)
        List<UserSummary> userSummaries = userPage.getContent().stream()
                .map(this::convertToUserSummary)
                .collect(Collectors.toList());

        // Bước 4: Build response với thông tin phân trang
        return UserListResponse.builder()
                .users(userSummaries)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .currentPage(userPage.getNumber())
                .pageSize(userPage.getSize())
                .build();
    }

    // ==================== 2. LẤY CHI TIẾT 1 USER ====================

    /**
     * Lấy thông tin chi tiết của 1 user theo ID.
     * 
     * @param userId - ID của user cần xem
     * @return UserDetailResponse - Thông tin chi tiết user
     * @throws RuntimeException nếu user không tồn tại hoặc đã bị xóa
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long userId) {
        // Tìm user theo ID và chưa bị xóa
        User user = userRepository.findByUserIdAndIsDeleted(userId, false)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        return convertToUserDetailResponse(user);
    }

    // ==================== 3. TẠO USER MỚI ====================

    /**
     * Tạo tài khoản nhân viên mới.
     * 
     * @param request - Thông tin user cần tạo (username, email, password, roles...)
     * @return UserDetailResponse - Thông tin user vừa tạo
     * @throws RuntimeException nếu username/email/cccd/phone đã tồn tại
     * 
     *                          Luồng xử lý:
     *                          1. Validate: kiểm tra trùng username, email, cccd,
     *                          phone
     *                          2. Hash password (mã hóa mật khẩu)
     *                          3. Tìm và gán các Role từ roleNames
     *                          4. Lưu user vào database
     *                          5. Trả về thông tin user vừa tạo
     */
    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        // Bước 1: Validate - kiểm tra trùng lặp
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.REGISTER_EMAIL_EXISTS);
        }
        if (request.getCccd() != null && !request.getCccd().isBlank()
                && userRepository.existsByCccd(request.getCccd())) {
            throw new AppException(ErrorCode.ACCOUNT_DUPLICATE_EMAIL_CCCD);
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.REGISTER_PHONE_EXISTS);
        }

        // Bước 2: Tìm các Role từ tên
        Set<Role> roles = findRolesByNames(request.getRoleNames());

        // Bước 3: Tự động tạo mã nhân viên
        String staffId = generateUniqueStaffId();

        // Bước 4: Tự động tạo mật khẩu ngẫu nhiên (Admin không được biết)
        String rawPassword = generateRandomPassword();

        // Bước 5: Tạo User entity
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(rawPassword)) // Hash password
                .fullName(request.getFullName())
                .cccd(request.getCccd())
                .gender(request.getGender())
                .phoneNumber(request.getPhoneNumber())
                .staffId(staffId) // Mã nhân viên tự động
                .isActive(true) // Mặc định active
                .isDeleted(false) // Chưa bị xóa
                .mustChangePassword(true) // Bắt buộc đổi mật khẩu lần đầu
                .roles(roles)
                .build();

        // Bước 6: Lưu vào database
        User savedUser = userRepository.save(user);

        // Bước 6.1: Tạo hồ sơ chuyên môn tương ứng (doctor/nurse) theo role
        synchronizeMedicalProfiles(savedUser);
        applyMedicalProfileDetails(savedUser, toMedicalProfilePayload(request));

        // Bước 7: Gửi email thông báo tài khoản và mật khẩu
        try {
            sendWelcomeEmail(savedUser, rawPassword);
            log.info("Email đã gửi thành công đến: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.warn("Không thể gửi email đến {}: {}", savedUser.getEmail(), e.getMessage());
            // Không throw exception - user đã được tạo thành công
        }

        // Bước 8: Trả về response
        return convertToUserDetailResponse(savedUser);
    }

    // ==================== 4. CẬP NHẬT THÔNG TIN USER ====================

    /**
     * Cập nhật thông tin tài khoản nhân viên.
     * 
     * @param userId  - ID của user cần cập nhật
     * @param request - Thông tin mới (chỉ cập nhật các field được gửi lên)
     * @return UserDetailResponse - Thông tin user sau khi cập nhật
     * @throws RuntimeException nếu user không tồn tại hoặc trùng cccd/phone
     * 
     *                          Luồng xử lý:
     *                          1. Tìm user theo ID
     *                          2. Validate: kiểm tra trùng cccd, phone với user
     *                          khác
     *                          3. Cập nhật từng field nếu có giá trị mới
     *                          4. Cập nhật roles nếu có
     *                          5. Lưu và trả về kết quả
     */
    @Transactional
    public UserDetailResponse updateUser(Long userId, UpdateUserRequest request) {
        // Bước 1: Tìm user
        User user = userRepository.findByUserIdAndIsDeleted(userId, false)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        // Bước 2: Validate trùng lặp với user khác
        if (request.getCccd() != null && !request.getCccd().isBlank()) {
            if (userRepository.existsByCccdAndUserIdNot(request.getCccd(), userId)) {
                throw new AppException(ErrorCode.ACCOUNT_DUPLICATE_EMAIL_CCCD);
            }
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (userRepository.existsByPhoneNumberAndUserIdNot(request.getPhoneNumber(), userId)) {
                throw new AppException(ErrorCode.REGISTER_PHONE_EXISTS);
            }
        }

        // Bước 3: Cập nhật từng field nếu có giá trị
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getCccd() != null) {
            user.setCccd(request.getCccd());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Bước 4: Cập nhật roles nếu có (thay thế toàn bộ roles cũ)
        if (request.getRoleNames() != null && !request.getRoleNames().isEmpty()) {
            Set<Role> newRoles = findRolesByNames(request.getRoleNames());
            user.setRoles(newRoles);
        }

        // Bước 5: Lưu và trả về
        User updatedUser = userRepository.save(user);

        // Bước 5.1: Đồng bộ hồ sơ chuyên môn theo role hiện tại
        synchronizeMedicalProfiles(updatedUser);
        applyMedicalProfileDetails(updatedUser, toMedicalProfilePayload(request));

        return convertToUserDetailResponse(updatedUser);
    }

    // ==================== 5. XÓA USER (SOFT DELETE) ====================

    /**
     * Xóa tài khoản nhân viên (soft delete - chỉ đánh dấu đã xóa).
     * 
     * @param userId - ID của user cần xóa
     * @return MessageResponse - Thông báo kết quả
     * @throws RuntimeException nếu user không tồn tại
     * 
     *                          Lưu ý: Không xóa thật khỏi database, chỉ set
     *                          isDeleted = true
     *                          để giữ lại lịch sử và có thể khôi phục nếu cần.
     */
    @Transactional
    public MessageResponse deleteUser(Long userId) {
        User user = userRepository.findByUserIdAndIsDeleted(userId, false)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        // Soft delete: chỉ đánh dấu đã xóa
        user.setIsDeleted(true);
        user.setIsActive(false); // Đồng thời khóa tài khoản
        userRepository.save(user);

        return new MessageResponse(ErrorCode.DELETE_USER_SUCCESS.getMessage());
    }

    // ==================== 6. KHÓA/MỞ KHÓA TÀI KHOẢN ====================

    /**
     * Khóa hoặc mở khóa tài khoản nhân viên.
     * 
     * @param userId  - ID của user
     * @param request - Chứa isActive (true = mở khóa, false = khóa)
     * @return UserDetailResponse - Thông tin user sau khi cập nhật
     * @throws RuntimeException nếu user không tồn tại
     */
    @Transactional
    public UserDetailResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findByUserIdAndIsDeleted(userId, false)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        // Cập nhật trạng thái
        user.setIsActive(request.getIsActive());
        User updatedUser = userRepository.save(user);

        return convertToUserDetailResponse(updatedUser);
    }

    /**
     * Đồng bộ dữ liệu user cũ theo 2 chiều:
     * 1) Role -> Hồ sơ chuyên môn (doctor/nurse)
     * 2) Hồ sơ chuyên môn -> Role (nếu thiếu role trong user_roles)
     */
    @Transactional
    public Map<String, Object> synchronizeExistingUsersByRoles() {
        List<User> users = userRepository.findByIsDeletedFalse();

        int usersUpdated = 0;
        int doctorProfilesCreated = 0;
        int nurseProfilesCreated = 0;
        int doctorRolesAdded = 0;
        int nurseRolesAdded = 0;

        for (User user : users) {
            RoleSyncResult roleSyncResult = synchronizeRolesFromExistingProfiles(user);
            ProfileSyncResult profileSyncResult = synchronizeMedicalProfiles(user);

            boolean changed = roleSyncResult.addedDoctorRole()
                    || roleSyncResult.addedNurseRole()
                    || profileSyncResult.createdDoctorProfile()
                    || profileSyncResult.createdNurseProfile();

            if (changed) {
                usersUpdated++;
                userRepository.save(user);
            }

            if (roleSyncResult.addedDoctorRole()) {
                doctorRolesAdded++;
            }
            if (roleSyncResult.addedNurseRole()) {
                nurseRolesAdded++;
            }
            if (profileSyncResult.createdDoctorProfile()) {
                doctorProfilesCreated++;
            }
            if (profileSyncResult.createdNurseProfile()) {
                nurseProfilesCreated++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsersScanned", users.size());
        result.put("usersUpdated", usersUpdated);
        result.put("doctorProfilesCreated", doctorProfilesCreated);
        result.put("nurseProfilesCreated", nurseProfilesCreated);
        result.put("doctorRolesAdded", doctorRolesAdded);
        result.put("nurseRolesAdded", nurseRolesAdded);

        return result;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Tìm danh sách Role từ danh sách tên role.
     * 
     * @param roleNames - Set các tên role (VD: ["ADMIN", "DOCTOR"])
     * @return Set<Role> - Set các Role entity
     * @throws RuntimeException nếu có role không tồn tại
     */
    private Set<Role> findRolesByNames(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = resolveRoleByAlias(roleName)
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            roles.add(role);
        }
        return roles;
    }

    /**
     * Chuẩn hóa role cũ/mới để tránh lỗi khi FE gửi alias khác định dạng.
     */
    private java.util.Optional<Role> resolveRoleByAlias(String requestedRoleName) {
        if (requestedRoleName == null || requestedRoleName.isBlank()) {
            return java.util.Optional.empty();
        }

        String normalized = requestedRoleName.trim().toUpperCase(Locale.ROOT);
        Set<String> candidates = new LinkedHashSet<>();

        switch (normalized) {
            case "ROLE_DOCTOR", "ROLE_CLINICALDOCTOR", "ROLE_CLINICAL_DOCTOR" -> {
                candidates.add("ROLE_DOCTOR");
                candidates.add("ROLE_CLINICALDOCTOR");
                candidates.add("ROLE_CLINICAL_DOCTOR");
            }
            case "ROLE_SPECIALISTDOCTOR", "ROLE_SPECIALIST_DOCTOR" -> {
                candidates.add("ROLE_SPECIALISTDOCTOR");
                candidates.add("ROLE_SPECIALIST_DOCTOR");
            }
            case "ROLE_CLINIC_MANAGER", "ROLE_MANAGER" -> {
                candidates.add("ROLE_CLINIC_MANAGER");
                candidates.add("ROLE_MANAGER");
            }
            default -> candidates.add(normalized);
        }

        for (String candidate : candidates) {
            java.util.Optional<Role> role = roleRepository.findByRoleName(candidate);
            if (role.isPresent()) {
                return role;
            }
        }

        return java.util.Optional.empty();
    }

    /**
     * Tự động tạo hồ sơ bác sĩ/y tá nếu role yêu cầu nhưng hồ sơ chưa tồn tại.
     * Không tự động xóa hồ sơ cũ để tránh mất liên kết lịch sử khám/chỉ định.
     */
    private ProfileSyncResult synchronizeMedicalProfiles(User user) {
        Set<Role> assignedRoles = user.getRoles() == null ? Set.of() : user.getRoles();

        Set<String> roleNames = assignedRoles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());

        boolean shouldHaveDoctorProfile = roleNames.stream().anyMatch(DOCTOR_ROLE_NAMES::contains);
        boolean shouldHaveNurseProfile = roleNames.contains(NURSE_ROLE_NAME);

        boolean createdDoctorProfile = false;
        boolean createdNurseProfile = false;

        if (shouldHaveDoctorProfile) {
            Doctor doctor;
            java.util.Optional<Doctor> existingDoctor = doctorRepository.findByUser_UserId(user.getUserId());
            if (existingDoctor.isPresent()) {
                doctor = existingDoctor.get();
            } else {
                createdDoctorProfile = true;
                doctor = Doctor.builder()
                        .doctorId(generateUniqueDoctorId(user))
                        .user(user)
                        .build();
            }

            doctor.setUser(user);
            doctor.setDoctorType(resolveDoctorType(roleNames, doctor));
            Doctor savedDoctor = doctorRepository.save(doctor);
            user.setDoctor(savedDoctor);
            synchronizeDoctorRolesWithDepartment(user, savedDoctor);
        }

        if (shouldHaveNurseProfile) {
            Nurse nurse;
            java.util.Optional<Nurse> existingNurse = nurseRepository.findByUser_UserId(user.getUserId());
            if (existingNurse.isPresent()) {
                nurse = existingNurse.get();
            } else {
                createdNurseProfile = true;
                nurse = Nurse.builder()
                        .nurseId(generateUniqueNurseId(user))
                        .user(user)
                        .build();
            }

            nurse.setUser(user);
            Nurse savedNurse = nurseRepository.save(nurse);
            user.setNurse(savedNurse);
        }

        return new ProfileSyncResult(createdDoctorProfile, createdNurseProfile);
    }

    /**
     * Đồng bộ role cho user đã có hồ sơ doctor/nurse nhưng thiếu role tương ứng.
     */
    private RoleSyncResult synchronizeRolesFromExistingProfiles(User user) {
        Set<Role> roles = user.getRoles();
        if (roles == null) {
            roles = new HashSet<>();
            user.setRoles(roles);
        }

        Set<String> roleNames = roles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());

        boolean hasDoctorProfile = user.getDoctor() != null
                || doctorRepository.findByUser_UserId(user.getUserId()).isPresent();
        boolean hasNurseProfile = user.getNurse() != null
                || nurseRepository.findByUser_UserId(user.getUserId()).isPresent();

        boolean addedDoctorRole = false;
        boolean addedNurseRole = false;

        if (hasDoctorProfile && roleNames.stream().noneMatch(DOCTOR_ROLE_NAMES::contains)) {
            String doctorRoleName = "ROLE_DOCTOR";
            if (user.getDoctor() != null && user.getDoctor().getDepartment() != null
                    && user.getDoctor().getDepartment().getDeptType() == g2.be.entity.Department.DeptType.PARACLINICAL) {
                doctorRoleName = "ROLE_SPECIALISTDOCTOR";
            }
            Role doctorRole = resolveRoleByAlias(doctorRoleName).orElse(null);
            if (doctorRole != null && roles.add(doctorRole)) {
                addedDoctorRole = true;
            }
        }

        if (hasNurseProfile && !roleNames.contains(NURSE_ROLE_NAME)) {
            Role nurseRole = resolveRoleByAlias(NURSE_ROLE_NAME).orElse(null);
            if (nurseRole != null && roles.add(nurseRole)) {
                addedNurseRole = true;
            }
        }

        return new RoleSyncResult(addedDoctorRole, addedNurseRole);
    }

    private record ProfileSyncResult(boolean createdDoctorProfile, boolean createdNurseProfile) {
    }

    private record RoleSyncResult(boolean addedDoctorRole, boolean addedNurseRole) {
    }

    private DoctorType resolveDoctorType(Set<String> roleNames, Doctor doctor) {
        if (doctor != null && doctor.getDepartment() != null && doctor.getDepartment().getDeptType() != null) {
            return doctor.getDepartment().getDeptType() == g2.be.entity.Department.DeptType.PARACLINICAL
                    ? DoctorType.SPECIALIST
                    : DoctorType.CLINICAL;
        }

        return roleNames.stream().anyMatch(SPECIALIST_DOCTOR_ROLE_NAMES::contains)
                ? DoctorType.SPECIALIST
                : DoctorType.CLINICAL;
    }

    private void synchronizeDoctorRolesWithDepartment(User user, Doctor doctor) {
        if (user == null || doctor == null || doctor.getDepartment() == null || doctor.getDepartment().getDeptType() == null) {
            return;
        }

        Set<Role> roles = user.getRoles() == null ? new HashSet<>() : user.getRoles();
        user.setRoles(roles);

        roles.removeIf(role -> {
            String roleName = role.getRoleName();
            return "ROLE_DOCTOR".equals(roleName)
                    || "ROLE_CLINICALDOCTOR".equals(roleName)
                    || "ROLE_CLINICAL_DOCTOR".equals(roleName)
                    || "ROLE_SPECIALISTDOCTOR".equals(roleName)
                    || "ROLE_SPECIALIST_DOCTOR".equals(roleName);
        });

        String targetRoleName = doctor.getDepartment().getDeptType() == g2.be.entity.Department.DeptType.PARACLINICAL
                ? "ROLE_SPECIALISTDOCTOR"
                : "ROLE_DOCTOR";
        Role targetRole = resolveRoleByAlias(targetRoleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        roles.add(targetRole);
    }

    private String generateUniqueDoctorId(User user) {
        return generateUniqueMedicalId("BS-", user.getStaffId(), doctorRepository::existsById);
    }

    private String generateUniqueNurseId(User user) {
        return generateUniqueMedicalId("YT-", user.getStaffId(), nurseRepository::existsById);
    }

    private String generateUniqueMedicalId(String prefix, String staffId,
            java.util.function.Predicate<String> existsChecker) {
        String normalizedStaff = normalizeStaffId(staffId);

        if (!normalizedStaff.isBlank()) {
            String candidate = prefix + normalizedStaff;
            if (!existsChecker.test(candidate)) {
                return candidate;
            }
        }

        String candidate;
        do {
            candidate = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (existsChecker.test(candidate));

        return candidate;
    }

    private String normalizeStaffId(String staffId) {
        if (staffId == null || staffId.isBlank()) {
            return "";
        }

        return staffId.toUpperCase(Locale.ROOT)
                .replace("NV-", "")
                .replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Tự động tạo mã nhân viên duy nhất.
     * Format: NV-XXXXXXXX (8 ký tự hex viết hoa)
     * 
     * @return String - Mã nhân viên duy nhất
     */
    private String generateUniqueStaffId() {
        String staffId;
        do {
            staffId = "NV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.existsByStaffId(staffId));
        return staffId;
    }

    /**
     * Tự động tạo mật khẩu ngẫu nhiên an toàn.
     * Format: 12 ký tự bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.
     * 
     * @return String - Mật khẩu ngẫu nhiên
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

    /**
     * Gửi email chào mừng với thông tin đăng nhập cho nhân viên mới.
     * 
     * @param user        - User vừa được tạo
     * @param rawPassword - Mật khẩu gốc (chưa hash)
     */
    private void sendWelcomeEmail(User user, String rawPassword) {
        String subject = "Tài khoản nhân viên đã được tạo - Phòng Khám FBT";
        String htmlContent = buildWelcomeEmailTemplate(user, rawPassword);
        emailService.sendHtmlEmail(user.getEmail(), subject, htmlContent);
    }

    /**
     * Tạo template HTML cho email chào mừng nhân viên mới.
     */
    private String buildWelcomeEmailTemplate(User user, String rawPassword) {
        String rolesDisplay = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.joining(", "));

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #667eea; padding: 20px; margin: 20px 0; }
                        .info-row { margin: 10px 0; }
                        .label { font-weight: bold; color: #555; }
                        .value { color: #333; }
                        .password-box { background: #fff3cd; border: 2px dashed #ffc107; padding: 15px; text-align: center; margin: 20px 0; }
                        .password { font-size: 24px; font-weight: bold; color: #d63384; letter-spacing: 2px; }
                        .warning { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; color: #721c24; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #888; border-top: 1px solid #e0e0e0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🏥 Phòng Khám Đa Khoa FBT</h1>
                        </div>
                        <div class="content">
                            <h2>Chào mừng %s!</h2>
                            <p>Tài khoản nhân viên của bạn đã được tạo thành công.</p>

                            <div class="info-box">
                                <div class="info-row"><span class="label">Mã nhân viên:</span> <span class="value">%s</span></div>
                                <div class="info-row"><span class="label">Tên đăng nhập:</span> <span class="value">%s</span></div>
                                <div class="info-row"><span class="label">Email:</span> <span class="value">%s</span></div>
                                <div class="info-row"><span class="label">Vai trò:</span> <span class="value">%s</span></div>
                            </div>

                            <div class="password-box">
                                <p style="margin: 0 0 10px 0; color: #856404;">MẬT KHẨU CỦA BẠN</p>
                                <div class="password">%s</div>
                            </div>

                            <div class="warning">
                                <strong>⚠️ Lưu ý quan trọng:</strong>
                                <ul style="margin: 10px 0; padding-left: 20px;">
                                    <li>Vui lòng đổi mật khẩu ngay sau khi đăng nhập lần đầu</li>
                                    <li>Không chia sẻ mật khẩu này với bất kỳ ai</li>
                                    <li>Nếu bạn không yêu cầu tài khoản này, vui lòng liên hệ quản trị viên</li>
                                </ul>
                            </div>
                        </div>
                        <div class="footer">
                            <p><strong>Phòng Khám Đa Khoa FBT</strong></p>
                            <p>© 2026 Clinic Management System</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(
                        user.getFullName(),
                        user.getStaffId(),
                        user.getUsername(),
                        user.getEmail(),
                        rolesDisplay,
                        rawPassword);
    }

    /**
     * Convert User entity sang UserSummary DTO (thông tin rút gọn).
     * Dùng cho danh sách users.
     * 
     * @param user - User entity từ database
     * @return UserSummary - DTO thông tin rút gọn
     */
    private UserSummary convertToUserSummary(User user) {
        Doctor doctor = user.getDoctor();
        Department effectiveDepartment = resolveDoctorDepartment(doctor);

        return UserSummary.builder()
                .userId(user.getUserId())
                .staffId(user.getStaffId())
                .patientId(user.getPatient() != null ? user.getPatient().getPatientId() : null)
            .doctorId(doctor != null ? doctor.getDoctorId() : null)
            .roomId(doctor != null && doctor.getRoom() != null
                ? doctor.getRoom().getRoomId() : null)
            .deptId(effectiveDepartment != null ? effectiveDepartment.getDeptId() : null)
            .deptName(effectiveDepartment != null ? effectiveDepartment.getDeptName() : null)
            .deptType(effectiveDepartment != null && effectiveDepartment.getDeptType() != null
                ? effectiveDepartment.getDeptType().name() : null)
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .cccd(user.getCccd())
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet()))
                .build();
    }

    private Department resolveDoctorDepartment(Doctor doctor) {
        if (doctor == null) {
            return null;
        }

        if (doctor.getDepartment() != null) {
            return doctor.getDepartment();
        }

        if (doctor.getRoom() != null) {
            return doctor.getRoom().getDepartment();
        }

        return null;
    }

    /**
     * Convert User entity sang UserDetailResponse DTO (thông tin chi tiết).
     * Dùng cho xem chi tiết, tạo mới, cập nhật user.
     * 
     * @param user - User entity từ database
     * @return UserDetailResponse - DTO thông tin chi tiết
     */
    private UserDetailResponse convertToUserDetailResponse(User user) {
        return UserDetailResponse.fromEntity(user);
    }

    private MedicalProfilePayload toMedicalProfilePayload(CreateUserRequest request) {
        return new MedicalProfilePayload(
                request.getSpecialization(),
                request.getPosition(),
                request.getDegree(),
                request.getLicenseNumber(),
                request.getLicenseDate(),
                request.getLicensePlace(),
                request.getSchool(),
                request.getGraduationYear(),
                request.getExperienceYears(),
                request.getPreviousWorkplace(),
                request.getBio(),
                request.getSupportSkills());
    }

    private MedicalProfilePayload toMedicalProfilePayload(UpdateUserRequest request) {
        return new MedicalProfilePayload(
                request.getSpecialization(),
                request.getPosition(),
                request.getDegree(),
                request.getLicenseNumber(),
                request.getLicenseDate(),
                request.getLicensePlace(),
                request.getSchool(),
                request.getGraduationYear(),
                request.getExperienceYears(),
                request.getPreviousWorkplace(),
                request.getBio(),
                request.getSupportSkills());
    }

    private void applyMedicalProfileDetails(User user, MedicalProfilePayload payload) {
        if (payload == null || user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return;
        }

        Set<String> currentRoleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());

        if (currentRoleNames.stream().anyMatch(DOCTOR_ROLE_NAMES::contains) && user.getDoctor() != null) {
            Doctor doctor = user.getDoctor();
            if (payload.specialization() != null) {
                doctor.setSpecialization(normalizeOptionalText(payload.specialization()));
            }
            if (payload.position() != null) {
                doctor.setPosition(normalizeOptionalText(payload.position()));
            }
            if (payload.degree() != null) {
                doctor.setDegree(normalizeOptionalText(payload.degree()));
            }
            if (payload.licenseNumber() != null) {
                doctor.setLicenseNumber(normalizeOptionalText(payload.licenseNumber()));
            }
            if (payload.licenseDate() != null) {
                doctor.setCertificationIssueDate(payload.licenseDate());
            }
            if (payload.licensePlace() != null) {
                doctor.setCertificationPlace(normalizeOptionalText(payload.licensePlace()));
            }
            if (payload.school() != null) {
                doctor.setTrainingPlace(normalizeOptionalText(payload.school()));
            }
            if (payload.graduationYear() != null) {
                doctor.setGraduationYear(normalizeOptionalText(payload.graduationYear()));
            }
            if (payload.experienceYears() != null) {
                doctor.setExperienceYears(payload.experienceYears());
            }
            if (payload.previousWorkplace() != null) {
                doctor.setPreviousWorkplace(normalizeOptionalText(payload.previousWorkplace()));
            }
            if (payload.bio() != null) {
                doctor.setBio(normalizeOptionalText(payload.bio()));
            }
            Doctor savedDoctor = doctorRepository.save(doctor);
            user.setDoctor(savedDoctor);
        }

        if (currentRoleNames.contains(NURSE_ROLE_NAME) && user.getNurse() != null) {
            Nurse nurse = user.getNurse();
            if (payload.position() != null) {
                nurse.setPosition(normalizeOptionalText(payload.position()));
            }
            if (payload.degree() != null) {
                nurse.setEducationLevel(normalizeOptionalText(payload.degree()));
            }
            if (payload.licenseNumber() != null) {
                nurse.setCertificationNumber(normalizeOptionalText(payload.licenseNumber()));
            }
            if (payload.licenseDate() != null) {
                nurse.setCertificationIssueDate(payload.licenseDate());
            }
            if (payload.licensePlace() != null) {
                nurse.setCertificationPlace(normalizeOptionalText(payload.licensePlace()));
            }
            if (payload.school() != null) {
                nurse.setTrainingPlace(normalizeOptionalText(payload.school()));
            }
            if (payload.graduationYear() != null) {
                nurse.setGraduationYear(normalizeOptionalText(payload.graduationYear()));
            }
            if (payload.experienceYears() != null) {
                nurse.setYearsOfExperience(payload.experienceYears());
            }
            if (payload.previousWorkplace() != null) {
                nurse.setPreviousWorkplaces(normalizeOptionalText(payload.previousWorkplace()));
            }
            if (payload.supportSkills() != null) {
                nurse.setSupportSkills(normalizeOptionalText(payload.supportSkills()));
            }
            Nurse savedNurse = nurseRepository.save(nurse);
            user.setNurse(savedNurse);
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record MedicalProfilePayload(
            String specialization,
            String position,
            String degree,
            String licenseNumber,
            java.time.LocalDate licenseDate,
            String licensePlace,
            String school,
            String graduationYear,
            Integer experienceYears,
            String previousWorkplace,
            String bio,
            String supportSkills) {
    }
}
