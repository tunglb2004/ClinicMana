package g2.be.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g2.be.dto.request.CreatePatientRequest;
import g2.be.dto.request.UpdatePatientRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.UserDetailResponse;
import g2.be.entity.Patient;
import g2.be.entity.Role;
import g2.be.entity.User;
import g2.be.entity.User.Gender;
import g2.be.repository.PatientRepository;
import g2.be.repository.RoleRepository;
import g2.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientManagementService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private static final String ROLE_PATIENT = "ROLE_PATIENT";

    /**
     * Lấy danh sách tất cả bệnh nhân với phân trang và tìm kiếm
     * Chỉ lấy các user có role PATIENT
     */
    @Transactional(readOnly = true)
    public PageResponse<UserDetailResponse> getAllPatients(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String keyword,
            Boolean isActive) {

        log.info("Getting all patients - page: {}, size: {}, sortBy: {}, keyword: {}",
                page, size, sortBy, keyword);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> patientPage;

        if (keyword != null && !keyword.isEmpty()) {
            // Tìm kiếm theo keyword và role PATIENT
            patientPage = userRepository.findByRoleAndKeyword(ROLE_PATIENT, keyword, false, pageable);
        } else if (isActive != null) {
            // Lọc theo trạng thái active và role PATIENT
            patientPage = userRepository.findByRoleAndIsActive(ROLE_PATIENT, isActive, false, pageable);
        } else {
            // Lấy tất cả patient chưa bị xóa
            patientPage = userRepository.findByRoleAndIsDeleted(ROLE_PATIENT, false, pageable);
        }

        List<UserDetailResponse> content = patientPage.getContent().stream()
                .map(UserDetailResponse::fromEntity)
                .collect(Collectors.toList());

        log.info("Found {} patients", patientPage.getTotalElements());

        return PageResponse.<UserDetailResponse>builder()
                .content(content)
                .pageNumber(patientPage.getNumber())
                .pageSize(patientPage.getSize())
                .totalElements(patientPage.getTotalElements())
                .totalPages(patientPage.getTotalPages())
                .last(patientPage.isLast())
                .first(patientPage.isFirst())
                .build();
    }

    /**
     * Lấy thông tin chi tiết một bệnh nhân
     * Kiểm tra user phải có role PATIENT
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getPatientById(Long userId) {
        log.info("Getting patient by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân với ID: " + userId));

        if (user.getIsDeleted()) {
            throw new RuntimeException("Bệnh nhân đã bị xóa");
        }

        // Kiểm tra user có role PATIENT không
        boolean isPatient = user.getRoles().stream()
                .anyMatch(role -> ROLE_PATIENT.equals(role.getRoleName()));

        if (!isPatient) {
            throw new RuntimeException("User này không phải là bệnh nhân");
        }

        return UserDetailResponse.fromEntity(user);
    }

    /**
     * Tạo mới bệnh nhân
     */
    @Transactional
    public UserDetailResponse createPatient(CreatePatientRequest request) {
        log.info("Creating new patient with email: {}", request.getEmail());

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại: " + request.getEmail());
        }

        // Kiểm tra số điện thoại đã tồn tại
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Số điện thoại đã tồn tại: " + request.getPhoneNumber());
        }

        // Kiểm tra CCCD đã tồn tại (nếu có)
        if (request.getCccd() != null && !request.getCccd().isEmpty()
                && userRepository.existsByCccd(request.getCccd())) {
            throw new RuntimeException("CCCD đã tồn tại: " + request.getCccd());
        }

        // Tạo username từ email (phần trước @)
        String username = request.getEmail().split("@")[0];

        // Nếu username đã tồn tại, thêm số random
        if (userRepository.existsByUsername(username)) {
            username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
        }

        // Tạo mật khẩu random
        String rawPassword = generateRandomPassword();

        // Lấy role PATIENT
        Role patientRole = roleRepository.findByRoleName(ROLE_PATIENT)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role ROLE_PATIENT"));

        Set<Role> roles = new HashSet<>();
        roles.add(patientRole);

        // Tạo User mới
        User user = User.builder()
                .username(username)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .cccd(request.getCccd())
                .gender(request.getGender())
                .isActive(true)
                .isDeleted(false)
                .mustChangePassword(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);

        // Tạo Patient profile
        Patient patient = Patient.builder()
                .patientId(generateUniquePatientId())
                .user(savedUser)
                .dateOfBirth(request.getDateOfBirth())
                .bloodType(request.getBloodType())
                .healthInsuranceNumber(request.getHealthInsuranceNumber())
                .allergies(request.getAllergies())
                .address(request.getAddress())
                .height(request.getHeight())
                .weight(request.getWeight())
                .backgroundDisease(request.getBackgroundDisease())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .emergencyContactRelationship(request.getEmergencyContactRelationship())
                .build();

        Patient savedPatient = patientRepository.save(patient);
        savedUser.setPatient(savedPatient);

        log.info("Patient created successfully - UserId: {}, PatientId: {}",
                savedUser.getUserId(), savedPatient.getPatientId());

        // Gửi email thông tin tài khoản cho patient
        try {
            emailService.sendAccountCreationEmail(
                    request.getEmail(),
                    request.getFullName(),
                    username,
                    rawPassword);
            log.info("Account creation email sent successfully to: {}", request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send account creation email to: {}", request.getEmail(), e);
            // Không throw exception, chỉ log lỗi để không ảnh hưởng việc tạo patient
        }

        return UserDetailResponse.fromEntity(savedUser);
    }

    /**
     * Cập nhật thông tin bệnh nhân
     */
    @Transactional
    public UserDetailResponse updatePatient(Long userId, UpdatePatientRequest request) {
        log.info("Updating patient with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân với ID: " + userId));

        if (user.getIsDeleted()) {
            throw new RuntimeException("Không thể cập nhật bệnh nhân đã bị xóa");
        }

        // Kiểm tra user có role PATIENT không
        boolean isPatient = user.getRoles().stream()
                .anyMatch(role -> ROLE_PATIENT.equals(role.getRoleName()));

        if (!isPatient) {
            throw new RuntimeException("User này không phải là bệnh nhân");
        }

        Patient patient = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ bệnh nhân cho user ID: " + userId));

        // Cập nhật email (kiểm tra trùng)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email đã tồn tại: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        // Cập nhật số điện thoại (kiểm tra trùng)
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new RuntimeException("Số điện thoại đã tồn tại: " + request.getPhoneNumber());
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        // Cập nhật CCCD (kiểm tra trùng)
        if (request.getCccd() != null && !request.getCccd().equals(user.getCccd())) {
            if (userRepository.existsByCccd(request.getCccd())) {
                throw new RuntimeException("CCCD đã tồn tại: " + request.getCccd());
            }
            user.setCccd(request.getCccd());
        }

        // Cập nhật các trường khác
        if (request.getFullName() != null) {
            user.setFullName(normalizeOptionalText(request.getFullName()));
        }
        if (request.getGender() != null) {
            user.setGender(parseGender(request.getGender()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getAddress() != null) {
            patient.setAddress(normalizeOptionalText(request.getAddress()));
        }
        if (request.getDateOfBirth() != null) {
            patient.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getBloodType() != null) {
            patient.setBloodType(normalizeOptionalText(request.getBloodType()));
        }
        if (request.getHealthInsuranceNumber() != null) {
            patient.setHealthInsuranceNumber(normalizeOptionalText(request.getHealthInsuranceNumber()));
        }
        if (request.getAllergies() != null) {
            patient.setAllergies(normalizeOptionalText(request.getAllergies()));
        }
        if (request.getHeight() != null) {
            patient.setHeight(request.getHeight());
        }
        if (request.getWeight() != null) {
            patient.setWeight(request.getWeight());
        }
        if (request.getBackgroundDisease() != null) {
            patient.setBackgroundDisease(normalizeOptionalText(request.getBackgroundDisease()));
        }
        if (request.getEmergencyContactName() != null) {
            patient.setEmergencyContactName(normalizeOptionalText(request.getEmergencyContactName()));
        }
        if (request.getEmergencyContactPhone() != null) {
            patient.setEmergencyContactPhone(normalizeOptionalText(request.getEmergencyContactPhone()));
        }
        if (request.getEmergencyContactRelationship() != null) {
            patient.setEmergencyContactRelationship(normalizeOptionalText(request.getEmergencyContactRelationship()));
        }

        User updatedUser = userRepository.save(user);
        Patient savedPatient = patientRepository.save(patient);
        updatedUser.setPatient(savedPatient);
        log.info("Patient updated successfully with ID: {}", updatedUser.getUserId());

        return UserDetailResponse.fromEntity(updatedUser);
    }

    /**
     * Xóa bệnh nhân (soft delete)
     */
    @Transactional
    public MessageResponse deletePatient(Long userId) {
        log.info("Deleting patient with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân với ID: " + userId));

        if (user.getIsDeleted()) {
            throw new RuntimeException("Bệnh nhân đã bị xóa trước đó");
        }

        // Kiểm tra user có role PATIENT không
        boolean isPatient = user.getRoles().stream()
                .anyMatch(role -> ROLE_PATIENT.equals(role.getRoleName()));

        if (!isPatient) {
            throw new RuntimeException("User này không phải là bệnh nhân");
        }

        // Soft delete: đánh dấu isDeleted = true
        user.setIsDeleted(true);
        user.setIsActive(false);
        userRepository.save(user);

        log.info("Patient deleted successfully with ID: {}", userId);

        return new MessageResponse("Xóa bệnh nhân thành công");
    }

    /**
     * Khóa tài khoản bệnh nhân
     */
    @Transactional
    public MessageResponse lockPatient(Long userId) {
        log.info("Locking patient account with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân với ID: " + userId));

        if (user.getIsDeleted()) {
            throw new RuntimeException("Không thể khóa bệnh nhân đã bị xóa");
        }

        // Kiểm tra user có role PATIENT không
        boolean isPatient = user.getRoles().stream()
                .anyMatch(role -> ROLE_PATIENT.equals(role.getRoleName()));

        if (!isPatient) {
            throw new RuntimeException("User này không phải là bệnh nhân");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã bị khóa trước đó");
        }

        user.setIsActive(false);
        userRepository.save(user);

        log.info("Patient account locked successfully with ID: {}", userId);

        return new MessageResponse("Khóa tài khoản bệnh nhân thành công");
    }

    /**
     * Mở khóa tài khoản bệnh nhân
     */
    @Transactional
    public MessageResponse unlockPatient(Long userId) {
        log.info("Unlocking patient account with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân với ID: " + userId));

        if (user.getIsDeleted()) {
            throw new RuntimeException("Không thể mở khóa bệnh nhân đã bị xóa");
        }

        // Kiểm tra user có role PATIENT không
        boolean isPatient = user.getRoles().stream()
                .anyMatch(role -> ROLE_PATIENT.equals(role.getRoleName()));

        if (!isPatient) {
            throw new RuntimeException("User này không phải là bệnh nhân");
        }

        if (user.getIsActive()) {
            throw new RuntimeException("Tài khoản đang hoạt động bình thường");
        }

        user.setIsActive(true);
        userRepository.save(user);

        log.info("Patient account unlocked successfully with ID: {}", userId);

        return new MessageResponse("Mở khóa tài khoản bệnh nhân thành công");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Tự động tạo mã bệnh nhân duy nhất.
     * Format: BN-XXXXXXXX (8 ký tự hex viết hoa)
     */
    private String generateUniquePatientId() {
        String patientId;
        do {
            patientId = "BN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (patientRepository.existsByPatientId(patientId));
        return patientId;
    }

    /**
     * Tự động tạo mật khẩu ngẫu nhiên an toàn.
     * Format: 8 ký tự bao gồm chữ hoa, chữ thường và số
     */
    private String generateRandomPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String allChars = upperCase + lowerCase + digits;

        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder();

        // Đảm bảo có ít nhất 1 ký tự mỗi loại
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));

        // Thêm 5 ký tự ngẫu nhiên nữa (tổng 8 ký tự)
        for (int i = 0; i < 5; i++) {
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

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Gender parseGender(String gender) {
        String normalized = normalizeOptionalText(gender);
        if (normalized == null) {
            return null;
        }

        try {
            return Gender.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Giới tính không hợp lệ: " + gender);
        }
    }
}
