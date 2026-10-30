package g2.be.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO request để cập nhật thông tin phòng.
 * Tất cả field đều optional - chỉ cập nhật field được gửi lên.
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomRequest {

    // Tên phòng mới
    @Size(max = 100, message = "Tên phòng tối đa 100 ký tự")
    private String roomName;

    // Loại phòng mới
    @Size(max = 50, message = "Loại phòng tối đa 50 ký tự")
    private String roomType;

    // Vị trí mới
    @Size(max = 100, message = "Vị trí tối đa 100 ký tự")
    private String location;

    // ID khoa mới mà phòng thuộc về
    private Integer deptId;

    // Trạng thái phòng
    @Size(max = 20, message = "Trạng thái tối đa 20 ký tự")
    private String status;

    // Thời gian khám trung bình (phút)
    private Integer baseAvgTime;
}
