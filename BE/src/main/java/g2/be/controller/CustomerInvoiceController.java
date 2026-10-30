package g2.be.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.FinancialResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.ReceiptResponse;
import g2.be.security.UserDetailsImpl;
import g2.be.service.FinancialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customer/invoices")
@RequiredArgsConstructor
@Tag(name = "Customer Invoice", description = "APIs hóa đơn dành cho bệnh nhân — chỉ cần đăng nhập, không phân quyền role")
public class CustomerInvoiceController {

    private final FinancialService financialService;

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy userId từ token đang đăng nhập
    // ─────────────────────────────────────────────────────────────────────────
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. GET /api/customer/invoices  →  Danh sách hóa đơn của tôi
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    @Operation(
            summary = "Danh sách hóa đơn của tôi",
            description = "Lấy danh sách hóa đơn của bệnh nhân đang đăng nhập. " +
                    "Hỗ trợ lọc theo trạng thái thanh toán (PAID, UNPAID, PENDING) và phân trang.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<FinancialResponse>> getMyInvoices(
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        return ApiResponse.<PageResponse<FinancialResponse>>builder()
                .success(true)
                .data(financialService.getMyInvoices(userId, paymentStatus, page - 1, size))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. GET /api/customer/invoices/{id}  →  Chi tiết hóa đơn & phiếu thu
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{invoiceId}")
    @Operation(
            summary = "Chi tiết hóa đơn & phiếu thu",
            description = "Xem chi tiết hóa đơn của tôi: thông tin bệnh nhân, bác sĩ, danh sách dịch vụ, " +
                    "thông tin thanh toán, mã phiếu thu và link hóa đơn điện tử (nếu đã xuất).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<ReceiptResponse> getMyInvoiceDetail(@PathVariable String invoiceId) {
        Long userId = getCurrentUserId();
        return ApiResponse.<ReceiptResponse>builder()
                .success(true)
                .data(financialService.getMyInvoiceDetail(userId, invoiceId))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. POST /api/customer/invoices/{id}/receipt  →  Xuất phiếu thu
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/{invoiceId}/receipt")
    @Operation(
            summary = "Xuất phiếu thu",
            description = "Xuất phiếu thu cho hóa đơn đã thanh toán của tôi. " +
                    "Tự động sinh mã phiếu thu (PT-yyyyMMdd-XXXX), mã hóa đơn điện tử (eInvoiceCode) " +
                    "và link xem hóa đơn điện tử (eInvoiceUrl). " +
                    "Chỉ cho phép xuất khi hóa đơn đã PAID, không cho xuất lại.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<ReceiptResponse> issueMyReceipt(@PathVariable String invoiceId) {
        Long userId = getCurrentUserId();
        return ApiResponse.<ReceiptResponse>builder()
                .success(true)
                .message("Xuất phiếu thu thành công.")
                .data(financialService.issueMyReceipt(userId, invoiceId))
                .build();
    }
}
