package g2.be.controller;

import g2.be.dto.request.CreatePatientRequest;
import g2.be.dto.request.UpdatePatientRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.UserDetailResponse;
import g2.be.service.PatientManagementService;
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
class PatientManagementControllerTest {

    @Mock
    private PatientManagementService patientManagementService;

    @InjectMocks
    private PatientManagementController patientManagementController;

    @Test
    void getAllPatientsReturnsPagedResponse() {
        UserDetailResponse user = mock(UserDetailResponse.class);
        PageResponse<UserDetailResponse> pageResponse = PageResponse.<UserDetailResponse>builder()
                .content(List.of(user))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(patientManagementService.getAllPatients(0, 10, "userId", "desc", "Lan", true)).thenReturn(pageResponse);

        ResponseEntity<PageResponse<UserDetailResponse>> response =
                patientManagementController.getAllPatients(0, 10, "userId", "desc", "Lan", true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(pageResponse, response.getBody());
    }

    @Test
    void getPatientByIdReturnsUserDetail() {
        UserDetailResponse userDetailResponse = mock(UserDetailResponse.class);

        when(patientManagementService.getPatientById(12L)).thenReturn(userDetailResponse);

        ResponseEntity<UserDetailResponse> response = patientManagementController.getPatientById(12L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(userDetailResponse, response.getBody());
    }

    @Test
    void createPatientReturnsCreatedResponse() {
        CreatePatientRequest request = new CreatePatientRequest();
        UserDetailResponse userDetailResponse = mock(UserDetailResponse.class);

        when(patientManagementService.createPatient(request)).thenReturn(userDetailResponse);

        ResponseEntity<UserDetailResponse> response = patientManagementController.createPatient(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(userDetailResponse, response.getBody());
    }

    @Test
    void updatePatientReturnsUpdatedUserDetail() {
        UpdatePatientRequest request = new UpdatePatientRequest();
        UserDetailResponse userDetailResponse = mock(UserDetailResponse.class);

        when(patientManagementService.updatePatient(12L, request)).thenReturn(userDetailResponse);

        ResponseEntity<UserDetailResponse> response = patientManagementController.updatePatient(12L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(userDetailResponse, response.getBody());
    }

    @Test
    void deletePatientReturnsMessageResponse() {
        MessageResponse messageResponse = MessageResponse.builder().message("deleted").build();

        when(patientManagementService.deletePatient(12L)).thenReturn(messageResponse);

        ResponseEntity<MessageResponse> response = patientManagementController.deletePatient(12L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(messageResponse, response.getBody());
    }

    @Test
    void lockPatientDelegatesToService() {
        MessageResponse messageResponse = MessageResponse.builder().message("locked").build();

        when(patientManagementService.lockPatient(12L)).thenReturn(messageResponse);

        ResponseEntity<MessageResponse> response = patientManagementController.lockPatient(12L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(messageResponse, response.getBody());
        verify(patientManagementService).lockPatient(12L);
    }

    @Test
    void unlockPatientDelegatesToService() {
        MessageResponse messageResponse = MessageResponse.builder().message("unlocked").build();

        when(patientManagementService.unlockPatient(12L)).thenReturn(messageResponse);

        ResponseEntity<MessageResponse> response = patientManagementController.unlockPatient(12L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(messageResponse, response.getBody());
        verify(patientManagementService).unlockPatient(12L);
    }
}