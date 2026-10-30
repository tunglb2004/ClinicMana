package g2.be.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Màn hình Cửa Phòng (Room Queue Board) - Đặt trước mỗi phòng khám.
 * Hiển thị hàng đợi ưu tiên + bệnh nhân đang khám.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomQueueBoardResponse {

    // Thông tin phòng
    private String roomId;
    private String roomName;
    private String roomType;
    private String location;
    private String departmentName;
    private String doctorName;

    // Bệnh nhân đang khám (IN_PROGRESS)
    private QueueEntry currentPatient;

    // Danh sách chờ (WAITING) — đã sắp theo priority score giảm dần
    private List<QueueEntry> waitingList;

    // Thống kê
    private int totalWaiting;
    private int estimatedWaitMinutes; // baseAvgTime * totalWaiting

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueueEntry {
        private String appointmentId;
        private String patientName;
        private String patientId;
        private int queuePosition;        // Vị trí trong hàng đợi (1-based)
        private Integer priorityScore;
        private LocalDateTime waitingSince;
        private int estimatedWaitMinutes;  // Ước tính chờ bao lâu nữa
        private String status;             // WAITING / IN_PROGRESS
    }
}
