package g2.be.dto.request;

import g2.be.entity.Department;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO request để tạo khoa mới.
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDepartmentRequest {

    // Tên khoa (bắt buộc, tối đa 100 ký tự, không được trùng)
    @NotBlank(message = "Tên khoa không được để trống")
    @Size(max = 100, message = "Tên khoa tối đa 100 ký tự")
    private String deptName;

    // Loại khoa: CLINICAL, PARACLINICAL, ADMINISTRATIVE, PHARMACY
    @NotNull(message = "Loại khoa không được để trống")
    private Department.DeptType deptType;

    // Mô tả khoa (không bắt buộc)
    private String description;
}
