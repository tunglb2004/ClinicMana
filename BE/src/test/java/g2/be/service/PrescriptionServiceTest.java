package g2.be.service;

import g2.be.dto.request.PrescriptionDetailRequest;
import g2.be.dto.request.PrescriptionRequest;
import g2.be.dto.response.PrescriptionResponse;
import g2.be.entity.*;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.MedicalRecordRepository;
import g2.be.repository.PrescriptionDetailRepository;
import g2.be.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @InjectMocks
    private PrescriptionService prescriptionService;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    private MedicalRecord waitingRecord;
    private MedicalRecord examiningRecord;
    private MedicalRecord finishedRecord;
    private Patient samplePatient;
    private Doctor sampleDoctor;
    private User doctorUser;
    private User patientUser;

    @BeforeEach
    void setUp() {
        patientUser = User.builder()
                .userId(100L)
                .fullName("Nguyễn Văn A")
                .gender(User.Gender.MALE)
                .build();

        samplePatient = Patient.builder()
                .patientId("PAT100")
                .user(patientUser)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        doctorUser = User.builder()
                .userId(200L)
                .fullName("Bác sĩ Trần B")
                .build();

        sampleDoctor = Doctor.builder()
                .doctorId("DOC200")
                .user(doctorUser)
                .department(Department.builder().deptName("Nội khoa").build())
                .build();

        waitingRecord = MedicalRecord.builder()
                .recordId("1")
                .status("WAITING")
                .patient(samplePatient)
                .doctor(sampleDoctor)
                .build();

        examiningRecord = MedicalRecord.builder()
                .recordId("2")
                .status("EXAMINING")
                .patient(samplePatient)
                .doctor(sampleDoctor)
                .build();

        finishedRecord = MedicalRecord.builder()
                .recordId("3")
                .status("FINISHED")
                .patient(samplePatient)
                .doctor(sampleDoctor)
                .build();
    }

    @Nested
    @DisplayName("Test issue (savePrescription)")
    class IssueTests {

        @Test
        @DisplayName("Tạo đơn thuốc thành công - Happy path")
        void issue_Success() {
            // GIVEN
            PrescriptionDetailRequest detail1 = PrescriptionDetailRequest.builder()
                    .medicineName("Paracetamol").dosage("500mg").quantity("10").build();
            PrescriptionDetailRequest detail2 = PrescriptionDetailRequest.builder()
                    .medicineName("Amoxicillin").dosage("250mg").quantity("6").build();
            
            PrescriptionRequest request = PrescriptionRequest.builder()
                    .medicalRecordId("2")
                    .notes("Uống sau khi ăn")
                    .details(List.of(detail1, detail2))
                    .build();

            when(medicalRecordRepository.findById("2")).thenReturn(Optional.of(examiningRecord));
            when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(i -> i.getArguments()[0]);

            // WHEN
            PrescriptionResponse response = prescriptionService.issue(request);

            // THEN
            assertNotNull(response);
            assertEquals("2", response.getMedicalRecordId());
            assertEquals("Uống sau khi ăn", response.getNotes());
            assertEquals(2, response.getDetails().size());
            
            verify(prescriptionRepository, times(1)).save(any(Prescription.class));
            verify(prescriptionDetailRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("Lỗi khi hồ sơ bệnh án không tồn tại")
        void issue_RecordNotFound_ThrowsException() {
            // GIVEN
            PrescriptionRequest request = PrescriptionRequest.builder().medicalRecordId("999").build();
            when(medicalRecordRepository.findById("999")).thenReturn(Optional.empty());

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> prescriptionService.issue(request));
            assertEquals(ErrorCode.MEDICAL_RECORD_NOT_FOUND, ex.getErrorCode());
            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lỗi khi hồ sơ không trong trạng thái EXAMINING (với WAITING)")
        void issue_NotExamining_Waiting_ThrowsException() {
            // GIVEN
            PrescriptionRequest request = PrescriptionRequest.builder().medicalRecordId("1").build();
            when(medicalRecordRepository.findById("1")).thenReturn(Optional.of(waitingRecord));

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> prescriptionService.issue(request));
            assertEquals(ErrorCode.MEDICAL_RECORD_NOT_EXAMINING, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Test updateDetail (updatePrescription)")
    class UpdateDetailTests {

        @Test
        @DisplayName("Cập nhật đơn thuốc thành công")
        void updateDetail_Success() {
            // GIVEN
            Prescription existingPrescription = Prescription.builder()
                    .prescriptionId("P10")
                    .medicalRecord(examiningRecord)
                    .details(new ArrayList<>())
                    .build();

            PrescriptionRequest request = PrescriptionRequest.builder()
                    .notes("Cập nhật ghi chú")
                    .details(List.of(PrescriptionDetailRequest.builder().medicineName("New").quantity("5").build()))
                    .build();

            when(prescriptionRepository.findById("P10")).thenReturn(Optional.of(existingPrescription));
            when(prescriptionRepository.save(any(Prescription.class))).thenReturn(existingPrescription);

            // WHEN
            PrescriptionResponse response = prescriptionService.updateDetail("P10", request);

            // THEN
            assertNotNull(response);
            assertEquals("Cập nhật ghi chú", response.getNotes());
            verify(prescriptionDetailRepository).deleteByPrescription(existingPrescription);
            verify(prescriptionDetailRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Lỗi khi đơn thuốc đã kết thúc (FINISHED)")
        void updateDetail_Finished_ThrowsException() {
            // GIVEN
            Prescription existingPrescription = Prescription.builder()
                    .prescriptionId("P10")
                    .medicalRecord(finishedRecord)
                    .build();

            when(prescriptionRepository.findById("P10")).thenReturn(Optional.of(existingPrescription));

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> prescriptionService.updateDetail("P10", any()));
            assertEquals(ErrorCode.MEDICAL_RECORD_ALREADY_FINISHED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Test generateExportDocument")
    class ExportTests {

        @Test
        @DisplayName("Xuất HTML đơn thuốc thành công")
        void generateExportDocument_Success() {
            // GIVEN
            Prescription prescription = Prescription.builder()
                    .prescriptionId("P10")
                    .medicalRecord(examiningRecord)
                    .details(List.of(PrescriptionDetail.builder().medicineName("Panadol").quantity("10").build()))
                    .build();

            when(prescriptionRepository.findById("P10")).thenReturn(Optional.of(prescription));

            // WHEN
            String html = prescriptionService.generateExportDocument("P10");

            // THEN
            assertNotNull(html);
            assertTrue(html.contains("Nguyễn Văn A"));
            assertTrue(html.contains("Bác sĩ Trần B"));
            assertTrue(html.contains("Panadol"));
        }

        @Test
        @DisplayName("Lỗi khi không tìm thấy đơn thuốc để xuất")
        void generateExportDocument_NotFound_ThrowsException() {
            // GIVEN
            when(prescriptionRepository.findById("999")).thenReturn(Optional.empty());

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> prescriptionService.generateExportDocument("999"));
            assertEquals(ErrorCode.PRESCRIPTION_NOT_FOUND, ex.getErrorCode());
        }
    }
}
