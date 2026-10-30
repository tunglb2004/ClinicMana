package g2.be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String invoiceId;
    private String paymentStatus; // PAID, PENDING, FAILED
    private BigDecimal amount; // Số tiền bệnh nhân cần trả (customerPaid)
    private BigDecimal totalServiceFee; // Tổng giá dịch vụ (giá phòng khám)
    private BigDecimal insurancePaid; // Tiền BHYT trả
    private BigDecimal customerPaid; // Tiền bệnh nhân trả
    private String paymentUrl; // URL QR code or checkout page (for Seepay/VNPay integration)
    private String message;

    /**
     * Chi tiết BHYT - thông tin công dân + breakdown từng dịch vụ
     * Null nếu bệnh nhân không có BHYT
     */
    private InsuranceBreakdownResponse insuranceBreakdown;
}
