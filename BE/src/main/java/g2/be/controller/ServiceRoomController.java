package g2.be.controller;

import g2.be.dto.request.ServiceRoomRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.ServiceRoomResponse;
import g2.be.service.ServiceRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/service-rooms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
@Tag(name = "Service-Room Mapping", description = "APIs quản lý mapping dịch vụ - phòng")
public class ServiceRoomController {

    private final ServiceRoomService serviceRoomService;

    @GetMapping("/list")
    @Operation(summary = "Danh sách mapping", description = "Lấy toàn bộ mapping dịch vụ - phòng")
    public ResponseEntity<List<ServiceRoomResponse>> getAllMappings() {
        return ResponseEntity.ok(serviceRoomService.getAllMappings());
    }

    @GetMapping("/by-room/{roomId}")
    @Operation(summary = "Dịch vụ theo phòng", description = "Lấy danh sách dịch vụ của 1 phòng")
    public ResponseEntity<ServiceRoomResponse> getServicesByRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(serviceRoomService.getServicesByRoom(roomId));
    }

    @GetMapping("/by-service/{serviceId}")
    @Operation(summary = "Phòng theo dịch vụ", description = "Lấy danh sách phòng thực hiện được 1 dịch vụ")
    public ResponseEntity<List<ServiceRoomResponse>> getRoomsByService(
            @PathVariable String serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long slotId) {
        return ResponseEntity.ok(serviceRoomService.getRoomsByService(serviceId, date, slotId));
    }

    @PutMapping("/assign/{roomId}")
    @Operation(summary = "Gán dịch vụ vào phòng", description = "Gán danh sách dịch vụ vào phòng (thay thế toàn bộ)")
    public ResponseEntity<ServiceRoomResponse> assignServicesToRoom(
            @PathVariable String roomId,
            @Valid @RequestBody ServiceRoomRequest request) {
        return ResponseEntity.ok(serviceRoomService.assignServicesToRoom(roomId, request));
    }

    @DeleteMapping("/{roomId}/service/{serviceId}")
    @Operation(summary = "Gỡ dịch vụ khỏi phòng", description = "Xóa 1 dịch vụ khỏi phòng")
    public ResponseEntity<MessageResponse> removeServiceFromRoom(
            @PathVariable String roomId,
            @PathVariable String serviceId) {
        return ResponseEntity.ok(serviceRoomService.removeServiceFromRoom(roomId, serviceId));
    }
}
