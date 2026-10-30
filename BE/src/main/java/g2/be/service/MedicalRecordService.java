package g2.be.service;

import g2.be.dto.response.*;
import g2.be.entity.Appointment;
import g2.be.entity.Department;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Patient;
import g2.be.entity.Prescription;
import g2.be.entity.Room;
import g2.be.entity.ServiceOrder;
import g2.be.entity.User;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.MedicalRecordRepository;
import g2.be.repository.PatientRepository;
import g2.be.repository.PrescriptionRepository;
import g2.be.repository.ServiceOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final PdfExportService pdfExportService;

    @Transactional(readOnly = true)
    public Page<MedicalRecordResponse> getPatientHistory(String patientId, String keyword, Pageable pageable) {
                return medicalRecordRepository.findHistoryWithFilter(patientId, List.of("FINISHED", "COMPLETED"), keyword, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<MedicalRecordResponse> getMyHistory(Long userId, String keyword, Pageable pageable) {
        Patient patient = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        return medicalRecordRepository
                .findVisibleHistoryWithFilter(patient.getPatientId(), List.of("FINISHED", "COMPLETED"), keyword, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<MedicalRecordResponse> getDoctorExaminedRecords(Long userId, String keyword, Pageable pageable) {
        return getDoctorExaminedRecords(userId, keyword, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MedicalRecordResponse> getDoctorExaminedRecords(
            Long userId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        DateTimeRange range = resolveCreatedAtRange(fromDate, toDate);
        return medicalRecordRepository
                .findDoctorHistoryWithFilter(
                        userId,
                        List.of("FINISHED", "COMPLETED"),
                        keyword,
                        range.fromDateTime(),
                        range.toDateTime(),
                        pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<FollowUpOptionResponse> getMyFollowUpOptions(Long userId) {
        Patient patient = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        return medicalRecordRepository
                .findVisibleFollowUpOptions(patient.getPatientId(), List.of("FINISHED", "COMPLETED"), Department.DeptType.CLINICAL)
                .stream()
                .map(this::mapToFollowUpOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FollowUpOptionResponse> getFollowUpOptions(String patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new AppException(ErrorCode.PATIENT_NOT_FOUND);
        }

        return medicalRecordRepository
                .findFollowUpOptions(patientId, List.of("FINISHED", "COMPLETED"), Department.DeptType.CLINICAL)
                .stream()
                .map(this::mapToFollowUpOption)
                .toList();
    }

        @Transactional(readOnly = true)
        public MedicalRecordDetailResponse getMyDetail(Long userId, String recordId) {
                Patient patient = patientRepository.findByUser_UserId(userId)
                                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

                MedicalRecord record = medicalRecordRepository.findById(recordId)
                                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

                if (!canPatientAccessRecord(record, patient.getPatientId())) {
                        throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
                }

                Prescription prescription = prescriptionRepository.findByMedicalRecord(record).orElse(null);
                return mapToDetailResponse(record, prescription);
        }

    @Transactional(readOnly = true)
    public byte[] exportMyPdf(Long userId, String recordId) {
        Patient patient = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        if (!canPatientAccessRecord(record, patient.getPatientId())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }

        Prescription prescription = prescriptionRepository.findByMedicalRecord(record).orElse(null);
        List<ServiceOrder> serviceOrders = serviceOrderRepository.findByMedicalRecord_RecordId(recordId);
        return pdfExportService.generateMedicalRecordPdf(record, serviceOrders, prescription);
    }

    @Transactional(readOnly = true)
    public MedicalRecordDetailResponse viewDetail(String id) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        Prescription prescription = prescriptionRepository.findByMedicalRecord(record).orElse(null);

        return mapToDetailResponse(record, prescription);
    }

    @Transactional
    public MedicalRecordDetailResponse updateExaminationStatus(String id, String newStatus) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        String currentStatus = record.getStatus();

        // Transition logic:
        // WAITING → EXAMINING ✅
        // EXAMINING → FINISHED ✅
        // WAITING or EXAMINING → CANCELLED ✅
        
        boolean valid = false;
        if ("WAITING".equals(currentStatus)) {
            if ("EXAMINING".equals(newStatus) || "CANCELLED".equals(newStatus)) {
                valid = true;
            }
        } else if ("EXAMINING".equals(currentStatus)) {
            if ("FINISHED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
                valid = true;
            }
        }

        if (!valid) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        record.setStatus(newStatus);
        MedicalRecord saved = medicalRecordRepository.save(record);
        return mapToDetailResponse(saved, prescriptionRepository.findByMedicalRecord(saved).orElse(null));
    }

    @Transactional
    public MedicalRecordDetailResponse confirmFinalResult(String id, Long currentUserId) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        if (!"EXAMINING".equals(record.getStatus())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_EXAMINING);
        }

        Prescription prescription = prescriptionRepository.findByMedicalRecord(record)
                .orElseThrow(() -> new AppException(ErrorCode.PRESCRIPTION_REQUIRED_FOR_CONFIRM));

        record.setStatus("FINISHED");
        // Note: confirmedBy and confirmedAt fields are not in entity yet, skipping save.
        
        MedicalRecord saved = medicalRecordRepository.save(record);
        return mapToDetailResponse(saved, prescription);
    }

    private MedicalRecordResponse mapToResponse(MedicalRecord record) {
        Optional<g2.be.entity.Service> clinicalService = resolveClinicalService(record);
        return MedicalRecordResponse.builder()
                .recordId(record.getRecordId())
                .examinationDate(record.getCreatedAt())
                .patientId(record.getPatient() != null ? record.getPatient().getPatientId() : null)
                .patientName(record.getPatient() != null && record.getPatient().getUser() != null
                        ? record.getPatient().getUser().getFullName()
                        : null)
                .doctorName(record.getDoctor() != null && record.getDoctor().getUser() != null ? 
                        record.getDoctor().getUser().getFullName() : "N/A")
                .diagnosisSummary(record.getDiagnosis())
                .status(record.getStatus())
                .visitType(record.getVisitType())
                .parentRecordId(record.getParentRecord() != null ? record.getParentRecord().getRecordId() : null)
                .rootRecordId(record.getRootRecord() != null ? record.getRootRecord().getRecordId() : null)
                .canFollowUp(isCompleted(record) && clinicalService.isPresent())
                .clinicalServiceId(clinicalService.map(g2.be.entity.Service::getServiceId).orElse(null))
                .clinicalServiceName(clinicalService.map(g2.be.entity.Service::getServiceName).orElse(null))
                .departmentName(record.getDoctor() != null && record.getDoctor().getDepartment() != null
                        ? record.getDoctor().getDepartment().getDeptName()
                        : clinicalService.map(g2.be.entity.Service::getDepartment)
                                .map(Department::getDeptName)
                                .orElse(null))
                .build();
    }

    private FollowUpOptionResponse mapToFollowUpOption(MedicalRecord record) {
        g2.be.entity.Service clinicalService = resolveClinicalService(record)
                .orElseThrow(() -> new AppException(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE));

        return FollowUpOptionResponse.builder()
                .recordId(record.getRecordId())
                .examinationDate(record.getCreatedAt())
                .status(record.getStatus())
                .visitType(record.getVisitType())
                .patientId(record.getPatient() != null ? record.getPatient().getPatientId() : null)
                .patientName(record.getPatient() != null && record.getPatient().getUser() != null
                        ? record.getPatient().getUser().getFullName()
                        : null)
                .doctorId(record.getDoctor() != null ? record.getDoctor().getDoctorId() : null)
                .doctorName(record.getDoctor() != null && record.getDoctor().getUser() != null
                        ? record.getDoctor().getUser().getFullName()
                        : null)
                .departmentName(record.getDoctor() != null && record.getDoctor().getDepartment() != null
                        ? record.getDoctor().getDepartment().getDeptName()
                        : clinicalService.getDepartment() != null ? clinicalService.getDepartment().getDeptName() : null)
                .clinicalServiceId(clinicalService.getServiceId())
                .clinicalServiceName(clinicalService.getServiceName())
                .diagnosisSummary(record.getDiagnosis())
                .build();
    }

    private MedicalRecordDetailResponse mapToDetailResponse(MedicalRecord record, Prescription prescription) {
        Patient patient = record.getPatient();
        User patientUser = patient != null ? patient.getUser() : null;
        Appointment appointment = record.getAppointment();
        Room room = appointment != null ? appointment.getRoom() : null;

        MedicalRecordDetailResponse.PatientInfo patientInfo = MedicalRecordDetailResponse.PatientInfo.builder()
                .fullName(patientUser != null ? patientUser.getFullName() : "N/A")
                .dob(patient != null && patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : null)
                .gender(patientUser != null && patientUser.getGender() != null ? patientUser.getGender().name() : null)
                .build();

        MedicalRecordDetailResponse.DoctorInfo doctorInfo = MedicalRecordDetailResponse.DoctorInfo.builder()
                .fullName(record.getDoctor() != null && record.getDoctor().getUser() != null ? 
                        record.getDoctor().getUser().getFullName() : "N/A")
                .department(record.getDoctor() != null && record.getDoctor().getDepartment() != null ? 
                        record.getDoctor().getDepartment().getDeptName() : "N/A")
                .build();

        PrescriptionResponse prescriptionResponse = null;
        if (prescription != null) {
            prescriptionResponse = PrescriptionResponse.builder()
                    .prescriptionId(prescription.getPrescriptionId())
                    .medicalRecordId(record.getRecordId())
                    .notes(prescription.getNotes())
                    .createdAt(prescription.getCreatedAt())
                    .details(prescription.getDetails().stream()
                            .map(detail -> PrescriptionDetailResponse.builder()
                                    .id(detail.getId())
                                    .medicineName(detail.getMedicineName())
                                    .dosage(detail.getDosage())
                                    .quantity(detail.getQuantity())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        }

        return MedicalRecordDetailResponse.builder()
                .recordId(record.getRecordId())
                .appointmentId(appointment != null ? appointment.getAppointmentId() : null)
                .appointmentDate(appointment != null && appointment.getAppointmentDate() != null ? appointment.getAppointmentDate().toString() : null)
                .appointmentTime(appointment != null && appointment.getAppointmentTime() != null ? appointment.getAppointmentTime().toString() : null)
                .patientId(patient != null ? patient.getPatientId() : null)
                .patientPhone(patientUser != null ? patientUser.getPhoneNumber() : null)
                .patientHealthInsuranceNumber(patient != null ? patient.getHealthInsuranceNumber() : null)
                .roomId(room != null ? room.getRoomId() : null)
                .roomName(room != null ? room.getRoomName() : null)
                .status(record.getStatus())
                .visitType(record.getVisitType())
                .parentRecordId(record.getParentRecord() != null ? record.getParentRecord().getRecordId() : null)
                .rootRecordId(record.getRootRecord() != null ? record.getRootRecord().getRecordId() : null)
                .symptoms(record.getSymptoms())
                .diagnosis(record.getDiagnosis())
                .doctorNotes(record.getDoctorNotes())
                .createdAt(record.getCreatedAt())
                .patient(patientInfo)
                .doctor(doctorInfo)
                .prescription(prescriptionResponse)
                .build();
    }

    private Optional<g2.be.entity.Service> resolveClinicalService(MedicalRecord record) {
        if (record == null || record.getAppointment() == null || record.getAppointment().getServices() == null) {
            return Optional.empty();
        }

        return record.getAppointment().getServices().stream()
                .filter(service -> service.getDepartment() != null
                        && service.getDepartment().getDeptType() == Department.DeptType.CLINICAL)
                .findFirst();
    }

    private boolean isCompleted(MedicalRecord record) {
        return record != null
                && ("COMPLETED".equals(record.getStatus()) || "FINISHED".equals(record.getStatus()));
    }

    private boolean canPatientAccessRecord(MedicalRecord record, String patientId) {
        if (record == null || patientId == null) {
            return false;
        }

        boolean ownRecord = record.getPatient() != null
                && patientId.equals(record.getPatient().getPatientId());
        boolean bookedByPatient = record.getAppointment() != null
                && patientId.equals(record.getAppointment().getBookedByPatientId());

        return ownRecord || bookedByPatient;
    }

    private DateTimeRange resolveCreatedAtRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            LocalDate swap = fromDate;
            fromDate = toDate;
            toDate = swap;
        }

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;
        return new DateTimeRange(fromDateTime, toDateTime);
    }

    private record DateTimeRange(LocalDateTime fromDateTime, LocalDateTime toDateTime) {
    }
}
