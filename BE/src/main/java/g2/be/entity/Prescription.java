package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import g2.be.util.ClinicTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @Column(name = "prescription_id", length = 20)
    private String prescriptionId;

    @ManyToOne
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrescriptionDetail> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
    }
}

