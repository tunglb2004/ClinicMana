package g2.be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_id")
    private Integer deptId;
    
    @Column(name = "dept_name", nullable = false, unique = true, length = 100)
    private String deptName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "dept_type", nullable = false)
    private DeptType deptType;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @JsonIgnore
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    @JsonIgnore
    @OneToMany(mappedBy = "department")
    @Builder.Default
    private List<Service> services = new ArrayList<>();
    
    public enum DeptType {
        CLINICAL, PARACLINICAL, ADMINISTRATIVE, PHARMACY
    }
}
