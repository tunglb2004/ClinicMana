package g2.be.controller;

import g2.be.dto.request.UpdatePatientRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.PatientResponse;
import g2.be.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Management", description = "APIs quản lý thông tin bệnh nhân")
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER')")
    @Operation(
        summary = "Danh sách bệnh nhân",
        description = "Lấy danh sách bệnh nhân có phân trang, tìm kiếm theo tên/số điện thoại/email, sắp xếp",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<PatientResponse>> getPatients(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "user.userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "false") boolean today) {
        // PageRequest is 0-indexed, so subtract 1 from page
        return ApiResponse.<PageResponse<PatientResponse>>builder()
                .success(true)
                .data(patientService.getPatients(page - 1, size, keyword, sortBy, sortDir, today))
                .build();
    }

    @PutMapping("/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER')")
    @Operation(
        summary = "Cập nhật bệnh nhân",
        description = "Cập nhật thông tin bệnh nhân (chỉ cập nhật field được gửi lên)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PatientResponse> updatePatient(
            @PathVariable String patientId,
            @RequestBody UpdatePatientRequest request) {
        return ApiResponse.<PatientResponse>builder()
                .success(true)
                .data(patientService.updatePatient(patientId, request))
                .build();
    }

    @DeleteMapping("/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER')")
    @Operation(
        summary = "Xóa bệnh nhân",
        description = "Xóa mềm thông tin bệnh nhân (soft delete) - không xóa vĩnh viễn khỏi database",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<String> deletePatient(@PathVariable String patientId) {
        patientService.deletePatient(patientId);
        return ApiResponse.<String>builder()
                .success(true)
                .data("Patient information has been canceled (soft deleted).")
                .build();
    }
}
