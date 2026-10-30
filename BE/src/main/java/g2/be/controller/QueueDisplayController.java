package g2.be.controller;

import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.LobbyDashboardResponse;
import g2.be.dto.response.RoomQueueBoardResponse;
import g2.be.service.QueueDisplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queue-display")
@RequiredArgsConstructor
@Tag(name = "Queue Display", description = "API cho màn hình hiển thị hàng đợi (public, không cần auth)")
public class QueueDisplayController {

    private final QueueDisplayService queueDisplayService;

    /**
     * Màn hình Tổng (Master Dashboard) — Sảnh chờ Lễ tân.
     * Trả về tổng quan tất cả phòng ACTIVE: queue length, thời gian chờ, bệnh nhân đang khám.
     * FE polling mỗi 10-15s.
     */
    @GetMapping("/lobby")
    @Operation(summary = "Master Dashboard - Tổng quan hàng đợi toàn phòng khám")
    public ApiResponse<LobbyDashboardResponse> getLobbyDashboard() {
        return ApiResponse.success("Lobby dashboard", queueDisplayService.getLobbyDashboard());
    }

    /**
     * Màn hình Cửa Phòng (Room Queue Board) — Đặt trước mỗi phòng khám.
     * Trả về hàng đợi ưu tiên của phòng: ai đang khám, danh sách chờ theo thứ tự priority.
     * FE polling mỗi 5-10s.
     */
    @GetMapping("/room/{roomId}")
    @Operation(summary = "Room Queue Board - Hàng đợi ưu tiên trước cửa phòng")
    public ApiResponse<RoomQueueBoardResponse> getRoomQueueBoard(@PathVariable String roomId) {
        return ApiResponse.success("Room queue board", queueDisplayService.getRoomQueueBoard(roomId));
    }
}
