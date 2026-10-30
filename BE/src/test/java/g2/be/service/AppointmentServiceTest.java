package g2.be.service;

import g2.be.dto.request.CompleteExaminationRequest;
import g2.be.dto.request.CreateAppointmentRequest;
import g2.be.dto.request.CreatePatientRequest;
import g2.be.dto.request.UpdateAppointmentRequest;
import g2.be.dto.response.AppointmentResponse;
import g2.be.dto.response.UserDetailResponse;
import g2.be.entity.Appointment;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Invoice;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Nurse;
import g2.be.entity.NurseDoctorAssignment;
import g2.be.entity.Room;
import g2.be.entity.Service;
import g2.be.entity.ServiceOrder;
import g2.be.entity.Patient;
import g2.be.entity.StaffSchedule;
import g2.be.entity.User;
import g2.be.entity.WorkingTimeSlot;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.MedicalRecordRepository;
import g2.be.repository.NurseDoctorAssignmentRepository;
import g2.be.repository.NurseRepository;
import g2.be.repository.PatientRepository;
import g2.be.repository.RoleRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.repository.ServiceRepository;
import g2.be.repository.RoomRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.repository.UserRepository;
import g2.be.repository.WorkingTimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @InjectMocks
    private AppointmentService appointmentService;

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private WorkingTimeSlotRepository workingTimeSlotRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ServiceOrderRepository serviceOrderRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private StaffScheduleRepository staffScheduleRepository;
    @Mock private NurseRepository nurseRepository;
    @Mock private NurseDoctorAssignmentRepository nurseDoctorAssignmentRepository;
    @Mock private BhytSyncService bhytSyncService;
    @Mock private OnDutyRoomAssignmentResolver onDutyRoomAssignmentResolver;
    @Mock private PatientManagementService patientManagementService;

    private Appointment appointment;
    private CompleteExaminationRequest request;

    @BeforeEach
    void setUp() {
        User patientUser = User.builder()
                .userId(100L)
                .fullName("Nguyễn Văn A")
                .build();

        Patient patient = Patient.builder()
                .patientId("PAT-001")
                .user(patientUser)
                .build();

        appointment = Appointment.builder()
                .appointmentId("APT-001")
                .patient(patient)
                .status("IN_PROGRESS")
                .build();

        request = new CompleteExaminationRequest();
        request.setSymptoms("Ho");
        request.setDiagnosis("Viêm họng");
        request.setDoctorNotes("Theo dõi thêm 3 ngày");
    }

    @Test
    @DisplayName("Không cho hoàn tất khám khi còn hóa đơn chưa thanh toán")
    void completeExamination_WithUnpaidInvoice_ThrowsException() {
        when(appointmentRepository.findById("APT-001")).thenReturn(Optional.of(appointment));
        when(invoiceRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(Invoice.builder().invoiceId("INV-001").paymentStatus("PENDING").build()));
        when(serviceOrderRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of());

        AppException exception = assertThrows(AppException.class,
                () -> appointmentService.completeExamination("APT-001", request));

        assertEquals(ErrorCode.APPOINTMENT_HAS_UNPAID_INVOICES, exception.getErrorCode());
        verify(medicalRecordRepository, never()).findByAppointment_AppointmentId(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cho phép hoàn tất khám khi tất cả hóa đơn liên quan đã thanh toán")
    void completeExamination_WithPaidInvoices_CompletesAppointment() {
        when(appointmentRepository.findById("APT-001")).thenReturn(Optional.of(appointment));
        when(invoiceRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(Invoice.builder().invoiceId("INV-001").paymentStatus("PAID").build()));
        when(serviceOrderRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of());
        when(medicalRecordRepository.findByAppointment_AppointmentId("APT-001")).thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.completeExamination("APT-001", request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        verify(appointmentRepository).save(appointment);
        verify(bhytSyncService).queueAppointmentSyncAsync("APT-001");
    }

    @Test
    @DisplayName("Cho phép hoàn tất khám khi tất cả cận lâm sàng đã có kết quả")
    void completeExamination_WithCompletedParaclinicalOrders_CompletesAppointment() {
        when(appointmentRepository.findById("APT-001")).thenReturn(Optional.of(appointment));
        when(invoiceRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(Invoice.builder().invoiceId("INV-001").paymentStatus("PAID").build()));
        when(serviceOrderRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(buildParaclinicalOrder("ORD-CLS-001", "COMPLETED")));
        when(medicalRecordRepository.findByAppointment_AppointmentId("APT-001")).thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.completeExamination("APT-001", request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        verify(appointmentRepository).save(appointment);
        verify(bhytSyncService).queueAppointmentSyncAsync("APT-001");
    }

    @Test
    @DisplayName("Không cho hoàn tất khám khi còn cận lâm sàng đã thanh toán nhưng chưa có kết quả")
    void completeExamination_WithPaidParaclinicalOrder_ThrowsException() {
        when(appointmentRepository.findById("APT-001")).thenReturn(Optional.of(appointment));
        when(invoiceRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(Invoice.builder().invoiceId("INV-001").paymentStatus("PAID").build()));
        when(serviceOrderRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(buildParaclinicalOrder("ORD-CLS-001", "PAID")));

        AppException exception = assertThrows(AppException.class,
                () -> appointmentService.completeExamination("APT-001", request));

        assertEquals(ErrorCode.APPOINTMENT_HAS_UNFINISHED_PARACLINICAL_ORDERS, exception.getErrorCode());
        verify(medicalRecordRepository, never()).findByAppointment_AppointmentId(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không cho hoàn tất khám khi còn cận lâm sàng đang chờ xử lý")
    void completeExamination_WithPendingParaclinicalOrder_ThrowsException() {
        when(appointmentRepository.findById("APT-001")).thenReturn(Optional.of(appointment));
        when(invoiceRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(Invoice.builder().invoiceId("INV-001").paymentStatus("PAID").build()));
        when(serviceOrderRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(buildParaclinicalOrder("ORD-CLS-001", "PENDING")));

        AppException exception = assertThrows(AppException.class,
                () -> appointmentService.completeExamination("APT-001", request));

        assertEquals(ErrorCode.APPOINTMENT_HAS_UNFINISHED_PARACLINICAL_ORDERS, exception.getErrorCode());
        verify(medicalRecordRepository, never()).findByAppointment_AppointmentId(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bỏ qua chỉ định cận lâm sàng đã hủy khi hoàn tất khám")
    void completeExamination_WithCancelledParaclinicalOrder_CompletesAppointment() {
        when(appointmentRepository.findById("APT-001")).thenReturn(Optional.of(appointment));
        when(invoiceRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(Invoice.builder().invoiceId("INV-001").paymentStatus("PAID").build()));
        when(serviceOrderRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(buildParaclinicalOrder("ORD-CLS-001", "CANCELLED")));
        when(medicalRecordRepository.findByAppointment_AppointmentId("APT-001")).thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.completeExamination("APT-001", request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        verify(appointmentRepository).save(appointment);
        verify(bhytSyncService).queueAppointmentSyncAsync("APT-001");
    }

    @Test
    @DisplayName("Không cho hoàn tất khám khi còn chỉ định bổ sung chưa gắn vào invoice PAID")
    void completeExamination_WithUnsettledSupplementOrder_ThrowsException() {
        when(appointmentRepository.findById("APT-001")).thenReturn(Optional.of(appointment));
        when(invoiceRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(Invoice.builder().invoiceId("INV-001").paymentStatus("PAID").build()));
        when(serviceOrderRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-001"))
                .thenReturn(List.of(ServiceOrder.builder()
                        .orderId("ORD-SUP-001")
                        .orderType("SUPPLEMENT")
                        .status("PENDING")
                        .invoiceId(null)
                        .build()));

        AppException exception = assertThrows(AppException.class,
                () -> appointmentService.completeExamination("APT-001", request));

        assertEquals(ErrorCode.APPOINTMENT_HAS_UNPAID_INVOICES, exception.getErrorCode());
        verify(medicalRecordRepository, never()).findByAppointment_AppointmentId(any());
        verify(appointmentRepository, never()).save(any());
    }

    private ServiceOrder buildParaclinicalOrder(String orderId, String status) {
        Department department = Department.builder()
                .deptId(2)
                .deptName("Cận lâm sàng")
                .deptType(Department.DeptType.PARACLINICAL)
                .build();
        Service service = Service.builder()
                .serviceId("S-CLS-001")
                .serviceName("Xét nghiệm máu")
                .department(department)
                .build();

        return ServiceOrder.builder()
                .orderId(orderId)
                .service(service)
                .status(status)
                .orderType("INITIAL")
                .invoiceId("INV-001")
                .build();
    }

    @Test
    @DisplayName("Tao lich tai kham tu ho so hop le tu dong dung dich vu lam sang cu")
    void createAppointment_FollowUp_UsesSourceClinicalServiceAndPreferredDoctor() {
        Department clinicalDept = Department.builder()
                .deptId(1)
                .deptName("Noi khoa")
                .deptType(Department.DeptType.CLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        Room clinicalRoom = Room.builder()
                .roomId("ROOM-001")
                .roomName("Noi 1")
                .roomType("CLINICAL")
                .department(clinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .build();
        Service clinicalService = Service.builder()
                .serviceId("SVC-CLINIC")
                .serviceName("Kham Noi")
                .price(BigDecimal.valueOf(100000))
                .department(clinicalDept)
                .rooms(List.of(clinicalRoom))
                .build();
        User doctorUser = User.builder()
                .userId(200L)
                .fullName("Bac si cu")
                .build();
        Doctor previousDoctor = Doctor.builder()
                .doctorId("DOC-OLD")
                .user(doctorUser)
                .department(clinicalDept)
                .room(clinicalRoom)
                .build();
        Patient patient = Patient.builder()
                .patientId("PAT-001")
                .user(User.builder().userId(100L).fullName("Benh nhan").build())
                .build();
        MedicalRecord sourceRecord = MedicalRecord.builder()
                .recordId("REC-SOURCE")
                .status("COMPLETED")
                .patient(patient)
                .doctor(previousDoctor)
                .appointment(Appointment.builder().services(List.of(clinicalService)).build())
                .build();
        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        LocalTime appointmentTime = LocalTime.of(8, 0);
        WorkingTimeSlot slot = WorkingTimeSlot.builder()
                .slotId(1L)
                .startTime(appointmentTime)
                .endTime(LocalTime.of(11, 30))
                .isActive(true)
                .isDeleted(false)
                .build();
        StaffSchedule schedule = StaffSchedule.builder()
                .user(doctorUser)
                .slot(slot)
                .workDate(appointmentDate)
                .room(clinicalRoom)
                .build();

        when(patientRepository.findById("PAT-001")).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findById("REC-SOURCE")).thenReturn(Optional.of(sourceRecord));
        when(workingTimeSlotRepository.findByStartTimeAndIsActiveTrueAndIsDeletedFalse(appointmentTime))
                .thenReturn(Optional.of(slot));
        when(staffScheduleRepository.findFirstByUser_UserIdAndWorkDateAndSlot_SlotId(
                200L, appointmentDate, 1L)).thenReturn(Optional.of(schedule));
        when(doctorRepository.findById("DOC-OLD")).thenReturn(Optional.of(previousDoctor));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setPatientId("PAT-001");
        request.setFollowUpSourceRecordId("REC-SOURCE");
        request.setAppointmentDate(appointmentDate);
        request.setAppointmentTime(appointmentTime);

        AppointmentResponse response = appointmentService.createAppointment(request);

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointmentCaptor.capture());
        Appointment savedAppointment = appointmentCaptor.getValue();
        assertNotNull(response);
        assertEquals("FOLLOW_UP", savedAppointment.getVisitType());
        assertSame(sourceRecord, savedAppointment.getFollowUpSourceRecord());
        assertEquals(List.of(clinicalService), savedAppointment.getServices());
        assertSame(previousDoctor, savedAppointment.getDoctor());
        assertSame(clinicalRoom, savedAppointment.getRoom());
        verify(serviceRepository, never()).findAllByServiceIdInAndIsDeletedFalseAndIsActiveTrue(any());
    }

    @Test
    @DisplayName("Le tan tao lich cho benh nhan moi se tao tai khoan benh nhan truoc khi luu lich")
    void createAppointment_NewPatient_CreatesPatientAccountAndAppointment() {
        Department clinicalDept = Department.builder()
                .deptId(1)
                .deptName("Noi khoa")
                .deptType(Department.DeptType.CLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        Service clinicalService = Service.builder()
                .serviceId("SVC-CLINIC")
                .serviceName("Kham Noi")
                .price(BigDecimal.valueOf(100000))
                .department(clinicalDept)
                .build();
        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        LocalTime appointmentTime = LocalTime.of(8, 0);
        WorkingTimeSlot slot = WorkingTimeSlot.builder()
                .slotId(1L)
                .startTime(appointmentTime)
                .endTime(LocalTime.of(11, 30))
                .isActive(true)
                .isDeleted(false)
                .build();
        Room clinicalRoom = Room.builder()
                .roomId("ROOM-CLINIC")
                .roomName("Phong Noi")
                .department(clinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .services(List.of(clinicalService))
                .build();
        clinicalService.setRooms(List.of(clinicalRoom));
        Doctor onDutyDoctor = Doctor.builder()
                .doctorId("DOC-CLINIC")
                .user(User.builder().userId(200L).fullName("Bac si truc").build())
                .department(clinicalDept)
                .build();
        OnDutyRoomAssignmentResolver.RoomDoctorAssignment assignment =
                new OnDutyRoomAssignmentResolver.RoomDoctorAssignment(clinicalRoom, onDutyDoctor);
        Patient createdPatient = Patient.builder()
                .patientId("BN-NEW")
                .user(User.builder().userId(101L).fullName("Benh nhan moi").build())
                .build();
        CreatePatientRequest newPatientRequest = CreatePatientRequest.builder()
                .fullName("Benh nhan moi")
                .email("newpatient@example.com")
                .phoneNumber("0912345678")
                .build();
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setNewPatient(newPatientRequest);
        request.setServiceIds(List.of("SVC-CLINIC"));
        request.setAppointmentDate(appointmentDate);
        request.setAppointmentTime(appointmentTime);
        request.setNotes("Dau dau");

        when(serviceRepository.findAllByServiceIdInAndIsDeletedFalseAndIsActiveTrue(List.of("SVC-CLINIC")))
                .thenReturn(List.of(clinicalService));
        when(workingTimeSlotRepository.findByStartTimeAndIsActiveTrueAndIsDeletedFalse(appointmentTime))
                .thenReturn(Optional.of(slot));
        when(onDutyRoomAssignmentResolver.findAssignmentsForServicesAt(
                List.of(clinicalService), appointmentDate, appointmentTime, true))
                .thenReturn(List.of(assignment));
        when(patientManagementService.createPatient(newPatientRequest))
                .thenReturn(UserDetailResponse.builder().patientId("BN-NEW").build());
        when(patientRepository.findById("BN-NEW")).thenReturn(Optional.of(createdPatient));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.createAppointment(request);

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(patientManagementService).createPatient(newPatientRequest);
        verify(appointmentRepository).save(appointmentCaptor.capture());
        Appointment savedAppointment = appointmentCaptor.getValue();
        assertNotNull(response);
        assertSame(createdPatient, savedAppointment.getPatient());
        assertEquals(List.of(clinicalService), savedAppointment.getServices());
        assertEquals("INITIAL", savedAppointment.getVisitType());
    }

    @Test
    @DisplayName("Khong cho tai kham neu client gui dich vu khac dich vu lam sang cu")
    void createAppointment_FollowUpWithDifferentService_ThrowsException() {
        Patient patient = Patient.builder().patientId("PAT-001").build();
        Department clinicalDept = Department.builder()
                .deptType(Department.DeptType.CLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        Service clinicalService = Service.builder()
                .serviceId("SVC-CLINIC")
                .serviceName("Kham Noi")
                .department(clinicalDept)
                .build();
        MedicalRecord sourceRecord = MedicalRecord.builder()
                .recordId("REC-SOURCE")
                .status("FINISHED")
                .patient(patient)
                .appointment(Appointment.builder().services(List.of(clinicalService)).build())
                .build();
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setPatientId("PAT-001");
        request.setFollowUpSourceRecordId("REC-SOURCE");
        request.setServiceIds(List.of("SVC-OTHER"));

        when(patientRepository.findById("PAT-001")).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findById("REC-SOURCE")).thenReturn(Optional.of(sourceRecord));

        AppException exception = assertThrows(AppException.class,
                () -> appointmentService.createAppointment(request));

        assertEquals(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE, exception.getErrorCode());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Khong cho cap nhat danh sach dich vu cua lich tai kham")
    void updateAppointment_FollowUpWithServiceIds_ThrowsException() {
        MedicalRecord sourceRecord = MedicalRecord.builder()
                .recordId("REC-SOURCE")
                .build();
        Appointment followUpAppointment = Appointment.builder()
                .appointmentId("APT-FOLLOW")
                .visitType("FOLLOW_UP")
                .followUpSourceRecord(sourceRecord)
                .build();
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setServiceIds(List.of("SVC-NEW"));

        when(appointmentRepository.findById("APT-FOLLOW")).thenReturn(Optional.of(followUpAppointment));

        AppException exception = assertThrows(AppException.class,
                () -> appointmentService.updateAppointment("APT-FOLLOW", request));

        assertEquals(ErrorCode.FOLLOW_UP_RECORD_NOT_ELIGIBLE, exception.getErrorCode());
        verify(serviceRepository, never()).findAllByServiceIdInAndIsDeletedFalseAndIsActiveTrue(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hang doi lam sang cua y ta dung phong cua bac si duoc phan cong")
    void getAppointmentsForStaffClinicalRooms_UsesAssignedDoctorClinicalRoom() {
        Long nurseUserId = 300L;
        Long doctorUserId = 200L;
        LocalDate today = g2.be.util.ClinicTime.today();

        Department clinicalDept = Department.builder()
                .deptId(1)
                .deptName("Noi khoa")
                .deptType(Department.DeptType.CLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        Room doctorRoom = Room.builder()
                .roomId("ROOM-DOC")
                .roomName("Phong bac si")
                .roomType("CLINICAL")
                .department(clinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .build();
        User doctorUser = User.builder()
                .userId(doctorUserId)
                .fullName("Bac si A")
                .build();
        Doctor assignedDoctor = Doctor.builder()
                .doctorId("DOC-001")
                .user(doctorUser)
                .department(clinicalDept)
                .room(doctorRoom)
                .build();
        Nurse nurse = Nurse.builder()
                .nurseId("NUR-001")
                .user(User.builder().userId(nurseUserId).fullName("Y ta A").build())
                .build();
        NurseDoctorAssignment assignment = NurseDoctorAssignment.builder()
                .nurse(nurse)
                .doctor(assignedDoctor)
                .effectiveDate(today.minusDays(1))
                .isActive(true)
                .build();
        Patient patient = Patient.builder()
                .patientId("PAT-QUEUE")
                .user(User.builder().fullName("Benh nhan hang doi").build())
                .build();
        Appointment queueAppointment = Appointment.builder()
                .appointmentId("APT-QUEUE")
                .patient(patient)
                .doctor(assignedDoctor)
                .room(doctorRoom)
                .appointmentDate(today)
                .status("WAITING")
                .build();
        WorkingTimeSlot currentSlot = WorkingTimeSlot.builder()
                .slotId(1L)
                .startTime(LocalTime.MIN)
                .endTime(LocalTime.MAX)
                .isActive(true)
                .isDeleted(false)
                .build();
        StaffSchedule doctorSchedule = StaffSchedule.builder()
                .user(doctorUser)
                .slot(currentSlot)
                .workDate(today)
                .room(doctorRoom)
                .build();

        when(nurseRepository.findByUser_UserId(nurseUserId)).thenReturn(Optional.of(nurse));
        when(nurseDoctorAssignmentRepository.findByNurse_NurseIdAndIsActiveTrue("NUR-001"))
                .thenReturn(List.of(assignment));
        when(staffScheduleRepository.findByUser_UserIdAndWorkDate(doctorUserId, today)).thenReturn(List.of(doctorSchedule));
        when(appointmentRepository.findByRoomIdsForDoctorQueue(any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(queueAppointment)));
        when(medicalRecordRepository.findByAppointment_AppointmentId("APT-QUEUE")).thenReturn(Optional.empty());
        when(invoiceRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-QUEUE"))
                .thenReturn(List.of());

        appointmentService.getAppointmentsForStaffClinicalRooms(
                nurseUserId,
                null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        ArgumentCaptor<java.util.Collection> roomIdsCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(appointmentRepository).findByRoomIdsForDoctorQueue(
                roomIdsCaptor.capture(),
                any(),
                any(),
                any(),
                any());
        assertEquals(List.of("ROOM-DOC"), List.copyOf(roomIdsCaptor.getValue()));
    }

    @Test
    @DisplayName("Chuyen phong chi tra phong dung dich vu va co bac si truc")
    void getRoomTransferOptions_UsesServiceRoomAndOnDutySchedule() {
        LocalDate appointmentDate = LocalDate.of(2026, 4, 29);
        LocalTime appointmentTime = LocalTime.of(8, 0);
        Department clinicalDept = Department.builder()
                .deptId(1)
                .deptType(Department.DeptType.CLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        Room eligibleRoom = Room.builder()
                .roomId("CL-1")
                .roomName("Clinical 1")
                .roomType("CLINICAL")
                .department(clinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .currentQueueLength(1)
                .build();
        Room unscheduledRoom = Room.builder()
                .roomId("CL-2")
                .roomName("Clinical 2")
                .roomType("CLINICAL")
                .department(clinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .currentQueueLength(0)
                .build();
        Service service = Service.builder()
                .serviceId("S-CL")
                .serviceName("Kham Noi")
                .price(BigDecimal.valueOf(100000))
                .department(clinicalDept)
                .rooms(List.of(eligibleRoom, unscheduledRoom))
                .isActive(true)
                .isDeleted(false)
                .build();
        eligibleRoom.setServices(List.of(service));
        unscheduledRoom.setServices(List.of(service));
        Doctor onDutyDoctor = Doctor.builder()
                .doctorId("DOC-1")
                .department(clinicalDept)
                .user(User.builder().fullName("Bac si truc").build())
                .build();
        Appointment transferAppointment = Appointment.builder()
                .appointmentId("APT-TRANSFER")
                .appointmentDate(appointmentDate)
                .appointmentTime(appointmentTime)
                .status("WAITING")
                .services(List.of(service))
                .build();

        when(appointmentRepository.findById("APT-TRANSFER")).thenReturn(Optional.of(transferAppointment));
        when(onDutyRoomAssignmentResolver.findAssignmentsForServicesAt(
                List.of(service),
                appointmentDate,
                appointmentTime,
                true)).thenReturn(List.of(
                new OnDutyRoomAssignmentResolver.RoomDoctorAssignment(eligibleRoom, onDutyDoctor)));

        List<g2.be.dto.response.RoomTransferOptionResponse> options =
                appointmentService.getRoomTransferOptions("APT-TRANSFER");

        assertEquals(1, options.size());
        assertEquals("CL-1", options.get(0).getRoomId());
        assertEquals("DOC-1", options.get(0).getDoctorId());
    }
}
