package g2.be.service;

import g2.be.dto.request.CreateServiceOrderRequest;
import g2.be.dto.response.PendingOrdersByRecordResponse;
import g2.be.dto.response.ServiceOrderResponse;
import g2.be.entity.Appointment;
import g2.be.entity.Doctor;
import g2.be.entity.Invoice;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Patient;
import g2.be.entity.Service;
import g2.be.entity.ServiceOrder;
import g2.be.entity.StaffSchedule;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.MedicalRecordRepository;
import g2.be.repository.PatientRepository;
import g2.be.repository.RoomRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.repository.ServiceRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.util.ClinicTime;
import g2.be.util.DoctorDepartmentClassifier;
import g2.be.util.QueuePriorityRules;
import g2.be.util.ServiceOrderResultFiles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ServiceRepository serviceRepository;
    private final CloudinaryService cloudinaryService;
    private final PdfExportService pdfExportService;
    private final DoctorRepository doctorRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final InvoiceRepository invoiceRepository;
    private final PatientRepository patientRepository;
    private final CitizenInsuranceService citizenInsuranceService;
    private final RoomRepository roomRepository;
    private final OnDutyRoomAssignmentResolver onDutyRoomAssignmentResolver;

    @Transactional(readOnly = true)
    public ServiceOrderResponse getServiceOrder(String orderId) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND)); 
        
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderResponse> getByRecord(String recordId) {
        return serviceOrderRepository.findByMedicalRecord_RecordId(recordId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderResponse> getCompletedByPatient(String patientId) {
        return serviceOrderRepository.findByMedicalRecord_Patient_PatientIdAndStatusOrderByCreatedAtDesc(patientId, "COMPLETED")
                .stream()
                .filter(so -> (so.getResultNote() != null && !so.getResultNote().isBlank())
                           || ServiceOrderResultFiles.hasAny(so.getResultFileUrl()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderResponse> getCompletedByCurrentPatient(Long userId) {
        Patient patient = resolveCurrentPatient(userId);
        return serviceOrderRepository
                .findVisibleByPatientAndStatusOrderByCreatedAtDesc(patient.getPatientId(), "COMPLETED")
                .stream()
                .filter(so -> (so.getResultNote() != null && !so.getResultNote().isBlank())
                           || ServiceOrderResultFiles.hasAny(so.getResultFileUrl()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderResponse> getMyRecordResults(Long userId, String recordId) {
        Patient patient = resolveCurrentPatient(userId);
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_FOUND));

        ensureRecordVisibleToPatient(medicalRecord, patient.getPatientId());

        return serviceOrderRepository.findByMedicalRecord_RecordId(recordId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderResponse> getByAppointment(String appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }

        return serviceOrderRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(appointmentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderResponse> getMyQueueByAppointment(Long userId, String appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }

        LocalDate queryDate = ClinicTime.today();
        Set<String> roomIds = resolveStaffRoomIds(userId, queryDate);

        if (roomIds.isEmpty()) {
            return List.of();
        }

        return serviceOrderRepository.findByRoomIdsAndAppointmentIdAndDate(roomIds, appointmentId, queryDate)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ServiceOrderResponse> createServiceOrders(Long actorUserId, CreateServiceOrderRequest request) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(request.getRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_FOUND)); 

        validateServiceOrderRequest(actorUserId, medicalRecord, request.getServiceIds());

        List<ServiceOrder> createdOrders = new ArrayList<>();

        for (String serviceId : request.getServiceIds()) {
            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
            g2.be.entity.Room assignedRoom = findOptimalRoomForService(service, medicalRecord.getAppointment());

            ServiceOrder serviceOrder = ServiceOrder.builder()
                    .orderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .medicalRecord(medicalRecord)
                    .service(service)
                    .assignedRoom(assignedRoom)
                    .technicianId(resolveTechnicianId(service, assignedRoom, medicalRecord.getAppointment()))
                    .indicationNote(normalizeOptionalText(request.getIndicationNote()))
                    .status("PENDING")
                    .orderType("INITIAL")
                    .build();

            createdOrders.add(serviceOrderRepository.save(serviceOrder));
        }

        return createdOrders.stream()
            .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ServiceOrderResponse> createSupplementOrders(Long actorUserId, CreateServiceOrderRequest request) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(request.getRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_FOUND));
        ensureRecordOpenForSupplement(medicalRecord);

        validateServiceOrderRequest(actorUserId, medicalRecord, request.getServiceIds());

        List<ServiceOrder> createdOrders = new ArrayList<>();

        for (String serviceId : request.getServiceIds()) {
            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
            g2.be.entity.Room assignedRoom = findOptimalRoomForService(service, medicalRecord.getAppointment());

            ServiceOrder serviceOrder = ServiceOrder.builder()
                    .orderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .medicalRecord(medicalRecord)
                    .service(service)
                    .assignedRoom(assignedRoom)
                    .technicianId(resolveTechnicianId(service, assignedRoom, medicalRecord.getAppointment()))
                    .indicationNote(normalizeOptionalText(request.getIndicationNote()))
                    .status("PENDING")
                    .orderType("SUPPLEMENT")
                    .build();

            createdOrders.add(serviceOrderRepository.save(serviceOrder));
        }

        return createdOrders.stream()
            .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void ensureRecordOpenForSupplement(MedicalRecord medicalRecord) {
        String medicalRecordStatus = medicalRecord.getStatus();
        String appointmentStatus = medicalRecord.getAppointment() != null
                ? medicalRecord.getAppointment().getStatus()
                : null;

        if ("COMPLETED".equals(medicalRecordStatus) || "COMPLETED".equals(appointmentStatus)) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_ALREADY_FINISHED);
        }
    }

    @Transactional
    public ServiceOrderResponse uploadResultFile(String orderId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new AppException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        String url = cloudinaryService.uploadFile(file, "clinic_results");
        order.setResultFileUrl(ServiceOrderResultFiles.append(order.getResultFileUrl(), url));
        order.setStatus("COMPLETED");

        ServiceOrder savedOrder = serviceOrderRepository.save(order);
        returnClinicalAppointmentIfParaclinicalDone(savedOrder);
        return toResponse(savedOrder);
    }

    @Transactional
    public ServiceOrderResponse updateResult(String orderId, String resultNote) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new AppException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        order.setResultNote(resultNote);
        order.setStatus("COMPLETED");

        ServiceOrder savedOrder = serviceOrderRepository.save(order);
        returnClinicalAppointmentIfParaclinicalDone(savedOrder);
        return toResponse(savedOrder);
    }

    @Transactional
    public ServiceOrderResponse cancelOrder(String orderId) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new AppException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }

        String paymentStatus = resolvePaymentStatus(order);
        boolean hasResult = (order.getResultNote() != null && !order.getResultNote().isBlank())
        || ServiceOrderResultFiles.hasAny(order.getResultFileUrl());

        if (hasResult || "COMPLETED".equals(order.getStatus()) || "PAID".equals(order.getStatus())
                || "PAID".equals(paymentStatus)) {
            throw new AppException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        order.setStatus("CANCELLED");
        ServiceOrder savedOrder = serviceOrderRepository.save(order);
        returnClinicalAppointmentIfParaclinicalDone(savedOrder);

        if (savedOrder.getInvoiceId() != null && !savedOrder.getInvoiceId().isBlank()) {
            synchronizeInvoiceAfterCancellation(savedOrder.getInvoiceId());
        }

        return toResponse(savedOrder);
    }

    private void returnClinicalAppointmentIfParaclinicalDone(ServiceOrder changedOrder) {
        if (changedOrder == null || changedOrder.getMedicalRecord() == null) {
            return;
        }

        MedicalRecord medicalRecord = changedOrder.getMedicalRecord();
        Appointment appointment = medicalRecord.getAppointment();
        if (appointment == null || !"WAITING_RESULTS".equalsIgnoreCase(appointment.getStatus())) {
            return;
        }

        List<ServiceOrder> paraclinicalOrders = serviceOrderRepository
                .findByMedicalRecord_RecordId(medicalRecord.getRecordId()).stream()
                .filter(this::isActiveParaclinicalOrder)
                .toList();

        if (paraclinicalOrders.isEmpty()) {
            return;
        }

        boolean allCompleted = paraclinicalOrders.stream()
                .allMatch(order -> "COMPLETED".equalsIgnoreCase(order.getStatus()));
        if (!allCompleted) {
            return;
        }

        appointment.setStatus("WAITING");
        appointment.setPriorityScore(QueuePriorityRules.RETURNING_FROM_PARACLINICAL);
        appointment.setWaitingSince(ClinicTime.now());
        appointmentRepository.save(appointment);
        incrementClinicalRoomQueueLength(appointment);

        log.info("Returned appointment {} to clinical queue after paraclinical completion with priorityScore={}",
                appointment.getAppointmentId(), appointment.getPriorityScore());
    }

    private boolean isActiveParaclinicalOrder(ServiceOrder order) {
        return order != null
                && !"CANCELLED".equalsIgnoreCase(order.getStatus())
                && order.getService() != null
                && order.getService().getDepartment() != null
                && order.getService().getDepartment().getDeptType() == g2.be.entity.Department.DeptType.PARACLINICAL;
    }

    private void incrementClinicalRoomQueueLength(Appointment appointment) {
        g2.be.entity.Room room = resolveAppointmentRoom(appointment);
        if (room == null) {
            return;
        }

        room.setCurrentQueueLength((room.getCurrentQueueLength() != null ? room.getCurrentQueueLength() : 0) + 1);
        roomRepository.save(room);
    }

    private g2.be.entity.Room resolveAppointmentRoom(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        if (appointment.getRoom() != null) {
            return appointment.getRoom();
        }

        List<Service> clinicalServices = appointment.getServices() == null
                ? List.of()
                : appointment.getServices().stream()
                        .filter(service -> service != null
                                && service.getDepartment() != null
                                && service.getDepartment().getDeptType() == g2.be.entity.Department.DeptType.CLINICAL)
                        .toList();
        if (clinicalServices.isEmpty()) {
            return null;
        }

        return onDutyRoomAssignmentResolver
                .findAssignmentsForServicesAt(
                        clinicalServices,
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime(),
                        true)
                .stream()
                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::room)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(String orderId) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        return pdfExportService.generateServiceOrderPdf(order);
    }

    @Transactional(readOnly = true)
    public byte[] exportPdfForPatient(Long userId, String orderId) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!belongsToPatientUser(order, userId)) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        return pdfExportService.generateServiceOrderPdf(order);
    }

    private boolean belongsToPatientUser(ServiceOrder order, Long userId) {
        if (order == null || order.getMedicalRecord() == null || userId == null) {
            return false;
        }

        MedicalRecord medicalRecord = order.getMedicalRecord();
        boolean ownRecord = medicalRecord.getPatient() != null
                && medicalRecord.getPatient().getUser() != null
                && userId.equals(medicalRecord.getPatient().getUser().getUserId());

        if (ownRecord) {
            return true;
        }

        return patientRepository.findByUser_UserId(userId)
                .map(patient -> recordVisibleToPatient(medicalRecord, patient.getPatientId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Page<ServiceOrderResponse> getAllOrders(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return serviceOrderRepository
                    .findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable)
                    .map(this::toResponse);
        }
        return serviceOrderRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ServiceOrderResponse> getSupplementOrders(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return serviceOrderRepository
                    .findByOrderTypeAndStatusOrderByCreatedAtDesc("SUPPLEMENT", status.toUpperCase(), pageable)
                    .map(this::toResponse);
        }
        return serviceOrderRepository
                .findByOrderTypeOrderByCreatedAtDesc("SUPPLEMENT", pageable)
                .map(this::toResponse);
    }

    /**
         * Lấy danh sách chỉ định bổ sung (SUPPLEMENT) của một hồ sơ khám cụ thể.
         * Dùng cho thu ngân xem trước khi thu tiền bổ sung.
         * Trả về các orders SUPPLEMENT có status=PENDING của hồ sơ, kèm thông tin bệnh nhân và tổng tiền.
     */
    @Transactional(readOnly = true)
    public PendingOrdersByRecordResponse getSupplementOrdersByRecord(String recordId) {
        MedicalRecord mr = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_FOUND));

        List<ServiceOrder> orders = serviceOrderRepository
            .findByMedicalRecord_RecordIdAndOrderTypeAndStatus(recordId, "SUPPLEMENT", "PENDING");

        List<ServiceOrderResponse> orderDtos = orders.stream()
                .map(order -> {
                    ServiceOrderResponse dto = ServiceOrderResponse.fromEntity(order);
                    dto.setPaymentStatus(resolvePaymentStatus(order));
                    dto.setUnpaid(isUnpaid(order));
                    g2.be.entity.Room assignedRoom = resolveAssignedRoom(order);
                    if (assignedRoom != null) {
                        dto.setAssignedRoomId(assignedRoom.getRoomId());
                        dto.setAssignedRoomName(assignedRoom.getRoomName());
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        java.math.BigDecimal totalAmount = orders.stream()
                .map(so -> so.getService().getPrice())
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        String patientId = null;
        String patientName = null;
        String phoneNumber = null;
        String healthInsuranceNumber = null;
        if (mr.getPatient() != null) {
            patientId = mr.getPatient().getPatientId();
            healthInsuranceNumber = mr.getPatient().getHealthInsuranceNumber();
            if (mr.getPatient().getUser() != null) {
                patientName = mr.getPatient().getUser().getFullName();
                phoneNumber = mr.getPatient().getUser().getPhoneNumber();
            }
        }

        // Get doctor/department/appointment info from medical record
        String doctorName = null;
        String departmentName = null;
        java.time.LocalDate appointmentDate = null;
        java.time.LocalTime appointmentTime = null;
        if (mr.getDoctor() != null) {
            if (mr.getDoctor().getUser() != null) {
                doctorName = mr.getDoctor().getUser().getFullName();
            }
            if (mr.getDoctor().getDepartment() != null) {
                departmentName = mr.getDoctor().getDepartment().getDeptName();
            }
        }
        if (mr.getAppointment() != null) {
            appointmentDate = mr.getAppointment().getAppointmentDate();
            appointmentTime = mr.getAppointment().getAppointmentTime();
        }

        return PendingOrdersByRecordResponse.builder()
                .recordId(recordId)
                .patientId(patientId)
                .patientName(patientName)
                .phoneNumber(phoneNumber)
                .healthInsuranceNumber(healthInsuranceNumber)
                .doctorName(doctorName)
                .departmentName(departmentName)
                .appointmentDate(appointmentDate)
                .appointmentTime(appointmentTime)
                .orders(orderDtos)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * Lấy danh sách service orders cho bác sĩ cận lâm sàng (specialist).
     * Tìm phòng bác sĩ được phân công trong ca hiện tại,
     * rồi lấy service orders được gán trực tiếp tới phòng đó.
     */
    @Transactional(readOnly = true)
    public Page<ServiceOrderResponse> getMyQueue(Long userId, String status, Pageable pageable) {
        LocalDate queryDate = ClinicTime.today();
        Set<String> roomIds = resolveStaffRoomIds(userId, queryDate);

        if (roomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        String statusParam = (status != null && !status.isBlank()) ? status.toUpperCase(Locale.ROOT) : "PAID";
        return serviceOrderRepository.findByRoomIdsAndDate(roomIds, queryDate, ClinicTime.now(), statusParam, pageable)
                .map(this::toResponse);
    }

    private ServiceOrderResponse toResponse(ServiceOrder order) {
        ServiceOrderResponse response = ServiceOrderResponse.fromEntity(order, resolvePaymentStatus(order));
        g2.be.entity.Room assignedRoom = resolveAssignedRoom(order);
        if (assignedRoom != null) {
            response.setAssignedRoomId(assignedRoom.getRoomId());
            response.setAssignedRoomName(assignedRoom.getRoomName());
        }
        return response;
    }

    private g2.be.entity.Room resolveAssignedRoom(ServiceOrder order) {
        if (order == null || order.getService() == null) {
            return null;
        }

        g2.be.entity.Appointment appointment = order.getMedicalRecord() != null
                ? order.getMedicalRecord().getAppointment()
                : null;

        if (roomCanReceiveService(order.getAssignedRoom(), order.getService(), appointment)) {
            return order.getAssignedRoom();
        }

        if (appointment != null && appointment.getServices() != null) {
            boolean matchesAppointmentService = appointment.getServices().stream()
                    .anyMatch(service -> service.getServiceId().equals(order.getService().getServiceId()));

            if (matchesAppointmentService
                    && order.getService().getDepartment() != null
                    && order.getService().getDepartment().getDeptType() == g2.be.entity.Department.DeptType.PARACLINICAL) {
                if (roomCanReceiveService(appointment.getParaclinicalRoom(), order.getService(), appointment)) {
                    return appointment.getParaclinicalRoom();
                }

                if (roomCanReceiveService(appointment.getRoom(), order.getService(), appointment)) {
                    return appointment.getRoom();
                }

                return findOptimalRoomForService(order.getService(), appointment);
            }

            if (matchesAppointmentService) {
                if (roomCanReceiveService(appointment.getRoom(), order.getService(), appointment)) {
                    return appointment.getRoom();
                }

                return findOptimalRoomForService(order.getService(), appointment);
            }
        }

        return findOptimalRoomForService(order.getService(), appointment);
    }

    private Set<String> resolveStaffRoomIds(Long userId, LocalDate queryDate) {
        List<StaffSchedule> todaySchedules = Optional.ofNullable(staffScheduleRepository
                .findByUser_UserIdAndWorkDate(userId, queryDate))
                .orElse(List.of());

        Set<String> currentShiftRoomIds = todaySchedules.stream()
                .filter(this::isCurrentShiftSchedule)
                .map(schedule -> resolveRoomIdFromScheduleOrLinkedDoctor(schedule, todaySchedules))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        if (!currentShiftRoomIds.isEmpty()) {
            return currentShiftRoomIds;
        }

        return Set.of();
    }

    private Optional<String> resolveRoomIdFromScheduleOrLinkedDoctor(
            StaffSchedule schedule,
            List<StaffSchedule> daySchedules) {
        if (schedule == null) {
            return Optional.empty();
        }

        if (schedule.getRoom() != null && schedule.getRoom().getRoomId() != null) {
            return Optional.of(schedule.getRoom().getRoomId());
        }

        String supportingDoctorName = extractSupportingDoctorName(schedule.getNote());
        if (supportingDoctorName == null || daySchedules == null || daySchedules.isEmpty()) {
            return Optional.empty();
        }

        return daySchedules.stream()
                .filter(this::isCurrentShiftSchedule)
                .filter(candidate -> candidate.getRoom() != null && candidate.getRoom().getRoomId() != null)
                .filter(candidate -> candidate.getUser() != null
                        && candidate.getUser().getFullName() != null
                        && supportingDoctorName.equalsIgnoreCase(candidate.getUser().getFullName().trim()))
                .map(candidate -> candidate.getRoom().getRoomId())
                .findFirst();
    }

    private String extractSupportingDoctorName(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }

        String trimmed = note.trim();
        for (String prefix : List.of("Hỗ trợ bác sĩ", "Ho tro bac si")) {
            if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String doctorName = trimmed.substring(prefix.length()).trim();
                return doctorName.isBlank() ? null : doctorName;
            }
        }
        return null;
    }

    private boolean isCurrentShiftSchedule(StaffSchedule schedule) {
        if (schedule == null) {
            return false;
        }

        java.time.LocalTime startTime = schedule.getSlot() != null
                ? schedule.getSlot().getStartTime()
                : schedule.getLegacyStartTime();
        java.time.LocalTime endTime = schedule.getSlot() != null
                ? schedule.getSlot().getEndTime()
                : schedule.getLegacyEndTime();
        java.time.LocalTime now = ClinicTime.now().toLocalTime();
        return startTime != null
                && endTime != null
                && !now.isBefore(startTime)
                && now.isBefore(endTime);
    }

    private Patient resolveCurrentPatient(Long userId) {
        return patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
    }

    private void ensureRecordVisibleToPatient(MedicalRecord medicalRecord, String patientId) {
        if (!recordVisibleToPatient(medicalRecord, patientId)) {
            throw new AppException(ErrorCode.RECORD_NOT_FOUND);
        }
    }

    private boolean recordVisibleToPatient(MedicalRecord medicalRecord, String patientId) {
        if (medicalRecord == null || patientId == null) {
            return false;
        }

        boolean ownRecord = medicalRecord.getPatient() != null
                && patientId.equals(medicalRecord.getPatient().getPatientId());
        boolean bookedByPatient = medicalRecord.getAppointment() != null
                && patientId.equals(medicalRecord.getAppointment().getBookedByPatientId());

        return ownRecord || bookedByPatient;
    }

    private String resolvePaymentStatus(ServiceOrder order) {
        if ("CANCELLED".equals(order.getStatus())) {
            return "CANCELLED";
        }

        if (order.getInvoiceId() == null || order.getInvoiceId().isBlank()) {
            return "UNPAID";
        }

        return invoiceRepository.findById(order.getInvoiceId())
                .map(invoice -> {
                    String paymentStatus = invoice.getPaymentStatus();
                    if (paymentStatus == null || paymentStatus.isBlank()) {
                        return "UNPAID";
                    }
                    return paymentStatus.toUpperCase(Locale.ROOT);
                })
                .orElse("UNPAID");
    }

    private boolean isUnpaid(ServiceOrder order) {
        String paymentStatus = resolvePaymentStatus(order);
        return "UNPAID".equals(paymentStatus) || "PENDING".equals(paymentStatus);
    }

    private void synchronizeInvoiceAfterCancellation(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if ("PAID".equals(invoice.getPaymentStatus())) {
            throw new AppException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        List<ServiceOrder> activeOrders = serviceOrderRepository.findByInvoiceId(invoiceId).stream()
                .filter(order -> !"CANCELLED".equals(order.getStatus()))
                .toList();

        if (activeOrders.isEmpty()) {
            invoice.setPaymentStatus("CANCELLED");
            invoice.setTotalServiceFee(BigDecimal.ZERO);
            invoice.setInsurancePaid(BigDecimal.ZERO);
            invoice.setCustomerPaid(BigDecimal.ZERO);
            invoiceRepository.save(invoice);
            return;
        }

        BigDecimal totalServiceFee = activeOrders.stream()
                .map(activeOrder -> activeOrder.getService().getPrice())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal insurancePaid = BigDecimal.ZERO;
        BigDecimal customerPaid = totalServiceFee;

        String cccd = null;
        if (invoice.getMedicalRecord() != null
                && invoice.getMedicalRecord().getPatient() != null
                && invoice.getMedicalRecord().getPatient().getUser() != null) {
            cccd = invoice.getMedicalRecord().getPatient().getUser().getCccd();
        }

        if (cccd != null && !cccd.isBlank()) {
            try {
                var insuranceBreakdown = citizenInsuranceService.calculateInsuranceForServices(
                        cccd,
                        activeOrders.stream().map(ServiceOrder::getService).toList());
                insurancePaid = insuranceBreakdown.getTotalInsurancePaid();
                customerPaid = insuranceBreakdown.getTotalPatientPaid();
            } catch (AppException ex) {
                log.warn("Could not recalculate insurance for invoice {} after cancel: {}", invoiceId, ex.getMessage());
            }
        }

        invoice.setTotalServiceFee(totalServiceFee);
        invoice.setInsurancePaid(insurancePaid);
        invoice.setCustomerPaid(customerPaid);
        invoiceRepository.save(invoice);
    }

    /**
     * Tìm phòng tối ưu cho một dịch vụ dựa trên service_room mapping.
     * Chỉ chọn phòng ACTIVE có bác sĩ được phân công và có hàng đợi ngắn nhất (load balancing).
     */
    private g2.be.entity.Room findOptimalRoomForService(g2.be.entity.Service service) {
        return findOptimalRoomForService(service, null);
    }

    private g2.be.entity.Room findOptimalRoomForService(g2.be.entity.Service service, Appointment appointment) {
        List<g2.be.entity.Room> availableRooms = findAvailableRoomsForService(service, appointment);
        if (availableRooms.isEmpty()) {
            return null;
        }

        return availableRooms.stream()
            .min(java.util.Comparator
                .comparingInt((g2.be.entity.Room room) -> room.getCurrentQueueLength() != null
                    ? room.getCurrentQueueLength()
                    : 0)
                .thenComparingInt(this::roomTypePriority))
                .orElse(null);
    }

    private List<g2.be.entity.Room> findAvailableRoomsForService(g2.be.entity.Service service) {
        return findAvailableRoomsForService(service, null);
    }

    private List<g2.be.entity.Room> findAvailableRoomsForService(g2.be.entity.Service service, Appointment appointment) {
        List<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> assignments = appointment != null
                ? onDutyRoomAssignmentResolver.findAssignmentsForServiceAt(
                        service,
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime())
                : onDutyRoomAssignmentResolver.findCurrentAssignmentsForService(service, null, null);

        return assignments.stream()
                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::room)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private String resolveTechnicianId(Service service, g2.be.entity.Room room, Appointment appointment) {
        if (service == null
                || room == null
                || service.getDepartment() == null
                || service.getDepartment().getDeptType() != g2.be.entity.Department.DeptType.PARACLINICAL) {
            return null;
        }

        Optional<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> assignment = appointment != null
                ? onDutyRoomAssignmentResolver.findAssignmentForRoomAt(
                        service,
                        room,
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime())
                : onDutyRoomAssignmentResolver.findCurrentAssignmentForRoom(service, room, null, null);

        return assignment
                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::doctor)
                .map(Doctor::getDoctorId)
                .orElse(null);
    }

    private boolean roomCanReceiveService(
            g2.be.entity.Room room,
            g2.be.entity.Service service,
            Appointment appointment) {
        if (room == null || service == null) {
            return false;
        }

        if (appointment != null) {
            return onDutyRoomAssignmentResolver.findAssignmentForRoomAt(
                    service,
                    room,
                    appointment.getAppointmentDate(),
                    appointment.getAppointmentTime()).isPresent();
        }

        return onDutyRoomAssignmentResolver.findCurrentAssignmentForRoom(service, room, null, null).isPresent();
    }

    private int roomTypePriority(g2.be.entity.Room room) {
        if (room == null || room.getRoomType() == null) {
            return 99;
        }

        return switch (room.getRoomType()) {
            case "SPECIALTY" -> 0;
            case "LAB" -> 1;
            case "GENERAL" -> 2;
            default -> 3;
        };
    }

    private void validateServiceOrderRequest(Long actorUserId, MedicalRecord medicalRecord, List<String> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new RuntimeException("Chỉ định cận lâm sàng phải có ít nhất một dịch vụ.");
        }

        if (actorUserId != null) {
            doctorRepository.findByUser_UserId(actorUserId).ifPresent(actorDoctor -> {
                if (isParaclinicalDoctor(actorDoctor)) {
                    throw new RuntimeException("Bác sĩ cận lâm sàng không thể tạo chỉ định cận lâm sàng mới.");
                }
            });
        }

        if (medicalRecord.getDoctor() != null && isParaclinicalDoctor(medicalRecord.getDoctor())) {
            throw new RuntimeException("Hồ sơ khám thuộc bác sĩ cận lâm sàng không hợp lệ để tạo chỉ định cận lâm sàng.");
        }

        List<Service> requestedServices = serviceIds.stream()
            .map(serviceId -> serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId)))
            .toList();

        List<String> invalidServices = requestedServices.stream()
                .filter(service -> service.getDepartment() == null || service.getDepartment().getDeptType() != g2.be.entity.Department.DeptType.PARACLINICAL)
                .map(Service::getServiceName)
                .toList();

        if (!invalidServices.isEmpty()) {
            throw new RuntimeException("Chỉ định cận lâm sàng chỉ được chứa dịch vụ PARACLINICAL. Dịch vụ không hợp lệ: "
                    + String.join(", ", invalidServices));
        }

        List<String> servicesWithoutAvailableRooms = requestedServices.stream()
            .filter(service -> findAvailableRoomsForService(service, medicalRecord.getAppointment()).isEmpty())
            .map(Service::getServiceName)
            .toList();

        if (!servicesWithoutAvailableRooms.isEmpty()) {
            throw new RuntimeException("Chưa có phòng thực hiện hợp lệ cho dịch vụ: "
                + String.join(", ", servicesWithoutAvailableRooms)
                + ". Vui lòng cấu hình phòng cho đúng dịch vụ trước khi chỉ định; hệ thống sẽ không tự xếp sang phòng của dịch vụ khác.");
        }
    }

    private boolean isParaclinicalDoctor(Doctor doctor) {
        return DoctorDepartmentClassifier.isParaclinical(doctor);
    }



    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
