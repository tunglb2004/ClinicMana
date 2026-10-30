package g2.be.service;

import g2.be.config.ClinicInsuranceConfig;
import g2.be.dto.request.CreateServiceOrderRequest;
import g2.be.dto.request.PrepaymentRequest;
import g2.be.dto.request.SupplementPaymentRequest;
import g2.be.dto.response.PendingOrdersByRecordResponse;
import g2.be.entity.Appointment;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Invoice;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Patient;
import g2.be.entity.Room;
import g2.be.entity.ServiceOrder;
import g2.be.entity.StaffSchedule;
import g2.be.entity.User;
import g2.be.entity.WorkingTimeSlot;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.MedicalRecordRepository;
import g2.be.repository.MockCitizenDataRepository;
import g2.be.repository.MockServiceCatalogRepository;
import g2.be.repository.PatientRepository;
import g2.be.repository.RoomRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.repository.ServiceRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.repository.WorkingTimeSlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplementPaymentScopeTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ServiceOrderRepository serviceOrderRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private StaffScheduleRepository staffScheduleRepository;
    @Mock private WorkingTimeSlotRepository workingTimeSlotRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private LoadBalancingService loadBalancingService;
    @Mock private ServiceRepository serviceRepository;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private PdfExportService pdfExportService;
    @Mock private PatientRepository patientRepository;
    @Mock private MockCitizenDataRepository mockCitizenDataRepository;
    @Mock private MockServiceCatalogRepository mockServiceCatalogRepository;
    @Mock private ClinicInsuranceConfig clinicInsuranceConfig;
    @Mock private OnDutyRoomAssignmentResolver onDutyRoomAssignmentResolver;

    @InjectMocks private PaymentService paymentService;
    @InjectMocks private ServiceOrderService serviceOrderService;
    @InjectMocks private CitizenInsuranceService citizenInsuranceService;

    @Test
    @DisplayName("Supplement payment chỉ lấy service orders type SUPPLEMENT")
    void collectSupplementPayment_UsesOnlySupplementOrders() {
        MedicalRecord medicalRecord = MedicalRecord.builder()
                .recordId("REC-001")
                .build();
        when(medicalRecordRepository.findById("REC-001")).thenReturn(Optional.of(medicalRecord));
        when(serviceOrderRepository.findByMedicalRecord_RecordIdAndOrderTypeAndStatus("REC-001", "SUPPLEMENT", "PENDING"))
                .thenReturn(List.of());

        SupplementPaymentRequest request = SupplementPaymentRequest.builder()
                .recordId("REC-001")
                .paymentMethod("CASH")
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> paymentService.collectSupplementPayment(request));

        assertEquals(ErrorCode.NO_SUPPLEMENT_ORDERS, exception.getErrorCode());
        verify(serviceOrderRepository)
                .findByMedicalRecord_RecordIdAndOrderTypeAndStatus("REC-001", "SUPPLEMENT", "PENDING");
        verify(serviceOrderRepository, never()).findByMedicalRecord_RecordIdAndStatus(anyString(), anyString());
    }

    @Test
    @DisplayName("Preview thanh toán bổ sung chỉ hiện service orders type SUPPLEMENT")
    void getSupplementOrdersByRecord_UsesOnlySupplementOrders() {
        User patientUser = User.builder()
                .fullName("Nguyen Van A")
                .phoneNumber("0900000000")
                .build();
        Patient patient = Patient.builder()
                .patientId("PAT-001")
                .healthInsuranceNumber("BHYT-001")
                .user(patientUser)
                .build();
        MedicalRecord medicalRecord = MedicalRecord.builder()
                .recordId("REC-001")
                .patient(patient)
                .build();

        when(medicalRecordRepository.findById("REC-001")).thenReturn(Optional.of(medicalRecord));
        when(serviceOrderRepository.findByMedicalRecord_RecordIdAndOrderTypeAndStatus("REC-001", "SUPPLEMENT", "PENDING"))
                .thenReturn(List.of());

        PendingOrdersByRecordResponse response = serviceOrderService.getSupplementOrdersByRecord("REC-001");

        assertEquals("REC-001", response.getRecordId());
        assertTrue(response.getOrders().isEmpty());
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
        verify(serviceOrderRepository)
                .findByMedicalRecord_RecordIdAndOrderTypeAndStatus("REC-001", "SUPPLEMENT", "PENDING");
        verify(serviceOrderRepository, never()).findByMedicalRecord_RecordIdAndStatus(anyString(), anyString());
    }

    @Test
    @DisplayName("Tra cứu BHYT theo record chỉ tính các service orders type SUPPLEMENT")
    void calculateInsuranceForRecord_UsesOnlySupplementOrders() {
        when(serviceOrderRepository.findByMedicalRecord_RecordIdAndOrderTypeAndStatus("REC-001", "SUPPLEMENT", "PENDING"))
                .thenReturn(List.of());

        AppException exception = assertThrows(AppException.class,
                () -> citizenInsuranceService.calculateInsuranceForRecord("012345678901", "REC-001"));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        verify(serviceOrderRepository)
                .findByMedicalRecord_RecordIdAndOrderTypeAndStatus("REC-001", "SUPPLEMENT", "PENDING");
        verify(serviceOrderRepository, never()).findByMedicalRecord_RecordIdAndStatus(anyString(), anyString());
    }

    @Test
    @DisplayName("Không cho tạo chỉ định bổ sung khi hồ sơ đã hoàn tất")
    void createSupplementOrders_WithCompletedRecord_ThrowsException() {
        MedicalRecord medicalRecord = MedicalRecord.builder()
                .recordId("REC-002")
                .status("COMPLETED")
                .build();
        when(medicalRecordRepository.findById("REC-002")).thenReturn(Optional.of(medicalRecord));

        CreateServiceOrderRequest request = new CreateServiceOrderRequest();
        request.setRecordId("REC-002");
        request.setServiceIds(List.of("SV-001"));

        AppException exception = assertThrows(AppException.class,
                () -> serviceOrderService.createSupplementOrders(1L, request));

        assertEquals(ErrorCode.MEDICAL_RECORD_ALREADY_FINISHED, exception.getErrorCode());
        verify(serviceRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("Không cho thanh toán bổ sung khi lịch hẹn đã hoàn tất")
    void collectSupplementPayment_WithCompletedAppointment_ThrowsException() {
        MedicalRecord medicalRecord = MedicalRecord.builder()
                .recordId("REC-003")
                .status("EXAMINING")
                .appointment(Appointment.builder().appointmentId("APT-003").status("COMPLETED").build())
                .build();
        when(medicalRecordRepository.findById("REC-003")).thenReturn(Optional.of(medicalRecord));

        SupplementPaymentRequest request = SupplementPaymentRequest.builder()
                .recordId("REC-003")
                .paymentMethod("CASH")
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> paymentService.collectSupplementPayment(request));

        assertEquals(ErrorCode.MEDICAL_RECORD_ALREADY_FINISHED, exception.getErrorCode());
        verify(serviceOrderRepository, never())
                .findByMedicalRecord_RecordIdAndOrderTypeAndStatus("REC-003", "SUPPLEMENT", "PENDING");
    }

    @Test
    @DisplayName("Thanh toan tai kham tao medical record moi lien ket parent va root")
    void collectPrepayment_FollowUpAppointment_CreatesLinkedMedicalRecord() {
        Patient patient = Patient.builder()
                .patientId("PAT-001")
                .user(User.builder().fullName("Benh nhan").build())
                .build();
        MedicalRecord rootRecord = MedicalRecord.builder()
                .recordId("REC-ROOT")
                .patient(patient)
                .status("COMPLETED")
                .build();
        MedicalRecord sourceRecord = MedicalRecord.builder()
                .recordId("REC-SOURCE")
                .patient(patient)
                .status("COMPLETED")
                .rootRecord(rootRecord)
                .build();
        Department clinicalDept = Department.builder()
                .deptName("Noi khoa")
                .deptType(Department.DeptType.CLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        g2.be.entity.Service clinicalService = g2.be.entity.Service.builder()
                .serviceId("SVC-CLINIC")
                .serviceName("Kham Noi")
                .price(BigDecimal.valueOf(100000))
                .department(clinicalDept)
                .build();
        LocalDate appointmentDate = LocalDate.of(2026, 4, 28);
        LocalTime appointmentTime = LocalTime.of(8, 0);
        Room clinicalRoom = Room.builder()
                .roomId("ROOM-FOLLOW")
                .roomName("Phong tai kham")
                .department(clinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .services(List.of(clinicalService))
                .build();
        Doctor onDutyDoctor = Doctor.builder()
                .doctorId("DOC-FOLLOW")
                .user(User.builder().userId(201L).fullName("Bac si tai kham").build())
                .department(clinicalDept)
                .build();
        OnDutyRoomAssignmentResolver.RoomDoctorAssignment followUpAssignment =
                new OnDutyRoomAssignmentResolver.RoomDoctorAssignment(clinicalRoom, onDutyDoctor);
        Appointment appointment = Appointment.builder()
                .appointmentId("APT-FOLLOW")
                .patient(patient)
                .services(List.of(clinicalService))
                .visitType("FOLLOW_UP")
                .followUpSourceRecord(sourceRecord)
                .appointmentDate(appointmentDate)
                .appointmentTime(appointmentTime)
                .room(clinicalRoom)
                .build();

        when(invoiceRepository.existsByMedicalRecord_Appointment_AppointmentIdAndPaymentStatus("APT-FOLLOW", "PAID"))
                .thenReturn(false);
        when(appointmentRepository.findById("APT-FOLLOW")).thenReturn(Optional.of(appointment));
        when(medicalRecordRepository.findByAppointment(appointment)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(onDutyRoomAssignmentResolver.findAssignmentsForServiceAt(
                clinicalService, appointmentDate, appointmentTime)).thenReturn(List.of(followUpAssignment));
        when(onDutyRoomAssignmentResolver.findAssignmentForRoomAt(
                clinicalService, clinicalRoom, appointmentDate, appointmentTime))
                .thenReturn(Optional.of(followUpAssignment));

        PrepaymentRequest request = PrepaymentRequest.builder()
                .appointmentId("APT-FOLLOW")
                .paymentMethod("QR")
                .build();

        paymentService.collectPrepayment(request);

        ArgumentCaptor<MedicalRecord> recordCaptor = ArgumentCaptor.forClass(MedicalRecord.class);
        verify(medicalRecordRepository).save(recordCaptor.capture());
        MedicalRecord createdRecord = recordCaptor.getValue();
        assertEquals("FOLLOW_UP", createdRecord.getVisitType());
        assertSame(sourceRecord, createdRecord.getParentRecord());
        assertSame(rootRecord, createdRecord.getRootRecord());
        assertSame(appointment, createdRecord.getAppointment());
    }

    @Test
    @DisplayName("Thanh toan phong lam sang theo lich truc gan bac si vao appointment")
    void collectPrepayment_ClinicalRoomWithScheduledDoctor_AssignsDoctor() {
        LocalDate appointmentDate = LocalDate.now();
        LocalTime appointmentTime = LocalTime.of(9, 0);
        Department clinicalDept = Department.builder()
                .deptName("Lam San")
                .deptType(Department.DeptType.CLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        Room clinicalRoom = Room.builder()
                .roomId("DE301")
                .roomName("DE301")
                .roomType("CLINICAL")
                .department(clinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .build();
        g2.be.entity.Service clinicalService = g2.be.entity.Service.builder()
                .serviceId("SVC-CLINIC")
                .serviceName("Kham suc khoe di lam")
                .price(BigDecimal.valueOf(350000))
                .department(clinicalDept)
                .rooms(List.of(clinicalRoom))
                .build();
        clinicalRoom.setServices(List.of(clinicalService));
        User doctorUser = User.builder()
                .userId(200L)
                .fullName("Bac si Mai")
                .build();
        Doctor scheduledDoctor = Doctor.builder()
                .doctorId("DOC-MAI")
                .user(doctorUser)
                .department(clinicalDept)
                .build();
        Patient patient = Patient.builder()
                .patientId("PAT-001")
                .user(User.builder().fullName("Benh nhan").build())
                .build();
        Appointment appointment = Appointment.builder()
                .appointmentId("APT-001")
                .patient(patient)
                .services(List.of(clinicalService))
                .appointmentDate(appointmentDate)
                .appointmentTime(appointmentTime)
                .room(clinicalRoom)
                .visitType("INITIAL")
                .build();

        when(invoiceRepository.existsByMedicalRecord_Appointment_AppointmentIdAndPaymentStatus("APT-001", "PAID"))
                .thenReturn(false);
        when(appointmentRepository.findById("APT-001")).thenReturn(Optional.of(appointment));
        when(medicalRecordRepository.findByAppointment(appointment)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OnDutyRoomAssignmentResolver.RoomDoctorAssignment assignment =
                new OnDutyRoomAssignmentResolver.RoomDoctorAssignment(clinicalRoom, scheduledDoctor);
        when(onDutyRoomAssignmentResolver.findAssignmentsForServicesAt(
                List.of(clinicalService),
                appointmentDate,
                appointmentTime,
                true)).thenReturn(List.of(assignment));
        when(onDutyRoomAssignmentResolver.findAssignmentsForServiceAt(
                clinicalService, appointmentDate, appointmentTime)).thenReturn(List.of(assignment));
        when(onDutyRoomAssignmentResolver.findAssignmentForRoomAt(
                clinicalService,
                clinicalRoom,
                appointmentDate,
                appointmentTime)).thenReturn(Optional.of(assignment));
        when(appointmentRepository.findMaxQueueNumberByRoomIdAndDate("DE301", appointmentDate)).thenReturn(0);

        PrepaymentRequest request = PrepaymentRequest.builder()
                .appointmentId("APT-001")
                .paymentMethod("CASH")
                .build();

        paymentService.collectPrepayment(request);

        assertSame(scheduledDoctor, appointment.getDoctor());
        assertSame(clinicalRoom, appointment.getRoom());
        assertEquals("WAITING", appointment.getStatus());
        assertNotNull(appointment.getQueueNumber());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    @DisplayName("Tao chi dinh gan phong tu service_room va lich truc cua dich vu do")
    void createSupplementOrders_AssignsMappedOnDutyRoomOnly() {
        LocalDate appointmentDate = LocalDate.of(2026, 4, 29);
        LocalTime appointmentTime = LocalTime.of(8, 0);
        Department paraclinicalDept = Department.builder()
                .deptId(2)
                .deptType(Department.DeptType.PARACLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        Room mappedRoom = Room.builder()
                .roomId("LAB-X")
                .roomName("Lab X")
                .roomType("LAB")
                .department(paraclinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .build();
        Room otherRoom = Room.builder()
                .roomId("LAB-OTHER")
                .roomName("Lab Other")
                .roomType("LAB")
                .department(paraclinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .build();
        g2.be.entity.Service requestedService = g2.be.entity.Service.builder()
                .serviceId("S-X")
                .serviceName("Xet nghiem X")
                .price(BigDecimal.valueOf(120000))
                .department(paraclinicalDept)
                .rooms(List.of(mappedRoom))
                .isActive(true)
                .isDeleted(false)
                .build();
        mappedRoom.setServices(List.of(requestedService));
        Doctor onDutyDoctor = Doctor.builder()
                .doctorId("DOC-X")
                .department(paraclinicalDept)
                .build();
        Appointment appointment = Appointment.builder()
                .appointmentId("APT-001")
                .appointmentDate(appointmentDate)
                .appointmentTime(appointmentTime)
                .services(List.of())
                .build();
        MedicalRecord medicalRecord = MedicalRecord.builder()
                .recordId("REC-001")
                .appointment(appointment)
                .status("IN_PROGRESS")
                .build();
        OnDutyRoomAssignmentResolver.RoomDoctorAssignment assignment =
                new OnDutyRoomAssignmentResolver.RoomDoctorAssignment(mappedRoom, onDutyDoctor);

        when(medicalRecordRepository.findById("REC-001")).thenReturn(Optional.of(medicalRecord));
        when(doctorRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        when(serviceRepository.findById("S-X")).thenReturn(Optional.of(requestedService));
        when(onDutyRoomAssignmentResolver.findAssignmentsForServiceAt(
                requestedService,
                appointmentDate,
                appointmentTime)).thenReturn(List.of(assignment));
        when(onDutyRoomAssignmentResolver.findAssignmentForRoomAt(
                requestedService,
                mappedRoom,
                appointmentDate,
                appointmentTime)).thenReturn(Optional.of(assignment));
        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateServiceOrderRequest request = new CreateServiceOrderRequest();
        request.setRecordId("REC-001");
        request.setServiceIds(List.of("S-X"));

        serviceOrderService.createSupplementOrders(1L, request);

        ArgumentCaptor<ServiceOrder> orderCaptor = ArgumentCaptor.forClass(ServiceOrder.class);
        verify(serviceOrderRepository).save(orderCaptor.capture());
        ServiceOrder savedOrder = orderCaptor.getValue();
        assertSame(mappedRoom, savedOrder.getAssignedRoom());
        assertEquals("DOC-X", savedOrder.getTechnicianId());
        assertEquals("LAB-X", savedOrder.getAssignedRoom().getRoomId());
    }
}
