package g2.be.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response chi tiết tính toán BHYT cho từng dịch vụ trong lịch hẹn
 * Công thức:
 * - Giá_Cơ_Sở = min(Giá_Phòng_Khám, Giá_Trần_BHYT)
 * - Tiền_BHYT_Trả = Giá_Cơ_Sở × Mức_Hưởng_Thẻ × Tỷ_Lệ_Dịch_Vụ
 * - Tiền_Bệnh_Nhân_Trả = Giá_Phòng_Khám − Tiền_BHYT_Trả
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceBreakdownResponse {

    /**
     * Thông tin công dân và thẻ BHYT
     */
    private CitizenInsuranceInfoResponse citizenInfo;

    /**
     * Chi tiết BHYT từng dịch vụ
     */
    private List<ServiceInsuranceDetail> serviceBreakdowns;

    /**
     * Tổng giá phòng khám (tất cả dịch vụ)
     */
    private BigDecimal totalClinicPrice;

    /**
     * Tổng tiền BHYT trả
     */
    private BigDecimal totalInsurancePaid;

    /**
     * Tổng tiền bệnh nhân trả
     */
    private BigDecimal totalPatientPaid;

    /**
     * Chi tiết BHYT cho 1 dịch vụ
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceInsuranceDetail {

        /**
         * Mã dịch vụ
         */
        private String serviceId;

        /**
         * Tên dịch vụ
         */
        private String serviceName;

        /**
         * Giá phòng khám
         */
        private BigDecimal clinicPrice;

        /**
         * Giá trần BHYT (null nếu ngoài danh mục BHYT)
         */
        private BigDecimal ceilingPrice;

        /**
         * Giá cơ sở = min(clinicPrice, ceilingPrice)
         */
        private BigDecimal basePrice;

        /**
         * Mức hưởng thẻ (%) - finalCoverageRate từ CitizenInsuranceService
         */
        private Integer cardCoverageRate;

        /**
         * Tỷ lệ thanh toán dịch vụ (%) - coverage_ratio từ mock_service_catalog
         */
        private Integer serviceCoverageRatio;

        /**
         * Tiền BHYT trả cho dịch vụ này
         */
        private BigDecimal insurancePaid;

        /**
         * Tiền bệnh nhân trả cho dịch vụ này
         */
        private BigDecimal patientPaid;

        /**
         * Dịch vụ có trong danh mục BHYT hay không
         */
        private Boolean isCoveredByInsurance;
    }
}
