package g2.be.dto.response;

import g2.be.entity.Patient;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private String patientId;
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String cccd;
    private String gender;
    private String avatarUrl;
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
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PatientResponse fromEntity(Patient patient) {
        return PatientResponse.builder()
                .patientId(patient.getPatientId())
                .userId(patient.getUser().getUserId())
                .fullName(patient.getUser().getFullName())
                .email(patient.getUser().getEmail())
                .phoneNumber(patient.getUser().getPhoneNumber())
                .cccd(patient.getUser().getCccd())
                .gender(patient.getUser().getGender() != null ? patient.getUser().getGender().name() : null)
                .avatarUrl(patient.getUser().getAvatarUrl())
                .dateOfBirth(patient.getDateOfBirth())
                .bloodType(patient.getBloodType())
                .healthInsuranceNumber(patient.getHealthInsuranceNumber())
                .allergies(patient.getAllergies())
                .address(patient.getAddress())
                .height(patient.getHeight())
                .weight(patient.getWeight())
                .backgroundDisease(patient.getBackgroundDisease())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .emergencyContactRelationship(patient.getEmergencyContactRelationship())
                .isActive(patient.getUser().getIsActive())
                .createdAt(patient.getUser().getCreatedAt())
                .updatedAt(patient.getUser().getUpdatedAt())
                .build();
    }
}
