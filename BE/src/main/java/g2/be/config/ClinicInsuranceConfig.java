package g2.be.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình thông tin phòng khám để xử lý BHYT
 */
@Configuration
@ConfigurationProperties(prefix = "clinic.insurance")
@Data
public class ClinicInsuranceConfig {

    /**
     * Mã cơ sở khám chữa bệnh (5 ký tự)
     * VD: 01012 - Phòng khám tại Hà Nội
     */
    private String maCskcb;

    /**
     * Tên phòng khám
     */
    private String name;

    /**
     * Mã tỉnh (2 số đầu) - Dùng để xác định trái tuyến
     * VD: 01 - Hà Nội
     */
    private String provinceCode;
}
