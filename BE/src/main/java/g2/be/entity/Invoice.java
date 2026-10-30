package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import g2.be.util.ClinicTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @Column(name = "invoice_id", length = 20)
    private String invoiceId;

    @ManyToOne
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @Column(name = "cashier_id", length = 20)
    private String cashierId;

    @Column(name = "total_service_fee", precision = 15, scale = 2)
    private BigDecimal totalServiceFee;

    @Column(name = "insurance_paid", precision = 15, scale = 2)
    private BigDecimal insurancePaid;

    @Column(name = "customer_paid", precision = 15, scale = 2)
    private BigDecimal customerPaid;

    @Builder.Default
    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "UNPAID";

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "gateway_payload", columnDefinition = "TEXT")
    private String gatewayPayload;

    @Builder.Default
    @Column(name = "bhyt_sync_status", length = 20)
    private String bhytSyncStatus = "PENDING";

    @Column(name = "sync_error_message", length = 2000)
    private String syncErrorMessage;

    // ── Receipt / E-Invoice fields ──────────────────────────────────────────

    @Column(name = "receipt_number", unique = true, length = 30)
    private String receiptNumber;

    @Column(name = "e_invoice_code", length = 50)
    private String eInvoiceCode;

    @Column(name = "e_invoice_url", length = 500)
    private String eInvoiceUrl;

    @Column(name = "receipt_issued_at")
    private LocalDateTime receiptIssuedAt;

    @Builder.Default
    @Column(name = "receipt_status", length = 20)
    private String receiptStatus = "NONE";

    @Column(name = "receipt_processing_started_at")
    private LocalDateTime receiptProcessingStartedAt;

    @Column(name = "receipt_error_message", length = 2000)
    private String receiptErrorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
    }
}
