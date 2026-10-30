package g2.be.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrepaymentRequest {
    private String appointmentId;
    // Amount is calculated on backend based on services
    private String paymentMethod; // "CASH", "QR", etc.

    /**
     * Số CCCD bệnh nhân (tùy chọn) - Cashier nhập tại quầy
     * Nếu có → tra BHYT và tính toán giảm trừ
     * Nếu không có → bệnh nhân trả full giá phòng khám
     */
    private String cccdNumber;
}
