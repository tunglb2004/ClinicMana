package g2.be.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g2.be.dto.request.ChangePasswordRequest;
import g2.be.dto.request.ChangeEmailRequest;
import g2.be.dto.request.ResetPasswordRequest;
import g2.be.dto.request.UpdateProfileRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.UserInfoResponse;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.entity.User;
import g2.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import g2.be.entity.Doctor;
import g2.be.entity.Nurse;
import g2.be.entity.Patient;
import g2.be.entity.Room;
import g2.be.entity.StaffSchedule;
import g2.be.entity.VerificationToken;
import g2.be.repository.StaffScheduleRepository;
import g2.be.util.ClinicTime;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> DOCTOR_ROLE_NAMES = Set.of(
            "ROLE_DOCTOR",
            "ROLE_CLINICALDOCTOR",
            "ROLE_CLINICAL_DOCTOR",
            "ROLE_SPECIALISTDOCTOR",
            "ROLE_SPECIALIST_DOCTOR");

    private final UserRepository userRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Luu ma reset password tam thoi (trong production nen dung Redis hoac DB)
    private final Map<String, String> resetPasswordCodes = new HashMap<>();

    private final RefreshTokenService refreshTokenService;
    private final VerificationTokenService verificationTokenService;

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        UserInfoResponse response = UserInfoResponse.builder()
                .id(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .cccd(user.getCccd())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream()
                        .map(role -> role.getRoleName())
                        .collect(Collectors.toSet()))
                .build();

        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getRoleName())
                .collect(Collectors.toSet());

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

            UserInfoResponse.DoctorProfile.RoomInfo roomInfo = resolveCurrentDoctorRoomInfo(user, doctor);

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
                    .certificationIssueDate(
                            doctor.getCertificationIssueDate() != null ? doctor.getCertificationIssueDate().toString()
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
        } else if (roles.contains("ROLE_NURSE") && user.getNurse() != null) {
            Nurse nurse = user.getNurse();
            UserInfoResponse.NurseProfile nurseProfile = UserInfoResponse.NurseProfile.builder()
                    .nurseId(nurse.getNurseId())
                    .certificationNumber(nurse.getCertificationNumber())
                    .certificationIssueDate(
                            nurse.getCertificationIssueDate() != null ? nurse.getCertificationIssueDate().toString()
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
        } else if (user.getPatient() != null) {
            Patient p = user.getPatient();
            response.setProfile(UserInfoResponse.PatientProfile.builder()
                    .patientId(p.getPatientId())
                    .dateOfBirth(p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : null)
                    .bloodType(p.getBloodType())
                    .healthInsuranceNumber(p.getHealthInsuranceNumber())
                    .allergies(p.getAllergies())
                    .address(p.getAddress())
                    .height(p.getHeight())
                    .weight(p.getWeight())
                    .backgroundDisease(p.getBackgroundDisease())
                    .emergencyContactName(p.getEmergencyContactName())
                    .emergencyContactPhone(p.getEmergencyContactPhone())
                    .emergencyContactRelationship(p.getEmergencyContactRelationship())
                    .build());
        }

        return response;
    }

    @Transactional
    public UserInfoResponse updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return getUserInfo(userId);
    }

    @Transactional
    public UserInfoResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        // Cap nhat cac truong neu co gia tri moi
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getCccd() != null && !request.getCccd().isBlank()) {
            // Kiem tra CCCD da ton tai chua (tru user hien tai)
            if (userRepository.existsByCccdAndUserIdNot(request.getCccd(), userId)) {
                throw new AppException(ErrorCode.ACCOUNT_DUPLICATE_EMAIL_CCCD);
            }
            user.setCccd(request.getCccd());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            // Kiem tra so dien thoai da ton tai chua
            if (userRepository.existsByPhoneNumberAndUserIdNot(request.getPhoneNumber(), userId)) {
                throw new AppException(ErrorCode.REGISTER_PHONE_EXISTS);
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Cap nhat thong tin benh nhan neu co
        if (user.getPatient() != null) {
            Patient p = user.getPatient();
            if (request.getDateOfBirth() != null)
                p.setDateOfBirth(request.getDateOfBirth());
            if (request.getBloodType() != null)
                p.setBloodType(request.getBloodType());
            if (request.getHealthInsuranceNumber() != null)
                p.setHealthInsuranceNumber(request.getHealthInsuranceNumber());
            if (request.getAllergies() != null)
                p.setAllergies(request.getAllergies());
            if (request.getAddress() != null)
                p.setAddress(request.getAddress());

            if (request.getHeight() != null)
                p.setHeight(request.getHeight().setScale(2, RoundingMode.HALF_UP));
            if (request.getWeight() != null)
                p.setWeight(request.getWeight().setScale(2, RoundingMode.HALF_UP));
            if (request.getBackgroundDisease() != null)
                p.setBackgroundDisease(request.getBackgroundDisease());
            if (request.getEmergencyContactName() != null)
                p.setEmergencyContactName(request.getEmergencyContactName());
            if (request.getEmergencyContactPhone() != null)
                p.setEmergencyContactPhone(request.getEmergencyContactPhone());
            if (request.getEmergencyContactRelationship() != null)
                p.setEmergencyContactRelationship(request.getEmergencyContactRelationship());
        }

        if (user.getDoctor() != null) {
            Doctor doctor = user.getDoctor();
            if (request.getLicenseNumber() != null)
                doctor.setLicenseNumber(request.getLicenseNumber());
            if (request.getSpecialization() != null)
                doctor.setSpecialization(request.getSpecialization());
            if (request.getDegree() != null)
                doctor.setDegree(request.getDegree());
            if (request.getCertificationIssueDate() != null)
                doctor.setCertificationIssueDate(request.getCertificationIssueDate());
            if (request.getCertificationPlace() != null)
                doctor.setCertificationPlace(request.getCertificationPlace());
            if (request.getCertificationFileUrl() != null)
                doctor.setCertificationFileUrl(request.getCertificationFileUrl());
            if (request.getTrainingPlace() != null)
                doctor.setTrainingPlace(request.getTrainingPlace());
            if (request.getGraduationYear() != null)
                doctor.setGraduationYear(request.getGraduationYear());
            if (request.getDegreeFileUrl() != null)
                doctor.setDegreeFileUrl(request.getDegreeFileUrl());
            if (request.getExperienceYears() != null)
                doctor.setExperienceYears(request.getExperienceYears());
            if (request.getPreviousWorkplace() != null)
                doctor.setPreviousWorkplace(request.getPreviousWorkplace());
            if (request.getBio() != null)
                doctor.setBio(request.getBio());
            if (request.getPosition() != null)
                doctor.setPosition(request.getPosition());
        }

        userRepository.save(user);
        return getUserInfo(userId);
    }

    private UserInfoResponse.DoctorProfile.RoomInfo resolveCurrentDoctorRoomInfo(User user, Doctor doctor) {
        Room room = findTodayScheduledRoom(user.getUserId()).orElse(doctor.getRoom());
        if (room == null) {
            return null;
        }

        return UserInfoResponse.DoctorProfile.RoomInfo.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .roomType(room.getRoomType())
                .location(room.getLocation())
                .build();
    }

    private Optional<Room> findTodayScheduledRoom(Long userId) {
        List<StaffSchedule> todaySchedules = staffScheduleRepository
                .findByUser_UserIdAndWorkDate(userId, ClinicTime.today())
                .stream()
                .filter(schedule -> schedule.getRoom() != null)
                .filter(schedule -> schedule.getSlot() != null)
                .toList();

        if (todaySchedules.isEmpty()) {
            return Optional.empty();
        }

        LocalTime now = ClinicTime.now().toLocalTime();

        Optional<StaffSchedule> currentSchedule = todaySchedules.stream()
                .filter(schedule -> {
                    LocalTime startTime = schedule.getSlot().getStartTime();
                    LocalTime endTime = schedule.getSlot().getEndTime();
                    return startTime != null
                            && endTime != null
                            && !now.isBefore(startTime)
                            && !now.isAfter(endTime);
                })
                .min(Comparator.comparing(schedule -> schedule.getSlot().getStartTime()));

        if (currentSchedule.isPresent()) {
            return Optional.of(currentSchedule.get().getRoom());
        }

        return todaySchedules.stream()
                .filter(schedule -> schedule.getSlot().getStartTime() != null)
                .min(Comparator.comparing(schedule -> schedule.getSlot().getStartTime()))
                .map(StaffSchedule::getRoom);
    }

    private boolean hasDoctorRole(Set<String> roles) {
        return roles != null && roles.stream().anyMatch(DOCTOR_ROLE_NAMES::contains);
    }

    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        // Kiem tra mat khau cu
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.CHANGE_PASSWORD_INCORRECT);
        }

        // Kiem tra mat khau moi va xac nhan khop nhau
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_INCORRECT);
        }

        // Cap nhat mat khau moi
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // Reset flag bắt buộc đổi MK (nếu có)
        user.setMustChangePassword(false);

        userRepository.save(user);

        return new MessageResponse(ErrorCode.CHANGE_PASSWORD_SUCCESS.getMessage());
    }

    @Transactional
    public MessageResponse changeEmail(Long userId, ChangeEmailRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        // Xác thực OTP (OTP gửi về email MỚI của user)
        // Lưu ý: User cần request OTP loại CHANGE_EMAIL tới email MỚI trước khi gọi API
        // này
        verificationTokenService.verifyAndMarkAsUsed(
                request.getNewEmail(),
                request.getOtpCode(),
                VerificationToken.TokenType.CHANGE_EMAIL); // Kiem tra email moi da ton tai chua
        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new AppException(ErrorCode.REGISTER_EMAIL_EXISTS);
        }

        user.setEmail(request.getNewEmail());
        userRepository.save(user);

        return new MessageResponse(ErrorCode.UPDATE_EMAIL_SUCCESS.getMessage());
    }

    @Transactional
    public MessageResponse forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXIST));

        // Tao ma xac nhan 6 chu so
        String resetCode = String.format("%06d", new Random().nextInt(999999));

        // Luu ma reset (trong production nen luu vao Redis voi TTL hoac DB)
        resetPasswordCodes.put(email, resetCode);

        emailService.sendOtpEmail(email, resetCode, "RESET_PASSWORD");

        return new MessageResponse(ErrorCode.FORGOT_PASSWORD_OTP_SENT.getMessage());
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXIST));

        // Kiem tra ma xac nhan
        String storedCode = resetPasswordCodes.get(request.getEmail());
        if (storedCode == null || !storedCode.equals(request.getOtpCode())) {
            throw new AppException(ErrorCode.FORGOT_PASSWORD_INVALID_OTP);
        }

        // Cap nhat mat khau moi
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xoa ma reset da su dung
        resetPasswordCodes.remove(request.getEmail());

        return new MessageResponse(ErrorCode.CHANGE_PASSWORD_SUCCESS.getMessage());
    }
}
