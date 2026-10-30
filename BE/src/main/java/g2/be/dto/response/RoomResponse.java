package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response thông tin rút gọn của 1 phòng.
 * Dùng cho danh sách phòng.
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    // Mã phòng
    private String roomId;

    // Tên phòng
    private String roomName;

    // Loại phòng
    private String roomType;

    // Vị trí phòng
    private String location;

    // ID khoa mà phòng thuộc về
    private Integer deptId;

    // Tên khoa mà phòng thuộc về
    private String deptName;

    // Số lượng bác sĩ trong phòng
    private int doctorCount;

    private String status;

    private Integer baseAvgTime;

    private Integer currentQueueLength;
}
