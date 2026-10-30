package g2.be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO để nhận dữ liệu khi Admin khóa/mở khóa tài khoản nhân viên.
 * Dùng trong API: PATCH /api/admin/users/{id}/status
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserStatusRequest {

    /**
     * Trạng thái tài khoản:
     * - true = Mở khóa (Active)
     * - false = Khóa (Inactive/Locked)
     */
    @NotNull(message = "Trạng thái isActive không được để trống")
    private Boolean isActive;
}
