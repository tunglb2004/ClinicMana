package g2.be.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import g2.be.dto.request.CreateWorkingTimeSlotRequest;
import g2.be.dto.request.UpdateWorkingTimeSlotRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.WorkingTimeSlotResponse;
import g2.be.service.WorkingTimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/working-time-slots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Working Time Slot Management", description = "APIs for managing working time slot templates")
@SecurityRequirement(name = "bearerAuth")
public class WorkingTimeSlotController {

    private final WorkingTimeSlotService workingTimeSlotService;

    /**
     * Lấy tất cả active time slots
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLINIC_MANAGER', 'MANAGER', 'RECEPTIONIST', 'DOCTOR')")
    @Operation(
        summary = "Get all active time slots",
        description = "Retrieve all active working time slot templates"
    )
    public ResponseEntity<List<WorkingTimeSlotResponse>> getAllActiveSlots() {
        log.info("Getting all active time slots");
        
        List<WorkingTimeSlotResponse> response = workingTimeSlotService.getAllActiveSlots();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo time slot mới
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLINIC_MANAGER')")
    @Operation(
        summary = "Create new working time slot template",
        description = "Create a new working time slot template"
    )
    public ResponseEntity<WorkingTimeSlotResponse> createTimeSlot(
            @Valid @RequestBody CreateWorkingTimeSlotRequest request
    ) {
        log.info("Creating time slot: {}", request.getShiftName());
        
        WorkingTimeSlotResponse response = workingTimeSlotService.createTimeSlot(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cập nhật time slot
     */
    @PutMapping("/{slotId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLINIC_MANAGER')")
    @Operation(
        summary = "Update working time slot template",
        description = "Update an existing working time slot template"
    )
    public ResponseEntity<WorkingTimeSlotResponse> updateTimeSlot(
            @PathVariable Long slotId,
            @Valid @RequestBody UpdateWorkingTimeSlotRequest request
    ) {
        log.info("Updating time slot ID: {}", slotId);
        
        WorkingTimeSlotResponse response = workingTimeSlotService.updateTimeSlot(slotId, request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa time slot (soft delete)
     */
    @DeleteMapping("/{slotId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLINIC_MANAGER')")
    @Operation(
        summary = "Delete working time slot template",
        description = "Soft delete a working time slot template (mark as deleted)"
    )
    public ResponseEntity<ApiResponse<Void>> deleteTimeSlot(
            @PathVariable Long slotId
    ) {
        log.info("Deleting time slot ID: {}", slotId);
        
        workingTimeSlotService.deleteTimeSlot(slotId);
        
        return ResponseEntity.ok(
            ApiResponse.success("Working time slot deleted successfully")
        );
    }
}
