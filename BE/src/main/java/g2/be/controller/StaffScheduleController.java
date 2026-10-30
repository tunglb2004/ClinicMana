package g2.be.controller;

import g2.be.dto.request.AssignNurseToDoctorRequest;
import g2.be.dto.request.CreateStaffScheduleRequest;
import g2.be.dto.request.UpdateStaffScheduleRequest;
import g2.be.dto.response.*;
import g2.be.service.StaffScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý API quản lý lịch làm việc nhân viên.
 * 
 * Base path: /api/manager/staff-schedules
 * Role: SYSTEM_ADMIN, CLINIC_MANAGER
 * 
 * @author Nam
 */
@RestController
@RequestMapping("/api/manager/staff-schedules")
@RequiredArgsConstructor
@Tag(name = "Staff Schedule Management", description = "APIs quản lý lịch làm việc nhân viên")
@SecurityRequirement(name = "bearerAuth")
public class StaffScheduleController {

    private final StaffScheduleService staffScheduleService;

    // ==================== DANH SÁCH LỊCH LÀM VIỆC ====================

    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Danh sách lịch làm việc", description = "Lấy danh sách lịch làm việc nhân viên theo ngày, có thể filter theo khoa")
    public ResponseEntity<?> getStaffSchedules(
            @Parameter(description = "Ngày cần xem (YYYY-MM-DD), mặc định hôm nay") @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Filter theo khoa") @RequestParam(name = "deptId", required = false) Integer deptId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        try {
            Map<String, Object> result = staffScheduleService.getStaffSchedules(date, deptId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "RUNTIME_ERROR",
                    "message", e.getMessage()));
        }
    }

    // ==================== CHI TIẾT LỊCH LÀM VIỆC ====================

    @GetMapping("/detail/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Chi tiết lịch làm việc nhân viên", description = "Lấy chi tiết lịch làm việc của 1 nhân viên trong khoảng thời gian")
    public ResponseEntity<?> getStaffScheduleDetail(
            @PathVariable("userId") Long userId,
            @Parameter(description = "Từ ngày (YYYY-MM-DD)") @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Đến ngày (YYYY-MM-DD)") @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            StaffScheduleDetailResponse result = staffScheduleService.getStaffScheduleDetail(userId, from, to);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "RUNTIME_ERROR",
                    "message", e.getMessage()));
        }
    }

    // ==================== GÁN NHÂN SỰ VÀO CA ====================

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Gán nhân viên vào ca làm việc", description = "Gán bác sĩ hoặc y tá vào 1 ca làm việc theo ngày cụ thể")
    public ResponseEntity<?> createStaffSchedule(
            @Valid @RequestBody CreateStaffScheduleRequest request) {
        try {
            StaffScheduleResponse result = staffScheduleService.createStaffSchedule(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "RUNTIME_ERROR",
                    "message", e.getMessage()));
        }
    }

    // ==================== CẬP NHẬT LỊCH LÀM VIỆC ====================

    @PutMapping("/update/{scheduleId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Cập nhật lịch làm việc", description = "Cập nhật ca hoặc ngày làm việc của nhân viên")
    public ResponseEntity<?> updateStaffSchedule(
            @PathVariable("scheduleId") Long scheduleId,
            @Valid @RequestBody UpdateStaffScheduleRequest request) {
        try {
            StaffScheduleResponse result = staffScheduleService.updateStaffSchedule(scheduleId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "RUNTIME_ERROR",
                    "message", e.getMessage()));
        }
    }

    // ==================== XÓA LỊCH LÀM VIỆC ====================

    @DeleteMapping("/delete/{scheduleId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Xóa lịch làm việc", description = "Xóa 1 lịch làm việc của nhân viên")
    public ResponseEntity<?> deleteStaffSchedule(
            @PathVariable("scheduleId") Long scheduleId) {
        try {
            MessageResponse result = staffScheduleService.deleteStaffSchedule(scheduleId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "RUNTIME_ERROR",
                    "message", e.getMessage()));
        }
    }

    // ==================== GÁN Y TÁ THEO DÕI BÁC SĨ ====================

    @PostMapping("/assign-nurse-to-doctor")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Gán y tá theo dõi bác sĩ", description = "Gán 1 y tá để theo dõi 1 bác sĩ")
    public ResponseEntity<?> assignNurseToDoctor(
            @Valid @RequestBody AssignNurseToDoctorRequest request) {
        try {
            NurseDoctorAssignmentResponse result = staffScheduleService.assignNurseToDoctor(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "RUNTIME_ERROR",
                    "message", e.getMessage()));
        }
    }

    // ==================== GỠ Y TÁ KHỎI BÁC SĨ ====================

    @DeleteMapping("/remove-nurse/{nurseUserId}/from-doctor/{doctorUserId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Gỡ y tá khỏi bác sĩ", description = "Gỡ y tá không còn theo dõi bác sĩ nữa")
    public ResponseEntity<?> removeNurseFromDoctor(
            @PathVariable("nurseUserId") Long nurseUserId,
            @PathVariable("doctorUserId") Long doctorUserId) {
        try {
            MessageResponse result = staffScheduleService.removeNurseFromDoctor(nurseUserId, doctorUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "RUNTIME_ERROR",
                    "message", e.getMessage()));
        }
    }

    // ==================== XEM Y TÁ ĐANG THEO DÕI BÁC SĨ ====================

    @GetMapping("/nurses-following/{doctorUserId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Danh sách y tá theo dõi bác sĩ", description = "Lấy danh sách y tá đang theo dõi 1 bác sĩ")
    public ResponseEntity<?> getNursesFollowingDoctor(
            @PathVariable("doctorUserId") Long doctorUserId) {
        try {
            List<NurseDoctorAssignmentResponse> result = staffScheduleService.getNursesFollowingDoctor(doctorUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "RUNTIME_ERROR",
                    "message", e.getMessage()));
        }
    }
}
