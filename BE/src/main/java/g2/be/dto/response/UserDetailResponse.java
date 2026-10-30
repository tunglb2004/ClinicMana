package g2.be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Nurse;
import g2.be.entity.Patient;
import g2.be.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về thông tin chi tiết của 1 tài khoản nhân viên.
 * Dùng trong API: GET /api/admin/users/{id}, POST /api/admin/users, PUT
 * /api/admin/users/{id}
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {

    /** ID của user */
    private Long userId;

    /** Mã nhân viên (VD: NV-A1B2C3D4) */
    private String staffId;

    /** Tên đăng nhập */
    private String username;

    /** Email */
    private String email;

    /** Họ tên đầy đủ */
    private String fullName;

    /** Số CCCD */
    private String cccd;

    /** Giới tính: MALE, FEMALE, OTHER */
    private String gender;

    /** Số điện thoại */
    private String phoneNumber;

    /** URL ảnh đại diện */
    private String avatarUrl;

    /** Trạng thái tài khoản: true = Active, false = Locked */
    private Boolean isActive;

    /** Đã bị xóa (soft delete): true = đã xóa */
    private Boolean isDeleted;

    /** Danh sách tên roles */
    private Set<String> roles;

    private String patientId;

    private String doctorId;

    private String nurseId;

    private String roomId;

    private Integer deptId;

    private String deptName;

    private String deptType;

    private LocalDate dateOfBirth;

    private String bloodType;

    private String healthInsuranceNumber;

    private String allergies;

    private String address;

    private BigDecimal height;

    private BigDecimal weight;

    private String backgroundDisease;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String emergencyContactRelationship;

    private String specialization;

    private String position;

    private String degree;

    private String licenseNumber;

    private LocalDate licenseDate;

    private String licensePlace;

    private String school;

    private String graduationYear;

    private Integer experienceYears;

    private String previousWorkplace;

    private String bio;

    private String supportSkills;

    /** Thời gian tạo tài khoản */
    private LocalDateTime createdAt;

    /** Thời gian cập nhật cuối */
    private LocalDateTime updatedAt;

    /**
     * Static method để convert từ Entity sang DTO
     */
    public static UserDetailResponse fromEntity(User user) {
        Patient patient = user.getPatient();
        Doctor doctor = user.getDoctor();
        Nurse nurse = user.getNurse();
        Department effectiveDepartment = resolveEffectiveDepartment(doctor, nurse);

        return UserDetailResponse.builder()
                .userId(user.getUserId())
                .staffId(user.getStaffId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .cccd(user.getCccd())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .roles(user.getRoles().stream()
                        .map(role -> role.getRoleName())
                        .collect(Collectors.toSet()))
                .patientId(patient != null ? patient.getPatientId() : null)
                .doctorId(doctor != null ? doctor.getDoctorId() : null)
                .nurseId(nurse != null ? nurse.getNurseId() : null)
                .roomId(doctor != null && doctor.getRoom() != null ? doctor.getRoom().getRoomId() : null)
                .deptId(effectiveDepartment != null ? effectiveDepartment.getDeptId() : null)
                .deptName(effectiveDepartment != null ? effectiveDepartment.getDeptName() : null)
                .deptType(effectiveDepartment != null && effectiveDepartment.getDeptType() != null
                    ? effectiveDepartment.getDeptType().name()
                    : null)
                .dateOfBirth(patient != null ? patient.getDateOfBirth() : null)
                .bloodType(patient != null ? patient.getBloodType() : null)
                .healthInsuranceNumber(patient != null ? patient.getHealthInsuranceNumber() : null)
                .allergies(patient != null ? patient.getAllergies() : null)
                .address(patient != null ? patient.getAddress() : null)
                .height(patient != null ? patient.getHeight() : null)
                .weight(patient != null ? patient.getWeight() : null)
                .backgroundDisease(patient != null ? patient.getBackgroundDisease() : null)
                .emergencyContactName(patient != null ? patient.getEmergencyContactName() : null)
                .emergencyContactPhone(patient != null ? patient.getEmergencyContactPhone() : null)
                .emergencyContactRelationship(patient != null ? patient.getEmergencyContactRelationship() : null)
                .specialization(doctor != null ? doctor.getSpecialization() : null)
                .position(doctor != null ? doctor.getPosition() : nurse != null ? nurse.getPosition() : null)
                .degree(doctor != null ? doctor.getDegree() : nurse != null ? nurse.getEducationLevel() : null)
                .licenseNumber(doctor != null ? doctor.getLicenseNumber()
                    : nurse != null ? nurse.getCertificationNumber() : null)
                .licenseDate(doctor != null ? doctor.getCertificationIssueDate()
                    : nurse != null ? nurse.getCertificationIssueDate() : null)
                .licensePlace(doctor != null ? doctor.getCertificationPlace()
                    : nurse != null ? nurse.getCertificationPlace() : null)
                .school(doctor != null ? doctor.getTrainingPlace() : nurse != null ? nurse.getTrainingPlace() : null)
                .graduationYear(doctor != null ? doctor.getGraduationYear()
                    : nurse != null ? nurse.getGraduationYear() : null)
                .experienceYears(doctor != null ? doctor.getExperienceYears()
                    : nurse != null ? nurse.getYearsOfExperience() : null)
                .previousWorkplace(doctor != null ? doctor.getPreviousWorkplace()
                    : nurse != null ? nurse.getPreviousWorkplaces() : null)
                .bio(doctor != null ? doctor.getBio() : null)
                .supportSkills(nurse != null ? nurse.getSupportSkills() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

            private static Department resolveEffectiveDepartment(Doctor doctor, Nurse nurse) {
            if (doctor != null) {
                if (doctor.getDepartment() != null) {
                return doctor.getDepartment();
                }

                if (doctor.getRoom() != null) {
                return doctor.getRoom().getDepartment();
                }
            }

            if (nurse != null) {
                return nurse.getDepartment();
            }

            return null;
            }
}
