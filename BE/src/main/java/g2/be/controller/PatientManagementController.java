package g2.be.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import g2.be.dto.request.CreatePatientRequest;
import g2.be.dto.request.UpdatePatientRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.UserDetailResponse;
import g2.be.service.PatientManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Management", description = "APIs quản lý bệnh nhân (chỉ các account có role PATIENT)")
@SecurityRequirement(name = "bearerAuth")
public class PatientManagementController {

    private final PatientManagementService patientManagementService;

    /**
     * Lấy danh sách tất cả bệnh nhân với phân trang, sắp xếp và tìm kiếm
     * Chỉ hiển thị các account có role PATIENT
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_CASHIER', 'ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Lấy danh sách bệnh nhân", description = "Lấy danh sách tất cả bệnh nhân (user có role PATIENT) với phân trang, sắp xếp và tìm kiếm")
    public ResponseEntity<PageResponse<UserDetailResponse>> getAllPatients(
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số lượng bản ghi mỗi trang") @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Trường sắp xếp (userId, username, email, fullName, createdAt)") @RequestParam(defaultValue = "userId") String sortBy,

            @Parameter(description = "Hướng sắp xếp (asc hoặc desc)") @RequestParam(defaultValue = "desc") String sortDir,

            @Parameter(description = "Từ khóa tìm kiếm (username, email, fullName, phoneNumber)") @RequestParam(required = false) String keyword,

            @Parameter(description = "Lọc theo trạng thái active (true/false)") @RequestParam(required = false) Boolean isActive) {
        PageResponse<UserDetailResponse> response = patientManagementService.getAllPatients(
                page, size, sortBy, sortDir, keyword, isActive);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thông tin chi tiết một bệnh nhân theo ID
     */
    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_CASHIER', 'ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Lấy thông tin bệnh nhân", description = "Lấy thông tin chi tiết của một bệnh nhân theo ID")
    public ResponseEntity<UserDetailResponse> getPatientById(
            @Parameter(description = "ID của bệnh nhân") @PathVariable Long patientId) {
        UserDetailResponse response = patientManagementService.getPatientById(patientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo mới bệnh nhân
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_CASHIER', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Tạo bệnh nhân mới", description = "Tạo tài khoản bệnh nhân mới (role PATIENT). RECEPTIONIST, CASHIER và SYSTEM_ADMIN có quyền.")
    public ResponseEntity<UserDetailResponse> createPatient(
            @Parameter(description = "Thông tin bệnh nhân mới") @Valid @RequestBody CreatePatientRequest request) {
        UserDetailResponse response = patientManagementService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cập nhật thông tin bệnh nhân
     */
    @PutMapping("/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_CASHIER', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Cập nhật thông tin bệnh nhân", description = "Cập nhật thông tin tài khoản bệnh nhân. RECEPTIONIST, CASHIER và SYSTEM_ADMIN có quyền.")
    public ResponseEntity<UserDetailResponse> updatePatient(
            @Parameter(description = "ID của bệnh nhân") @PathVariable Long patientId,

            @Parameter(description = "Thông tin cập nhật") @Valid @RequestBody UpdatePatientRequest request) {
        UserDetailResponse response = patientManagementService.updatePatient(patientId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa bệnh nhân (soft delete)
     */
    @DeleteMapping("/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER')")
    @Operation(summary = "Xóa bệnh nhân", description = "Xóa (soft delete) tài khoản bệnh nhân. Chỉ SYSTEM_ADMIN và CASHIER có quyền.")
    public ResponseEntity<MessageResponse> deletePatient(
            @Parameter(description = "ID của bệnh nhân cần xóa") @PathVariable Long patientId) {
        MessageResponse response = patientManagementService.deletePatient(patientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Khóa tài khoản bệnh nhân
     */
    @PatchMapping("/{patientId}/lock")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER')")
    @Operation(summary = "Khóa tài khoản bệnh nhân", description = "Khóa tài khoản bệnh nhân (set isActive = false). Chỉ SYSTEM_ADMIN và CASHIER có quyền.")
    public ResponseEntity<MessageResponse> lockPatient(
            @Parameter(description = "ID của bệnh nhân cần khóa") @PathVariable Long patientId) {
        MessageResponse response = patientManagementService.lockPatient(patientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Mở khóa tài khoản bệnh nhân
     */
    @PatchMapping("/{patientId}/unlock")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER')")
    @Operation(summary = "Mở khóa tài khoản bệnh nhân", description = "Mở khóa tài khoản bệnh nhân (set isActive = true). Chỉ SYSTEM_ADMIN và CASHIER có quyền.")
    public ResponseEntity<MessageResponse> unlockPatient(
            @Parameter(description = "ID của bệnh nhân cần mở khóa") @PathVariable Long patientId) {
        MessageResponse response = patientManagementService.unlockPatient(patientId);
        return ResponseEntity.ok(response);
    }
}
