package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @Column(name = "patient_id", length = 20)
    private String patientId;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "blood_type", length = 5)
    private String bloodType;

    @Column(name = "health_insurance_number", length = 50)
    private String healthInsuranceNumber;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(precision = 5, scale = 2)
    private BigDecimal height;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "background_disease", columnDefinition = "TEXT")
    private String backgroundDisease;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 15)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;
}
