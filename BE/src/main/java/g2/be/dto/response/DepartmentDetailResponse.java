package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO response chi tiết của 1 khoa.
 * Bao gồm danh sách bác sĩ và y tá thuộc khoa.
 * Dùng khi xem chi tiết khoa (GET /detail/{deptId}).
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDetailResponse {

    // ID khoa
    private Integer deptId;

    // Tên khoa
    private String deptName;

    // Loại khoa
    private String deptType;

    // Mô tả khoa
    private String description;

    private Boolean isActive;

    // Danh sách bác sĩ thuộc khoa
    private List<MemberResponse> doctors;

    // Danh sách y tá thuộc khoa
    private List<MemberResponse> nurses;
}
