package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Mock Master Database - Dữ liệu công dân giả lập
 * Mô phỏng cơ sở dữ liệu BHXH để test luồng tra cứu thẻ BHYT
 */
@Entity
@Table(name = "mock_citizen_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockCitizenData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Số căn cước công dân (12 số)
     */
    @Column(name = "cccd_number", length = 12, nullable = false, unique = true)
    private String cccdNumber;

    /**
     * Mã thẻ BHYT (15 ký tự)
     */
    @Column(name = "insurance_code", length = 15, unique = true)
    private String insuranceCode;

    /**
     * Họ tên đầy đủ
     */
    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    /**
     * Ngày sinh (Format: yyyyMMdd theo chuẩn Bộ Y tế)
     */
    @Column(name = "date_of_birth", length = 8)
    private String dateOfBirth;

    /**
     * Giới tính (1: Nam, 2: Nữ)
     */
    @Column(name = "gender")
    private Integer gender;

    /**
     * Địa chỉ
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * Mức hưởng BHYT (%, ví dụ: 80, 95, 100)
     */
    @Column(name = "coverage_rate")
    private Integer coverageRate;

    /**
     * Nơi đăng ký ban đầu (Mã ĐKBĐ)
     */
    @Column(name = "ma_dkbd", length = 5)
    private String maDkbd;

    /**
     * Mức hưởng (1, 2, 3, 4)
     */
    @Column(name = "benefit_level")
    private Integer benefitLevel;

    /**
     * Ngày hết hạn thẻ BHYT
     */
    @Column(name = "valid_to")
    private LocalDate validTo;
}
