package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prescription_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "medicine_name", nullable = false, length = 255)
    private String medicineName;

    @Column(length = 255)
    private String dosage;

    @Column(length = 50)
    private String quantity;
}

