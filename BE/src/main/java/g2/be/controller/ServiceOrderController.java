package g2.be.controller;

import g2.be.dto.request.CreateServiceOrderRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.PendingOrdersByRecordResponse;
import g2.be.dto.response.ServiceOrderResponse;
import g2.be.security.UserDetailsImpl;
import g2.be.service.ServiceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-orders")
@RequiredArgsConstructor
@Tag(name = "Service Order Management", description = "APIs quản lý chỉ định cận lâm sàng / dịch vụ khám")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserDetailsImpl) auth.getPrincipal()).getId();
    }

    @GetMapping("/my-queue")
    @PreAuthorize("hasAnyAuthority('ROLE_SPECIALISTDOCTOR', 'ROLE_DOCTOR', 'ROLE_NURSE')")
    @Operation(summary = "Hàng đợi cận lâm sàng theo phòng của nhân viên",
            description = "Lấy danh sách chỉ định cận lâm sàng thuộc phòng của bác sĩ/y tá hiện tại (hôm nay)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Page<ServiceOrderResponse>> getMyQueue(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<ServiceOrderResponse>>builder()
                .success(true)
                .data(serviceOrderService.getMyQueue(userId, status, pageable))
                .build();
    }

    @GetMapping("/my-queue/by-appointment/{appointmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SPECIALISTDOCTOR', 'ROLE_DOCTOR', 'ROLE_NURSE')")
    @Operation(summary = "Chỉ định cận lâm sàng theo lịch hẹn và phòng của nhân viên",
            description = "Lấy các phiếu cận lâm sàng thuộc phòng của bác sĩ/y tá hiện tại cho một appointment cụ thể trong ngày",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<ServiceOrderResponse>> getMyQueueByAppointment(@PathVariable String appointmentId) {
        Long userId = getCurrentUserId();
        return ApiResponse.<List<ServiceOrderResponse>>builder()
                .success(true)
                .data(serviceOrderService.getMyQueueByAppointment(userId, appointmentId))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Danh sách tất cả chỉ định cận lâm sàng", description = "Lấy danh sách phân trang, có thể lọc theo status", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Page<ServiceOrderResponse>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<ServiceOrderResponse>>builder()
                .success(true)
                .data(serviceOrderService.getAllOrders(status, pageable))
                .build();
    }

    @GetMapping("/supplement")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Danh sách chỉ định bổ sung", description = "Lấy danh sách phân trang các phiếu orderType=SUPPLEMENT, có thể lọc theo status", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Page<ServiceOrderResponse>> getSupplementOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<ServiceOrderResponse>>builder()
                .success(true)
                .data(serviceOrderService.getSupplementOrders(status, pageable))
                .build();
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Xem chi tiết chỉ định cận lâm sàng", description = "Lấy thông tin chi tiết một phiếu chỉ định cận lâm sàng", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<ServiceOrderResponse> getServiceOrder(@PathVariable String orderId) {
        return ApiResponse.<ServiceOrderResponse>builder()
                .success(true)
                .data(serviceOrderService.getServiceOrder(orderId))
                .build();
    }

        @GetMapping("/my-completed")
        @PreAuthorize("hasAuthority('ROLE_PATIENT')")
        @Operation(summary = "Kết quả CLS đã hoàn thành của bệnh nhân hiện tại", security = @SecurityRequirement(name = "bearerAuth"))
        public ApiResponse<List<ServiceOrderResponse>> getMyCompleted() {
                Long userId = getCurrentUserId();
                return ApiResponse.<List<ServiceOrderResponse>>builder()
                                .success(true)
                                .data(serviceOrderService.getCompletedByCurrentPatient(userId))
                                .build();
        }

        @GetMapping("/my-record/{recordId}")
        @PreAuthorize("hasAuthority('ROLE_PATIENT')")
        @Operation(summary = "Kết quả cận lâm sàng theo hồ sơ của bệnh nhân hiện tại", security = @SecurityRequirement(name = "bearerAuth"))
        public ApiResponse<List<ServiceOrderResponse>> getMyRecordResults(@PathVariable String recordId) {
                Long userId = getCurrentUserId();
                return ApiResponse.<List<ServiceOrderResponse>>builder()
                                .success(true)
                                .data(serviceOrderService.getMyRecordResults(userId, recordId))
                                .build();
        }

    @GetMapping("/by-record/{recordId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Danh sách chỉ định theo hồ sơ khám", description = "Lấy tất cả phiếu chỉ định cận lâm sàng của một hồ sơ khám", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<ServiceOrderResponse>> getByRecord(@PathVariable String recordId) {
        return ApiResponse.<List<ServiceOrderResponse>>builder()
                .success(true)
                .data(serviceOrderService.getByRecord(recordId))
                .build();
    }

    @GetMapping("/by-patient/{patientId}/completed")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Kết quả CLS đã hoàn thành theo bệnh nhân", description = "Lấy tất cả phiếu chỉ định đã có kết quả (COMPLETED) của một bệnh nhân, across tất cả hồ sơ khám", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<ServiceOrderResponse>> getCompletedByPatient(@PathVariable String patientId) {
        return ApiResponse.<List<ServiceOrderResponse>>builder()
                .success(true)
                .data(serviceOrderService.getCompletedByPatient(patientId))
                .build();
    }

    @GetMapping("/by-appointment/{appointmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Danh sách chỉ định theo lịch hẹn", description = "Lấy toàn bộ những gì đã được chỉ định trong một appointmentId", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<ServiceOrderResponse>> getByAppointment(@PathVariable String appointmentId) {
        return ApiResponse.<List<ServiceOrderResponse>>builder()
                .success(true)
                .data(serviceOrderService.getByAppointment(appointmentId))
                .build();
    }

    @PostMapping
        @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Chỉ định cận lâm sàng lần đầu", description = "Bác sĩ chỉ định các dịch vụ cận lâm sàng lần đầu cho bệnh nhân (thanh toán trước khám)", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<ServiceOrderResponse>> createServiceOrders(@RequestBody CreateServiceOrderRequest request) {
        Long userId = getCurrentUserId();
        return ApiResponse.<List<ServiceOrderResponse>>builder()
                .success(true)
                .data(serviceOrderService.createServiceOrders(userId, request))
                .build();
    }

    @GetMapping("/supplement/by-record/{recordId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN', 'ROLE_RECEPTIONIST', 'ROLE_CASHIER')")
    @Operation(summary = "Chỉ định bổ sung chưa thanh toán theo hồ sơ khám",
            description = "Lấy danh sách chỉ định orderType=SUPPLEMENT, status=PENDING của một hồ sơ. Thu ngân dùng để xem trước khi thu tiền bổ sung.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<PendingOrdersByRecordResponse> getSupplementOrdersByRecord(@PathVariable String recordId) {
        return ApiResponse.<PendingOrdersByRecordResponse>builder()
                .success(true)
                .data(serviceOrderService.getSupplementOrdersByRecord(recordId))
                .build();
    }

    @PostMapping("/supplement")
        @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Bổ sung chỉ định cận lâm sàng", description = "Bổ sung thêm chỉ định sau khi đã khám, bệnh nhân cần thanh toán bổ sung", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<ServiceOrderResponse>> createSupplementOrders(@RequestBody CreateServiceOrderRequest request) {
        Long userId = getCurrentUserId();
        return ApiResponse.<List<ServiceOrderResponse>>builder()
                .success(true)
                .data(serviceOrderService.createSupplementOrders(userId, request))
                .build();
    }

    @PutMapping("/{orderId}/cancel")
        @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Hủy chỉ định chưa thanh toán",
            description = "Chỉ cho hủy chỉ định chưa có kết quả và chưa thanh toán. Nếu thuộc hóa đơn chờ thanh toán thì tổng tiền hóa đơn sẽ được cập nhật lại.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<ServiceOrderResponse> cancelOrder(@PathVariable String orderId) {
        return ApiResponse.<ServiceOrderResponse>builder()
                .success(true)
                .data(serviceOrderService.cancelOrder(orderId))
                .build();
    }

    @PostMapping(value = "/{orderId}/upload-result", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Tải lên file kết quả cận lâm sàng", description = "Tải file (ảnh/PDF) kết quả lên Cloudinary và lưu vào phiếu", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<ServiceOrderResponse> uploadResultFile(
            @PathVariable String orderId,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        return ApiResponse.<ServiceOrderResponse>builder()
                .success(true)
                .data(serviceOrderService.uploadResultFile(orderId, file))
                .build();
    }

    @PutMapping("/{orderId}/result")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Cập nhật kết luận/nội dung kết quả khám", description = "Bác sĩ hoặc kỹ thuật viên nhập kết luận kết quả", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<ServiceOrderResponse> updateResult(
            @PathVariable String orderId,
            @RequestBody g2.be.dto.request.UpdateServiceOrderResultRequest request) {
        return ApiResponse.<ServiceOrderResponse>builder()
                .success(true)
                .data(serviceOrderService.updateResult(orderId, request.getResultNote()))
                .build();
    }

    @GetMapping(value = "/{orderId}/export", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Xuất phiếu kết quả ra PDF", description = "Trả về file PDF chứa thông tin kết quả khám", security = @SecurityRequirement(name = "bearerAuth"))
    public org.springframework.http.ResponseEntity<byte[]> exportPdf(@PathVariable String orderId) {
        byte[] pdfBytes = serviceOrderService.exportPdf(orderId);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Result_" + orderId + ".pdf");

        return new org.springframework.http.ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping(value = "/my/{orderId}/export", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    @Operation(summary = "Bá»‡nh nhÃ¢n xuáº¥t phiáº¿u káº¿t quáº£ ra PDF", description = "Tráº£ vá» file PDF káº¿t quáº£ cho chá»‰ Ä‘á»‹nh thuá»™c bá»‡nh nhÃ¢n hiá»‡n táº¡i", security = @SecurityRequirement(name = "bearerAuth"))
    public org.springframework.http.ResponseEntity<byte[]> exportMyPdf(@PathVariable String orderId) {
        Long userId = getCurrentUserId();
        byte[] pdfBytes = serviceOrderService.exportPdfForPatient(userId, orderId);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.inline()
                .filename("Result_" + orderId + ".pdf")
                .build());

        return new org.springframework.http.ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }
}
