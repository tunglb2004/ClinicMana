package g2.be.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String cccd;
    private String gender;
    private String phoneNumber;
    private String avatarUrl;
    private Boolean isActive;
    private Set<String> roles;
    private Object profile; // Can be DoctorProfile or PatientProfile

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorProfile {
        private String doctorId;
        private String specialization;
        private String degree;
        private String licenseNumber;
        private Boolean isHeadOfDept;
        private RoomInfo room;
        private String bio;

        private DepartmentInfo department;
        private String position;
        private String certificationIssueDate;
        private String certificationPlace;
        private String certificationFileUrl;
        private String trainingPlace;
        private String graduationYear;
        private String degreeFileUrl;
        private Integer experienceYears;
        private String previousWorkplace;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class DepartmentInfo {
            private Integer deptId;
            private String deptName;
            private String deptType;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class RoomInfo {
            private String roomId;
            private String roomName;
            private String roomType;
            private String location;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PatientProfile {
        private String patientId;
        private String dateOfBirth;
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
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NurseProfile {
        private String nurseId;
        private String certificationNumber;
        private String certificationIssueDate;
        private String certificationPlace;
        private String certificationFileUrl;
        private String educationLevel;
        private String trainingPlace;
        private String graduationYear;
        private String degreeFileUrl;
        private String department;
        private String position;
        private Integer yearsOfExperience;
        private String previousWorkplaces;
        private String supportSkills;
    }
}
