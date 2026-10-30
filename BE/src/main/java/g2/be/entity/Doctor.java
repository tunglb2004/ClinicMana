package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @Column(name = "doctor_id", length = 20)
    private String doctorId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    // Loại bác sĩ: Bác sĩ lâm sàng (CLINICAL) hoặc Bác sĩ chuyên khoa (SPECIALIST)
    @Enumerated(EnumType.STRING)
    @Column(name = "doctor_type", length = 30)
    private DoctorType doctorType;

    @Column(length = 100)
    private String specialization;

    @Column(length = 100)
    private String degree;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Builder.Default
    @Column(name = "is_head_of_dept")
    private Boolean isHeadOfDept = false;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 100)
    private String position;

    @Column(name = "certification_issue_date")
    private java.time.LocalDate certificationIssueDate;

    @Column(name = "certification_place", length = 150)
    private String certificationPlace;

    @Column(name = "certification_file_url", length = 255)
    private String certificationFileUrl;

    @Column(name = "training_place", length = 150)
    private String trainingPlace;

    @Column(name = "graduation_year", length = 4)
    private String graduationYear;

    @Column(name = "degree_file_url", length = 255)
    private String degreeFileUrl;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "previous_workplace", length = 255)
    private String previousWorkplace;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;
}
