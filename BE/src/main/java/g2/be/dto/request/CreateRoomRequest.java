package g2.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO request để tạo phòng mới.
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    // Mã phòng (bắt buộc, tối đa 20 ký tự, ví dụ: "P-001")
    @NotBlank(message = "Mã phòng không được để trống")
    @Size(max = 20, message = "Mã phòng tối đa 20 ký tự")
    private String roomId;

    // Tên phòng (bắt buộc, tối đa 100 ký tự)
    @NotBlank(message = "Tên phòng không được để trống")
    @Size(max = 100, message = "Tên phòng tối đa 100 ký tự")
    private String roomName;

    // Loại phòng được backend tự suy ra từ khoa nếu FE không gửi lên
    @Size(max = 20, message = "Loại phòng tối đa 20 ký tự")
    private String roomType;

    // Vị trí phòng (ví dụ: "Tầng 1, Tòa A")
    @Size(max = 100, message = "Vị trí tối đa 100 ký tự")
    private String location;

    // ID của khoa mà phòng thuộc về (không bắt buộc)
    private Integer deptId;

    // Trạng thái phòng (mặc định ACTIVE)
    @Size(max = 20, message = "Trạng thái tối đa 20 ký tự")
    private String status;

    // Thời gian khám trung bình (phút, mặc định 15)
    private Integer baseAvgTime;
}
