package g2.be.controller;

import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.MedicalRecordDetailResponse;
import g2.be.dto.response.MedicalRecordResponse;
import g2.be.security.UserDetailsImpl;
import g2.be.service.MedicalRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalRecordControllerTest {

    @Mock
    private MedicalRecordService medicalRecordService;

    @InjectMocks
    private MedicalRecordController medicalRecordController;

    @Test
    void getMyHistoryReturnsPatientMedicalHistory() {
        UserDetailsImpl user = new UserDetailsImpl(15L, "patient", "patient@test.com", "secret", List.of());
        Page<MedicalRecordResponse> page = new PageImpl<>(List.of(mock(MedicalRecordResponse.class)));

        when(medicalRecordService.getMyHistory(15L, "đau đầu", PageRequest.of(0, 10))).thenReturn(page);

        ApiResponse<Page<MedicalRecordResponse>> response = medicalRecordController.getMyHistory(user, "đau đầu", 0, 10);

        assertTrue(response.isSuccess());
        assertSame(page, response.getData());
    }

    @Test
    void getMyDetailReturnsOwnedMedicalRecordDetail() {
        UserDetailsImpl user = new UserDetailsImpl(15L, "patient", "patient@test.com", "secret", List.of());
        MedicalRecordDetailResponse detailResponse = mock(MedicalRecordDetailResponse.class);

        when(medicalRecordService.getMyDetail(15L, "MR-001")).thenReturn(detailResponse);

        ApiResponse<MedicalRecordDetailResponse> response = medicalRecordController.getMyDetail("MR-001", user);

        assertTrue(response.isSuccess());
        assertSame(detailResponse, response.getData());
    }

    @Test
    void getPatientHistoryReturnsClinicalHistoryPage() {
        Page<MedicalRecordResponse> page = new PageImpl<>(List.of(mock(MedicalRecordResponse.class)));

        when(medicalRecordService.getPatientHistory("BN-001", "viêm", PageRequest.of(0, 10))).thenReturn(page);

        ApiResponse<Page<MedicalRecordResponse>> response = medicalRecordController.getPatientHistory("BN-001", "viêm", 0, 10);

        assertTrue(response.isSuccess());
        assertSame(page, response.getData());
    }

    @Test
    void viewDetailReturnsMedicalRecordDetail() {
        MedicalRecordDetailResponse detailResponse = mock(MedicalRecordDetailResponse.class);

        when(medicalRecordService.viewDetail("MR-001")).thenReturn(detailResponse);

        ApiResponse<MedicalRecordDetailResponse> response = medicalRecordController.viewDetail("MR-001");

        assertTrue(response.isSuccess());
        assertSame(detailResponse, response.getData());
    }
}