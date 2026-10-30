package g2.be.controller;

import g2.be.dto.response.CitizenInsuranceInfoResponse;
import g2.be.dto.response.InsuranceBreakdownResponse;
import g2.be.service.CitizenInsuranceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller tra cứu thông tin công dân và thẻ BHYT
 * Mock API mô phỏng hệ thống tra cứu BHXH
 */
@RestController
@RequestMapping("/api/citizen-insurance")
@RequiredArgsConstructor
@Tag(name = "Citizen Insurance", description = "API tra cứu thông tin công dân và thẻ BHYT")
public class CitizenInsuranceController {

    private final CitizenInsuranceService citizenInsuranceService;

    /**
     * Tra cứu thông tin công dân theo số CCCD
     */
    @GetMapping("/lookup/cccd/{cccdNumber}")
    @Operation(summary = "Tra cứu theo CCCD", description = "Tra cứu thông tin công dân và thẻ BHYT bằng số CCCD")
    public ResponseEntity<?> lookupByCccd(@PathVariable String cccdNumber) {
        try {
            CitizenInsuranceInfoResponse response = citizenInsuranceService.getCitizenInfoByCccd(cccdNumber);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                new ErrorResponse(e.getMessage())
            );
        }
    }

    /**
     * Tra cứu thông tin công dân theo mã thẻ BHYT
     */
    @GetMapping("/lookup/insurance/{insuranceCode}")
    @Operation(summary = "Tra cứu theo mã thẻ BHYT", description = "Tra cứu thông tin công dân bằng mã thẻ BHYT")
    public ResponseEntity<?> lookupByInsuranceCode(@PathVariable String insuranceCode) {
        try {
            CitizenInsuranceInfoResponse response = citizenInsuranceService.getCitizenInfoByInsuranceCode(insuranceCode);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                new ErrorResponse(e.getMessage())
            );
        }
    }

    /**
     * Tra cứu CCCD + Tính toán BHYT chi tiết cho lịch hẹn
     * 
     * Trả về chi tiết BHYT cho từng dịch vụ trong appointment:
     * - Giá_Cơ_Sở = min(Giá_Phòng_Khám, Giá_Trần_BHYT)
     * - Tiền_BHYT_Trả = Giá_Cơ_Sở × Mức_Hưởng_Thẻ × Tỷ_Lệ_Dịch_Vụ
     * - Tiền_Bệnh_Nhân_Trả = Giá_Phòng_Khám − Tiền_BHYT_Trả
     */
    @GetMapping("/lookup/cccd/{cccdNumber}/appointment/{appointmentId}")
    @Operation(
        summary = "Tra cứu CCCD + Tính BHYT cho lịch hẹn",
        description = "Tra cứu thông tin BHYT theo CCCD và tính toán chi tiết BHYT cho từng dịch vụ trong lịch hẹn. " +
                      "Trả về tổng tiền BHYT trả, tổng tiền bệnh nhân trả, và chi tiết từng dịch vụ."
    )
    public ResponseEntity<InsuranceBreakdownResponse> lookupAndCalculateInsurance(
            @PathVariable String cccdNumber,
            @PathVariable String appointmentId) {
        InsuranceBreakdownResponse response = citizenInsuranceService
            .calculateInsuranceForAppointment(cccdNumber, appointmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Tra cứu CCCD + Tính toán BHYT cho các dịch vụ SUPPLEMENT đang chờ thanh toán của hồ sơ khám.
     * Dùng cho thu ngân tra cứu BHYT trước khi thu tiền bổ sung.
     */
    @GetMapping("/lookup/cccd/{cccdNumber}/record/{recordId}")
    @Operation(
        summary = "Tra cứu CCCD + Tính BHYT cho hồ sơ khám",
        description = "Tra cứu thông tin BHYT theo CCCD và tính toán chi tiết BHYT cho các dịch vụ SUPPLEMENT " +
                      "đang chờ thanh toán của một hồ sơ. " +
                      "Trả về tổng tiền BHYT trả, tổng tiền bệnh nhân trả, và chi tiết từng dịch vụ."
    )
    public ResponseEntity<InsuranceBreakdownResponse> lookupAndCalculateInsuranceForRecord(
            @PathVariable String cccdNumber,
            @PathVariable String recordId) {
        InsuranceBreakdownResponse response = citizenInsuranceService
            .calculateInsuranceForRecord(cccdNumber, recordId);
        return ResponseEntity.ok(response);
    }

    /**
     * Error Response DTO
     */
    private record ErrorResponse(String error) {}
}
