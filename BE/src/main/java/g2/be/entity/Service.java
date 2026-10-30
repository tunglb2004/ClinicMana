package g2.be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @Column(name = "service_id", length = 20)
    private String serviceId;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @JsonIgnore
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * Mã dịch vụ BHYT - mapping với mock_service_catalog.service_code
     * Nếu null → dịch vụ ngoài danh mục BHYT
     */
    @Column(name = "bhyt_service_code", length = 50)
    private String bhytServiceCode;

    /**
     * Khoa phụ trách dịch vụ
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department department;

    @Builder.Default
    @Column(name = "max_patients", nullable = false)
    private Integer maxPatients = 8;

    @Transient
    @Builder.Default
    private Boolean hasOperationalRoom = true;

    @JsonIgnore
    @ManyToMany(mappedBy = "services")
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();
}

