package g2.be.dto.request;

import g2.be.validation.ValidationPatterns;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCatalogRequest {

    @NotBlank(message = "Ten dich vu khong duoc de trong")
    @Size(max = 200, message = "Ten dich vu khong duoc vuot qua 200 ky tu")
    private String serviceName;

    @NotNull(message = "Gia dich vu khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gia dich vu phai lon hon 0")
    private BigDecimal price;

    @Size(max = 1000, message = "Mo ta khong duoc vuot qua 1000 ky tu")
    private String description;

    @NotNull(message = "Trang thai hoat dong khong duoc de trong")
    private Boolean isActive;

    @Size(max = 50, message = "Ma BHYT khong duoc vuot qua 50 ky tu")
    @Pattern(regexp = "^$|" + ValidationPatterns.SERVICE_CODE, message = "Ma BHYT chi duoc gom chu, so, dau cham, gach duoi hoac gach ngang")
    private String bhytServiceCode;

    @NotNull(message = "Khoa phu trach khong duoc de trong")
    private Integer deptId;

    @NotNull(message = "Gioi han benh nhan khong duoc de trong")
    @Min(value = 1, message = "Gioi han benh nhan phai tu 1 den 10")
    @Max(value = 10, message = "Gioi han benh nhan phai tu 1 den 10")
    private Integer maxPatients;
}
