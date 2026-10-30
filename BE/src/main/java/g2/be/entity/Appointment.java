package g2.be.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import g2.be.util.ClinicTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @Column(name = "appointment_id", length = 20)
    private String appointmentId;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "paraclinical_doctor_id")
    private Doctor paraclinicalDoctor;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "paraclinical_room_id")
    private Room paraclinicalRoom;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "appointment_services", joinColumns = @JoinColumn(name = "appointment_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private java.util.List<Service> services = new java.util.ArrayList<>();

    @Column(name = "appointment_date")
    private LocalDate appointmentDate;

    @Column(name = "appointment_time")
    private LocalTime appointmentTime;

    @Builder.Default
    @Column(length = 20)
    private String status = "WAITING";

    @Builder.Default
    @Column(name = "visit_type", length = 20)
    private String visitType = "INITIAL";

    @ManyToOne
    @JoinColumn(name = "follow_up_source_record_id")
    private MedicalRecord followUpSourceRecord;

    @Column(name = "queue_number")
    private Integer queueNumber;

    /** Base priority: 0 = initial visit, 500 = returning with paraclinical results. */
    @Builder.Default
    @Column(name = "priority_score")
    private Integer priorityScore = 0;

    /** Waiting start time, set after payment/check-in. */
    @Column(name = "waiting_since")
    private LocalDateTime waitingSince;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    /** patientId of the user who booked this appointment (differs from patient when booked for a relative). */
    @Column(name = "booked_by_patient_id", length = 20)
    private String bookedByPatientId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
    }
}

