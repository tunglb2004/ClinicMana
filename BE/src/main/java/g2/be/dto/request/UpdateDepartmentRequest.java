package g2.be.dto.request;

import g2.be.entity.Department;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO request để cập nhật thông tin khoa.
 * Tất cả field đều optional - chỉ cập nhật field được gửi lên.
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDepartmentRequest {

    // Tên khoa mới (tối đa 100 ký tự)
    @Size(max = 100, message = "Tên khoa tối đa 100 ký tự")
    private String deptName;

    // Loại khoa mới: CLINICAL, PARACLINICAL, ADMINISTRATIVE, PHARMACY
    private Department.DeptType deptType;

    // Mô tả khoa mới
    private String description;
}
