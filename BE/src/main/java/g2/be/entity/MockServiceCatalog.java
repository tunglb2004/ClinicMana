package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Mock Service Catalog - Danh mục dịch vụ BHYT giả lập
 * Giá trần BHYT theo chuẩn Bộ Y tế
 */
@Entity
@Table(name = "mock_service_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockServiceCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Mã chuẩn Bộ Y tế (mapping với Service.serviceId)
     */
    @Column(name = "service_code", length = 50, nullable = false, unique = true)
    private String serviceCode;

    /**
     * Tên dịch vụ
     */
    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    /**
     * Giá trần BHYT - Cực kỳ quan trọng
     */
    @Column(name = "ceiling_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal ceilingPrice;

    /**
     * Tỷ lệ thanh toán (100% đối với khám thường, thuốc ung thư có thể 50%)
     */
    @Builder.Default
    @Column(name = "coverage_ratio")
    private Integer coverageRatio = 100;

    /**
     * Trạng thái hiệu lực
     */
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;
}
