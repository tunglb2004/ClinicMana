package g2.be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO request để gán nhân sự vào khoa hoặc phòng.
 * Dùng chung cho cả Department và Room.
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignMemberRequest {

    // ID của user cần gán (userId trong bảng users)
    @NotNull(message = "User ID không được để trống")
    private Long userId;
}
