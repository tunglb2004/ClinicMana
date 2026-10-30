package g2.be.controller;

import g2.be.dto.request.PrescriptionRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.PrescriptionResponse;
import g2.be.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical-examination/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescription Management", description = "APIs for issuing and managing prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/issue")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE', 'ROLE_DOCTOR')")
    @Operation(summary = "Issue a new prescription")
    public ApiResponse<PrescriptionResponse> issue(@Valid @RequestBody PrescriptionRequest request) {
        return ApiResponse.success("Tạo đơn thuốc thành công.", prescriptionService.issue(request));
    }

    @PutMapping("/update-detail/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE', 'ROLE_DOCTOR')")
    @Operation(summary = "Update an existing prescription")
    public ApiResponse<PrescriptionResponse> updateDetail(
            @PathVariable String id,
            @Valid @RequestBody PrescriptionRequest request) {
        return ApiResponse.success("Cập nhật đơn thuốc thành công.", prescriptionService.updateDetail(id, request));
    }

    @GetMapping("/export-document/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE', 'ROLE_DOCTOR')")
    @Operation(summary = "Export prescription as HTML document")
    public ApiResponse<String> exportDocument(@PathVariable String id) {
        return ApiResponse.success("Xuất đơn thuốc thành công.", prescriptionService.generateExportDocument(id));
    }
}
