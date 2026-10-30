package g2.be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashHandoverRequest {
    private LocalDate businessDate;
    private Long toUserId;
    private BigDecimal amount;
    private String note;
}
