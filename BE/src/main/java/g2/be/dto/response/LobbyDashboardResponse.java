package g2.be.dto.response;

import lombok.*;

import java.util.List;

/**
 * Màn hình Tổng (Master Dashboard) - Sảnh chờ Lễ tân.
 * Hiển thị tổng quan toàn bộ phòng khám để phục vụ Load Balancing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LobbyDashboardResponse {

    private List<RoomOverview> rooms;
    private int totalWaiting;
    private int totalInProgress;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomOverview {
        private String roomId;
        private String roomName;
        private String roomType;
        private String location;
        private String departmentName;
        private String status;           // ACTIVE / INACTIVE
        private int currentQueueLength;
        private int baseAvgTime;         // phút / bệnh nhân
        private int estimatedWaitMinutes; // = baseAvgTime * currentQueueLength

        // Bác sĩ đang trực trong phòng
        private String doctorName;

        // Bệnh nhân đang khám (IN_PROGRESS) - nếu null = phòng rảnh
        private CurrentPatient currentPatient;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentPatient {
        private String appointmentId;
        private String patientName;
        private String patientId;
    }
}
