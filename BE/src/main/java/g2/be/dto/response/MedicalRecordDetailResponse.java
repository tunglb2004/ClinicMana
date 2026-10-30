package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordDetailResponse {
    private String recordId;
    private String appointmentId;
    private String appointmentDate;
    private String appointmentTime;
    private String patientId;
    private String patientPhone;
    private String patientHealthInsuranceNumber;
    private String roomId;
    private String roomName;
    private String status;
    private String visitType;
    private String parentRecordId;
    private String rootRecordId;
    private String symptoms;
    private String diagnosis;
    private String doctorNotes;
    private LocalDateTime createdAt;
    
    private PatientInfo patient;
    private DoctorInfo doctor;
    private PrescriptionResponse prescription;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientInfo {
        private String fullName;
        private String dob;
        private String gender;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorInfo {
        private String fullName;
        private String department;
    }
}
