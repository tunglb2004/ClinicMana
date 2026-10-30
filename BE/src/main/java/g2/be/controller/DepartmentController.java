package g2.be.controller;

import g2.be.dto.request.AssignMemberRequest;
import g2.be.dto.request.CreateDepartmentRequest;
import g2.be.dto.request.UpdateDepartmentRequest;
import g2.be.dto.request.UpdateDepartmentStatusRequest;
import g2.be.dto.response.DepartmentDetailResponse;
import g2.be.dto.response.DepartmentResponse;
import g2.be.dto.response.MessageResponse;
import g2.be.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/departments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
@Tag(name = "Department Management", description = "APIs quan ly co cau khoa phong")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping("/list")
    @Operation(summary = "Danh sach khoa")
    public ResponseEntity<Map<String, Object>> getAllDepartments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(departmentService.getAllDepartments(keyword, page, size, includeInactive));
    }

    @GetMapping("/detail/{deptId}")
    @Operation(summary = "Chi tiet khoa")
    public ResponseEntity<DepartmentDetailResponse> getDepartmentDetail(@PathVariable Integer deptId) {
        return ResponseEntity.ok(departmentService.getDepartmentById(deptId));
    }

    @PostMapping("/create")
    @Operation(summary = "Tao khoa")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.createDepartment(request));
    }

    @PutMapping("/update/{deptId}")
    @Operation(summary = "Cap nhat khoa")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Integer deptId,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(deptId, request));
    }

    @PatchMapping("/{deptId}/status")
    @Operation(summary = "Khoa/mo khoa")
    public ResponseEntity<DepartmentResponse> updateDepartmentStatus(
            @PathVariable Integer deptId,
            @Valid @RequestBody UpdateDepartmentStatusRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartmentStatus(deptId, request.getIsActive()));
    }

    @DeleteMapping("/delete/{deptId}")
    @Operation(summary = "Xoa mem khoa")
    public ResponseEntity<MessageResponse> deleteDepartment(@PathVariable Integer deptId) {
        return ResponseEntity.ok(departmentService.deleteDepartment(deptId));
    }

    @PostMapping("/{deptId}/assign-member")
    @Operation(summary = "Gan nhan su vao khoa")
    public ResponseEntity<MessageResponse> assignMember(
            @PathVariable Integer deptId,
            @Valid @RequestBody AssignMemberRequest request) {
        return ResponseEntity.ok(departmentService.assignMember(deptId, request));
    }

    @DeleteMapping("/{deptId}/remove-member/{userId}")
    @Operation(summary = "Go nhan su khoi khoa")
    public ResponseEntity<MessageResponse> removeMember(
            @PathVariable Integer deptId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(departmentService.removeMember(deptId, userId));
    }
}
