package g2.be.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplementPaymentRequest {
    private String recordId;
    private String paymentMethod; // "CASH", "QR"

    /**
     * Số CCCD bệnh nhân (tùy chọn) - Cashier nhập tại quầy
     * Nếu có → tra BHYT và tính toán giảm trừ
     * Nếu không có → bệnh nhân trả full giá phòng khám
     */
    private String cccdNumber;
}
