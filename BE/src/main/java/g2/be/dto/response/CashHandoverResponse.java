package g2.be.dto.response;

import g2.be.entity.CashHandover;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashHandoverResponse {
    private Long handoverId;
    private LocalDate businessDate;
    private Long fromCashierUserId;
    private String fromCashierName;
    private Long toCashierUserId;
    private String toCashierName;
    private BigDecimal amount;
    private String status;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private boolean canConfirm;
    private boolean canCancel;

    public static CashHandoverResponse fromEntity(
            CashHandover handover,
            String fromCashierName,
            String toCashierName,
            Long currentUserId
    ) {
        return CashHandoverResponse.builder()
                .handoverId(handover.getHandoverId())
                .businessDate(handover.getBusinessDate())
                .fromCashierUserId(handover.getFromCashierUserId())
                .fromCashierName(fromCashierName)
                .toCashierUserId(handover.getToCashierUserId())
                .toCashierName(toCashierName)
                .amount(handover.getAmount())
                .status(handover.getStatus())
                .note(handover.getNote())
                .createdAt(handover.getCreatedAt())
                .confirmedAt(handover.getConfirmedAt())
                .cancelledAt(handover.getCancelledAt())
                .canConfirm("PENDING".equals(handover.getStatus())
                        && currentUserId != null
                        && currentUserId.equals(handover.getToCashierUserId()))
                .canCancel("PENDING".equals(handover.getStatus())
                        && currentUserId != null
                        && currentUserId.equals(handover.getFromCashierUserId()))
                .build();
    }
}
