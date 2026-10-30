package g2.be.controller;

import g2.be.dto.request.DataCleanupExecuteRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.DataCleanupExecutionResponse;
import g2.be.dto.response.DataCleanupReport;
import g2.be.service.DataCleanupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/data-cleanup")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN')")
@Tag(name = "Data Cleanup", description = "APIs audit và cleanup consistency dữ liệu")
public class DataCleanupController {

    private final DataCleanupService dataCleanupService;

    @GetMapping("/audit")
    @Operation(
            summary = "Audit consistency dữ liệu",
            description = "Quét inconsistency giữa department, room, service_room, doctor, role, staff schedule và appointment assignment mà chưa ghi dữ liệu.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<DataCleanupReport> audit() {
        return ApiResponse.success("Audit completed", dataCleanupService.audit());
    }

    @PostMapping("/execute")
    @Operation(
            summary = "Execute cleanup dữ liệu",
            description = "Chuẩn hóa room_type, service_room, doctor_type, user roles, doctors.room_id, staff_schedules.room_id và appointment room/doctor assignment theo department là nguồn sự thật. Yêu cầu payload xác nhận để tránh ghi nhầm dữ liệu.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<DataCleanupExecutionResponse> execute(@org.springframework.web.bind.annotation.RequestBody(required = false) DataCleanupExecuteRequest request) {
        dataCleanupService.validateExecutionRequest(request);
        return ApiResponse.success("Cleanup executed", dataCleanupService.executeCleanup());
    }
}