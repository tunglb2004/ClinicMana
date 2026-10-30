package g2.be.controller;

import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.CombinedXmlResponse;
import g2.be.service.BhytSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller - Kết thúc khám: Gửi toàn bộ hồ sơ của lần khám sang BHXH
 * 
 * Bác sĩ nhấn "Kết thúc khám" → gửi tất cả hóa đơn + dịch vụ của appointmentId sang Mock API BHXH
 */
@RestController
@RequestMapping("/api/xml-mapper")
@RequiredArgsConstructor
@Tag(name = "XML Mapper", description = "Gửi dữ liệu khám bệnh sang API BHXH")
public class Xml1MapperController {

    private final BhytSyncService bhytSyncService;

    /**
     * Test mapper - xem dữ liệu XML1 + XML4 trước khi gửi
     */
    @GetMapping("/test/{appointmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Xem trước dữ liệu XML1 + XML4",
        description = "Nhập Appointment ID → nhận JSON XML1 + XML4 tổng hợp từ tất cả hóa đơn",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<CombinedXmlResponse> testMapper(@PathVariable String appointmentId) {
        CombinedXmlResponse response = bhytSyncService.buildCombinedXml(appointmentId);
        return ApiResponse.<CombinedXmlResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    /**
     * Kết thúc khám - Gửi toàn bộ hồ sơ sang BHXH (background)
     */
    @PostMapping("/send-to-bhxh/{appointmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_SPECIALISTDOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SYSTEM_ADMIN')")
    @Operation(
        summary = "Kết thúc khám - Gửi hồ sơ sang API BHXH",
        description = "Tổng hợp tất cả hóa đơn + dịch vụ của lần khám → Xử lý nền sang Mock API BHXH",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<Map<String, String>> sendToBhxh(@PathVariable String appointmentId) {
        bhytSyncService.queueAppointmentSyncAsync(appointmentId);
        return ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Yêu cầu đồng bộ BHYT đã được ghi nhận và đang xử lý nền.")
                .data(Map.of(
                        "appointmentId", appointmentId,
                        "status", "QUEUED"))
                .build();
    }
}
