package g2.be.dto.response;

import lombok.*;
import java.time.LocalDate;

/**
 * Response DTO cho thông tin công dân tra cứu từ Mock Master DB
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenInsuranceInfoResponse {

    /**
     * Số CCCD
     */
    private String cccdNumber;

    /**
     * Mã thẻ BHYT (15 ký tự)
     */
    private String insuranceCode;

    /**
     * Họ tên đầy đủ
     */
    private String fullName;

    /**
     * Ngày sinh (yyyyMMdd)
     */
    private String dateOfBirth;

    /**
     * Giới tính (1: Nam, 2: Nữ)
     */
    private Integer gender;

    /**
     * Tên giới tính (để hiển thị trên FE)
     */
    private String genderName;

    /**
     * Địa chỉ
     */
    private String address;

    /**
     * Mức hưởng BHYT (%)
     */
    private Integer coverageRate;

    /**
     * Nơi đăng ký ban đầu (Mã ĐKBĐ)
     */
    private String maDkbd;

    /**
     * Mức hưởng (1, 2, 3, 4)
     */
    private Integer benefitLevel;

    /**
     * Mã tỉnh (2 số đầu mã thẻ)
     */
    private String provinceCode;

    /**
     * Đúng tuyến hay không
     */
    private Boolean isDungTuyen;

    /**
     * Tỷ lệ tuyến (%)
     */
    private Integer tuyenRatio;

    /**
     * Tỷ lệ hưởng gốc từ mã thẻ (%)
     */
    private Integer originalCoverageRate;

    /**
     * Tỷ lệ hưởng cuối cùng = originalCoverageRate * tuyenRatio (%)
     */
    private Integer finalCoverageRate;

    /**
     * Ngày hết hạn thẻ
     */
    private LocalDate validTo;

    /**
     * Trạng thái thẻ (ACTIVE/EXPIRED)
     */
    private String status;

    /**
     * Thông báo cho FE
     */
    private String message;
}
