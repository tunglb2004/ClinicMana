package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import g2.be.util.ClinicTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    @Column(name = "record_id", length = 20)
    private String recordId;

    @OneToOne
    @JoinColumn(name = "appointment_id", unique = true)
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "doctor_notes", columnDefinition = "TEXT")
    private String doctorNotes;

    @Builder.Default
    @Column(length = 20)
    private String status = "EXAMINING";

    @Builder.Default
    @Column(name = "visit_type", length = 20)
    private String visitType = "INITIAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_record_id")
    private MedicalRecord parentRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_record_id")
    private MedicalRecord rootRecord;

    @OneToMany(mappedBy = "medicalRecord", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Invoice> invoices = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
    }
}

