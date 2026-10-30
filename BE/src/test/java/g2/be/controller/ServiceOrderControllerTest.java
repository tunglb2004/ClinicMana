package g2.be.controller;

import g2.be.dto.request.CreateServiceOrderRequest;
import g2.be.dto.request.UpdateServiceOrderResultRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.ServiceOrderResponse;
import g2.be.security.UserDetailsImpl;
import g2.be.service.ServiceOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderControllerTest {

    @Mock
    private ServiceOrderService serviceOrderService;

    @InjectMocks
    private ServiceOrderController serviceOrderController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyQueueReturnsPagedOrders() {
        setAuthenticatedUser(21L);
        Page<ServiceOrderResponse> page = new PageImpl<>(List.of(mock(ServiceOrderResponse.class)));

        when(serviceOrderService.getMyQueue(21L, "ORDERED", PageRequest.of(0, 20))).thenReturn(page);

        ApiResponse<Page<ServiceOrderResponse>> response = serviceOrderController.getMyQueue("ORDERED", 0, 20);

        assertTrue(response.isSuccess());
        assertSame(page, response.getData());
    }

    @Test
    void getServiceOrderReturnsOrderDetail() {
        ServiceOrderResponse serviceOrderResponse = mock(ServiceOrderResponse.class);

        when(serviceOrderService.getServiceOrder("SO-001")).thenReturn(serviceOrderResponse);

        ApiResponse<ServiceOrderResponse> response = serviceOrderController.getServiceOrder("SO-001");

        assertTrue(response.isSuccess());
        assertSame(serviceOrderResponse, response.getData());
    }

    @Test
    void createServiceOrdersReturnsCreatedOrders() {
        setAuthenticatedUser(21L);
        CreateServiceOrderRequest request = new CreateServiceOrderRequest();
        List<ServiceOrderResponse> responses = List.of(mock(ServiceOrderResponse.class));

        when(serviceOrderService.createServiceOrders(21L, request)).thenReturn(responses);

        ApiResponse<List<ServiceOrderResponse>> response = serviceOrderController.createServiceOrders(request);

        assertTrue(response.isSuccess());
        assertSame(responses, response.getData());
    }

    @Test
    void uploadResultFileReturnsUpdatedOrder() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "result.pdf", "application/pdf", new byte[] {1, 2, 3});
        ServiceOrderResponse serviceOrderResponse = mock(ServiceOrderResponse.class);

        when(serviceOrderService.uploadResultFile("SO-001", file)).thenReturn(serviceOrderResponse);

        ApiResponse<ServiceOrderResponse> response = serviceOrderController.uploadResultFile("SO-001", file);

        assertTrue(response.isSuccess());
        assertSame(serviceOrderResponse, response.getData());
    }

    @Test
    void updateResultReturnsUpdatedOrder() {
        UpdateServiceOrderResultRequest request = mock(UpdateServiceOrderResultRequest.class);
        ServiceOrderResponse serviceOrderResponse = mock(ServiceOrderResponse.class);

        when(request.getResultNote()).thenReturn("Kết quả ổn định");
        when(serviceOrderService.updateResult("SO-001", "Kết quả ổn định")).thenReturn(serviceOrderResponse);

        ApiResponse<ServiceOrderResponse> response = serviceOrderController.updateResult("SO-001", request);

        assertTrue(response.isSuccess());
        assertSame(serviceOrderResponse, response.getData());
    }

    @Test
    void exportMyPdfReturnsPatientPdf() {
        setAuthenticatedUser(21L);
        byte[] pdfBytes = new byte[] {1, 2, 3};

        when(serviceOrderService.exportPdfForPatient(21L, "SO-001")).thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = serviceOrderController.exportMyPdf("SO-001");

        assertSame(pdfBytes, response.getBody());
        assertSame(HttpStatus.OK, response.getStatusCode());
    }

    private void setAuthenticatedUser(long userId) {
        UserDetailsImpl userDetails = new UserDetailsImpl(userId, "doctor", "doctor@test.com", "secret", List.of());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
