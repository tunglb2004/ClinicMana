package g2.be.controller;

import g2.be.dto.request.ServiceCatalogRequest;
import g2.be.dto.request.UpdateServiceStatusRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.entity.Department;
import g2.be.entity.Service;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.DepartmentRepository;
import g2.be.repository.ServiceRepository;
import g2.be.service.ServiceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Service Catalog", description = "APIs lấy danh mục dịch vụ khám")
public class ServiceController {

    private static final Set<String> ADMIN_SERVICE_AUTHORITIES = Set.of(
            "ROLE_SYSTEM_ADMIN",
            "ROLE_ADMIN",
            "ROLE_CLINIC_MANAGER");

    private final ServiceRepository serviceRepository;
    private final DepartmentRepository departmentRepository;
    private final ServiceManagementService serviceManagementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'CASHIER', 'SYSTEM_ADMIN', 'CLINIC_MANAGER', 'MANAGER', 'DOCTOR', 'NURSE', 'PATIENT')")
    @Operation(
            summary = "Danh sách dịch vụ",
            description = "Lấy danh sách dịch vụ khám đang hoạt động; admin có thể yêu cầu xem thêm dịch vụ đang khóa",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<Service>> getAllServices(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer deptId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Authentication authentication) {
        String normalizedCategory = normalizeOptionalCategory(category);
        boolean canIncludeInactive = includeInactive && hasAdminServiceAccess(authentication);

        List<Service> services = serviceManagementService.getVisibleServices(canIncludeInactive).stream()
                .filter(service -> normalizedCategory == null
                        || (service.getDepartment() != null
                        && normalizedCategory.equals(service.getDepartment().getDeptType().name())))
                .filter(service -> deptId == null
                        || (service.getDepartment() != null
                        && deptId.equals(service.getDepartment().getDeptId())))
                .toList();

        serviceManagementService.populateOperationalRoomAvailability(services);

        return ApiResponse.<List<Service>>builder()
                .success(true)
                .data(services)
                .build();
    }

    @PutMapping("/{serviceId}/config")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_ADMIN')")
    @Operation(
            summary = "Cập nhật cấu hình dịch vụ",
            description = "Cập nhật phân loại dịch vụ và số bệnh nhân tối đa theo ca cho dịch vụ",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Service> updateServiceConfig(
            @PathVariable String serviceId,
            @RequestBody Map<String, Object> body) {
        Service service = serviceRepository.findByServiceIdAndIsDeletedFalse(serviceId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

        applyServiceConfig(service, body);
        serviceRepository.save(service);

        return ApiResponse.<Service>builder()
                .success(true)
                .data(service)
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    @Operation(summary = "Tạo mới dịch vụ", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Service> createService(@Valid @RequestBody ServiceCatalogRequest request) {
        return ApiResponse.<Service>builder()
                .success(true)
                .data(serviceManagementService.createService(request))
                .build();
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    @Operation(summary = "Cập nhật dịch vụ", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Service> updateService(
            @PathVariable String serviceId,
            @Valid @RequestBody ServiceCatalogRequest request) {
        return ApiResponse.<Service>builder()
                .success(true)
                .data(serviceManagementService.updateService(serviceId, request))
                .build();
    }

    @PatchMapping("/{serviceId}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    @Operation(summary = "Khóa/mở khóa dịch vụ", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Service> updateServiceStatus(
            @PathVariable String serviceId,
            @Valid @RequestBody UpdateServiceStatusRequest request) {
        return ApiResponse.<Service>builder()
                .success(true)
                .data(serviceManagementService.updateServiceStatus(serviceId, request.getIsActive()))
                .build();
    }

    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    @Operation(summary = "Xóa mềm dịch vụ", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> deleteService(@PathVariable String serviceId) {
        serviceManagementService.deleteService(serviceId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa mềm dịch vụ thành công")
                .build();
    }

    private void applyServiceConfig(Service service, Map<String, Object> body) {
        if (body.containsKey("deptId")) {
            Object deptIdObj = body.get("deptId");
            if (deptIdObj == null) {
                throw new RuntimeException("deptId là bắt buộc");
            } else {
                try {
                    int deptId = Integer.parseInt(String.valueOf(deptIdObj));
                    Department dept = departmentRepository.findByDeptIdAndIsDeletedFalse(deptId)
                            .orElseThrow(() -> new AppException(ErrorCode.DEPT_NOT_FOUND));
                    service.setDepartment(dept);
                } catch (NumberFormatException ex) {
                    throw new RuntimeException("deptId phải là số");
                }
            }
        }

        if (body.containsKey("maxPatients")) {
            Integer maxPatients = parseMaxPatients(body.get("maxPatients"));
            service.setMaxPatients(maxPatients);
        }
    }

    private Integer parseMaxPatients(Object value) {
        if (value == null) {
            throw new RuntimeException("maxPatients là bắt buộc khi gửi trường này");
        }

        int parsed;
        if (value instanceof Number number) {
            parsed = number.intValue();
        } else {
            try {
                parsed = Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ex) {
                throw new RuntimeException("maxPatients phải là số nguyên hợp lệ");
            }
        }

        if (parsed < 1 || parsed > 10) {
            throw new RuntimeException("maxPatients phải nằm trong khoảng 1-10");
        }

        return parsed;
    }

    private String normalizeOptionalCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return normalizeCategory(category);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new RuntimeException("serviceCategory phải là CLINICAL hoặc PARACLINICAL");
        }

        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (!"CLINICAL".equals(normalized) && !"PARACLINICAL".equals(normalized)) {
            throw new RuntimeException("serviceCategory phải là CLINICAL hoặc PARACLINICAL");
        }
        return normalized;
    }

    private boolean hasAdminServiceAccess(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(ADMIN_SERVICE_AUTHORITIES::contains);
    }
}
