package g2.be.controller;

import g2.be.dto.request.PrepaymentRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void collectPrepaymentReturnsSuccessfulApiResponse() {
        PrepaymentRequest request = new PrepaymentRequest();
        g2.be.dto.response.PaymentResponse paymentResponse = mock(g2.be.dto.response.PaymentResponse.class);

        when(paymentService.collectPrepayment(request)).thenReturn(paymentResponse);

        ApiResponse<g2.be.dto.response.PaymentResponse> response = paymentController.collectPrepayment(request);

        assertTrue(response.isSuccess());
        assertSame(paymentResponse, response.getData());
    }
}