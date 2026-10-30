package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import g2.be.util.ClinicTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceOrder {

    @Id
    @Column(name = "order_id", length = 20)
    private String orderId;

    @ManyToOne
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "technician_id", length = 20)
    private String technicianId;

    @ManyToOne
    @JoinColumn(name = "assigned_room_id")
    private Room assignedRoom;

    @Column(name = "result_note", columnDefinition = "TEXT")
    private String resultNote;

    @Column(name = "result_file_url", columnDefinition = "TEXT")
    private String resultFileUrl;

    @Column(name = "indication_note", columnDefinition = "TEXT")
    private String indicationNote;

    @Builder.Default
    @Column(length = 20)
    private String status = "PENDING";

    @Builder.Default
    @Column(name = "order_type", length = 20)
    private String orderType = "INITIAL";

    @Column(name = "invoice_id", length = 20)
    private String invoiceId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
    }
}

