package g2.be.controller;

import g2.be.dto.request.PrepaymentRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "APIs quản lý thanh toán và thu tiền")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/prepayment")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Thu tiền lần đầu (trước khám)",
        description = "Thu tiền các dịch vụ chỉ định ban đầu. Bệnh nhân thanh toán trước khi vào khám cận lâm sàng.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<g2.be.dto.response.PaymentResponse> collectPrepayment(@RequestBody PrepaymentRequest request) {
        return ApiResponse.<g2.be.dto.response.PaymentResponse>builder()
                .success(true)
                .data(paymentService.collectPrepayment(request))
                .build();
    }

    @PostMapping("/supplement-payment")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Thu tiền bổ sung (sau khám)",
        description = "Thu tiền các dịch vụ chỉ định bổ sung sau khi bác sĩ khám xong đợt đầu và cần thêm xét nghiệm. Tạo hóa đơn riêng cho đợt bổ sung.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<g2.be.dto.response.PaymentResponse> collectSupplementPayment(
            @RequestBody g2.be.dto.request.SupplementPaymentRequest request) {
        return ApiResponse.<g2.be.dto.response.PaymentResponse>builder()
                .success(true)
                .data(paymentService.collectSupplementPayment(request))
                .build();
    }

    @PostMapping("/sepay-webhook")
    @Operation(
        summary = "SePay Webhook",
        description = "Webhook callback từ SePay khi có giao dịch chuyển khoản. API này được gọi tự động từ SePay, không cần authentication token (xác thực qua API Key trong header)",
        security = {}
    )
    public org.springframework.http.ResponseEntity<String> sepayWebhook(
            @RequestBody java.util.Map<String, Object> payload,
            @Parameter(description = "API Key từ SePay (format: 'Apikey YOUR_KEY')", example = "Apikey YBLZKVV8...")
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        paymentService.processSepayWebhook(payload, authorizationHeader);
        return org.springframework.http.ResponseEntity.ok("OK");
    }
}
