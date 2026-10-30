package g2.be.controller;

import g2.be.dto.request.AssignMemberRequest;
import g2.be.dto.request.CreateRoomRequest;
import g2.be.dto.request.UpdateRoomRequest;
import g2.be.dto.request.UpdateRoomStatusRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.RoomDetailResponse;
import g2.be.dto.response.RoomResponse;
import g2.be.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
@Tag(name = "Room Management", description = "APIs quan ly phong kham")
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/list")
    @Operation(summary = "Danh sach phong")
    public ResponseEntity<Map<String, Object>> getAllRooms(
            @RequestParam(required = false) Integer deptId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(roomService.getAllRooms(deptId, keyword, page, size, includeInactive));
    }

    @GetMapping("/detail/{roomId}")
    @Operation(summary = "Chi tiet phong")
    public ResponseEntity<RoomDetailResponse> getRoomDetail(@PathVariable String roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @PostMapping("/create")
    @Operation(summary = "Tao phong")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(roomService.createRoom(request));
    }

    @PutMapping("/update/{roomId}")
    @Operation(summary = "Cap nhat phong")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable String roomId,
            @Valid @RequestBody UpdateRoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, request));
    }

    @PatchMapping("/{roomId}/status")
    @Operation(summary = "Khoa/mo khoa phong")
    public ResponseEntity<RoomResponse> updateRoomStatus(
            @PathVariable String roomId,
            @Valid @RequestBody UpdateRoomStatusRequest request) {
        return ResponseEntity.ok(roomService.updateRoomStatus(roomId, request.getStatus()));
    }

    @DeleteMapping("/delete/{roomId}")
    @Operation(summary = "Xoa mem phong")
    public ResponseEntity<MessageResponse> deleteRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(roomService.deleteRoom(roomId));
    }

    @PostMapping("/{roomId}/assign-member")
    @Operation(summary = "Gan bac si vao phong")
    public ResponseEntity<MessageResponse> assignMember(
            @PathVariable String roomId,
            @Valid @RequestBody AssignMemberRequest request) {
        return ResponseEntity.ok(roomService.assignMember(roomId, request));
    }

    @DeleteMapping("/{roomId}/remove-member/{userId}")
    @Operation(summary = "Go bac si khoi phong")
    public ResponseEntity<MessageResponse> removeMember(
            @PathVariable String roomId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(roomService.removeMember(roomId, userId));
    }
}
