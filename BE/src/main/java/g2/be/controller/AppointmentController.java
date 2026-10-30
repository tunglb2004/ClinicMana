package g2.be.controller;

import g2.be.dto.request.BookAppointmentRequest;
import g2.be.dto.request.CancelAppointmentRequest;
import g2.be.dto.request.CompleteExaminationRequest;
import g2.be.dto.request.CreateAppointmentRequest;
import g2.be.dto.request.TransferAppointmentRoomRequest;
import g2.be.dto.request.UpdateAppointmentRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.AppointmentDetailResponse;
import g2.be.dto.response.AppointmentResponse;
import g2.be.dto.response.AvailableSlotResponse;
import g2.be.dto.response.RoomTransferOptionResponse;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.PatientRepository;
import g2.be.security.UserDetailsImpl;
import g2.be.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Management", description = "APIs quản lý lịch hẹn khám bệnh")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;
    private final g2.be.service.StaffScheduleService staffScheduleService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserDetailsImpl) auth.getPrincipal()).getId();
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lịch hẹn của tôi", description = "Lấy danh sách lịch hẹn của bệnh nhân đang đăng nhập", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Page<AppointmentResponse>> getMyAppointments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        String patientId = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND))
                .getPatientId();
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<AppointmentResponse>>builder()
                .success(true)
                .data(appointmentService.getVisitHistoryByPatient(patientId, pageable))
                .build();
    }

        @GetMapping("/my/{appointmentId}")
        @PreAuthorize("hasAuthority('ROLE_PATIENT')")
        @Operation(summary = "Chi tiết lịch hẹn của tôi", description = "Lấy chi tiết lịch hẹn của bệnh nhân đang đăng nhập", security = @SecurityRequirement(name = "bearerAuth"))
        public ApiResponse<AppointmentDetailResponse> getMyAppointment(@PathVariable String appointmentId) {
                Long userId = getCurrentUserId();
                return ApiResponse.<AppointmentDetailResponse>builder()
                                .success(true)
                                .data(appointmentService.getMyAppointmentDetail(userId, appointmentId))
                                .build();
        }

    @PatchMapping("/my/{appointmentId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hủy lịch hẹn của tôi", description = "Bệnh nhân hủy lịch hẹn của chính mình hoặc lịch đã đặt hộ khi chưa thanh toán.", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<AppointmentResponse> cancelMyAppointment(
            @PathVariable String appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request) {
        Long userId = getCurrentUserId();
        String patientId = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND))
                .getPatientId();
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .data(appointmentService.cancelMyAppointment(appointmentId, patientId, request))
                .build();
    }

        @PatchMapping("/{appointmentId}/cancel")
        @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER')")
        @Operation(summary = "Hủy lịch hẹn", description = "Lễ tân hoặc quản trị hủy lịch hẹn của bệnh nhân", security = @SecurityRequirement(name = "bearerAuth"))
        public ApiResponse<AppointmentResponse> cancelAppointment(
                        @PathVariable String appointmentId,
                        @Valid @RequestBody CancelAppointmentRequest request) {
                return ApiResponse.<AppointmentResponse>builder()
                                .success(true)
                                .data(appointmentService.cancelAppointment(appointmentId, request))
                                .build();
        }

    @GetMapping("/available-slots")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách ca khám còn trống", description = "Trả về các ca làm việc và số lượng lịch hẹn hiện tại theo ngày", security = @SecurityRequirement(name = "bearerAuth"))
        public ApiResponse<List<AvailableSlotResponse>> getAvailableSlots(
                        @RequestParam String date,
                        @RequestParam(required = false) List<String> serviceIds) {
        LocalDate localDate = LocalDate.parse(date);
        return ApiResponse.<List<AvailableSlotResponse>>builder()
                .success(true)
                                .data(appointmentService.getAvailableSlots(localDate, serviceIds))
                .build();
    }

    @GetMapping("/clinical-rooms")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Lấy phòng lâm sàng có bác sĩ", description = "Nhân viên lấy danh sách phòng ACTIVE có bác sĩ, nếu chọn ca+ngày thì chỉ trả phòng có bác sĩ trực", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<java.util.Map<String, Object>>> getClinicalRooms(
            @RequestParam(required = false) Long slotId,
                        @RequestParam(required = false) String date,
                        @RequestParam(required = false) List<String> serviceIds) {
        LocalDate localDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : null;
        return ApiResponse.<List<java.util.Map<String, Object>>>builder()
                .success(true)
                                .data(staffScheduleService.getClinicalRooms(slotId, localDate, serviceIds))
                .build();
    }

    @PostMapping("/my/book")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đặt lịch khám", description = "Bệnh nhân tự đặt lịch khám cho bản thân hoặc người thân; hệ thống tự xếp bác sĩ/phòng", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<AppointmentResponse> bookMyAppointment(@Valid @RequestBody BookAppointmentRequest request) {
        Long userId = getCurrentUserId();
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .data(appointmentService.bookAppointment(userId, request))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(
        summary = "Tạo lịch hẹn",
        description = "Tạo lịch hẹn mới cho bệnh nhân với bác sĩ",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .data(appointmentService.createAppointment(request))
                .build();
    }

    @PutMapping("/{appointmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER')")
    @Operation(summary = "Cập nhật lịch hẹn", description = "Cập nhật thông tin lịch hẹn (thời gian, trạng thái, ghi chú, etc.)", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<AppointmentResponse> updateAppointment(
            @PathVariable String appointmentId,
            @RequestBody UpdateAppointmentRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .data(appointmentService.updateAppointment(appointmentId, request))
                .build();
    }

    @GetMapping("/{appointmentId}/room-transfer-options")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Danh sach phong co the dieu phoi",
        description = "Le tan lay danh sach phong ACTIVE cung thuc hien dich vu cua lich hen de dieu phoi trong truong hop bat kha khang.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<List<RoomTransferOptionResponse>> getRoomTransferOptions(@PathVariable String appointmentId) {
        return ApiResponse.<List<RoomTransferOptionResponse>>builder()
                .success(true)
                .data(appointmentService.getRoomTransferOptions(appointmentId))
                .build();
    }

    @PatchMapping("/{appointmentId}/transfer-room")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Dieu phoi benh nhan sang phong khac",
        description = "Le tan chuyen phong chinh thuc cho benh nhan sang phong cung dich vu. Neu benh nhan dang cho, he thong cap nhat lai hang doi phong.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentResponse> transferRoom(
            @PathVariable String appointmentId,
            @RequestBody TransferAppointmentRoomRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Da dieu phoi benh nhan sang phong moi.")
                .data(appointmentService.transferAppointmentRoom(appointmentId, request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Danh sách lịch hẹn", description = "Lấy danh sách tất cả lịch hẹn trong hệ thống", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Page<AppointmentResponse>> getAppointments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<AppointmentResponse>>builder()
                .success(true)
                .data(appointmentService.getAppointments(status, pageable))
                .build();
    }

    @GetMapping("/nurse-clinical-patients")
    @PreAuthorize("hasAuthority('ROLE_NURSE')")
    @Operation(
        summary = "Hàng đợi lâm sàng cho y tá",
        description = "Y tá xem danh sách bệnh nhân hôm nay thuộc phòng lâm sàng mình đang trực.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<Page<AppointmentResponse>> getNurseClinicalPatients(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<AppointmentResponse>>builder()
                .success(true)
                .data(appointmentService.getAppointmentsForStaffClinicalRooms(userId, status, pageable))
                .build();
    }

    @PostMapping("/nurse-clinical-patients/call-next")
    @PreAuthorize("hasAuthority('ROLE_NURSE')")
    @Operation(
        summary = "Y tá gọi bệnh nhân lâm sàng tiếp theo",
        description = "Y tá gọi bệnh nhân tiếp theo trong hàng đợi phòng lâm sàng mình đang trực.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentResponse> callNextNurseClinicalPatient() {
        Long userId = getCurrentUserId();
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Đã gọi bệnh nhân tiếp theo.")
                .data(appointmentService.callNextPatientForStaffClinicalRooms(userId))
                .build();
    }

    @PostMapping("/nurse-clinical-patients/{appointmentId}/postpone")
    @PreAuthorize("hasAuthority('ROLE_NURSE')")
    @Operation(
        summary = "Y ta tam hoan benh nhan lam sang",
        description = "Y ta tam hoan benh nhan trong hang doi lam sang minh phu trach va goi ca tiep theo neu co.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentResponse> postponeNurseClinicalPatient(@PathVariable String appointmentId) {
        Long userId = getCurrentUserId();
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Da tam hoan benh nhan.")
                .data(appointmentService.postponePatientForStaffClinicalRooms(userId, appointmentId))
                .build();
    }

    @PostMapping("/nurse-clinical-patients/{appointmentId}/return-to-queue")
    @PreAuthorize("hasAuthority('ROLE_NURSE')")
    @Operation(
        summary = "Y ta dua benh nhan quay lai hang doi lam sang",
        description = "Y ta dua benh nhan tam hoan quay lai hang doi lam sang minh phu trach.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentResponse> returnNurseClinicalPatientToQueue(@PathVariable String appointmentId) {
        Long userId = getCurrentUserId();
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Da dua benh nhan quay lai hang doi.")
                .data(appointmentService.returnPostponedPatientToStaffClinicalQueue(userId, appointmentId))
                .build();
    }

    @GetMapping("/nurse-clinical-patients/{appointmentId}")
    @PreAuthorize("hasAuthority('ROLE_NURSE')")
    @Operation(
        summary = "Chi tiết bệnh nhân lâm sàng cho y tá",
        description = "Y tá xem chi tiết lịch hẹn thuộc phòng lâm sàng mình đang trực để nhập hộ sinh hiệu/thông tin ban đầu.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentDetailResponse> getNurseClinicalPatient(@PathVariable String appointmentId) {
        Long userId = getCurrentUserId();
        return ApiResponse.<AppointmentDetailResponse>builder()
                .success(true)
                .data(appointmentService.getAppointmentDetailForStaffRoom(userId, appointmentId))
                .build();
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER', 'ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR')")
    @Operation(
        summary = "Chi tiết lịch hẹn",
        description = "Xem chi tiết thông tin một lịch hẹn cụ thể (bao gồm thông tin bệnh nhân và hồ sơ khám nếu có)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentDetailResponse> getAppointment(@PathVariable String appointmentId) {
        return ApiResponse.<AppointmentDetailResponse>builder()
                .success(true)
                .data(appointmentService.getAppointmentDetail(appointmentId))
                .build();
    }

    @PatchMapping("/{appointmentId}/checkin")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Check-in bệnh nhân", description = "Lễ tân xác nhận bệnh nhân đã đến — chuyển trạng thái sang WAITING", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<AppointmentResponse> checkIn(@PathVariable String appointmentId) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .data(appointmentService.checkIn(appointmentId))
                .build();
    }

    @GetMapping("/my-patients")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Danh sách bệnh nhân của bác sĩ (Priority Queue)",
        description = "Danh sách sắp xếp theo Priority Score giảm dần. " +
                "Tổng điểm = priorityScore + số phút đã chờ (tính realtime). " +
                "Bệnh nhân IN_PROGRESS luôn ở trên cùng, sau đó là WAITING theo điểm.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<Page<AppointmentResponse>> getMyPatients(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<AppointmentResponse>>builder()
                .success(true)
                .data(appointmentService.getAppointmentsForDoctor(userId, status, pageable))
                .build();
    }

    @PostMapping("/my-patients/call-next")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Gọi bệnh nhân tiếp theo",
        description = "Rút bệnh nhân có tổng điểm cao nhất ra khỏi hàng đợi WAITING → IN_PROGRESS. " +
                "Nếu bác sĩ đang khám ai (IN_PROGRESS) thì phải hoàn tất trước.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentResponse> callNextPatient() {
        Long userId = getCurrentUserId();
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Đã gọi bệnh nhân tiếp theo.")
                .data(appointmentService.callNextPatient(userId))
                .build();
    }

    @PostMapping("/my-patients/{appointmentId}/postpone")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Tam hoan benh nhan hien tai",
        description = "Tam hoan benh nhan dang dung dau hang doi va dua benh nhan ke tiep len kham.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentResponse> postponePatient(@PathVariable String appointmentId) {
        Long userId = getCurrentUserId();
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Da tam hoan benh nhan va goi benh nhan ke tiep.")
                .data(appointmentService.postponePatient(userId, appointmentId))
                .build();
    }

    @PostMapping("/my-patients/{appointmentId}/return-to-queue")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Dua benh nhan tam hoan quay lai hang doi",
        description = "Go trang thai tam hoan va dua benh nhan quay lai hang doi cho goi tu dong.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentResponse> returnPostponedPatientToQueue(@PathVariable String appointmentId) {
        Long userId = getCurrentUserId();
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Da dua benh nhan quay lai hang doi.")
                .data(appointmentService.returnPostponedPatientToQueue(userId, appointmentId))
                .build();
    }

    @PatchMapping("/{appointmentId}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Hoàn tất khám",
        description = "Chuyển trạng thái IN_PROGRESS → COMPLETED sau khi bác sĩ khám xong.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<AppointmentResponse> completeExamination(
            @PathVariable String appointmentId,
            @RequestBody CompleteExaminationRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .message("Đã hoàn tất khám.")
                .data(appointmentService.completeExamination(appointmentId, request))
                .build();
    }

        @PatchMapping("/{appointmentId}/save-progress")
        @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
        @Operation(
                summary = "Lưu tiến độ khám",
                description = "Lưu thông tin khám lâm sàng và ghi chú mà không hoàn tất ca khám.",
                security = @SecurityRequirement(name = "bearerAuth")
        )
        public ApiResponse<AppointmentResponse> saveExaminationProgress(
                        @PathVariable String appointmentId,
                        @RequestBody CompleteExaminationRequest request) {
                return ApiResponse.<AppointmentResponse>builder()
                                .success(true)
                                .message("Đã lưu thông tin khám.")
                                .data(appointmentService.saveExaminationProgress(appointmentId, request))
                                .build();
        }

        @PatchMapping("/{appointmentId}/pause-for-paraclinical")
        @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
        @Operation(
                summary = "Luu tam va cho ket qua can lam sang",
                description = "Luu thong tin kham hien tai va chuyen lich hen sang trang thai cho ket qua can lam sang de bac si co the goi benh nhan tiep theo.",
                security = @SecurityRequirement(name = "bearerAuth")
        )
        public ApiResponse<AppointmentResponse> pauseForParaclinicalResults(
                        @PathVariable String appointmentId,
                        @RequestBody CompleteExaminationRequest request) {
                return ApiResponse.<AppointmentResponse>builder()
                                .success(true)
                                .message("Da luu tam va chuyen benh nhan sang cho ket qua can lam sang.")
                                .data(appointmentService.pauseForParaclinicalResults(appointmentId, request))
                                .build();
        }

        @PatchMapping("/{appointmentId}/nurse-save-progress")
        @PreAuthorize("hasAuthority('ROLE_NURSE')")
        @Operation(
                summary = "Y tá lưu nhập hộ lâm sàng",
                description = "Y tá chỉ được lưu sinh hiệu, thông tin ban đầu và triệu chứng sơ bộ cho bệnh nhân thuộc phòng mình đang trực.",
                security = @SecurityRequirement(name = "bearerAuth")
        )
        public ApiResponse<AppointmentResponse> nurseSaveExaminationProgress(
                        @PathVariable String appointmentId,
                        @RequestBody CompleteExaminationRequest request) {
                Long userId = getCurrentUserId();
                return ApiResponse.<AppointmentResponse>builder()
                                .success(true)
                                .message("Đã lưu thông tin nhập hộ.")
                                .data(appointmentService.saveNurseExaminationProgress(userId, appointmentId, request))
                                .build();
        }
}
