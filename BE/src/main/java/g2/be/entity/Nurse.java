package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import g2.be.util.ClinicTime;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nurses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nurse {

    @Id
    @Column(name = "nurse_id", length = 20)
    private String nurseId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "certification_number", length = 50)
    private String certificationNumber;

    @Column(name = "certification_issue_date")
    private LocalDate certificationIssueDate;

    @Column(name = "certification_place", length = 150)
    private String certificationPlace;

    @Column(name = "certification_file_url", length = 255)
    private String certificationFileUrl;

    @Column(name = "education_level", length = 50)
    private String educationLevel;

    @Column(name = "training_place", length = 150)
    private String trainingPlace;

    @Column(name = "graduation_year", length = 4)
    private String graduationYear;

    @Column(name = "degree_file_url", length = 255)
    private String degreeFileUrl;

    // Khoa mà y tá thuộc về (liên kết ManyToOne với bảng departments)
    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    @Column(length = 100)
    private String position;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "previous_workplaces", columnDefinition = "TEXT")
    private String previousWorkplaces;

    @Column(name = "support_skills", columnDefinition = "TEXT")
    private String supportSkills;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
        updatedAt = ClinicTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ClinicTime.now();
    }
}
