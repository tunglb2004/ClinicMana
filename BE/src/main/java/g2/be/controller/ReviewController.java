package g2.be.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import g2.be.dto.request.CreateReviewRequest;
import g2.be.dto.request.ReplyReviewRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.ReviewResponse;
import g2.be.security.UserDetailsImpl;
import g2.be.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Management", description = "APIs quản lý đánh giá & phản hồi phòng khám")
public class ReviewController {

    private final ReviewService reviewService;

        private Long getCurrentUserId() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                return ((UserDetailsImpl) authentication.getPrincipal()).getId();
        }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT')")
    @Operation(
            summary = "Tạo đánh giá",
            description = "Bệnh nhân tạo đánh giá cho bác sĩ/lịch hẹn",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<ReviewResponse> createReview(@RequestBody CreateReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .message("Cảm ơn bạn! Đánh giá của bạn đã được gửi.")
                .data(reviewService.createReview(getCurrentUserId(), request))
                .build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    @Operation(
            summary = "Danh sách đánh giá của tôi",
            description = "Lấy danh sách đánh giá của bệnh nhân đang đăng nhập có phân trang",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .success(true)
                .data(reviewService.getMyReviews(getCurrentUserId(), page - 1, size))
                .build();
    }

    @GetMapping("/my/appointment/{appointmentId}")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    @Operation(
            summary = "Đánh giá của tôi theo ca khám",
            description = "Lấy danh sách đánh giá của bệnh nhân hiện tại cho một ca khám cụ thể",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<java.util.List<ReviewResponse>> getMyAppointmentReviews(@PathVariable String appointmentId) {
        return ApiResponse.<java.util.List<ReviewResponse>>builder()
                .success(true)
                .data(reviewService.getMyAppointmentReviews(getCurrentUserId(), appointmentId))
                .build();
    }

    @PutMapping("/{reviewId}/reply")
    @PreAuthorize("hasAnyRole('DOCTOR', 'SYSTEM_ADMIN', 'RECEPTIONIST', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Phản hồi đánh giá",
            description = "Bác sĩ hoặc nhân viên phản hồi đánh giá của bệnh nhân",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<ReviewResponse> replyReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReplyReviewRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .message("Đã đăng phản hồi thành công.")
                .data(reviewService.replyReview(reviewId, request, userDetails.getId()))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'RECEPTIONIST', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Danh sách đánh giá",
            description = "Lấy tất cả đánh giá có phân trang",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<ReviewResponse>> getAllReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .success(true)
                .data(reviewService.getAllReviews(page - 1, size))
                .build();
    }

    @GetMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'RECEPTIONIST', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Chi tiết đánh giá",
            description = "Xem chi tiết một đánh giá",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<ReviewResponse> getReviewById(@PathVariable Long reviewId) {
        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .data(reviewService.getReviewById(reviewId))
                .build();
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(
            summary = "Đánh giá theo bác sĩ",
            description = "Lấy danh sách đánh giá của một bác sĩ có phân trang",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<ReviewResponse>> getReviewsByDoctor(
            @PathVariable String doctorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .success(true)
                .data(reviewService.getReviewsByDoctor(doctorId, page - 1, size))
                .build();
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'SYSTEM_ADMIN', 'RECEPTIONIST', 'CLINIC_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Đánh giá theo bệnh nhân",
            description = "Lấy danh sách đánh giá của một bệnh nhân có phân trang",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<ReviewResponse>> getReviewsByPatient(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .success(true)
                .data(reviewService.getReviewsByPatient(patientId, page - 1, size))
                .build();
    }
}
