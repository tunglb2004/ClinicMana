package g2.be.controller;

import g2.be.dto.request.StatusUpdateRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.FollowUpOptionResponse;
import g2.be.dto.response.MedicalRecordDetailResponse;
import g2.be.dto.response.MedicalRecordResponse;
import g2.be.service.MedicalRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import g2.be.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/medical-examination/records")
@RequiredArgsConstructor
@Tag(name = "Medical Record Management", description = "APIs for managing patient medical records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping("/my-history")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    @Operation(summary = "Customer views own medical visit history (sorted by date, oldest first)")
    public ApiResponse<Page<MedicalRecordResponse>> getMyHistory(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success("Tải lịch sử khám thành công.", medicalRecordService.getMyHistory(user.getId(), keyword, pageable));
    }

    @GetMapping("/follow-up-options/my")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    @Operation(summary = "Customer views own records eligible for follow-up booking")
    public ApiResponse<List<FollowUpOptionResponse>> getMyFollowUpOptions(
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResponse.success("Tai danh sach ho so tai kham thanh cong.", medicalRecordService.getMyFollowUpOptions(user.getId()));
    }

    @GetMapping("/follow-up-options")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST', 'ROLE_SYSTEM_ADMIN', 'ROLE_CASHIER')")
    @Operation(summary = "Staff views records eligible for follow-up booking")
    public ApiResponse<List<FollowUpOptionResponse>> getFollowUpOptions(@RequestParam String patientId) {
        return ApiResponse.success("Tai danh sach ho so tai kham thanh cong.", medicalRecordService.getFollowUpOptions(patientId));
    }

    @GetMapping("/doctor-history")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SPECIALISTDOCTOR')")
    @Operation(summary = "Doctor views own completed examination records")
    public ApiResponse<Page<MedicalRecordResponse>> getDoctorHistory(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success("Tai danh sach ho so da kham thanh cong.",
                medicalRecordService.getDoctorExaminedRecords(user.getId(), keyword, fromDate, toDate, pageable));
    }

    @GetMapping("/my-detail/{id}")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    @Operation(summary = "Customer views own medical record detail")
    public ApiResponse<MedicalRecordDetailResponse> getMyDetail(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResponse.success("Tải chi tiết hồ sơ thành công.", medicalRecordService.getMyDetail(user.getId(), id));
    }

    @GetMapping(value = "/my/{id}/export", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    @Operation(summary = "Customer exports own medical record PDF")
    public ResponseEntity<byte[]> exportMyPdf(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl user) {
        byte[] pdfBytes = medicalRecordService.exportMyPdf(user.getId(), id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("Medical_Record_" + id + ".pdf")
                .build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/get-patient-history/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE', 'ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SPECIALISTDOCTOR')")
    @Operation(summary = "Get patient medical history (paginated)")
    public ApiResponse<Page<MedicalRecordResponse>> getPatientHistory(
            @PathVariable String patientId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success("Tải lịch sử khám thành công.", medicalRecordService.getPatientHistory(patientId, keyword, pageable));
    }

    @GetMapping("/view-detail/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE', 'ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SPECIALISTDOCTOR')")
    @Operation(summary = "View medical record details")
    public ApiResponse<MedicalRecordDetailResponse> viewDetail(@PathVariable String id) {
        return ApiResponse.success("Tải chi tiết hồ sơ thành công.", medicalRecordService.viewDetail(id));
    }

    @PatchMapping("/update-examination-status/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE', 'ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SPECIALISTDOCTOR')")
    @Operation(summary = "Update medical record status")
    public ApiResponse<MedicalRecordDetailResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ApiResponse.success("Cập nhật trạng thái thành công.", medicalRecordService.updateExaminationStatus(id, request.getStatus()));
    }

    @PatchMapping("/confirm-final-result/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SPECIALISTDOCTOR')")
    @Operation(summary = "Confirm final examination result")
    public ApiResponse<MedicalRecordDetailResponse> confirmResult(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl user) { 
        return ApiResponse.success("Xác nhận kết quả khám thành công.", medicalRecordService.confirmFinalResult(id, user.getId()));
    }
}
