package g2.be.entity;

import g2.be.util.ClinicTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_handovers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "handover_id")
    private Long handoverId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "from_cashier_user_id", nullable = false)
    private Long fromCashierUserId;

    @Column(name = "to_cashier_user_id", nullable = false)
    private Long toCashierUserId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
    }
}
