package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerCashierSummaryResponse {
    private Long cashierUserId;
    private String cashierName;
    private String cashierStaffId;
    private CashierDaySummaryResponse summary;
}
