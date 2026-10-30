package g2.be.controller;

import g2.be.dto.request.BookAppointmentRequest;
import g2.be.dto.request.CancelAppointmentRequest;
import g2.be.dto.request.CreateAppointmentRequest;
import g2.be.dto.request.UpdateAppointmentRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.AppointmentDetailResponse;
import g2.be.dto.response.AppointmentResponse;
import g2.be.dto.response.AvailableSlotResponse;
import g2.be.entity.Patient;
import g2.be.repository.PatientRepository;
import g2.be.security.UserDetailsImpl;
import g2.be.service.AppointmentService;
import g2.be.service.StaffScheduleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StaffScheduleService staffScheduleService;

    @InjectMocks
    private AppointmentController appointmentController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyAppointmentsReturnsPatientVisitHistory() {
        setAuthenticatedUser(5L);
        mockCurrentPatient(5L, "BN-001");
        Page<AppointmentResponse> page = new PageImpl<>(List.of(mock(AppointmentResponse.class)));

        when(appointmentService.getVisitHistoryByPatient("BN-001", PageRequest.of(0, 10))).thenReturn(page);

        ApiResponse<Page<AppointmentResponse>> response = appointmentController.getMyAppointments("WAITING", 0, 10);

        assertTrue(response.isSuccess());
        assertSame(page, response.getData());
    }

    @Test
    void cancelMyAppointmentDelegatesToService() {
        setAuthenticatedUser(5L);
        mockCurrentPatient(5L, "BN-001");
        CancelAppointmentRequest request = new CancelAppointmentRequest();
        AppointmentResponse appointmentResponse = mock(AppointmentResponse.class);

        when(appointmentService.cancelMyAppointment("APT-001", "BN-001", request)).thenReturn(appointmentResponse);

        ApiResponse<AppointmentResponse> response = appointmentController.cancelMyAppointment("APT-001", request);

        assertTrue(response.isSuccess());
        assertSame(appointmentResponse, response.getData());
    }

    @Test
    void getAvailableSlotsReturnsAvailableSlotList() {
        List<AvailableSlotResponse> slots = List.of(mock(AvailableSlotResponse.class));

        when(appointmentService.getAvailableSlots(LocalDate.of(2026, 4, 17), List.of("S-01"))).thenReturn(slots);

        ApiResponse<List<AvailableSlotResponse>> response =
                appointmentController.getAvailableSlots("2026-04-17", List.of("S-01"));

        assertTrue(response.isSuccess());
        assertSame(slots, response.getData());
    }

    @Test
    void getClinicalRoomsReturnsRoomList() {
        List<Map<String, Object>> rooms = List.of(Map.of("roomId", "R-01"));
        List<String> serviceIds = List.of("S-01");

        when(staffScheduleService.getClinicalRooms(3L, LocalDate.of(2026, 4, 17), serviceIds)).thenReturn(rooms);

        ApiResponse<List<Map<String, Object>>> response = appointmentController.getClinicalRooms(3L, "2026-04-17", serviceIds);

        assertTrue(response.isSuccess());
        assertSame(rooms, response.getData());
    }

    @Test
    void bookMyAppointmentDelegatesToService() {
        setAuthenticatedUser(5L);
        BookAppointmentRequest request = new BookAppointmentRequest();
        AppointmentResponse appointmentResponse = mock(AppointmentResponse.class);

        when(appointmentService.bookAppointment(5L, request)).thenReturn(appointmentResponse);

        ApiResponse<AppointmentResponse> response = appointmentController.bookMyAppointment(request);

        assertTrue(response.isSuccess());
        assertSame(appointmentResponse, response.getData());
    }

    @Test
    void createAppointmentReturnsCreatedAppointment() {
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        AppointmentResponse appointmentResponse = mock(AppointmentResponse.class);

        when(appointmentService.createAppointment(request)).thenReturn(appointmentResponse);

        ApiResponse<AppointmentResponse> response = appointmentController.createAppointment(request);

        assertTrue(response.isSuccess());
        assertSame(appointmentResponse, response.getData());
    }

    @Test
    void updateAppointmentReturnsUpdatedAppointment() {
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        AppointmentResponse appointmentResponse = mock(AppointmentResponse.class);

        when(appointmentService.updateAppointment("APT-001", request)).thenReturn(appointmentResponse);

        ApiResponse<AppointmentResponse> response = appointmentController.updateAppointment("APT-001", request);

        assertTrue(response.isSuccess());
        assertSame(appointmentResponse, response.getData());
    }

    @Test
    void getAppointmentsReturnsPagedAppointments() {
        Page<AppointmentResponse> page = new PageImpl<>(List.of(mock(AppointmentResponse.class)));

        when(appointmentService.getAppointments("WAITING", PageRequest.of(0, 20))).thenReturn(page);

        ApiResponse<Page<AppointmentResponse>> response = appointmentController.getAppointments("WAITING", 0, 20);

        assertTrue(response.isSuccess());
        assertSame(page, response.getData());
    }

    @Test
    void getAppointmentReturnsAppointmentDetail() {
        AppointmentDetailResponse detailResponse = mock(AppointmentDetailResponse.class);

        when(appointmentService.getAppointmentDetail("APT-001")).thenReturn(detailResponse);

        ApiResponse<AppointmentDetailResponse> response = appointmentController.getAppointment("APT-001");

        assertTrue(response.isSuccess());
        assertSame(detailResponse, response.getData());
    }

    @Test
    void getMyPatientsReturnsDoctorQueue() {
        setAuthenticatedUser(8L);
        Page<AppointmentResponse> page = new PageImpl<>(List.of(mock(AppointmentResponse.class)));

        when(appointmentService.getAppointmentsForDoctor(8L, "WAITING", PageRequest.of(0, 10))).thenReturn(page);

        ApiResponse<Page<AppointmentResponse>> response = appointmentController.getMyPatients("WAITING", 0, 10);

        assertTrue(response.isSuccess());
        assertSame(page, response.getData());
    }

    private void setAuthenticatedUser(long userId) {
        UserDetailsImpl userDetails = new UserDetailsImpl(userId, "tester", "tester@test.com", "secret", List.of());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private void mockCurrentPatient(long userId, String patientId) {
        Patient patient = mock(Patient.class);
        when(patient.getPatientId()).thenReturn(patientId);
        when(patientRepository.findByUser_UserId(userId)).thenReturn(Optional.of(patient));
    }
}