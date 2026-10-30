package g2.be.controller;

import g2.be.dto.request.CreateWorkingTimeSlotRequest;
import g2.be.dto.request.UpdateWorkingTimeSlotRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.WorkingTimeSlotResponse;
import g2.be.service.WorkingTimeSlotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingTimeSlotControllerTest {

    @Mock
    private WorkingTimeSlotService workingTimeSlotService;

    @InjectMocks
    private WorkingTimeSlotController workingTimeSlotController;

    @Test
    void getAllActiveSlotsReturnsOkResponse() {
        WorkingTimeSlotResponse slotResponse = mock(WorkingTimeSlotResponse.class);
        List<WorkingTimeSlotResponse> slotResponses = List.of(slotResponse);

        when(workingTimeSlotService.getAllActiveSlots()).thenReturn(slotResponses);

        ResponseEntity<List<WorkingTimeSlotResponse>> response = workingTimeSlotController.getAllActiveSlots();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(slotResponses, response.getBody());
    }

    @Test
    void createTimeSlotReturnsCreatedResponse() {
        CreateWorkingTimeSlotRequest request = new CreateWorkingTimeSlotRequest();
        WorkingTimeSlotResponse slotResponse = mock(WorkingTimeSlotResponse.class);

        when(workingTimeSlotService.createTimeSlot(request)).thenReturn(slotResponse);

        ResponseEntity<WorkingTimeSlotResponse> response = workingTimeSlotController.createTimeSlot(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(slotResponse, response.getBody());
    }

    @Test
    void updateTimeSlotReturnsOkResponse() {
        UpdateWorkingTimeSlotRequest request = new UpdateWorkingTimeSlotRequest();
        WorkingTimeSlotResponse slotResponse = mock(WorkingTimeSlotResponse.class);

        when(workingTimeSlotService.updateTimeSlot(5L, request)).thenReturn(slotResponse);

        ResponseEntity<WorkingTimeSlotResponse> response = workingTimeSlotController.updateTimeSlot(5L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(slotResponse, response.getBody());
    }

    @Test
    void deleteTimeSlotReturnsSuccessApiResponse() {
        ResponseEntity<ApiResponse<Void>> response = workingTimeSlotController.deleteTimeSlot(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Working time slot deleted successfully", response.getBody().getMessage());
        verify(workingTimeSlotService).deleteTimeSlot(5L);
    }
}