package g2.be.service;

import g2.be.dto.response.MedicalRecordDetailResponse;
import g2.be.dto.response.MedicalRecordResponse;
import g2.be.entity.*;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.MedicalRecordRepository;
import g2.be.repository.PatientRepository;
import g2.be.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    private static final List<String> HISTORY_STATUSES = List.of("FINISHED", "COMPLETED");

    @InjectMocks
    private MedicalRecordService medicalRecordService;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    private MedicalRecord waitingRecord;
    private MedicalRecord examiningRecord;
    private MedicalRecord finishedRecord;
    private MedicalRecord cancelledRecord;
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

        cancelledRecord = MedicalRecord.builder()
                .recordId("4")
                .status("CANCELLED")
                .patient(samplePatient)
                .doctor(sampleDoctor)
                .build();
    }

    @Nested
    @DisplayName("Test getPatientHistory")
    class GetHistoryTests {

        @Test
        @DisplayName("Lấy lịch sử khám bệnh thành công (không từ khóa)")
        void getPatientHistory_Success() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 10);
            Page<MedicalRecord> page = new PageImpl<>(List.of(finishedRecord));
            when(medicalRecordRepository.findHistoryWithFilter(eq("PAT100"), eq(HISTORY_STATUSES), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // WHEN
            Page<MedicalRecordResponse> result = medicalRecordService.getPatientHistory("PAT100", null, pageable);

            // THEN
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("3", result.getContent().get(0).getRecordId());
            verify(medicalRecordRepository).findHistoryWithFilter(anyString(), any(Collection.class), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Lấy lịch sử khám bệnh với từ khóa tìm kiếm")
        void getPatientHistory_WithKeyword_Success() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 10);
            Page<MedicalRecord> page = new PageImpl<>(List.of(finishedRecord));
            when(medicalRecordRepository.findHistoryWithFilter(eq("PAT100"), eq(HISTORY_STATUSES), eq("Viêm họng"), any(Pageable.class)))
                    .thenReturn(page);

            // WHEN
            Page<MedicalRecordResponse> result = medicalRecordService.getPatientHistory("PAT100", "Viêm họng", pageable);

            // THEN
            assertNotNull(result);
            verify(medicalRecordRepository).findHistoryWithFilter(anyString(), any(Collection.class), eq("Viêm họng"), any(Pageable.class));
        }

        @Test
        @DisplayName("Trả về page trống khi không có lịch sử khám")
        void getPatientHistory_Empty_ReturnsEmptyPage() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 10);
            when(medicalRecordRepository.findHistoryWithFilter(anyString(), any(Collection.class), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // WHEN
            Page<MedicalRecordResponse> result = medicalRecordService.getPatientHistory("PAT999", null, pageable);

            // THEN
            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }
    }

    @Nested
    @DisplayName("Test getMyHistory (FE_39 - Customer xem lịch sử khám)")
    class GetMyHistoryTests {

        @Test
        @DisplayName("Customer xem lịch sử khám của mình thành công")
        void getMyHistory_Success() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 10);
            Page<MedicalRecord> page = new PageImpl<>(List.of(finishedRecord));
            when(patientRepository.findByUser_UserId(100L)).thenReturn(Optional.of(samplePatient));
            when(medicalRecordRepository.findVisibleHistoryWithFilter(eq("PAT100"), eq(HISTORY_STATUSES), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            // WHEN
            Page<MedicalRecordResponse> result = medicalRecordService.getMyHistory(100L, null, pageable);

            // THEN
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("3", result.getContent().get(0).getRecordId());
            verify(patientRepository).findByUser_UserId(100L);
        }

        @Test
        @DisplayName("Lỗi khi không tìm thấy hồ sơ bệnh nhân từ userId")
        void getMyHistory_PatientNotFound_ThrowsException() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 10);
            when(patientRepository.findByUser_UserId(999L)).thenReturn(Optional.empty());

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> medicalRecordService.getMyHistory(999L, null, pageable));
            assertEquals(ErrorCode.PATIENT_NOT_FOUND, ex.getErrorCode());
            verify(medicalRecordRepository, never()).findVisibleHistoryWithFilter(any(), any(Collection.class), any(), any());
        }

        @Test
        @DisplayName("Trả về page trống khi bệnh nhân chưa có lịch sử khám")
        void getMyHistory_Empty_ReturnsEmptyPage() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 10);
            when(patientRepository.findByUser_UserId(100L)).thenReturn(Optional.of(samplePatient));
            when(medicalRecordRepository.findVisibleHistoryWithFilter(eq("PAT100"), eq(HISTORY_STATUSES), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // WHEN
            Page<MedicalRecordResponse> result = medicalRecordService.getMyHistory(100L, null, pageable);

            // THEN
            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }
    }

    @Nested
    @DisplayName("Test viewDetail (getRecordDetail)")
    class ViewDetailTests {

        @Test
        @DisplayName("Lấy chi tiết hồ sơ bệnh án thành công (có đơn thuốc)")
        void viewDetail_WithPrescription_Success() {
            // GIVEN
            Prescription prescription = Prescription.builder()
                    .prescriptionId("P10")
                    .medicalRecord(examiningRecord)
                    .details(List.of(PrescriptionDetail.builder().medicineName("A").quantity("1").build()))
                    .build();

            when(medicalRecordRepository.findById("2")).thenReturn(Optional.of(examiningRecord));
            when(prescriptionRepository.findByMedicalRecord(examiningRecord)).thenReturn(Optional.of(prescription));

            // WHEN
            MedicalRecordDetailResponse response = medicalRecordService.viewDetail("2");

            // THEN
            assertNotNull(response);
            assertEquals("2", response.getRecordId());
            assertNotNull(response.getPrescription());
            assertEquals(1, response.getPrescription().getDetails().size());
        }

        @Test
        @DisplayName("Lấy chi tiết hồ sơ bệnh án thành công (không có đơn thuốc)")
        void viewDetail_NoPrescription_Success() {
            // GIVEN
            when(medicalRecordRepository.findById("1")).thenReturn(Optional.of(waitingRecord));
            when(prescriptionRepository.findByMedicalRecord(waitingRecord)).thenReturn(Optional.empty());

            // WHEN
            MedicalRecordDetailResponse response = medicalRecordService.viewDetail("1");

            // THEN
            assertNotNull(response);
            assertEquals("1", response.getRecordId());
            assertNull(response.getPrescription());
        }

        @Test
        @DisplayName("Lỗi khi không tìm thấy hồ sơ bệnh án")
        void viewDetail_NotFound_ThrowsException() {
            // GIVEN
            when(medicalRecordRepository.findById("999")).thenReturn(Optional.empty());

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> medicalRecordService.viewDetail("999"));
            assertEquals(ErrorCode.MEDICAL_RECORD_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Test updateExaminationStatus (changeStatus)")
    class UpdateStatusTests {

        @Test
        @DisplayName("Chuyển trạng thái hợp lệ: WAITING -> EXAMINING")
        void updateStatus_WaitingToExamining_Success() {
            // GIVEN
            when(medicalRecordRepository.findById("1")).thenReturn(Optional.of(waitingRecord));
            when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(i -> i.getArguments()[0]);

            // WHEN
            MedicalRecordDetailResponse response = medicalRecordService.updateExaminationStatus("1", "EXAMINING");

            // THEN
            assertNotNull(response);
            assertEquals("EXAMINING", response.getStatus());
            verify(medicalRecordRepository, times(1)).save(waitingRecord);
        }

        @Test
        @DisplayName("Lỗi khi chuyển trạng thái không hợp lệ: WAITING -> FINISHED")
        void updateStatus_WaitingToFinished_ThrowsException() {
            // GIVEN
            when(medicalRecordRepository.findById("1")).thenReturn(Optional.of(waitingRecord));

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> medicalRecordService.updateExaminationStatus("1", "FINISHED"));
            assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
            verify(medicalRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lỗi khi chuyển trạng thái từ CANCELLED")
        void updateStatus_FromCancelled_ThrowsException() {
            // GIVEN
            when(medicalRecordRepository.findById("4")).thenReturn(Optional.of(cancelledRecord));

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> medicalRecordService.updateExaminationStatus("4", "EXAMINING"));
            assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Test confirmFinalResult")
    class ConfirmResultTests {

        @Test
        @DisplayName("Xác nhận kết quả thành công")
        void confirmFinalResult_Success() {
            // GIVEN
            Prescription prescription = Prescription.builder().prescriptionId("P10").build();
            when(medicalRecordRepository.findById("2")).thenReturn(Optional.of(examiningRecord));
            when(prescriptionRepository.findByMedicalRecord(examiningRecord)).thenReturn(Optional.of(prescription));
            when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(i -> i.getArguments()[0]);

            // WHEN
            MedicalRecordDetailResponse response = medicalRecordService.confirmFinalResult("2", 200L);

            // THEN
            assertNotNull(response);
            assertEquals("FINISHED", response.getStatus());
            verify(medicalRecordRepository, times(1)).save(examiningRecord);
        }

        @Test
        @DisplayName("Lỗi khi xác nhận kết quả mà chưa có đơn thuốc")
        void confirmFinalResult_NoPrescription_ThrowsException() {
            // GIVEN
            when(medicalRecordRepository.findById("2")).thenReturn(Optional.of(examiningRecord));
            when(prescriptionRepository.findByMedicalRecord(examiningRecord)).thenReturn(Optional.empty());

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> medicalRecordService.confirmFinalResult("2", 200L));
            assertEquals(ErrorCode.PRESCRIPTION_REQUIRED_FOR_CONFIRM, ex.getErrorCode());
            verify(medicalRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lỗi khi xác nhận kết quả cho hồ sơ đang ở trạng thái WAITING")
        void confirmFinalResult_Waiting_ThrowsException() {
            // GIVEN
            when(medicalRecordRepository.findById("1")).thenReturn(Optional.of(waitingRecord));

            // WHEN & THEN
            AppException ex = assertThrows(AppException.class, () -> medicalRecordService.confirmFinalResult("1", 200L));
            assertEquals(ErrorCode.MEDICAL_RECORD_NOT_EXAMINING, ex.getErrorCode());
        }
    }
}
