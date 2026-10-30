package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response thông tin rút gọn của 1 khoa.
 * Dùng cho danh sách khoa (FE_17).
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponse {

    // ID khoa
    private Integer deptId;

    // Tên khoa
    private String deptName;

    // Loại khoa (CLINICAL, PARACLINICAL, ADMINISTRATIVE, PHARMACY)
    private String deptType;

    // Mô tả khoa
    private String description;

    private Boolean isActive;

    // Số lượng bác sĩ trong khoa
    private int doctorCount;

    // Số lượng y tá trong khoa
    private int nurseCount;
}
