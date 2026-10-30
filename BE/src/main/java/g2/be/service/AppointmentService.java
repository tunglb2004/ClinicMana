package g2.be.service;

import g2.be.dto.request.BookAppointmentRequest;
import g2.be.dto.request.CancelAppointmentRequest;
import g2.be.dto.request.CompleteExaminationRequest;
import g2.be.dto.request.CreateAppointmentRequest;
import g2.be.dto.request.CreatePatientRequest;
import g2.be.dto.request.TransferAppointmentRoomRequest;
import g2.be.dto.request.UpdateAppointmentRequest;
import g2.be.dto.response.AppointmentDetailResponse;
import g2.be.dto.response.AppointmentResponse;
import g2.be.dto.response.AvailableSlotResponse;
import g2.be.dto.response.RoomTransferOptionResponse;
import g2.be.dto.response.UserDetailResponse;
import g2.be.entity.Appointment;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Invoice;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Nurse;
import g2.be.entity.NurseDoctorAssignment;
import g2.be.entity.Patient;
import g2.be.entity.Role;
import g2.be.entity.Room;
import g2.be.entity.Service;
import g2.be.entity.ServiceOrder;
import g2.be.entity.User;
import g2.be.entity.WorkingTimeSlot;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.MedicalRecordRepository;
import g2.be.repository.NurseDoctorAssignmentRepository;
import g2.be.repository.NurseRepository;
import g2.be.repository.PatientRepository;
import g2.be.repository.RoleRepository;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.ServiceRepository;
import g2.be.repository.UserRepository;
import g2.be.repository.WorkingTimeSlotRepository;
import g2.be.util.ClinicTime;
import g2.be.util.DoctorDepartmentClassifier;
import g2.be.util.QueuePriorityRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private static final String TEMPORARILY_POSTPONED_STATUS = "POSTPONED";
    private static final int QUEUE_LOOKAHEAD_SIZE = 100;

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ServiceRepository serviceRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final WorkingTimeSlotRepository workingTimeSlotRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final g2.be.repository.ServiceOrderRepository serviceOrderRepository;
    private final g2.be.repository.RoomRepository roomRepository;
    private final g2.be.repository.StaffScheduleRepository staffScheduleRepository;
    private final NurseRepository nurseRepository;
    private final NurseDoctorAssignmentRepository nurseDoctorAssignmentRepository;
    private final BhytSyncService bhytSyncService;
    private final OnDutyRoomAssignmentResolver onDutyRoomAssignmentResolver;
    private final PatientManagementService patientManagementService;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Create appointment
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        Patient patient = null;
        FollowUpBookingContext followUpContext = null;
        boolean creatingNewPatient = request.getNewPatient() != null;
        if (creatingNewPatient) {
            ensureNewPatientAppointmentRequest(request);
        } else {
            patient = patientRepository.findById(requirePatientId(request.getPatientId()))
                    .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

            followUpContext = resolveFollowUpBookingContext(
                    patient,
                    request.getFollowUpSourceRecordId(),
                    request.getServiceIds(),
                    request.getAppointmentDate(),
                    request.getAppointmentTime());
        }
        List<Service> services = followUpContext != null
                ? followUpContext.services()
                : loadSelectableServices(request.getServiceIds());
        List<Service> schedulingServices = extractSchedulingServices(services);

        if (services.isEmpty()) {
            throw new AppException(ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS);
        }

        AppointmentSelections selections = resolveAppointmentSelections(
            services,
            followUpContext != null ? followUpContext.preferredDoctorId() : request.getDoctorId(),
            request.getParaclinicalDoctorId(),
            request.getRoomId(),
            request.getAppointmentDate(),
            request.getAppointmentTime());

        validateSlotCapacity(
            request.getAppointmentDate(),
            request.getAppointmentTime(),
            schedulingServices,
            selections.primaryRoom());

        if (creatingNewPatient) {
            patient = createPatientForAppointment(request.getNewPatient());
        }

        String appointmentId = "APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Appointment appointment = Appointment.builder()
                .appointmentId(appointmentId)
                .patient(patient)
            .doctor(selections.primaryDoctor())
            .paraclinicalDoctor(selections.paraclinicalDoctor())
            .room(selections.primaryRoom())
            .paraclinicalRoom(selections.paraclinicalRoom())
                .services(services)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .status(normalizeInitialAppointmentStatus(request.getStatus()))
                .visitType(followUpContext != null ? "FOLLOW_UP" : "INITIAL")
                .followUpSourceRecord(followUpContext != null ? followUpContext.sourceRecord() : null)
                .notes(request.getNotes())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(savedAppointment);
    }

    private void ensureNewPatientAppointmentRequest(CreateAppointmentRequest request) {
        if (request.getPatientId() != null && !request.getPatientId().isBlank()) {
            throw new AppException(ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS);
        }
        if (request.getFollowUpSourceRecordId() != null && !request.getFollowUpSourceRecordId().isBlank()) {
            throw new AppException(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE);
        }
    }

    private String requirePatientId(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new AppException(ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS);
        }
        return patientId.trim();
    }

    private Patient createPatientForAppointment(CreatePatientRequest request) {
        UserDetailResponse createdPatient = patientManagementService.createPatient(request);
        if (createdPatient == null || createdPatient.getPatientId() == null || createdPatient.getPatientId().isBlank()) {
            throw new AppException(ErrorCode.PATIENT_NOT_FOUND);
        }
        return patientRepository.findById(createdPatient.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Confirm appointment booking
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AppointmentResponse confirmAppointment(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new AppException(ErrorCode.CANCEL_APPOINTMENT_NOT_ALLOWED);
        }

        appointment.setStatus("CONFIRMED");
        return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public AppointmentResponse cancelMyAppointment(String appointmentId, String patientId, CancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        boolean ownAppointment = appointment.getPatient() != null
                && patientId.equals(appointment.getPatient().getPatientId());
        boolean bookedForRelative = patientId.equals(appointment.getBookedByPatientId());
        if (!ownAppointment && !bookedForRelative) {
            throw new AppException(ErrorCode.CANCEL_APPOINTMENT_ACCESS_DENIED);
        }

        return cancelAppointment(appointmentId, request);
    }

    // 3. Cancel appointment
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AppointmentResponse cancelAppointment(String appointmentId, CancelAppointmentRequest request) {
        String cancelReason = requireCancelReason(request);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        String currentStatus = appointment.getStatus();

        if ("CANCELLED".equals(currentStatus)) {
            throw new AppException(ErrorCode.CANCEL_APPOINTMENT_ALREADY_CANCELLED);
        }

        // Receptionist-facing flows still need to cancel freshly requested appointments.
        if (!"REQUESTED".equals(currentStatus)
                && !"CHECKED_IN".equals(currentStatus)
                && !"WAITING".equals(currentStatus)
                && !"CONFIRMED".equals(currentStatus)) {
            throw new AppException(ErrorCode.CANCEL_APPOINTMENT_STATUS_NOT_ALLOWED);
        }

        ensureAppointmentHasNoPaidItems(appointment.getAppointmentId());

        appointment.setStatus("CANCELLED");
        appointment.setWaitingSince(null);
        appointment.setPriorityScore(0);
        appointment.setCancelReason(cancelReason);

        if ("WAITING".equals(currentStatus)) {
            Room assignedRoom = getAssignedRoom(appointment);
            if (assignedRoom != null) {
                Room room = assignedRoom;
                int current = room.getCurrentQueueLength() != null ? room.getCurrentQueueLength() : 0;
                room.setCurrentQueueLength(Math.max(0, current - 1));
                roomRepository.save(room);
            }
        }

        return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. View appointment detail (with medical record info)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AppointmentDetailResponse getAppointmentDetail(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        return buildAppointmentDetail(appointment);
        }

    @Transactional(readOnly = true)
    public AppointmentDetailResponse getMyAppointmentDetail(Long userId, String appointmentId) {
        String patientId = patientRepository.findByUser_UserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND))
            .getPatientId();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        ensureAppointmentVisibleToPatient(appointment, patientId);

        return buildAppointmentDetail(appointment);
        }

        @Transactional(readOnly = true)
        public AppointmentDetailResponse getAppointmentDetailForStaffRoom(Long userId, String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        ensureAppointmentInStaffClinicalRooms(userId, appointment);

        return buildAppointmentDetail(appointment);
        }

        private AppointmentDetailResponse buildAppointmentDetail(Appointment appointment) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointment(appointment).orElse(null);
        List<ServiceOrder> serviceOrders = serviceOrderRepository
            .findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(appointment.getAppointmentId());
        Set<String> technicianIds = serviceOrders.stream()
            .map(ServiceOrder::getTechnicianId)
            .filter(id -> id != null && !id.isBlank())
            .collect(Collectors.toSet());
        Map<String, Doctor> techniciansById = technicianIds.isEmpty()
            ? Map.of()
            : doctorRepository.findAllById(technicianIds).stream()
                .collect(Collectors.toMap(Doctor::getDoctorId, Function.identity()));

        AppointmentDetailResponse response = AppointmentDetailResponse.fromEntity(
            appointment,
            medicalRecord,
            serviceOrders,
            techniciansById);

        Map<String, Service> appointmentServicesById = new java.util.LinkedHashMap<>();
        if (appointment.getServices() != null) {
            appointment.getServices().stream()
                .filter(service -> service != null && service.getServiceId() != null)
                .forEach(service -> appointmentServicesById.putIfAbsent(service.getServiceId(), service));
        }
        serviceOrders.stream()
            .filter(order -> order != null
                    && (order.getStatus() == null || !"CANCELLED".equalsIgnoreCase(order.getStatus()))
                    && order.getService() != null
                    && order.getService().getServiceId() != null)
            .forEach(order -> appointmentServicesById.putIfAbsent(order.getService().getServiceId(), order.getService()));
        populateMissingServiceDoctors(response, appointment, appointmentServicesById);

        // Fallback: lấy phòng từ staff_schedules nếu chưa có
        if (response.getRoomName() == null && appointment.getDoctor() != null
                && appointment.getDoctor().getUser() != null && appointment.getAppointmentDate() != null) {
            findScheduledRoom(appointment.getDoctor(), appointment.getAppointmentDate(), appointment.getAppointmentTime())
                    .ifPresent(room -> {
                        response.setRoomId(room.getRoomId());
                        response.setRoomName(room.getRoomName());
                        if (response.getDepartmentName() == null && room.getDepartment() != null) {
                            response.setDepartmentName(room.getDepartment().getDeptName());
                        }
                    });
        }

        return response;
    }

    private void populateMissingServiceDoctors(
            AppointmentDetailResponse response,
            Appointment appointment,
            Map<String, Service> appointmentServicesById) {
        if (response == null || response.getServices() == null || response.getServices().isEmpty() || appointment == null) {
            return;
        }

        for (AppointmentDetailResponse.ServiceInfo serviceInfo : response.getServices()) {
            if (serviceInfo == null
                    || (serviceInfo.getAssignedDoctorId() != null && !serviceInfo.getAssignedDoctorId().isBlank())
                    || serviceInfo.getAssignedRoomId() == null
                    || serviceInfo.getAssignedRoomId().isBlank()) {
                continue;
            }

            Service service = appointmentServicesById.get(serviceInfo.getServiceId());
            Doctor inferredDoctor = inferDoctorForServiceRoom(
                    appointment,
                    service,
                    serviceInfo.getAssignedRoomId());
            if (inferredDoctor != null) {
                serviceInfo.setAssignedDoctorId(inferredDoctor.getDoctorId());
                if (inferredDoctor.getUser() != null) {
                    serviceInfo.setAssignedDoctorName(inferredDoctor.getUser().getFullName());
                }
            }
        }
    }

    private Doctor inferDoctorForServiceRoom(
            Appointment appointment,
            Service service,
            String roomId) {
        if (appointment == null || service == null || roomId == null || roomId.isBlank()) {
            return null;
        }

        return roomRepository.findByRoomIdAndIsDeletedFalse(roomId)
                .flatMap(room -> onDutyRoomAssignmentResolver.findAssignmentForRoomAt(
                        service,
                        room,
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime()))
                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::doctor)
                .orElse(null);
    }

    private boolean doctorMatchesService(Doctor doctor, Service service) {
        if (doctor == null || service == null || service.getDepartment() == null) {
            return false;
        }

        Department.DeptType deptType = service.getDepartment().getDeptType();
        if (deptType == null) {
            return false;
        }

        return deptType == Department.DeptType.PARACLINICAL
                ? isParaclinicalDoctor(doctor)
                : !isParaclinicalDoctor(doctor);
    }

    private void ensureAppointmentVisibleToPatient(Appointment appointment, String patientId) {
        boolean ownAppointment = appointment.getPatient() != null
                && patientId.equals(appointment.getPatient().getPatientId());
        boolean bookedForRelative = patientId.equals(appointment.getBookedByPatientId());

        if (!ownAppointment && !bookedForRelative) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. View visit history list (by patient, paginated)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getVisitHistoryByPatient(String patientId, Pageable pageable) {
        if (!patientRepository.existsById(patientId)) {
            throw new AppException(ErrorCode.PATIENT_NOT_FOUND);
        }
        return appointmentRepository
                .findByPatientOrBooker(patientId, pageable)
                .map(this::toAppointmentResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Existing: update appointment
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AppointmentResponse updateAppointment(String appointmentId, UpdateAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        boolean servicesChanged = request.getServiceIds() != null;
        if (servicesChanged && isFollowUpAppointment(appointment)) {
            throw new AppException(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE);
        }

        LocalDate appointmentDate = request.getAppointmentDate() != null
                ? request.getAppointmentDate()
                : appointment.getAppointmentDate();
        LocalTime appointmentTime = request.getAppointmentTime() != null
                ? request.getAppointmentTime()
                : appointment.getAppointmentTime();
        List<Service> services = request.getServiceIds() != null && !request.getServiceIds().isEmpty()
                ? loadSelectableServices(request.getServiceIds())
                : appointment.getServices();
        String clinicalRoomId = request.getRoomId() != null
            ? request.getRoomId()
            : (request.getDoctorId() == null
                && request.getAppointmentDate() == null
                && request.getAppointmentTime() == null
            && !servicesChanged
                    ? resolveCurrentClinicalRoomId(appointment)
                    : null);

        String clinicalDoctorId = request.getDoctorId() != null
            ? request.getDoctorId()
            : (servicesChanged ? null : resolveCurrentClinicalDoctorId(appointment));

        String paraclinicalDoctorId = request.getParaclinicalDoctorId() != null
            ? request.getParaclinicalDoctorId()
            : (servicesChanged ? null : resolveCurrentParaclinicalDoctorId(appointment));

        AppointmentSelections selections = resolveAppointmentSelections(
                services,
            clinicalDoctorId,
            paraclinicalDoctorId,
            clinicalRoomId,
                appointmentDate,
                appointmentTime);

        boolean schedulingChanged = request.getAppointmentDate() != null
                || request.getAppointmentTime() != null
                || request.getDoctorId() != null
                || request.getParaclinicalDoctorId() != null
                || request.getRoomId() != null
                || servicesChanged;
        if (schedulingChanged) {
            validateSlotCapacity(appointmentDate, appointmentTime, extractSchedulingServices(services), selections.primaryRoom());
        }

        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentTime(appointmentTime);
        if (request.getStatus() != null) {
            String requestedStatus = request.getStatus().trim().toUpperCase(Locale.ROOT);
            boolean hasPaidInvoice = invoiceRepository.existsByMedicalRecord_Appointment_AppointmentIdAndPaymentStatus(
                    appointmentId, "PAID");
            appointment.setStatus("WAITING".equals(requestedStatus) && !hasPaidInvoice
                    ? "CHECKED_IN"
                    : requestedStatus);
        }
        appointment.setDoctor(selections.primaryDoctor());
        appointment.setParaclinicalDoctor(selections.paraclinicalDoctor());
        appointment.setRoom(selections.primaryRoom());
        appointment.setParaclinicalRoom(selections.paraclinicalRoom());
        syncAppointmentServices(appointment, services);

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(updatedAppointment);
    }

    @Transactional(readOnly = true)
    public List<RoomTransferOptionResponse> getRoomTransferOptions(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        ensureAppointmentCanBeTransferred(appointment);

        List<Service> transferServices = resolveTransferServices(appointment);
        if (transferServices.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        Department.DeptType targetDeptType = resolveSchedulingDeptType(transferServices);
        String currentRoomId = Optional.ofNullable(resolveTransferCurrentRoom(appointment))
                .map(Room::getRoomId)
                .orElse(null);

        return onDutyRoomAssignmentResolver
                .findAssignmentsForServicesAt(
                        transferServices,
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime(),
                        true)
                .stream()
                .filter(assignment -> assignment.room() != null && roomMatchesDeptType(assignment.room(), targetDeptType))
                .map(assignment -> buildRoomTransferOption(
                        appointment,
                        assignment.room(),
                        assignment.doctor(),
                        transferServices,
                        currentRoomId,
                        targetDeptType))
                .filter(Objects::nonNull)
                .sorted(java.util.Comparator
                        .comparing(RoomTransferOptionResponse::getCurrentRoom, java.util.Comparator.reverseOrder())
                        .thenComparing(option -> option.getCurrentQueueLength() == null ? Integer.MAX_VALUE : option.getCurrentQueueLength())
                        .thenComparing(RoomTransferOptionResponse::getRoomName, java.util.Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Transactional
    public AppointmentResponse transferAppointmentRoom(String appointmentId, TransferAppointmentRoomRequest request) {
        if (request == null || request.getRoomId() == null || request.getRoomId().isBlank()) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        ensureAppointmentCanBeTransferred(appointment);

        List<Service> transferServices = resolveTransferServices(appointment);
        if (transferServices.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        Room targetRoom = roomRepository.findByRoomIdAndIsDeletedFalse(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (!isOperationalRoom(targetRoom) || !roomSupportsServices(targetRoom, transferServices)) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        Department.DeptType targetDeptType = resolveSchedulingDeptType(transferServices);
        if (!roomMatchesDeptType(targetRoom, targetDeptType)) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        Doctor targetDoctor = onDutyRoomAssignmentResolver
                .findAssignmentsForServicesAt(
                        transferServices,
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime(),
                        true)
                .stream()
                .filter(assignment -> assignment.room() != null
                        && targetRoom.getRoomId().equals(assignment.room().getRoomId()))
                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::doctor)
                .findFirst()
                .orElse(null);
        if (targetDoctor == null) {
            throw new AppException(ErrorCode.DOCTOR_NOT_FOUND);
        }

        Room previousRoom = resolveTransferCurrentRoom(appointment);
        String previousRoomId = previousRoom != null ? previousRoom.getRoomId() : null;
        if (targetRoom.getRoomId().equals(previousRoomId)) {
            return AppointmentResponse.fromEntity(appointment);
        }

        boolean clinicalTransfer = !extractClinicalServices(appointment.getServices()).isEmpty();
        boolean queued = "WAITING".equalsIgnoreCase(appointment.getStatus());
        if (queued) {
            decrementQueueLength(previousRoom);
        }

        if (clinicalTransfer) {
            appointment.setRoom(targetRoom);
            appointment.setDoctor(targetDoctor);
        } else {
            appointment.setRoom(targetRoom);
            appointment.setDoctor(targetDoctor);
            appointment.setParaclinicalRoom(targetRoom);
            appointment.setParaclinicalDoctor(targetDoctor);
        }

        if (queued || appointment.getQueueNumber() != null) {
            appointment.setQueueNumber(nextQueueNumber(targetRoom, appointment.getAppointmentDate()));
        }

        if (queued) {
            incrementQueueLength(targetRoom);
        }

        syncTransferredServiceOrders(appointment, transferServices, targetRoom, targetDoctor);
        syncTransferredMedicalRecordDoctor(appointment, targetDoctor);
        appendTransferReason(appointment, previousRoom, targetRoom, request.getReason());

        return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
    }

    private void ensureAppointmentHasNoPaidItems(String appointmentId) {
        boolean hasPaidInvoice = invoiceRepository.existsByMedicalRecord_Appointment_AppointmentIdAndPaymentStatus(
                appointmentId, "PAID");
        boolean hasPaidServiceOrder = serviceOrderRepository
                .findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(appointmentId)
                .stream()
                .anyMatch(order -> "PAID".equalsIgnoreCase(order.getStatus()));

        if (hasPaidInvoice || hasPaidServiceOrder) {
            throw new AppException(ErrorCode.APPOINTMENT_ALREADY_PAID_CANNOT_CANCEL);
        }
    }

    private void ensureAppointmentCanBeTransferred(Appointment appointment) {
        if (appointment == null || appointment.getStatus() == null) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        String status = appointment.getStatus().toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(status) || "COMPLETED".equals(status) || "IN_PROGRESS".equals(status)) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }
    }

    private List<Service> resolveTransferServices(Appointment appointment) {
        if (appointment == null || appointment.getServices() == null) {
            return List.of();
        }

        List<Service> clinicalServices = extractClinicalServices(appointment.getServices());
        if (!clinicalServices.isEmpty()) {
            return clinicalServices;
        }

        return extractParaclinicalServices(appointment.getServices());
    }

    private Room resolveTransferCurrentRoom(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        if (!extractClinicalServices(appointment.getServices()).isEmpty()) {
            return getAssignedRoom(appointment);
        }

        if (appointment.getParaclinicalRoom() != null) {
            return appointment.getParaclinicalRoom();
        }

        return getAssignedRoom(appointment);
    }

    private RoomTransferOptionResponse buildRoomTransferOption(
            Appointment appointment,
            Room room,
            List<Service> transferServices,
            String currentRoomId,
            Department.DeptType targetDeptType) {
        Doctor doctor = resolveTransferDoctorForRoom(room, transferServices, appointment);
        return buildRoomTransferOption(appointment, room, doctor, transferServices, currentRoomId, targetDeptType);
    }

    private RoomTransferOptionResponse buildRoomTransferOption(
            Appointment appointment,
            Room room,
            Doctor doctor,
            List<Service> transferServices,
            String currentRoomId,
            Department.DeptType targetDeptType) {
        if (doctor == null || !isDoctorAssignableForDeptType(doctor, targetDeptType)) {
            return null;
        }

        return RoomTransferOptionResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .departmentName(room.getDepartment() != null ? room.getDepartment().getDeptName() : null)
                .location(room.getLocation())
                .currentQueueLength(room.getCurrentQueueLength())
                .baseAvgTime(room.getBaseAvgTime())
                .doctorId(doctor.getDoctorId())
                .doctorName(doctor.getUser() != null ? doctor.getUser().getFullName() : null)
                .currentRoom(room.getRoomId() != null && room.getRoomId().equals(currentRoomId))
                .serviceId(transferServices.size() == 1 ? transferServices.get(0).getServiceId() : null)
                .serviceName(transferServices.stream()
                        .map(Service::getServiceName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", ")))
                .build();
    }

    private Doctor resolveTransferDoctorForRoom(Room room, List<Service> services, Appointment appointment) {
        if (room == null || room.getRoomId() == null) {
            return null;
        }

        if (appointment == null) {
            return null;
        }

        return onDutyRoomAssignmentResolver
                .findAssignmentsForServicesAt(
                        services,
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime(),
                        true)
                .stream()
                .filter(assignment -> assignment.room() != null
                        && room.getRoomId().equals(assignment.room().getRoomId()))
                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::doctor)
                .filter(doctor -> doctorSupportsTransferServices(doctor, services))
                .findFirst()
                .orElse(null);
    }

    private boolean doctorSupportsTransferServices(Doctor doctor, List<Service> services) {
        if (doctor == null || services == null || services.isEmpty()) {
            return false;
        }

        return services.stream().allMatch(service -> doctorMatchesService(doctor, service));
    }

    private void syncTransferredServiceOrders(
            Appointment appointment,
            List<Service> transferServices,
            Room targetRoom,
            Doctor targetDoctor) {
        if (appointment == null || transferServices == null || transferServices.isEmpty()) {
            return;
        }

        Set<String> transferServiceIds = transferServices.stream()
                .map(Service::getServiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (transferServiceIds.isEmpty()) {
            return;
        }

        medicalRecordRepository.findByAppointment(appointment).ifPresent(record -> {
            List<ServiceOrder> orders = serviceOrderRepository.findByMedicalRecord_RecordId(record.getRecordId());
            for (ServiceOrder order : orders) {
                if (order == null
                        || order.getService() == null
                        || !transferServiceIds.contains(order.getService().getServiceId())
                        || "CANCELLED".equalsIgnoreCase(order.getStatus())) {
                    continue;
                }

                order.setAssignedRoom(targetRoom);
                order.setTechnicianId(targetDoctor != null ? targetDoctor.getDoctorId() : null);
                serviceOrderRepository.save(order);
            }
        });
    }

    private void syncTransferredMedicalRecordDoctor(Appointment appointment, Doctor targetDoctor) {
        if (appointment == null || targetDoctor == null) {
            return;
        }

        medicalRecordRepository.findByAppointment(appointment).ifPresent(record -> {
            if (!"COMPLETED".equalsIgnoreCase(record.getStatus())
                    && !"CANCELLED".equalsIgnoreCase(record.getStatus())) {
                record.setDoctor(targetDoctor);
                medicalRecordRepository.save(record);
            }
        });
    }

    private void appendTransferReason(Appointment appointment, Room previousRoom, Room targetRoom, String reason) {
        if (appointment == null) {
            return;
        }

        String trimmedReason = reason != null ? reason.trim() : "";
        if (trimmedReason.isEmpty()) {
            return;
        }

        String previousRoomName = previousRoom != null ? previousRoom.getRoomName() : "chua co phong";
        String targetRoomName = targetRoom != null ? targetRoom.getRoomName() : "phong moi";
        String transferNote = "[Dieu phoi phong: " + previousRoomName + " -> " + targetRoomName
                + ". Ly do: " + trimmedReason + "]";
        String currentNotes = appointment.getNotes();
        appointment.setNotes(currentNotes == null || currentNotes.isBlank()
                ? transferNote
                : currentNotes + "\n" + transferNote);
    }

    private Integer nextQueueNumber(Room room, LocalDate appointmentDate) {
        if (room == null || room.getRoomId() == null || appointmentDate == null) {
            return null;
        }

        Integer maxQueueNumber = appointmentRepository.findMaxQueueNumberByRoomIdAndDate(room.getRoomId(), appointmentDate);
        return (maxQueueNumber == null ? 0 : maxQueueNumber) + 1;
    }

    private void decrementQueueLength(Room room) {
        if (room == null) {
            return;
        }

        int current = room.getCurrentQueueLength() != null ? room.getCurrentQueueLength() : 0;
        room.setCurrentQueueLength(Math.max(0, current - 1));
        roomRepository.save(room);
    }

    private void incrementQueueLength(Room room) {
        if (room == null) {
            return;
        }

        room.setCurrentQueueLength((room.getCurrentQueueLength() != null ? room.getCurrentQueueLength() : 0) + 1);
        roomRepository.save(room);
    }

    private void syncAppointmentServices(Appointment appointment, List<Service> services) {
        if (appointment == null) {
            return;
        }

        List<Service> targetServices = services == null
                ? new ArrayList<>()
                : new ArrayList<>(services);

        if (appointment.getServices() == null) {
            appointment.setServices(targetServices);
            return;
        }

        appointment.getServices().clear();
        appointment.getServices().addAll(targetServices);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Existing: get all appointments / single appointment
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointments(String status, Pageable pageable) {
        Page<Appointment> page;
        if (status != null && !status.isBlank()) {
            page = appointmentRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        } else {
            page = appointmentRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::toAppointmentResponse);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        return AppointmentResponse.fromEntity(appointment);
    }

    @Transactional
    public AppointmentResponse checkIn(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        if ("CANCELLED".equals(appointment.getStatus()) || "COMPLETED".equals(appointment.getStatus())) {
            throw new AppException(ErrorCode.CANCEL_APPOINTMENT_NOT_ALLOWED);
        }

        String previousStatus = appointment.getStatus();
        boolean hasPaidInvoice = invoiceRepository.existsByMedicalRecord_Appointment_AppointmentIdAndPaymentStatus(
                appointmentId, "PAID");
        if (!hasPaidInvoice) {
            if ("WAITING".equalsIgnoreCase(previousStatus)) {
                decrementQueueLength(getAssignedRoom(appointment));
            }
            appointment.setStatus("CHECKED_IN");
            appointment.setWaitingSince(null);
            appointment.setPriorityScore(QueuePriorityRules.basePriorityForVisitType(appointment.getVisitType()));
            log.info("Check-in appointment {} without paid invoice; kept out of examination queue.", appointmentId);
            return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
        }

        appointment.setStatus("WAITING");

        boolean isReturning = isReturningFromParaclinical(appointment);
        appointment.setPriorityScore(isReturning
                ? QueuePriorityRules.RETURNING_FROM_PARACLINICAL
                : QueuePriorityRules.basePriorityForVisitType(appointment.getVisitType()));
        if (appointment.getWaitingSince() == null || !"WAITING".equalsIgnoreCase(previousStatus)) {
            appointment.setWaitingSince(ClinicTime.now());
        }

        log.info("Check-in appointment {}: isReturning={}, priorityScore={}",
                appointmentId, isReturning, appointment.getPriorityScore());

        Room assignedRoom = getAssignedRoom(appointment);
        if (appointment.getQueueNumber() == null) {
            appointment.setQueueNumber(nextQueueNumber(assignedRoom, appointment.getAppointmentDate()));
        }
        if (!"WAITING".equalsIgnoreCase(previousStatus)) {
            incrementQueueLength(assignedRoom);
        }

        return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
    }

    private String normalizeInitialAppointmentStatus(String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            return "REQUESTED";
        }

        String normalized = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if ("WAITING".equals(normalized)) {
            return "CHECKED_IN";
        }
        return normalized;
    }

    private boolean isReturningFromParaclinical(Appointment appointment) {
        if (appointment == null || appointment.getPatient() == null) {
            return false;
        }

        MedicalRecord mr = medicalRecordRepository.findByAppointment(appointment).orElse(null);
        if (mr == null || "FOLLOW_UP".equalsIgnoreCase(appointment.getVisitType())) {
            return false;
        }

        return serviceOrderRepository.findByMedicalRecord_RecordId(mr.getRecordId()).stream()
                .anyMatch(o -> "PAID".equalsIgnoreCase(o.getStatus()));
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForDoctor(Long userId, String status, Pageable pageable) {
        Doctor doctor = doctorRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
        LocalDate appointmentDate = ClinicTime.today();
        Set<String> roomIds = resolveDoctorRoomIds(userId, appointmentDate);
        if (roomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        String normalizedStatus = status != null && !status.isBlank()
                ? status.toUpperCase(Locale.ROOT)
                : null;
        java.time.LocalDateTime queueNow = ClinicTime.now();

        return appointmentRepository
            .findByRoomIdsForDoctorQueue(roomIds, appointmentDate, queueNow, normalizedStatus, pageable)
                .map(this::toQueueAppointmentResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForStaffClinicalRooms(Long userId, String status, Pageable pageable) {
        LocalDate appointmentDate = ClinicTime.today();
        Set<String> roomIds = resolveNurseClinicalRoomIds(userId, appointmentDate);
        if (roomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        String normalizedStatus = status != null && !status.isBlank()
                ? status.toUpperCase(Locale.ROOT)
                : null;
        java.time.LocalDateTime queueNow = ClinicTime.now();

        return appointmentRepository
                .findByRoomIdsForDoctorQueue(roomIds, appointmentDate, queueNow, normalizedStatus, pageable)
                .map(this::toQueueAppointmentResponse);
    }

    private AppointmentResponse toAppointmentResponse(Appointment appointment) {
        String recordId = medicalRecordRepository
                .findByAppointment_AppointmentId(appointment.getAppointmentId())
                .map(MedicalRecord::getRecordId)
                .orElse(null);
        AppointmentResponse response = AppointmentResponse.fromEntity(appointment, recordId);
        response.setPaymentStatus(resolveAppointmentPaymentStatus(appointment.getAppointmentId()));
        return response;
    }

    private AppointmentResponse toQueueAppointmentResponse(Appointment appointment) {
        return toAppointmentResponse(appointment);
    }

    private String resolveAppointmentPaymentStatus(String appointmentId) {
        List<Invoice> invoices = invoiceRepository
                .findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(appointmentId);
        if (invoices.stream().anyMatch(invoice -> "PAID".equalsIgnoreCase(invoice.getPaymentStatus()))) {
            return "PAID";
        }
        if (invoices.stream().anyMatch(invoice -> "PENDING".equalsIgnoreCase(invoice.getPaymentStatus()))) {
            return "PENDING";
        }
        if (invoices.stream().anyMatch(invoice -> "UNPAID".equalsIgnoreCase(invoice.getPaymentStatus()))) {
            return "UNPAID";
        }
        if (invoices.stream().anyMatch(invoice -> "CANCELLED".equalsIgnoreCase(invoice.getPaymentStatus()))) {
            return "CANCELLED";
        }
        return "UNPAID";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIORITY QUEUE: Gọi bệnh nhân tiếp theo
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bác sĩ gọi bệnh nhân tiếp theo (Top 1 điểm cao nhất trong WAITING).
     * Chuyển status WAITING → IN_PROGRESS.
     * Nếu bác sĩ đang khám ai (IN_PROGRESS) → không cho gọi thêm.
     */
    private Set<String> resolveDoctorRoomIds(Long userId, LocalDate queryDate) {
        return resolveStaffRoomIds(userId, queryDate, null);
    }

    private Set<String> resolveStaffRoomIds(
            Long userId,
            LocalDate queryDate,
            Department.DeptType roomDeptType) {
        List<g2.be.entity.StaffSchedule> todaySchedules = Optional.ofNullable(
                staffScheduleRepository.findByUser_UserIdAndWorkDate(userId, queryDate))
                .orElse(List.of());

        Set<String> currentShiftRoomIds = todaySchedules.stream()
                .filter(this::isCurrentShiftSchedule)
                .map(g2.be.entity.StaffSchedule::getRoom)
                .filter(java.util.Objects::nonNull)
                .filter(this::isOperationalRoom)
                .filter(room -> roomDeptType == null || roomMatchesDeptType(room, roomDeptType))
                .map(Room::getRoomId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!currentShiftRoomIds.isEmpty()) {
            return currentShiftRoomIds;
        }

        return Set.of();
    }

    private Set<String> resolveNurseClinicalRoomIds(Long userId, LocalDate queryDate) {
        LocalDate effectiveDate = queryDate != null ? queryDate : ClinicTime.today();
        Set<String> assignedDoctorRoomIds = resolveNurseAssignedDoctorClinicalRoomIds(userId, effectiveDate);
        if (!assignedDoctorRoomIds.isEmpty()) {
            return assignedDoctorRoomIds;
        }

        return resolveStaffRoomIds(userId, effectiveDate, Department.DeptType.CLINICAL);
    }

    private Set<String> resolveNurseAssignedDoctorClinicalRoomIds(Long userId, LocalDate queryDate) {
        Set<String> roomIds = new LinkedHashSet<>();
        Optional<Nurse> nurse = Optional.ofNullable(nurseRepository.findByUser_UserId(userId))
                .orElse(Optional.empty());
        if (nurse.isEmpty() || nurse.get().getNurseId() == null) {
            return roomIds;
        }

        Optional.ofNullable(nurseDoctorAssignmentRepository.findByNurse_NurseIdAndIsActiveTrue(nurse.get().getNurseId()))
                .orElse(List.of())
                .stream()
                .filter(assignment -> assignmentActiveOn(assignment, queryDate))
                .map(NurseDoctorAssignment::getDoctor)
                .filter(Objects::nonNull)
                .forEach(doctor -> roomIds.addAll(resolveAssignedDoctorClinicalRoomIds(doctor, queryDate)));

        return roomIds;
    }

    private Set<String> resolveAssignedDoctorClinicalRoomIds(Doctor doctor, LocalDate queryDate) {
        if (doctor == null) {
            return Set.of();
        }

        Long doctorUserId = doctor.getUser() != null ? doctor.getUser().getUserId() : null;
        if (doctorUserId != null) {
            return resolveStaffRoomIds(doctorUserId, queryDate, Department.DeptType.CLINICAL);
        }

        return Set.of();
    }

    private boolean assignmentActiveOn(NurseDoctorAssignment assignment, LocalDate queryDate) {
        if (assignment == null || !Boolean.TRUE.equals(assignment.getIsActive())) {
            return false;
        }
        LocalDate effectiveDate = assignment.getEffectiveDate();
        LocalDate endDate = assignment.getEndDate();
        return (effectiveDate == null || !effectiveDate.isAfter(queryDate))
                && (endDate == null || !endDate.isBefore(queryDate));
    }

    private boolean isCurrentShiftSchedule(g2.be.entity.StaffSchedule schedule) {
        if (schedule == null) {
            return false;
        }

        LocalTime startTime = schedule.getSlot() != null ? schedule.getSlot().getStartTime() : schedule.getLegacyStartTime();
        LocalTime endTime = schedule.getSlot() != null ? schedule.getSlot().getEndTime() : schedule.getLegacyEndTime();
        LocalTime now = ClinicTime.now().toLocalTime();
        return startTime != null
                && endTime != null
                && !now.isBefore(startTime)
                && now.isBefore(endTime);
    }

    @Transactional
    public AppointmentResponse callNextPatient(Long userId) {
        Doctor doctor = doctorRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
        LocalDate appointmentDate = ClinicTime.today();
        Set<String> roomIds = resolveDoctorRoomIds(userId, appointmentDate);
        if (roomIds.isEmpty()) {
            throw new AppException(ErrorCode.NO_WAITING_PATIENT);
        }

        org.springframework.data.domain.Pageable top1 =
                org.springframework.data.domain.PageRequest.of(0, 1);
        java.time.LocalDateTime queueNow = ClinicTime.now();

        Page<Appointment> inProgress = appointmentRepository
                .findByRoomIdsForDoctorQueue(roomIds, appointmentDate, queueNow, "IN_PROGRESS", top1);
        if (inProgress.hasContent()) {
            throw new AppException(ErrorCode.DOCTOR_BUSY);
        }

        Appointment next = findNextCallableWaitingPatient(roomIds, appointmentDate, queueNow, null);
        if (next == null) {
            throw new AppException(ErrorCode.NO_WAITING_PATIENT);
        }

        next.setStatus("IN_PROGRESS");
        appointmentRepository.save(next);

        // Giảm currentQueueLength của phòng
        Room assignedRoom = getAssignedRoom(next);
        if (assignedRoom != null) {
            Room room = assignedRoom;
            int current = room.getCurrentQueueLength() != null ? room.getCurrentQueueLength() : 0;
            room.setCurrentQueueLength(Math.max(0, current - 1));
            roomRepository.save(room);
        }

        log.info("Doctor {} called next patient: appointment={}, priorityScore={}, waitingSince={}",
                doctor.getDoctorId(), next.getAppointmentId(), next.getPriorityScore(), next.getWaitingSince());

        return toQueueAppointmentResponse(next);
    }

    @Transactional
    public AppointmentResponse returnPostponedPatientToQueue(Long userId, String appointmentId) {
        Doctor doctor = doctorRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
        Appointment target = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        LocalDate appointmentDate = ClinicTime.today();
        ensureAppointmentBelongsToDoctorQueue(userId, doctor, target, appointmentDate);

        if (!isTemporarilyPostponed(target)) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        target.setStatus("WAITING");
        if (target.getWaitingSince() == null) {
            target.setWaitingSince(ClinicTime.now());
        }
        incrementQueueLength(getAssignedRoom(target));

        Appointment savedTarget = appointmentRepository.save(target);
        log.info("Doctor {} returned postponed patient to queue: appointment={}",
                doctor.getDoctorId(), savedTarget.getAppointmentId());

        return toQueueAppointmentResponse(savedTarget);
    }

    @Transactional
    public AppointmentResponse postponePatient(Long userId, String appointmentId) {
        Doctor doctor = doctorRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
        Appointment target = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        LocalDate appointmentDate = ClinicTime.today();
        ensureAppointmentBelongsToDoctorQueue(userId, doctor, target, appointmentDate);

        Set<String> targetRoomIds = Set.of(getAssignedRoom(target).getRoomId());
        java.time.LocalDateTime queueNow = ClinicTime.now();
        String targetStatus = target.getStatus();

        if ("IN_PROGRESS".equals(targetStatus)) {
            return postponeInProgressPatient(doctor, target, targetRoomIds, appointmentDate, queueNow);
        }

        if ("WAITING".equals(targetStatus)) {
            return postponeWaitingPatient(doctor, target, targetRoomIds, appointmentDate, queueNow);
        }

        throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
    }

    private AppointmentResponse postponeInProgressPatient(
            Doctor doctor,
            Appointment target,
            Set<String> targetRoomIds,
            LocalDate appointmentDate,
            java.time.LocalDateTime queueNow) {
        org.springframework.data.domain.Pageable top1 = org.springframework.data.domain.PageRequest.of(0, 1);
        Page<Appointment> currentPage = appointmentRepository
                .findByRoomIdsForDoctorQueue(targetRoomIds, appointmentDate, queueNow, "IN_PROGRESS", top1);
        if (!currentPage.hasContent()
                || !target.getAppointmentId().equals(currentPage.getContent().get(0).getAppointmentId())) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        Appointment promoted = findNextCallableWaitingPatient(targetRoomIds, appointmentDate, queueNow, target.getAppointmentId());
        markTemporarilyPostponed(target, queueNow);
        if (promoted != null) {
            promoted.setStatus("IN_PROGRESS");
            decrementQueueLength(getAssignedRoom(promoted));
        }

        appointmentRepository.save(target);
        Appointment responseAppointment = promoted != null ? appointmentRepository.save(promoted) : target;

        log.info("Doctor {} postponed patient: postponed={}, promoted={}",
                doctor.getDoctorId(), target.getAppointmentId(), promoted != null ? promoted.getAppointmentId() : null);

        return toQueueAppointmentResponse(responseAppointment);
    }

    private AppointmentResponse postponeWaitingPatient(
            Doctor doctor,
            Appointment target,
            Set<String> targetRoomIds,
            LocalDate appointmentDate,
            java.time.LocalDateTime queueNow) {
        org.springframework.data.domain.Pageable top1 = org.springframework.data.domain.PageRequest.of(0, 1);
        Page<Appointment> currentPage = appointmentRepository
                .findByRoomIdsForDoctorQueue(targetRoomIds, appointmentDate, queueNow, "IN_PROGRESS", top1);
        if (currentPage.hasContent()) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        Appointment firstWaiting = appointmentRepository
                .findByRoomIdsForDoctorQueue(targetRoomIds, appointmentDate, queueNow, "WAITING", top1)
                .getContent()
                .stream()
                .findFirst()
                .orElse(null);
        if (firstWaiting == null || !target.getAppointmentId().equals(firstWaiting.getAppointmentId())) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        markTemporarilyPostponed(target, queueNow);
        decrementQueueLength(getAssignedRoom(target));
        Appointment promoted = findNextCallableWaitingPatient(targetRoomIds, appointmentDate, queueNow, target.getAppointmentId());
        if (promoted != null) {
            promoted.setStatus("IN_PROGRESS");
            decrementQueueLength(getAssignedRoom(promoted));
        }

        appointmentRepository.save(target);
        Appointment responseAppointment = promoted != null ? appointmentRepository.save(promoted) : target;

        log.info("Doctor {} postponed waiting patient: postponed={}, promoted={}",
                doctor.getDoctorId(), target.getAppointmentId(), promoted != null ? promoted.getAppointmentId() : null);

        return toQueueAppointmentResponse(responseAppointment);
    }

    private void markTemporarilyPostponed(Appointment appointment, java.time.LocalDateTime queueNow) {
        if (appointment.getWaitingSince() == null) {
            appointment.setWaitingSince(queueNow);
        }
        appointment.setStatus(TEMPORARILY_POSTPONED_STATUS);
    }

    private Appointment findNextCallableWaitingPatient(
            Set<String> roomIds,
            LocalDate appointmentDate,
            java.time.LocalDateTime queueNow,
            String excludedAppointmentId) {
        return appointmentRepository
                .findByRoomIdsForDoctorQueue(roomIds, appointmentDate, queueNow, "WAITING",
                        org.springframework.data.domain.PageRequest.of(0, QUEUE_LOOKAHEAD_SIZE))
                .getContent()
                .stream()
                .filter(appointment -> excludedAppointmentId == null
                        || !excludedAppointmentId.equals(appointment.getAppointmentId()))
                .filter(appointment -> !isTemporarilyPostponed(appointment))
                .findFirst()
                .orElse(null);
    }

    private boolean isTemporarilyPostponed(Appointment appointment) {
        return appointment != null
                && TEMPORARILY_POSTPONED_STATUS.equalsIgnoreCase(appointment.getStatus());
    }

    private void ensureAppointmentBelongsToDoctorQueue(
            Long userId,
            Doctor doctor,
            Appointment appointment,
            LocalDate appointmentDate) {
        if (appointment.getAppointmentDate() == null || !appointmentDate.equals(appointment.getAppointmentDate())) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }

        Set<String> roomIds = resolveDoctorRoomIds(userId, appointmentDate);
        Room assignedRoom = getAssignedRoom(appointment);
        if (assignedRoom == null || assignedRoom.getRoomId() == null || !roomIds.contains(assignedRoom.getRoomId())) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
    }

    private void ensureAppointmentBelongsToStaffClinicalQueue(
            Long userId,
            Appointment appointment,
            LocalDate appointmentDate) {
        if (appointment.getAppointmentDate() == null || !appointmentDate.equals(appointment.getAppointmentDate())) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }

        Set<String> roomIds = resolveNurseClinicalRoomIds(userId, appointmentDate);
        Room assignedRoom = getAssignedRoom(appointment);
        if (assignedRoom == null || assignedRoom.getRoomId() == null || !roomIds.contains(assignedRoom.getRoomId())) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
    }

    @Transactional
    public AppointmentResponse callNextPatientForStaffClinicalRooms(Long userId) {
        LocalDate appointmentDate = ClinicTime.today();
        Set<String> roomIds = resolveNurseClinicalRoomIds(userId, appointmentDate);
        if (roomIds.isEmpty()) {
            throw new AppException(ErrorCode.NO_WAITING_PATIENT);
        }

        org.springframework.data.domain.Pageable top1 =
                org.springframework.data.domain.PageRequest.of(0, 1);
        java.time.LocalDateTime queueNow = ClinicTime.now();

        Page<Appointment> inProgress = appointmentRepository
                .findByRoomIdsForDoctorQueue(roomIds, appointmentDate, queueNow, "IN_PROGRESS", top1);
        if (inProgress.hasContent()) {
            throw new AppException(ErrorCode.DOCTOR_BUSY);
        }

        Appointment next = findNextCallableWaitingPatient(roomIds, appointmentDate, queueNow, null);
        if (next == null) {
            throw new AppException(ErrorCode.NO_WAITING_PATIENT);
        }

        next.setStatus("IN_PROGRESS");
        appointmentRepository.save(next);

        Room assignedRoom = getAssignedRoom(next);
        if (assignedRoom != null) {
            int current = assignedRoom.getCurrentQueueLength() != null ? assignedRoom.getCurrentQueueLength() : 0;
            assignedRoom.setCurrentQueueLength(Math.max(0, current - 1));
            roomRepository.save(assignedRoom);
        }

        log.info("Nurse user {} called next clinical patient: appointment={}, priorityScore={}, waitingSince={}",
                userId, next.getAppointmentId(), next.getPriorityScore(), next.getWaitingSince());

        return toQueueAppointmentResponse(next);
    }

    @Transactional
    public AppointmentResponse returnPostponedPatientToStaffClinicalQueue(Long userId, String appointmentId) {
        Appointment target = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        LocalDate appointmentDate = ClinicTime.today();
        ensureAppointmentBelongsToStaffClinicalQueue(userId, target, appointmentDate);

        if (!isTemporarilyPostponed(target)) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        target.setStatus("WAITING");
        if (target.getWaitingSince() == null) {
            target.setWaitingSince(ClinicTime.now());
        }
        incrementQueueLength(getAssignedRoom(target));

        Appointment savedTarget = appointmentRepository.save(target);
        log.info("Nurse user {} returned postponed clinical patient to queue: appointment={}",
                userId, savedTarget.getAppointmentId());

        return toQueueAppointmentResponse(savedTarget);
    }

    @Transactional
    public AppointmentResponse postponePatientForStaffClinicalRooms(Long userId, String appointmentId) {
        Appointment target = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        LocalDate appointmentDate = ClinicTime.today();
        ensureAppointmentBelongsToStaffClinicalQueue(userId, target, appointmentDate);

        Set<String> targetRoomIds = Set.of(getAssignedRoom(target).getRoomId());
        java.time.LocalDateTime queueNow = ClinicTime.now();
        String targetStatus = target.getStatus();

        if ("IN_PROGRESS".equals(targetStatus)) {
            return postponeInProgressPatientForStaffClinicalRooms(userId, target, targetRoomIds, appointmentDate, queueNow);
        }

        if ("WAITING".equals(targetStatus)) {
            return postponeWaitingPatientForStaffClinicalRooms(userId, target, targetRoomIds, appointmentDate, queueNow);
        }

        throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
    }

    private AppointmentResponse postponeInProgressPatientForStaffClinicalRooms(
            Long userId,
            Appointment target,
            Set<String> targetRoomIds,
            LocalDate appointmentDate,
            java.time.LocalDateTime queueNow) {
        org.springframework.data.domain.Pageable top1 = org.springframework.data.domain.PageRequest.of(0, 1);
        Page<Appointment> currentPage = appointmentRepository
                .findByRoomIdsForDoctorQueue(targetRoomIds, appointmentDate, queueNow, "IN_PROGRESS", top1);
        if (!currentPage.hasContent()
                || !target.getAppointmentId().equals(currentPage.getContent().get(0).getAppointmentId())) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        Appointment promoted = findNextCallableWaitingPatient(targetRoomIds, appointmentDate, queueNow, target.getAppointmentId());
        markTemporarilyPostponed(target, queueNow);
        if (promoted != null) {
            promoted.setStatus("IN_PROGRESS");
            decrementQueueLength(getAssignedRoom(promoted));
        }

        appointmentRepository.save(target);
        Appointment responseAppointment = promoted != null ? appointmentRepository.save(promoted) : target;

        log.info("Nurse user {} postponed clinical patient: postponed={}, promoted={}",
                userId, target.getAppointmentId(), promoted != null ? promoted.getAppointmentId() : null);

        return toQueueAppointmentResponse(responseAppointment);
    }

    private AppointmentResponse postponeWaitingPatientForStaffClinicalRooms(
            Long userId,
            Appointment target,
            Set<String> targetRoomIds,
            LocalDate appointmentDate,
            java.time.LocalDateTime queueNow) {
        org.springframework.data.domain.Pageable top1 = org.springframework.data.domain.PageRequest.of(0, 1);
        Page<Appointment> currentPage = appointmentRepository
                .findByRoomIdsForDoctorQueue(targetRoomIds, appointmentDate, queueNow, "IN_PROGRESS", top1);
        if (currentPage.hasContent()) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        Appointment firstWaiting = appointmentRepository
                .findByRoomIdsForDoctorQueue(targetRoomIds, appointmentDate, queueNow, "WAITING", top1)
                .getContent()
                .stream()
                .findFirst()
                .orElse(null);
        if (firstWaiting == null || !target.getAppointmentId().equals(firstWaiting.getAppointmentId())) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        markTemporarilyPostponed(target, queueNow);
        decrementQueueLength(getAssignedRoom(target));
        Appointment promoted = findNextCallableWaitingPatient(targetRoomIds, appointmentDate, queueNow, target.getAppointmentId());
        if (promoted != null) {
            promoted.setStatus("IN_PROGRESS");
            decrementQueueLength(getAssignedRoom(promoted));
        }

        appointmentRepository.save(target);
        Appointment responseAppointment = promoted != null ? appointmentRepository.save(promoted) : target;

        log.info("Nurse user {} postponed waiting clinical patient: postponed={}, promoted={}",
                userId, target.getAppointmentId(), promoted != null ? promoted.getAppointmentId() : null);

        return toQueueAppointmentResponse(responseAppointment);
    }

    /**
     * Bác sĩ hoàn tất khám → chuyển IN_PROGRESS → COMPLETED.
     * Lưu triệu chứng (JSON), chẩn đoán, ghi chú vào MedicalRecord.
     */
    @Transactional
    public AppointmentResponse saveExaminationProgress(String appointmentId, CompleteExaminationRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        ensureAppointmentInProgress(appointment);
        saveProgressToMedicalRecord(appointment, appointmentId, request);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(savedAppointment);
    }

    @Transactional
    public AppointmentResponse pauseForParaclinicalResults(String appointmentId, CompleteExaminationRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        String previousStatus = appointment.getStatus();
        ensureAppointmentInProgress(appointment);
        MedicalRecord medicalRecord = saveProgressToMedicalRecord(appointment, appointmentId, request);

        if (!hasOpenParaclinicalOrders(medicalRecord)) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        appointment.setStatus("WAITING_RESULTS");
        appointment.setWaitingSince(null);
        appointment.setPriorityScore(QueuePriorityRules.basePriorityForVisitType(appointment.getVisitType()));
        if ("WAITING".equals(previousStatus)) {
            decrementQueueLengthForAppointment(appointment);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(savedAppointment);
    }

    private MedicalRecord saveProgressToMedicalRecord(
            Appointment appointment,
            String appointmentId,
            CompleteExaminationRequest request) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointment_AppointmentId(appointmentId)
                .orElseGet(() -> medicalRecordRepository.save(buildMedicalRecordForAppointment(appointment, "EXAMINING")));

        if (request.getSymptoms() != null) {
            medicalRecord.setSymptoms(request.getSymptoms());
        }
        if (request.getDiagnosis() != null) {
            medicalRecord.setDiagnosis(request.getDiagnosis());
        }
        if (request.getDoctorNotes() != null) {
            medicalRecord.setDoctorNotes(request.getDoctorNotes());
        }
        if (!"COMPLETED".equals(medicalRecord.getStatus()) && !"CANCELLED".equals(medicalRecord.getStatus())) {
            medicalRecord.setStatus("EXAMINING");
        }

        return medicalRecordRepository.save(medicalRecord);
    }

    private boolean hasOpenParaclinicalOrders(MedicalRecord medicalRecord) {
        if (medicalRecord == null || medicalRecord.getRecordId() == null) {
            return false;
        }

        return serviceOrderRepository.findByMedicalRecord_RecordId(medicalRecord.getRecordId()).stream()
                .anyMatch(this::isOpenParaclinicalOrder);
    }

    private boolean isOpenParaclinicalOrder(ServiceOrder order) {
        return order != null
                && !"CANCELLED".equalsIgnoreCase(order.getStatus())
                && !"COMPLETED".equalsIgnoreCase(order.getStatus())
                && order.getService() != null
                && order.getService().getDepartment() != null
                && order.getService().getDepartment().getDeptType() == Department.DeptType.PARACLINICAL;
    }

    private void decrementQueueLengthForAppointment(Appointment appointment) {
        Room assignedRoom = getAssignedRoom(appointment);
        if (assignedRoom == null) {
            return;
        }

        int current = assignedRoom.getCurrentQueueLength() != null ? assignedRoom.getCurrentQueueLength() : 0;
        assignedRoom.setCurrentQueueLength(Math.max(0, current - 1));
        roomRepository.save(assignedRoom);
    }

    @Transactional
    public AppointmentResponse saveNurseExaminationProgress(Long userId, String appointmentId, CompleteExaminationRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        ensureAppointmentInStaffClinicalRooms(userId, appointment);
        if (!"IN_PROGRESS".equals(appointment.getStatus())) {
            throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
        }

        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointment_AppointmentId(appointmentId)
                .orElseGet(() -> medicalRecordRepository.save(buildMedicalRecordForAppointment(appointment, "EXAMINING")));

        if (request.getSymptoms() != null) {
            medicalRecord.setSymptoms(request.getSymptoms());
        }
        if (!"COMPLETED".equals(medicalRecord.getStatus()) && !"CANCELLED".equals(medicalRecord.getStatus())) {
            medicalRecord.setStatus("EXAMINING");
        }

        medicalRecordRepository.save(medicalRecord);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toQueueAppointmentResponse(savedAppointment);
    }

    private MedicalRecord buildMedicalRecordForAppointment(Appointment appointment, String status) {
        MedicalRecord parentRecord = appointment.getFollowUpSourceRecord();
        MedicalRecord rootRecord = parentRecord == null
                ? null
                : parentRecord.getRootRecord() != null ? parentRecord.getRootRecord() : parentRecord;

        return MedicalRecord.builder()
                .recordId("REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .appointment(appointment)
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .status(status)
                .visitType("FOLLOW_UP".equalsIgnoreCase(appointment.getVisitType()) ? "FOLLOW_UP" : "INITIAL")
                .parentRecord(parentRecord)
                .rootRecord(rootRecord)
                .build();
    }

    @Transactional
    public AppointmentResponse completeExamination(String appointmentId, CompleteExaminationRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        ensureAppointmentInProgress(appointment);
        ensureAllLinkedInvoicesPaid(appointmentId);
        ensureNoUnfinishedParaclinicalOrders(appointmentId);

        // Update MedicalRecord if exists
        medicalRecordRepository.findByAppointment_AppointmentId(appointmentId)
                .ifPresent(mr -> {
                    if (request.getSymptoms() != null) mr.setSymptoms(request.getSymptoms());
                    if (request.getDiagnosis() != null) mr.setDiagnosis(request.getDiagnosis());
                    if (request.getDoctorNotes() != null) mr.setDoctorNotes(request.getDoctorNotes());
                    mr.setStatus("COMPLETED");
                    medicalRecordRepository.save(mr);
                });

        appointment.setStatus("COMPLETED");
        Appointment savedAppointment = appointmentRepository.save(appointment);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    bhytSyncService.queueAppointmentSyncAsync(appointmentId);
                }
            });
        } else {
            bhytSyncService.queueAppointmentSyncAsync(appointmentId);
        }
        return AppointmentResponse.fromEntity(savedAppointment);
    }

    private void ensureNoUnfinishedParaclinicalOrders(String appointmentId) {
        boolean hasUnfinishedParaclinicalOrders = serviceOrderRepository
                .findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(appointmentId)
                .stream()
                .anyMatch(this::isUnfinishedParaclinicalOrder);

        if (hasUnfinishedParaclinicalOrders) {
            throw new AppException(ErrorCode.APPOINTMENT_HAS_UNFINISHED_PARACLINICAL_ORDERS);
        }
    }

    private boolean isUnfinishedParaclinicalOrder(ServiceOrder order) {
        return order != null
                && !"CANCELLED".equalsIgnoreCase(order.getStatus())
                && !"COMPLETED".equalsIgnoreCase(order.getStatus())
                && order.getService() != null
                && order.getService().getDepartment() != null
                && order.getService().getDepartment().getDeptType() == Department.DeptType.PARACLINICAL;
    }

    private void ensureAllLinkedInvoicesPaid(String appointmentId) {
        List<Invoice> linkedInvoices = invoiceRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(appointmentId);
        boolean hasPaidInvoice = linkedInvoices.stream()
                .anyMatch(invoice -> "PAID".equals(invoice.getPaymentStatus()));
        boolean hasBlockingInvoice = linkedInvoices.stream()
                .anyMatch(invoice -> {
                    String paymentStatus = invoice.getPaymentStatus();
                    return !("PAID".equals(paymentStatus) || "CANCELLED".equals(paymentStatus));
                });

                java.util.Set<String> paidInvoiceIds = linkedInvoices.stream()
                    .filter(invoice -> "PAID".equals(invoice.getPaymentStatus()))
                    .map(Invoice::getInvoiceId)
                    .filter(id -> id != null && !id.isBlank())
                    .collect(Collectors.toSet());

                boolean hasUnsettledSupplementOrders = serviceOrderRepository
                    .findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(appointmentId).stream()
                    .filter(order -> !"CANCELLED".equals(order.getStatus()))
                    .filter(order -> "SUPPLEMENT".equals(order.getOrderType()))
                    .anyMatch(order -> order.getInvoiceId() == null || !paidInvoiceIds.contains(order.getInvoiceId()));

                if (!hasPaidInvoice || hasBlockingInvoice || hasUnsettledSupplementOrders) {
            throw new AppException(ErrorCode.APPOINTMENT_HAS_UNPAID_INVOICES);
        }
    }

    private void ensureAppointmentInProgress(Appointment appointment) {
        String status = appointment.getStatus();

        if ("IN_PROGRESS".equals(status)) {
            return;
        }

        if ("WAITING".equals(status) || "CONFIRMED".equals(status)) {
            appointment.setStatus("IN_PROGRESS");
            return;
        }

        throw new AppException(ErrorCode.INVALID_APPOINTMENT_STATUS);
    }

    private void ensureAppointmentInStaffClinicalRooms(Long userId, Appointment appointment) {
        LocalDate appointmentDate = appointment.getAppointmentDate() != null
                ? appointment.getAppointmentDate()
                : ClinicTime.today();
        Set<String> roomIds = resolveNurseClinicalRoomIds(userId, appointmentDate);
        Room assignedRoom = getAssignedRoom(appointment);

        if (assignedRoom == null
                || assignedRoom.getRoomId() == null
                || !roomIds.contains(assignedRoom.getRoomId())) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC-24: Available slots & book appointment
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(LocalDate date, List<String> serviceIds) {
        List<Service> services = loadSelectableServices(serviceIds);
        if (!services.isEmpty()) {
            validateSchedulableServices(services);
        }
        List<Service> schedulingServices = extractAllSchedulableServices(services);
        List<WorkingTimeSlot> slots = workingTimeSlotRepository.findByIsDeletedFalse()
                .stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                .sorted(java.util.Comparator.comparing(WorkingTimeSlot::getStartTime))
                .collect(Collectors.toList());
        List<AvailableSlotResponse> responses = new ArrayList<>();
        for (WorkingTimeSlot slot : slots) {
            boolean unavailableForServices = !schedulingServices.isEmpty()
                    && !hasAvailableAssignmentsForAllServices(
                            schedulingServices,
                            date,
                            slot.getStartTime());
            responses.add(AvailableSlotResponse.builder()
                    .slotId(slot.getSlotId())
                    .shiftName(slot.getShiftName())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .maxPatients(slot.getMaxPatients() != null ? slot.getMaxPatients() : 0)
                    .currentCount(0)
                    .isFull(isSlotExpired(date, slot.getEndTime()) || unavailableForServices)
                    .build());
        }
        return responses;
    }

    @Transactional
    public AppointmentResponse bookAppointment(Long userId, BookAppointmentRequest request) {
        // Always resolve the logged-in user's own patient record first (for bookedByPatientId)
        Patient booker = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        Patient patient;
        if (request.getRelative() == null) {
            // Book for self — booker IS the patient
            patient = booker;
        } else {
            // Find or create a patient record for the relative
            BookAppointmentRequest.RelativeInfo rel = request.getRelative();
            if (rel.getPatientId() != null && !rel.getPatientId().isBlank()) {
                if (request.getFollowUpSourceRecordId() == null || request.getFollowUpSourceRecordId().isBlank()) {
                    throw new AppException(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE);
                }
                patient = patientRepository.findById(rel.getPatientId().trim())
                        .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
            } else {
                Optional<Patient> existing = Optional.empty();
                String relativeCccd = blankToNull(rel.getCccd());
                if (relativeCccd != null) {
                    existing = patientRepository.findByUser_Cccd(relativeCccd);
                }
                if (existing.isEmpty() && rel.getPhoneNumber() != null && !rel.getPhoneNumber().isBlank()) {
                    existing = patientRepository.findByUser_PhoneNumber(rel.getPhoneNumber());
                }
                if (existing.isPresent()) {
                    patient = existing.get();
                } else {
                    if (rel.getPhoneNumber() == null || rel.getPhoneNumber().isBlank()) {
                        throw new AppException(ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS);
                    }

                    String email = (rel.getEmail() != null && !rel.getEmail().isBlank())
                            ? rel.getEmail()
                            : "patient_" + rel.getPhoneNumber() + "@placeholder.local";
                    User newUser = User.builder()
                            .username("patient_" + rel.getPhoneNumber())
                            .fullName(rel.getFullName())
                            .phoneNumber(rel.getPhoneNumber())
                            .email(email)
                            .cccd(relativeCccd)
                            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .isActive(true)
                            .isDeleted(false)
                            .build();
                    Role patientRole = roleRepository.findByRoleName("ROLE_PATIENT")
                            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
                    Set<Role> roles = new HashSet<>();
                    roles.add(patientRole);
                    newUser.setRoles(roles);
                    User savedUser = userRepository.save(newUser);
                    String patientId;
                    do {
                        patientId = "BN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    } while (patientRepository.existsByPatientId(patientId));
                    Patient newPatient = Patient.builder()
                            .patientId(patientId)
                            .user(savedUser)
                            .dateOfBirth(rel.getDateOfBirth())
                            .build();
                    patient = patientRepository.save(newPatient);
                }
            }
        }

        FollowUpBookingContext followUpContext = resolveFollowUpBookingContext(
                patient,
                request.getFollowUpSourceRecordId(),
                request.getServiceIds(),
                request.getAppointmentDate(),
                request.getAppointmentTime());
        if (followUpContext != null) {
            ensureBookerCanUseFollowUpSource(booker, patient, followUpContext.sourceRecord());
        }
        List<Service> services = followUpContext != null
                ? followUpContext.services()
                : loadSelectableServices(request.getServiceIds());
        List<Service> schedulingServices = extractSchedulingServices(services);

        if (services.isEmpty()) {
            throw new AppException(ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS);
        }

        // Build notes (prepend relationship for relative booking)
        String notes = request.getNotes();
        if (request.getRelative() != null && request.getRelative().getRelationship() != null) {
            notes = "[Quan hệ: " + request.getRelative().getRelationship() + "] "
                    + (notes != null ? notes : "");
        }

        if (followUpContext != null) {
            notes = "[Tai kham: " + followUpContext.sourceRecord().getRecordId() + "] "
                    + (notes != null ? notes : "");
        }

        AppointmentSelections selections = resolveAppointmentSelections(
            services,
            followUpContext != null ? followUpContext.preferredDoctorId() : null,
            null,
            null,
            request.getAppointmentDate(),
            request.getAppointmentTime());

        validateSlotCapacity(
            request.getAppointmentDate(),
            request.getAppointmentTime(),
            schedulingServices,
            selections.primaryRoom());

        String apptId;
        do {
            apptId = "APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (appointmentRepository.existsById(apptId));

        Appointment appt = Appointment.builder()
                .appointmentId(apptId)
                .patient(patient)
                .bookedByPatientId(booker.getPatientId())
                .services(services)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .doctor(selections.primaryDoctor())
                .paraclinicalDoctor(selections.paraclinicalDoctor())
                .room(selections.primaryRoom())
                .paraclinicalRoom(selections.paraclinicalRoom())
                .status("REQUESTED")
                .visitType(followUpContext != null ? "FOLLOW_UP" : "INITIAL")
                .followUpSourceRecord(followUpContext != null ? followUpContext.sourceRecord() : null)
                .notes(notes)
                .build();

        return AppointmentResponse.fromEntity(appointmentRepository.save(appt));
    }

    private FollowUpBookingContext resolveFollowUpBookingContext(
            Patient patient,
            String sourceRecordId,
            List<String> requestedServiceIds,
            LocalDate appointmentDate,
            LocalTime appointmentTime) {
        if (sourceRecordId == null || sourceRecordId.isBlank()) {
            return null;
        }
        if (patient == null || patient.getPatientId() == null) {
            throw new AppException(ErrorCode.PATIENT_NOT_FOUND);
        }

        MedicalRecord sourceRecord = medicalRecordRepository.findById(sourceRecordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        if (sourceRecord.getPatient() == null
                || !patient.getPatientId().equals(sourceRecord.getPatient().getPatientId())
                || !isCompletedRecord(sourceRecord)) {
            throw new AppException(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE);
        }

        Service clinicalService = resolveClinicalServiceForFollowUp(sourceRecord)
                .orElseThrow(() -> new AppException(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE));
        validateFollowUpRequestedServices(requestedServiceIds, clinicalService);

        List<Service> services = new ArrayList<>(List.of(clinicalService));
        String preferredDoctorId = resolvePreferredFollowUpDoctorId(
                sourceRecord,
                services,
                appointmentDate,
                appointmentTime);
        return new FollowUpBookingContext(sourceRecord, services, preferredDoctorId);
    }

    private Optional<Service> resolveClinicalServiceForFollowUp(MedicalRecord record) {
        if (record == null || record.getAppointment() == null || record.getAppointment().getServices() == null) {
            return Optional.empty();
        }

        return record.getAppointment().getServices().stream()
                .filter(service -> service.getDepartment() != null
                        && service.getDepartment().getDeptType() == Department.DeptType.CLINICAL)
                .findFirst();
    }

    private void validateFollowUpRequestedServices(List<String> requestedServiceIds, Service clinicalService) {
        Set<String> requested = requestedServiceIds == null
                ? Set.of()
                : requestedServiceIds.stream()
                        .filter(serviceId -> serviceId != null && !serviceId.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return;
        }

        if (requested.size() != 1 || !requested.contains(clinicalService.getServiceId())) {
            throw new AppException(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE);
        }
    }

    private String resolvePreferredFollowUpDoctorId(
            MedicalRecord sourceRecord,
            List<Service> services,
            LocalDate appointmentDate,
            LocalTime appointmentTime) {
        Doctor sourceDoctor = sourceRecord != null ? sourceRecord.getDoctor() : null;
        if (sourceDoctor == null || sourceDoctor.getDoctorId() == null) {
            return null;
        }
        if (services == null || services.isEmpty() || services.stream().anyMatch(service -> !doctorMatchesService(sourceDoctor, service))) {
            return null;
        }

        Optional<Room> scheduledRoom = findScheduledRoom(sourceDoctor, appointmentDate, appointmentTime);
        if (scheduledRoom.isPresent() && roomSupportsServices(scheduledRoom.get(), services)) {
            return sourceDoctor.getDoctorId();
        }

        return null;
    }

    private boolean isCompletedRecord(MedicalRecord record) {
        return record != null && ("COMPLETED".equals(record.getStatus()) || "FINISHED".equals(record.getStatus()));
    }

    private void ensureBookerCanUseFollowUpSource(Patient booker, Patient appointmentPatient, MedicalRecord sourceRecord) {
        if (booker == null || appointmentPatient == null || sourceRecord == null) {
            throw new AppException(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE);
        }

        boolean bookingForSelf = booker.getPatientId() != null
                && booker.getPatientId().equals(appointmentPatient.getPatientId());
        boolean sourceWasBookedByCurrentPatient = sourceRecord.getAppointment() != null
                && booker.getPatientId() != null
                && booker.getPatientId().equals(sourceRecord.getAppointment().getBookedByPatientId());

        if (!bookingForSelf && !sourceWasBookedByCurrentPatient) {
            throw new AppException(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE);
        }
    }

    private boolean isFollowUpAppointment(Appointment appointment) {
        return appointment != null
                && ("FOLLOW_UP".equalsIgnoreCase(appointment.getVisitType())
                || appointment.getFollowUpSourceRecord() != null);
    }

    private record FollowUpBookingContext(
            MedicalRecord sourceRecord,
            List<Service> services,
            String preferredDoctorId) {
    }

    private AppointmentSelections resolveAppointmentSelections(
            List<Service> services,
            String doctorId,
            String paraclinicalDoctorId,
            String roomId,
            LocalDate appointmentDate,
            LocalTime appointmentTime) {
        List<Service> clinicalServices = extractClinicalServices(services);
        List<Service> paraclinicalServices = extractParaclinicalServices(services);

        Doctor primaryDoctor = resolveOptionalDoctor(doctorId);
        Doctor selectedParaclinicalDoctor = resolveOptionalDoctor(paraclinicalDoctorId);

        if (clinicalServices.isEmpty()
                && !paraclinicalServices.isEmpty()
                && selectedParaclinicalDoctor == null
                && primaryDoctor != null
                && isParaclinicalDoctor(primaryDoctor)) {
            selectedParaclinicalDoctor = primaryDoctor;
            primaryDoctor = null;
        }

        validateAppointmentDoctorAndServices(primaryDoctor, selectedParaclinicalDoctor, services);

        Room clinicalRoom = null;
        if (!clinicalServices.isEmpty()) {
            if (roomId != null && !roomId.isBlank()) {
                clinicalRoom = resolveAppointmentRoom(roomId, primaryDoctor, appointmentDate, appointmentTime);
                if (primaryDoctor == null) {
                    OnDutyRoomAssignmentResolver.RoomDoctorAssignment roomAssignment = findAssignmentForRoom(
                            clinicalServices,
                            clinicalRoom,
                            appointmentDate,
                            appointmentTime,
                            true).orElse(null);
                    if (roomAssignment == null || roomAssignment.doctor() == null) {
                        throw new RuntimeException("Phòng đã chọn chưa có bác sĩ trực phù hợp với dịch vụ trong ca đã chọn.");
                    }
                    primaryDoctor = roomAssignment.doctor();
                }
            } else if (primaryDoctor != null) {
                clinicalRoom = resolveAppointmentRoom(null, primaryDoctor, appointmentDate, appointmentTime);
            } else {
                OnDutyRoomAssignmentResolver.RoomDoctorAssignment bestAssignment = pickBestAssignment(
                        clinicalServices,
                        appointmentDate,
                        appointmentTime,
                        true);
                if (bestAssignment == null || bestAssignment.doctor() == null || bestAssignment.room() == null) {
                    throw new RuntimeException("Không có bác sĩ/phòng trực phù hợp với dịch vụ lâm sàng trong ca đã chọn.");
                }
                primaryDoctor = bestAssignment.doctor();
                clinicalRoom = bestAssignment.room();
            }

            if (primaryDoctor == null || clinicalRoom == null || !roomSupportsServices(clinicalRoom, clinicalServices)) {
                throw new RuntimeException("Bác sĩ được chọn chưa trực tại phòng phù hợp với dịch vụ đã chọn.");
            }
        }

        Room paraclinicalRoom = null;
        if (!paraclinicalServices.isEmpty()) {
            ensureServicesHaveOnDutyAssignments(
                    paraclinicalServices,
                    appointmentDate,
                    appointmentTime,
                    "cận lâm sàng");
            OnDutyRoomAssignmentResolver.RoomDoctorAssignment paraclinicalAssignment = pickBestAssignment(
                    List.of(paraclinicalServices.get(0)),
                    appointmentDate,
                    appointmentTime,
                    true);
            if (paraclinicalAssignment == null || paraclinicalAssignment.doctor() == null || paraclinicalAssignment.room() == null) {
                throw new RuntimeException("Không có bác sĩ/phòng trực phù hợp với dịch vụ cận lâm sàng trong ca đã chọn.");
            }
            selectedParaclinicalDoctor = paraclinicalAssignment.doctor();
            paraclinicalRoom = paraclinicalAssignment.room();
        }

        if (clinicalServices.isEmpty()) {
            primaryDoctor = selectedParaclinicalDoctor;
            clinicalRoom = paraclinicalRoom;
        }

        return new AppointmentSelections(
                primaryDoctor,
                clinicalRoom,
                selectedParaclinicalDoctor,
                paraclinicalRoom);
    }

    private Doctor resolveOptionalDoctor(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) {
            return null;
        }

        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
    }

    private Room resolveParaclinicalRoom(
            Doctor paraclinicalDoctor,
            List<Service> paraclinicalServices,
            LocalDate appointmentDate,
            LocalTime appointmentTime) {
        if (paraclinicalDoctor == null || paraclinicalServices == null || paraclinicalServices.isEmpty()) {
            return null;
        }

        Room room = resolveAppointmentRoom(null, paraclinicalDoctor, appointmentDate, appointmentTime);
        if (room == null) {
            throw new RuntimeException("Bác sĩ cận lâm sàng được chọn chưa có phòng hoạt động.");
        }

        if (!roomSupportsServices(room, paraclinicalServices)) {
            throw new RuntimeException("Bác sĩ cận lâm sàng được chọn không phụ trách dịch vụ đã chọn.");
        }

        return room;
    }

    private void ensureServicesHaveOnDutyAssignments(
            List<Service> services,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            String serviceGroupLabel) {
        List<String> missingServices = services == null
                ? List.of()
                : services.stream()
                .filter(service -> onDutyRoomAssignmentResolver
                        .findAssignmentsForServiceAt(service, appointmentDate, appointmentTime)
                        .isEmpty())
                .map(Service::getServiceName)
                .filter(Objects::nonNull)
                .toList();

        if (!missingServices.isEmpty()) {
            throw new RuntimeException("Không có bác sĩ/phòng trực phù hợp cho dịch vụ "
                    + serviceGroupLabel + ": " + String.join(", ", missingServices));
        }
    }

    private OnDutyRoomAssignmentResolver.RoomDoctorAssignment pickBestAssignment(
            List<Service> services,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            boolean requireAllServices) {
        if (services == null || services.isEmpty()) {
            return null;
        }

        return onDutyRoomAssignmentResolver
                .findAssignmentsForServicesAt(services, appointmentDate, appointmentTime, requireAllServices)
                .stream()
                .filter(assignment -> assignment.room() != null && assignment.doctor() != null)
                .min(java.util.Comparator
                        .comparingInt((OnDutyRoomAssignmentResolver.RoomDoctorAssignment assignment) ->
                                roomLoadScore(assignment.room()))
                        .thenComparing(assignment -> assignment.room().getRoomId(),
                                java.util.Comparator.nullsLast(String::compareTo)))
                .orElse(null);
    }

    private Optional<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> findAssignmentForRoom(
            List<Service> services,
            Room room,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            boolean requireAllServices) {
        if (services == null || services.isEmpty() || room == null || room.getRoomId() == null) {
            return Optional.empty();
        }

        return onDutyRoomAssignmentResolver
                .findAssignmentsForServicesAt(services, appointmentDate, appointmentTime, requireAllServices)
                .stream()
                .filter(assignment -> assignment.room() != null
                        && room.getRoomId().equals(assignment.room().getRoomId()))
                .findFirst();
    }

    private int roomLoadScore(Room room) {
        if (room == null || room.getCurrentQueueLength() == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, room.getCurrentQueueLength());
    }

    private boolean roomSupportsServices(Room room, List<Service> services) {
        if (room == null || services == null || services.isEmpty()) {
            return false;
        }

        return services.stream().allMatch(service -> service.getRooms() != null
                && service.getRooms().stream().anyMatch(candidate -> room.getRoomId().equals(candidate.getRoomId())));
    }

    private String resolveCurrentClinicalDoctorId(Appointment appointment) {
        if (appointment == null || appointment.getDoctor() == null) {
            return null;
        }

        return extractClinicalServices(appointment.getServices()).isEmpty()
                ? null
                : appointment.getDoctor().getDoctorId();
    }

    private String resolveCurrentParaclinicalDoctorId(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        if (appointment.getParaclinicalDoctor() != null) {
            return appointment.getParaclinicalDoctor().getDoctorId();
        }

        if (!extractParaclinicalServices(appointment.getServices()).isEmpty()
                && extractClinicalServices(appointment.getServices()).isEmpty()
                && appointment.getDoctor() != null
                && isParaclinicalDoctor(appointment.getDoctor())) {
            return appointment.getDoctor().getDoctorId();
        }

        return null;
    }

    private String resolveCurrentClinicalRoomId(Appointment appointment) {
        if (appointment == null || appointment.getRoom() == null) {
            return null;
        }

        return extractClinicalServices(appointment.getServices()).isEmpty()
                ? null
                : appointment.getRoom().getRoomId();
    }

    private record AppointmentSelections(
            Doctor primaryDoctor,
            Room primaryRoom,
            Doctor paraclinicalDoctor,
            Room paraclinicalRoom) {
    }

    private Room resolveAppointmentRoom(
            String roomId,
            Doctor doctor,
            LocalDate appointmentDate,
            LocalTime appointmentTime) {
        if (roomId == null || roomId.isBlank()) {
            if (doctor == null) {
                return null;
            }

            return findScheduledRoom(doctor, appointmentDate, appointmentTime)
                    .orElse(null);
        }

        Room room = roomRepository.findByRoomIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại."));
        if (!isOperationalRoom(room)) {
            throw new RuntimeException("Phòng đã bị khóa hoặc xóa mềm.");
        }

        if (doctor == null || doctor.getUser() == null || appointmentDate == null || appointmentTime == null) {
            return room;
        }

        Optional<WorkingTimeSlot> slot = findSlotByAppointmentTime(appointmentTime);
        if (slot.isEmpty()) {
            return room;
        }

        boolean matchedSchedule = staffScheduleRepository
                .findFirstByUser_UserIdAndWorkDateAndSlot_SlotIdAndRoom_RoomId(
                        doctor.getUser().getUserId(),
                        appointmentDate,
                        slot.get().getSlotId(),
                        roomId)
                .isPresent();

        if (!matchedSchedule) {
            throw new RuntimeException("Bác sĩ được chọn không trực tại phòng này trong ca đã chọn.");
        }

        return room;
    }

    private Optional<Room> findScheduledRoom(Doctor doctor, LocalDate appointmentDate, LocalTime appointmentTime) {
        if (doctor == null || doctor.getUser() == null || appointmentDate == null || appointmentTime == null) {
            return Optional.empty();
        }

        return findSlotByAppointmentTime(appointmentTime)
                .flatMap(slot -> staffScheduleRepository.findFirstByUser_UserIdAndWorkDateAndSlot_SlotId(
                        doctor.getUser().getUserId(),
                        appointmentDate,
                        slot.getSlotId()))
                .map(g2.be.entity.StaffSchedule::getRoom)
                .filter(this::isOperationalRoom);
    }

    private Optional<WorkingTimeSlot> findSlotByAppointmentTime(LocalTime appointmentTime) {
        if (appointmentTime == null) {
            return Optional.empty();
        }
        Optional<WorkingTimeSlot> exactSlot =
                workingTimeSlotRepository.findByStartTimeAndIsActiveTrueAndIsDeletedFalse(appointmentTime);
        if (exactSlot.isPresent()) {
            return exactSlot;
        }
        return workingTimeSlotRepository.findActiveSlotsContainingTime(appointmentTime)
                .stream()
                .findFirst();
    }

    private Room getAssignedRoom(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        if (appointment.getRoom() != null) {
            return appointment.getRoom();
        }
        if (appointment.getDoctor() != null) {
            List<Service> schedulingServices = extractSchedulingServices(appointment.getServices());
            return findScheduledRoom(
                    appointment.getDoctor(),
                    appointment.getAppointmentDate(),
                    appointment.getAppointmentTime())
                    .filter(room -> roomSupportsServices(room, schedulingServices))
                    .orElse(null);
        }
        return null;
    }

    private void validateAppointmentDoctorAndServices(
            Doctor doctor,
            Doctor paraclinicalDoctor,
            List<Service> services) {
        if (services == null || services.isEmpty()) {
            throw new AppException(ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS);
        }

        validateSchedulableServices(services);

        List<Service> clinicalServices = extractClinicalServices(services);
        List<Service> paraclinicalServices = extractParaclinicalServices(services);

        if (clinicalServices.size() > 1) {
            throw new RuntimeException("Mỗi lịch hẹn chỉ được chọn 1 dịch vụ lâm sàng.");
        }

        if (!clinicalServices.isEmpty()) {
            if (doctor != null && isParaclinicalDoctor(doctor)) {
                throw new RuntimeException("Lịch hẹn có dịch vụ lâm sàng phải được gán bác sĩ lâm sàng.");
            }
        } else if (doctor != null) {
            throw new RuntimeException("Chỉ được chọn bác sĩ khi lịch hẹn có dịch vụ lâm sàng.");
        }

        if (paraclinicalDoctor != null) {
            throw new RuntimeException("Dịch vụ cận lâm sàng được hệ thống tự xếp lịch, không hỗ trợ chọn bác sĩ.");
        }
    }

    private void validateSchedulableServices(List<Service> services) {
        if (services == null || services.isEmpty()) {
            throw new AppException(ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS);
        }

        List<String> invalidServices = services.stream()
                .filter(service -> service.getDepartment() == null
                        || (service.getDepartment().getDeptType() != Department.DeptType.CLINICAL
                        && service.getDepartment().getDeptType() != Department.DeptType.PARACLINICAL))
                .map(Service::getServiceName)
                .toList();

        if (!invalidServices.isEmpty()) {
            throw new RuntimeException("Lịch hẹn chỉ được chứa dịch vụ lâm sàng hoặc cận lâm sàng. Dịch vụ không hợp lệ: "
                    + String.join(", ", invalidServices));
        }
    }

    private void validateSlotCapacity(LocalDate appointmentDate, LocalTime appointmentTime, List<Service> services) {
        validateSlotCapacity(appointmentDate, appointmentTime, services, null);
    }

    private void validateSlotCapacity(
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            List<Service> services,
            Room room) {
        validateAppointmentTimeSelectable(appointmentDate, appointmentTime);
    }

    private void validateAppointmentTimeSelectable(LocalDate appointmentDate, LocalTime appointmentTime) {
        if (appointmentDate == null || appointmentTime == null) {
            throw new AppException(ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS);
        }

        WorkingTimeSlot slot = findSlotByAppointmentTime(appointmentTime)
                .orElseThrow(() -> new AppException(ErrorCode.CREATE_APPOINTMENT_TIME_EXPIRED));
        if (slot.getSlotId() == null || isSlotExpired(appointmentDate, slot.getEndTime())) {
            throw new AppException(ErrorCode.CREATE_APPOINTMENT_TIME_EXPIRED);
        }
    }

    private boolean isSlotExpired(LocalDate appointmentDate, LocalTime slotEndTime) {
        if (appointmentDate == null || slotEndTime == null) {
            return false;
        }

        LocalDate today = ClinicTime.today();
        if (appointmentDate.isBefore(today)) {
            return true;
        }

        return appointmentDate.isEqual(today)
                && !ClinicTime.now().toLocalTime().isBefore(slotEndTime);
    }

    private List<Service> loadSchedulableServices(List<String> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return List.of();
        }

        List<Service> services = loadSelectableServices(serviceIds);
        validateSchedulableServices(services);

        List<Service> schedulingServices = extractSchedulingServices(services);
        if (schedulingServices.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một dịch vụ để xem ca khám.");
        }
        return schedulingServices;
    }

    private List<Service> extractClinicalServices(List<Service> services) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }

        return services.stream()
                .filter(service -> service.getDepartment() != null
                        && service.getDepartment().getDeptType() == g2.be.entity.Department.DeptType.CLINICAL)
                .toList();
    }

    private List<Service> extractParaclinicalServices(List<Service> services) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }

        return services.stream()
                .filter(service -> service.getDepartment() != null
                        && service.getDepartment().getDeptType() == Department.DeptType.PARACLINICAL)
                .toList();
    }

    private List<Service> extractSchedulingServices(List<Service> services) {
        List<Service> clinicalServices = extractClinicalServices(services);
        if (!clinicalServices.isEmpty()) {
            return clinicalServices;
        }
        return extractParaclinicalServices(services);
    }

    private List<Service> extractAllSchedulableServices(List<Service> services) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }

        return services.stream()
                .filter(service -> service.getDepartment() != null
                        && (service.getDepartment().getDeptType() == Department.DeptType.CLINICAL
                        || service.getDepartment().getDeptType() == Department.DeptType.PARACLINICAL))
                .distinct()
                .toList();
    }

    private boolean hasAvailableAssignmentsForAllServices(
            List<Service> services,
            LocalDate appointmentDate,
            LocalTime appointmentTime) {
        if (services == null || services.isEmpty()) {
            return false;
        }

        return services.stream().allMatch(service -> !onDutyRoomAssignmentResolver
                .findAssignmentsForServiceAt(service, appointmentDate, appointmentTime)
                .isEmpty());
    }

    private Department.DeptType resolveSchedulingDeptType(List<Service> services) {
        if (!extractClinicalServices(services).isEmpty()) {
            return Department.DeptType.CLINICAL;
        }
        if (!extractParaclinicalServices(services).isEmpty()) {
            return Department.DeptType.PARACLINICAL;
        }
        return Department.DeptType.CLINICAL;
    }

    private List<Service> loadSelectableServices(List<String> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return List.of();
        }

        List<String> normalizedServiceIds = serviceIds.stream()
                .filter(serviceId -> serviceId != null && !serviceId.isBlank())
                .distinct()
            .collect(Collectors.toCollection(ArrayList::new));

        if (normalizedServiceIds.isEmpty()) {
            return List.of();
        }

        List<Service> services = serviceRepository.findAllByServiceIdInAndIsDeletedFalseAndIsActiveTrue(normalizedServiceIds);
        if (services.size() != normalizedServiceIds.size()) {
            throw new AppException(ErrorCode.SERVICE_NOT_FOUND);
        }

        var servicesById = services.stream()
                .collect(Collectors.toMap(Service::getServiceId, Function.identity()));

        return normalizedServiceIds.stream()
                .map(servicesById::get)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean roomMatchesDeptType(Room room, Department.DeptType deptType) {
        return room != null
                && room.getDepartment() != null
                && room.getDepartment().getDeptType() == deptType;
    }

    private boolean isDoctorAssignableForDeptType(Doctor doctor, Department.DeptType deptType) {
        if (doctor == null) {
            return false;
        }
        return deptType == Department.DeptType.PARACLINICAL ? isParaclinicalDoctor(doctor) : !isParaclinicalDoctor(doctor);
    }

    private boolean isOperationalRoom(Room room) {
        return room != null
                && !Boolean.TRUE.equals(room.getIsDeleted())
                && "ACTIVE".equalsIgnoreCase(room.getStatus())
                && room.getDepartment() != null
                && !Boolean.TRUE.equals(room.getDepartment().getIsDeleted())
                && Boolean.TRUE.equals(room.getDepartment().getIsActive());
    }

    private String requireCancelReason(CancelAppointmentRequest request) {
        if (request == null || request.getCancelReason() == null || request.getCancelReason().isBlank()) {
            throw new AppException(ErrorCode.UPDATE_PROFILE_MISSING_FIELDS);
        }
        return request.getCancelReason().trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }



    private boolean isParaclinicalDoctor(Doctor doctor) {
        return DoctorDepartmentClassifier.isParaclinical(doctor);
    }
}

