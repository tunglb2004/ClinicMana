package g2.be.controller;

import g2.be.dto.request.UpdatePatientRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.PatientResponse;
import g2.be.service.PatientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientController patientController;

    @Test
    void getPatientsUsesZeroBasedPageIndex() {
        PatientResponse patientResponse = mock(PatientResponse.class);
        PageResponse<PatientResponse> pageResponse = PageResponse.<PatientResponse>builder()
                .content(List.of(patientResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(patientService.getPatients(0, 10, "Lan", "user.userId", "asc", false)).thenReturn(pageResponse);

        ApiResponse<PageResponse<PatientResponse>> response =
                patientController.getPatients(1, 10, "Lan", "user.userId", "asc", false);

        assertTrue(response.isSuccess());
        assertSame(pageResponse, response.getData());
        verify(patientService).getPatients(0, 10, "Lan", "user.userId", "asc", false);
    }

    @Test
    void updatePatientReturnsUpdatedPatientResponse() {
        UpdatePatientRequest request = new UpdatePatientRequest();
        PatientResponse patientResponse = mock(PatientResponse.class);

        when(patientService.updatePatient("BN-001", request)).thenReturn(patientResponse);

        ApiResponse<PatientResponse> response = patientController.updatePatient("BN-001", request);

        assertTrue(response.isSuccess());
        assertSame(patientResponse, response.getData());
    }

    @Test
    void deletePatientReturnsSuccessMessage() {
        ApiResponse<String> response = patientController.deletePatient("BN-001");

        assertTrue(response.isSuccess());
        assertEquals("Patient information has been canceled (soft deleted).", response.getData());
        verify(patientService).deletePatient("BN-001");
    }
}