package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO response chi tiết của 1 phòng.
 * Bao gồm danh sách bác sĩ trong phòng.
 * Dùng khi xem chi tiết phòng (GET /detail/{roomId}).
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDetailResponse {

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

    // Danh sách bác sĩ trong phòng
    private List<MemberResponse> doctors;

    private String status;

    private Integer baseAvgTime;

    private Integer currentQueueLength;
}
