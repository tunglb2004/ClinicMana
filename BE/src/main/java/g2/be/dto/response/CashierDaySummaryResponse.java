package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashierDaySummaryResponse {
    private LocalDate businessDate;
    private BigDecimal totalCollected;
    private BigDecimal cashCollected;
    private BigDecimal transferCollected;
    private BigDecimal otherCollected;
    private BigDecimal pendingHandovers;
    private BigDecimal confirmedHandovers;
    private BigDecimal cashRemaining;
    private long invoiceCount;
    private long cashInvoiceCount;
    private long transferInvoiceCount;
    private long otherInvoiceCount;
    private List<PaymentMethodBreakdownResponse> methodBreakdown;
    private List<FinancialResponse> recentInvoices;
}
