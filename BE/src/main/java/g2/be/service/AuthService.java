package g2.be.service;

import g2.be.dto.request.LoginRequest;
import g2.be.dto.request.RegisterRequest;
import g2.be.dto.response.LoginResponse;
import g2.be.dto.response.RefreshTokenResponse;
import g2.be.dto.response.UserInfoResponse;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.entity.Doctor;
import g2.be.entity.Nurse;
import g2.be.entity.Patient;
import g2.be.entity.RefreshToken;
import g2.be.entity.Role;
import g2.be.entity.User;
import g2.be.entity.VerificationToken;
import g2.be.repository.PatientRepository;
import g2.be.repository.RoleRepository;
import g2.be.repository.UserRepository;
import g2.be.security.JwtUtil;
import g2.be.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

        private static final Set<String> DOCTOR_ROLE_NAMES = Set.of(
                        "ROLE_DOCTOR",
                        "ROLE_CLINICALDOCTOR",
                        "ROLE_CLINICAL_DOCTOR",
                        "ROLE_SPECIALISTDOCTOR",
                        "ROLE_SPECIALIST_DOCTOR");

        private final AuthenticationManager authenticationManager;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PatientRepository patientRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final RefreshTokenService refreshTokenService;
        private final VerificationTokenService verificationTokenService;

        @Transactional
        public LoginResponse login(LoginRequest request) {
                String identifier = request.getIdentifier().trim();
                // Tìm user theo email hoặc số điện thoại
                User user = userRepository.findByEmailOrPhoneNumber(identifier)
                                .orElseThrow(() -> new AppException(ErrorCode.LOGIN_INVALID_CREDENTIALS));

                if (Boolean.TRUE.equals(user.getIsDeleted()) || !Boolean.TRUE.equals(user.getIsActive())) {
                        throw new AppException(ErrorCode.LOGIN_ACCOUNT_LOCKED);
                }

                // Xác thực với username thực của user
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Tạo access token
                String accessToken = jwtUtil.generateAccessToken(authentication);

                // Lấy thông tin user
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

                // Tạo refresh token
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

                // Lấy roles
                Set<String> roles = userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toSet());

                LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                                .id(user.getUserId())
                                .fullName(user.getFullName())
                                .roles(roles)
                                .avatar(user.getAvatarUrl())
                                .mustChangePassword(user.getMustChangePassword())
                                .build();

                return LoginResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken.getToken())
                                .user(userInfo)
                                .build();
        }

        @Transactional
        public void register(RegisterRequest request) {
                String email = request.getEmail().trim();
                String phone = request.getPhone().trim();
                String fullName = request.getFullName().trim();
                String cccd = request.getCccd() == null ? "" : request.getCccd().trim();
                if (cccd.isBlank()) {
                        throw new AppException(ErrorCode.UPDATE_PROFILE_MISSING_FIELDS);
                }

                // Bước 1: Xác thực OTP trước khi cho phép đăng ký
                verificationTokenService.verifyAndMarkAsUsed(
                                email,
                                request.getOtpCode(),
                                VerificationToken.TokenType.REGISTER);

                // Bước 2: Kiểm tra trùng lặp
                if (userRepository.existsByEmail(email)) {
                        throw new AppException(ErrorCode.REGISTER_EMAIL_EXISTS);
                }

                if (userRepository.existsByPhoneNumber(phone)) {
                        throw new AppException(ErrorCode.REGISTER_PHONE_EXISTS);
                }

                if (userRepository.existsByCccd(cccd)) {
                        throw new AppException(ErrorCode.ACCOUNT_DUPLICATE_EMAIL_CCCD);
                }

                // Tạo username từ email (phần trước @)
                String username = email.split("@")[0];

                // Nếu username đã tồn tại, thêm số random
                if (userRepository.existsByUsername(username)) {
                        username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
                }

                // Parse gender
                User.Gender gender;
                try {
                        gender = User.Gender.valueOf(request.getGender().toUpperCase());
                } catch (Exception e) {
                        gender = User.Gender.OTHER;
                }

                // Tạo User mới
                User user = User.builder()
                                .username(username)
                                .email(email)
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .fullName(fullName)
                                .phoneNumber(phone)
                                .cccd(cccd)
                                .gender(gender)
                                .isActive(true)
                                .isDeleted(false)
                                .build();

                // Gán role ROLE_PATIENT
                Role patientRole = roleRepository.findByRoleName("ROLE_PATIENT")
                                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

                Set<Role> roles = new HashSet<>();
                roles.add(patientRole);
                user.setRoles(roles);

                // Lưu user
                User savedUser = userRepository.save(user);

                // Tạo Profile Patient rỗng
                Patient patient = Patient.builder()
                                .user(savedUser)
                                .patientId(generateUniquePatientId())
                                .build();

                patientRepository.save(patient);
        }

        private String generateUniquePatientId() {
                String patientId;
                do {
                        patientId = "BN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                } while (patientRepository.existsByPatientId(patientId));
                return patientId;
        }

        @Transactional
        public RefreshTokenResponse refreshToken(String refreshToken) {
                RefreshToken token = refreshTokenService.findByToken(refreshToken)
                                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

                // Verify token chưa hết hạn
                refreshTokenService.verifyExpiration(token);

                User user = token.getUser();

                // Tạo access token mới
                String roles = user.getRoles().stream()
                                .map(Role::getRoleName)
                                .collect(Collectors.joining(","));

                String newAccessToken = jwtUtil.generateAccessTokenFromUsername(
                                user.getUsername(),
                                user.getUserId(),
                                roles);

                // Tạo refresh token mới
                RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getUserId());

                return RefreshTokenResponse.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(newRefreshToken.getToken())
                                .build();
        }

        @Transactional
        public void logout(String refreshToken) {
                refreshTokenService.deleteByToken(refreshToken);
        }

        @Transactional(readOnly = true)
        public UserInfoResponse getCurrentUserInfo(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

                // Lấy roles
                Set<String> roles = user.getRoles().stream()
                                .map(Role::getRoleName)
                                .collect(Collectors.toSet());

                UserInfoResponse response = UserInfoResponse.builder()
                                .id(user.getUserId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .cccd(user.getCccd())
                                .gender(user.getGender() != null ? user.getGender().name() : null)
                                .phoneNumber(user.getPhoneNumber())
                                .avatarUrl(user.getAvatarUrl())
                                .roles(roles)
                                .build();

                // Check nếu là Doctor
                if (hasDoctorRole(roles) && user.getDoctor() != null) {
                        Doctor doctor = user.getDoctor();
                        UserInfoResponse.DoctorProfile.DepartmentInfo deptInfo = null;

                        if (doctor.getDepartment() != null) {
                                deptInfo = UserInfoResponse.DoctorProfile.DepartmentInfo.builder()
                                                .deptId(doctor.getDepartment().getDeptId())
                                                .deptName(doctor.getDepartment().getDeptName())
                                                .deptType(doctor.getDepartment().getDeptType().name())
                                                .build();
                        }

                        UserInfoResponse.DoctorProfile.RoomInfo roomInfo = null;
                        if (doctor.getRoom() != null) {
                                roomInfo = UserInfoResponse.DoctorProfile.RoomInfo.builder()
                                                .roomId(doctor.getRoom().getRoomId())
                                                .roomName(doctor.getRoom().getRoomName())
                                                .roomType(doctor.getRoom().getRoomType())
                                                .location(doctor.getRoom().getLocation())
                                                .build();
                        }

                        UserInfoResponse.DoctorProfile doctorProfile = UserInfoResponse.DoctorProfile.builder()
                                        .doctorId(doctor.getDoctorId())
                                        .specialization(doctor.getSpecialization())
                                        .degree(doctor.getDegree())
                                        .licenseNumber(doctor.getLicenseNumber())
                                        .isHeadOfDept(doctor.getIsHeadOfDept())
                                        .room(roomInfo)
                                        .bio(doctor.getBio())
                                        .department(deptInfo)
                                        .position(doctor.getPosition())
                                        .certificationIssueDate(doctor.getCertificationIssueDate() != null
                                                        ? doctor.getCertificationIssueDate().toString()
                                                        : null)
                                        .certificationPlace(doctor.getCertificationPlace())
                                        .certificationFileUrl(doctor.getCertificationFileUrl())
                                        .trainingPlace(doctor.getTrainingPlace())
                                        .graduationYear(doctor.getGraduationYear())
                                        .degreeFileUrl(doctor.getDegreeFileUrl())
                                        .experienceYears(doctor.getExperienceYears())
                                        .previousWorkplace(doctor.getPreviousWorkplace())
                                        .build();

                        response.setProfile(doctorProfile);
                }
                // Check nếu là Nurse
                else if (roles.contains("ROLE_NURSE") && user.getNurse() != null) {
                        Nurse nurse = user.getNurse();
                        UserInfoResponse.NurseProfile nurseProfile = UserInfoResponse.NurseProfile.builder()
                                        .nurseId(nurse.getNurseId())
                                        .certificationNumber(nurse.getCertificationNumber())
                                        .certificationIssueDate(nurse.getCertificationIssueDate() != null
                                                        ? nurse.getCertificationIssueDate().toString()
                                                        : null)
                                        .certificationPlace(nurse.getCertificationPlace())
                                        .certificationFileUrl(nurse.getCertificationFileUrl())
                                        .educationLevel(nurse.getEducationLevel())
                                        .trainingPlace(nurse.getTrainingPlace())
                                        .graduationYear(nurse.getGraduationYear())
                                        .degreeFileUrl(nurse.getDegreeFileUrl())
                                        .department(nurse.getDepartment() != null ? nurse.getDepartment().getDeptName() : null)
                                        .position(nurse.getPosition())
                                        .yearsOfExperience(nurse.getYearsOfExperience())
                                        .previousWorkplaces(nurse.getPreviousWorkplaces())
                                        .supportSkills(nurse.getSupportSkills())
                                        .build();
                        response.setProfile(nurseProfile);
                }
                // Check nếu là Patient (Fallback)
                else if (roles.contains("ROLE_PATIENT") && user.getPatient() != null) {
                        Patient patient = user.getPatient();
                        UserInfoResponse.PatientProfile patientProfile = UserInfoResponse.PatientProfile.builder()
                                        .patientId(patient.getPatientId())
                                        .dateOfBirth(patient.getDateOfBirth() != null
                                                        ? patient.getDateOfBirth().toString()
                                                        : null)
                                        .bloodType(patient.getBloodType())
                                        .healthInsuranceNumber(patient.getHealthInsuranceNumber())
                                        .allergies(patient.getAllergies())
                                        .address(patient.getAddress())
                                        .build();

                        response.setProfile(patientProfile);
                }

                return response;
        }

        private boolean hasDoctorRole(Set<String> roles) {
                return roles != null && roles.stream().anyMatch(DOCTOR_ROLE_NAMES::contains);
        }

        /**
         * Kiểm tra email hoặc số điện thoại đã tồn tại trong hệ thống chưa
         * @param email Email cần kiểm tra
         * @param phoneNumber Số điện thoại cần kiểm tra
         * @throws RuntimeException nếu email hoặc số điện thoại đã tồn tại
         */
        @Transactional(readOnly = true)
        public void checkDuplicateBeforeRegister(String email, String phoneNumber) {
                if (userRepository.existsByEmail(email)) {
                        throw new RuntimeException("Email đã tồn tại trong hệ thống");
                }
                if (userRepository.existsByPhoneNumber(phoneNumber)) {
                        throw new RuntimeException("Số điện thoại đã tồn tại trong hệ thống");
                }
        }

        /**
         * Đổi mật khẩu cho user
         * @param userId ID của user
         * @param request Request chứa mật khẩu cũ và mật khẩu mới
         * @throws RuntimeException nếu user không tồn tại hoặc mật khẩu cũ không đúng
         */
        @Transactional
        public void changePassword(Long userId, g2.be.dto.request.ChangePasswordRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

                // Kiểm tra mật khẩu cũ
                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                        throw new RuntimeException("Mật khẩu cũ không đúng");
                }

                // Kiểm tra mật khẩu mới không được trùng với mật khẩu cũ
                if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
                        throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu cũ");
                }

                // Cập nhật mật khẩu mới
                user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
                
                // Nếu user đang trong trạng thái phải đổi mật khẩu, set lại flag
                if (Boolean.TRUE.equals(user.getMustChangePassword())) {
                        user.setMustChangePassword(false);
                }

                userRepository.save(user);
        }
}
