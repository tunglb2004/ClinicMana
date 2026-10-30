package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Mock Insurance Services - Dữ liệu dịch vụ bảo hiểm giả lập
 * Mô phỏng danh mục dịch vụ y tế và mức giá BHYT
 */
@Entity
@Table(name = "mock_insurance_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockInsuranceService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Mã dịch vụ
     */
    @Column(name = "service_code", length = 20, unique = true)
    private String serviceCode;

    /**
     * Tên dịch vụ
     */
    @Column(name = "service_name", length = 255)
    private String serviceName;

    /**
     * Giá BHYT
     */
    @Column(name = "insurance_price", precision = 15, scale = 2)
    private BigDecimal insurancePrice;

    /**
     * Tỷ lệ chi trả BHYT (% - mặc định 100)
     */
    @Column(name = "coverage_ratio")
    private Integer coverageRatio = 100;
}
