package g2.be.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import g2.be.dto.request.CashHandoverRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.CashHandoverResponse;
import g2.be.dto.response.CashierDaySummaryResponse;
import g2.be.dto.response.FinancialDetailResponse;
import g2.be.dto.response.FinancialResponse;
import g2.be.dto.response.ManagerCashierSummaryResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.ReceiptResponse;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.UserRepository;
import g2.be.security.UserDetailsImpl;
import g2.be.service.CashHandoverService;
import g2.be.service.FinancialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
@Tag(name = "Financial Management", description = "APIs quản lý tài chính - hóa đơn, phiếu thu, hóa đơn điện tử")
public class FinancialController {

        private final FinancialService financialService;
        private final UserRepository userRepository;
        private final CashHandoverService cashHandoverService;

    // ─────────────────────────────────────────────────────────────────────────
    // 0. GET /api/financial/unpaid-records  →  Danh sách lượt khám chưa thanh toán
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/unpaid-records")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Danh sách lượt khám chưa thanh toán",
            description = "Lấy danh sách các lượt khám (Medical Records) có chứa dịch vụ chưa lập hóa đơn (invoice_id IS NULL). " +
                    "Backend tự GROUP BY theo medical record và SUM giá dịch vụ để tính tổng tiền nợ. " +
                    "Hỗ trợ tìm kiếm theo tên bệnh nhân, mã bệnh nhân, mã lịch hẹn.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<FinancialResponse>> getUnpaidRecords(
            @Parameter(description = "Số trang (bắt đầu từ 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Số lượng phần tử trên mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Từ khóa tìm kiếm theo tên bệnh nhân, mã bệnh nhân hoặc mã lịch hẹn")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Chỉ lấy danh sách chờ thanh toán trong ngày hôm nay")
            @RequestParam(defaultValue = "false") boolean todayOnly) {
        return ApiResponse.<PageResponse<FinancialResponse>>builder()
                .success(true)
                .data(financialService.getUnpaidRecords(page - 1, size, keyword, todayOnly))
                .build();
    }

    public ApiResponse<PageResponse<FinancialResponse>> getUnpaidRecords(int page, int size, String keyword) {
        return getUnpaidRecords(page, size, keyword, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 0b. GET /api/financial/supplement-records  →  Danh sách chỉ định bổ sung chưa thu tiền
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/supplement-records")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Danh sách lượt khám có chỉ định bổ sung chưa thanh toán",
            description = "Trả về danh sách Medical Records có ServiceOrder SUPPLEMENT + PENDING chưa có invoiceId. " +
                    "Response chứa recordId để FE truyền vào POST /api/payments/supplement-payment. " +
                    "Hỗ trợ tìm kiếm theo tên bệnh nhân, mã bệnh nhân, mã hồ sơ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<FinancialResponse>> getSupplementPendingRecords(
            @Parameter(description = "Số trang (bắt đầu từ 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Số lượng phần tử trên mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Tìm theo tên bệnh nhân, mã bệnh nhân hoặc mã hồ sơ")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Chỉ lấy danh sách chỉ định bổ sung trong ngày hôm nay")
            @RequestParam(defaultValue = "false") boolean todayOnly) {
        return ApiResponse.<PageResponse<FinancialResponse>>builder()
                .success(true)
                .data(financialService.getSupplementPendingRecords(page - 1, size, keyword, todayOnly))
                .build();
    }

    public ApiResponse<PageResponse<FinancialResponse>> getSupplementPendingRecords(int page, int size, String keyword) {
        return getSupplementPendingRecords(page, size, keyword, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. GET /api/financial/invoices  →  Danh sách hóa đơn thanh toán
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Danh sách hóa đơn thanh toán",
            description = "Lấy danh sách hóa đơn có phân trang, tìm kiếm theo tên bệnh nhân/mã hóa đơn, lọc theo trạng thái thanh toán (PAID, UNPAID, PENDING)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<FinancialResponse>> getInvoices(
            @Parameter(description = "Số trang (bắt đầu từ 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Số lượng phần tử trên mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Từ khóa tìm kiếm theo mã hóa đơn hoặc tên bệnh nhân")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc theo trạng thái thanh toán (PAID/UNPAID/PENDING)")
            @RequestParam(required = false) String paymentStatus,
            @Parameter(description = "Chi lay giao dich trong ngay hom nay")
            @RequestParam(defaultValue = "false") boolean todayOnly,
            @Parameter(description = "So ngay gan nhat can lay, tinh ca hom nay")
            @RequestParam(defaultValue = "0") int recentDays) {
        boolean sharedHistory = todayOnly || recentDays > 0;
        CashierScope cashierScope = sharedHistory ? new CashierScope(null, null) : getCashierInvoiceScope();
        return ApiResponse.<PageResponse<FinancialResponse>>builder()
                .success(true)
                .data(financialService.getInvoices(page - 1, size, keyword, paymentStatus,
                        cashierScope.userId(), cashierScope.staffId(), todayOnly, recentDays))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1b. GET /api/financial/invoices/{id}/status  →  Polling trạng thái hóa đơn (QR)
    // ─────────────────────────────────────────────────────────────────────────
    public ApiResponse<PageResponse<FinancialResponse>> getInvoices(
            int page,
            int size,
            String keyword,
            String paymentStatus) {
        return getInvoices(page, size, keyword, paymentStatus, false, 0);
    }

    @GetMapping("/invoices/{invoiceId}/status")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Kiểm tra trạng thái thanh toán hóa đơn",
            description = "FE polling endpoint này sau khi hiển thị QR. " +
                    "Trả về paymentStatus (PENDING / PAID). " +
                    "Dùng chung cho cả thanh toán phí khám ban đầu (prepayment) và thanh toán bổ sung (supplement-payment).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<Map<String, String>> getInvoiceStatus(
            @Parameter(description = "Mã hóa đơn (VD: INVE76B6B2F)", required = true)
            @PathVariable String invoiceId) {
        return ApiResponse.<Map<String, String>>builder()
                .success(true)
                .data(financialService.getInvoiceStatus(invoiceId))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. GET /api/financial/invoices/{id}  →  Chi tiết hóa đơn & phiếu thu
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Chi tiết hóa đơn & phiếu thu",
            description = "Xem chi tiết đầy đủ một hóa đơn: thông tin bệnh nhân, lịch hẹn, bác sĩ, danh sách dịch vụ, thông tin thanh toán",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<FinancialDetailResponse> getInvoiceDetail(
            @Parameter(description = "UID của hóa đơn (Ví dụ: INV0766D505)", required = true)
            @PathVariable String invoiceId) {
        return ApiResponse.<FinancialDetailResponse>builder()
                .success(true)
                .data(financialService.getInvoiceDetail(invoiceId))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. POST /api/financial/invoices/{id}/receipt  →  Xuất phiếu thu
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/invoices/{invoiceId}/receipt")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Xuất phiếu thu",
            description = "Xuất phiếu thu cho hóa đơn đã thanh toán. " +
                    "Tự động sinh mã phiếu thu (PT-yyyyMMdd-XXXX), mã hóa đơn điện tử (eInvoiceCode) " +
                    "và link xem hóa đơn điện tử (eInvoiceUrl). " +
                    "Chỉ cho phép xuất với hóa đơn có trạng thái PAID, không cho xuất lại nếu đã xuất.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<ReceiptResponse> issueReceipt(
            @Parameter(description = "UID của hóa đơn cần xuất phiếu (Ví dụ: INV0766D505)", required = true)
            @PathVariable String invoiceId) {
        return ApiResponse.<ReceiptResponse>builder()
                .success(true)
                .message("Xuất phiếu thu thành công.")
                .data(financialService.issueReceipt(invoiceId))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. GET /api/financial/invoices/{id}/receipt  →  Xem chi tiết phiếu thu
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/invoices/{invoiceId}/receipt")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Xem chi tiết phiếu thu",
            description = "Xem thông tin phiếu thu bao gồm mã phiếu thu, mã hóa đơn điện tử, " +
                    "link hóa đơn điện tử, thông tin bệnh nhân, danh sách dịch vụ và chi tiết thanh toán",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<ReceiptResponse> getReceiptDetail(@PathVariable String invoiceId) {
        return ApiResponse.<ReceiptResponse>builder()
                .success(true)
                .data(financialService.getReceiptDetail(invoiceId))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. GET /api/financial/summary  →  Tổng quan tài chính
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/cashier-dashboard")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Dashboard thu ngan",
            description = "Tra ve cac chi so dashboard bang query rieng. Doanh thu va hoa don hom nay duoc loc theo thu ngan dang dang nhap.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<Map<String, Object>> getCashierDashboardSummary() {
        CashierScope cashierScope = getCashierInvoiceScope();
        return ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(financialService.getCashierDashboardSummary(cashierScope.userId(), cashierScope.staffId()))
                .build();
    }

    @GetMapping("/cashier-day-summary")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Tong ket thu tien trong ngay",
            description = "Tra ve tong tien da thu, tien mat, chuyen khoan va tien mat con phai ban giao theo ngay.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<CashierDaySummaryResponse> getCashierDaySummary(
            @Parameter(description = "Ngay nghiep vu yyyy-MM-dd")
            @RequestParam(required = false) LocalDate date) {
        CashierScope cashierScope = getCashierInvoiceScope();
        return ApiResponse.<CashierDaySummaryResponse>builder()
                .success(true)
                .data(cashHandoverService.getCashierDaySummary(date, cashierScope.userId(), cashierScope.staffId()))
                .build();
    }

    @PostMapping("/cash-handovers")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Tao phieu ban giao tien mat",
            description = "Thu ngan tao phieu ban giao tien mat cho thu ngan ca sau.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<CashHandoverResponse> createCashHandover(@RequestBody CashHandoverRequest request) {
        return ApiResponse.<CashHandoverResponse>builder()
                .success(true)
                .message("Tao phieu ban giao tien mat thanh cong.")
                .data(cashHandoverService.createCashHandover(request, Long.valueOf(getCurrentUserId())))
                .build();
    }

    @GetMapping("/cash-handovers")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Danh sach phieu ban giao tien mat",
            description = "scope=mine tra phieu da giao; scope=incoming tra phieu dang cho nhan.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<List<CashHandoverResponse>> getCashHandovers(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "mine") String scope) {
        return ApiResponse.<List<CashHandoverResponse>>builder()
                .success(true)
                .data(cashHandoverService.getCashHandovers(date, scope, Long.valueOf(getCurrentUserId())))
                .build();
    }

    @PatchMapping("/cash-handovers/{handoverId}/confirm")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Xac nhan nhan ban giao tien mat",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<CashHandoverResponse> confirmCashHandover(@PathVariable Long handoverId) {
        return ApiResponse.<CashHandoverResponse>builder()
                .success(true)
                .message("Xac nhan ban giao tien mat thanh cong.")
                .data(cashHandoverService.confirmCashHandover(handoverId, Long.valueOf(getCurrentUserId())))
                .build();
    }

    @PatchMapping("/cash-handovers/{handoverId}/cancel")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Huy phieu ban giao tien mat",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<CashHandoverResponse> cancelCashHandover(@PathVariable Long handoverId) {
        return ApiResponse.<CashHandoverResponse>builder()
                .success(true)
                .message("Huy phieu ban giao tien mat thanh cong.")
                .data(cashHandoverService.cancelCashHandover(handoverId, Long.valueOf(getCurrentUserId())))
                .build();
    }

    @GetMapping("/manager/cashier-day-summary")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Manager xem tong ket thu tien theo thu ngan",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<ManagerCashierSummaryResponse> getManagerCashierDaySummary(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam Long cashierId) {
        return ApiResponse.<ManagerCashierSummaryResponse>builder()
                .success(true)
                .data(cashHandoverService.getManagerCashierSummary(date, fromDate, toDate, cashierId))
                .build();
    }

    @GetMapping("/manager/cashier-summaries")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Manager xem tong ket thu tien cua tat ca thu ngan",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<List<ManagerCashierSummaryResponse>> getManagerCashierSummaries(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ApiResponse.<List<ManagerCashierSummaryResponse>>builder()
                .success(true)
                .data(cashHandoverService.getManagerCashierSummaries(date, fromDate, toDate))
                .build();
    }

    @GetMapping("/manager/cash-handovers")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Manager xem danh sach phieu ban giao tien mat",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<List<CashHandoverResponse>> getManagerCashHandovers(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) String status) {
        return ApiResponse.<List<CashHandoverResponse>>builder()
                .success(true)
                .data(cashHandoverService.getManagerCashHandovers(date, fromDate, toDate, cashierId, status))
                .build();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('CASHIER', 'SYSTEM_ADMIN', 'ADMIN', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Tổng quan tài chính",
            description = "Thống kê tổng doanh thu, số hóa đơn theo trạng thái, tổng BHYT chi trả",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<Map<String, Object>> getFinancialSummary() {
        return ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(financialService.getFinancialSummary())
                .build();
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return String.valueOf(userDetails.getId());
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    private CashierScope getCashierInvoiceScope() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean cashier = hasAuthority(authentication, "ROLE_CASHIER");
        boolean canSeeAll = hasAuthority(authentication, "ROLE_SYSTEM_ADMIN")
                || hasAuthority(authentication, "ROLE_ADMIN")
                || hasAuthority(authentication, "ROLE_CLINIC_MANAGER")
                || hasAuthority(authentication, "ROLE_MANAGER");
        if (!cashier || canSeeAll) {
            return new CashierScope(null, null);
        }

        String userId = getCurrentUserId();
        String staffId = userRepository.findById(Long.valueOf(userId))
                .map(user -> user.getStaffId())
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
        return new CashierScope(userId, staffId);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private record CashierScope(String userId, String staffId) {
    }
}

