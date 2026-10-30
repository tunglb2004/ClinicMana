package g2.be.controller;

import g2.be.dto.request.AssignNurseToDoctorRequest;
import g2.be.dto.request.CreateStaffScheduleRequest;
import g2.be.dto.request.UpdateStaffScheduleRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.NurseDoctorAssignmentResponse;
import g2.be.dto.response.StaffScheduleDetailResponse;
import g2.be.dto.response.StaffScheduleResponse;
import g2.be.service.StaffScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffScheduleControllerTest {

    @Mock
    private StaffScheduleService staffScheduleService;

    @InjectMocks
    private StaffScheduleController staffScheduleController;

    @Test
    void getStaffSchedulesReturnsOkMap() {
        Map<String, Object> result = Map.of("staffSchedules", List.of(), "totalItems", 0L);

        when(staffScheduleService.getStaffSchedules(LocalDate.of(2026, 4, 17), 1, 0, 20)).thenReturn(result);

        ResponseEntity<?> response = staffScheduleController.getStaffSchedules(LocalDate.of(2026, 4, 17), 1, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(result, response.getBody());
    }

    @Test
    void getStaffScheduleDetailReturnsOkResponse() {
        StaffScheduleDetailResponse result = mock(StaffScheduleDetailResponse.class);

        when(staffScheduleService.getStaffScheduleDetail(9L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(result);

        ResponseEntity<?> response =
                staffScheduleController.getStaffScheduleDetail(9L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(result, response.getBody());
    }

    @Test
    void createStaffScheduleReturnsCreatedResponse() {
        CreateStaffScheduleRequest request = new CreateStaffScheduleRequest();
        StaffScheduleResponse result = mock(StaffScheduleResponse.class);

        when(staffScheduleService.createStaffSchedule(request)).thenReturn(result);

        ResponseEntity<?> response = staffScheduleController.createStaffSchedule(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(result, response.getBody());
    }

    @Test
    void updateStaffScheduleReturnsOkResponse() {
        UpdateStaffScheduleRequest request = new UpdateStaffScheduleRequest();
        StaffScheduleResponse result = mock(StaffScheduleResponse.class);

        when(staffScheduleService.updateStaffSchedule(22L, request)).thenReturn(result);

        ResponseEntity<?> response = staffScheduleController.updateStaffSchedule(22L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(result, response.getBody());
    }

    @Test
    void deleteStaffScheduleReturnsOkResponse() {
        MessageResponse result = MessageResponse.builder().message("deleted").build();

        when(staffScheduleService.deleteStaffSchedule(22L)).thenReturn(result);

        ResponseEntity<?> response = staffScheduleController.deleteStaffSchedule(22L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(result, response.getBody());
    }

    @Test
    void assignNurseToDoctorReturnsCreatedResponse() {
        AssignNurseToDoctorRequest request = new AssignNurseToDoctorRequest();
        NurseDoctorAssignmentResponse result = mock(NurseDoctorAssignmentResponse.class);

        when(staffScheduleService.assignNurseToDoctor(request)).thenReturn(result);

        ResponseEntity<?> response = staffScheduleController.assignNurseToDoctor(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(result, response.getBody());
    }
}