package g2.be.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g2.be.dto.request.CreateWorkingTimeSlotRequest;
import g2.be.dto.request.UpdateWorkingTimeSlotRequest;
import g2.be.dto.response.WorkingTimeSlotResponse;
import g2.be.entity.WorkingTimeSlot;
import g2.be.repository.WorkingTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkingTimeSlotService {

    private final WorkingTimeSlotRepository workingTimeSlotRepository;

    /**
     * Lấy tất cả active time slots
     */
    @Transactional(readOnly = true)
    public List<WorkingTimeSlotResponse> getAllActiveSlots() {
        List<WorkingTimeSlot> timeSlots = workingTimeSlotRepository.findAllActiveSlots();
        
        return timeSlots.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo time slot mới
     */
    @Transactional
    public WorkingTimeSlotResponse createTimeSlot(CreateWorkingTimeSlotRequest request) {
        // Validate time logic
        if (request.getStartTime().isAfter(request.getEndTime()) || 
            request.getStartTime().equals(request.getEndTime())) {
            throw new RuntimeException("Start time must be before end time");
        }

        // Kiểm tra tên ca làm việc đã tồn tại chưa
        if (workingTimeSlotRepository.existsByShiftNameAndIsDeletedFalse(request.getShiftName())) {
            throw new RuntimeException("Shift name already exists: " + request.getShiftName());
        }

        // Check overlapping time slots
        boolean hasOverlap = workingTimeSlotRepository.existsOverlappingSlot(
                request.getStartTime(),
                request.getEndTime()
        );

        if (hasOverlap) {
            throw new RuntimeException("Time slot overlaps with existing slot");
        }

        // Create time slot
        WorkingTimeSlot timeSlot = WorkingTimeSlot.builder()
                .shiftName(request.getShiftName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
            .maxPatients(request.getMaxPatients())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isDeleted(false)
                .build();

        WorkingTimeSlot savedTimeSlot = workingTimeSlotRepository.save(timeSlot);
        log.info("Created time slot ID: {} - {}", savedTimeSlot.getSlotId(), savedTimeSlot.getShiftName());

        return convertToResponse(savedTimeSlot);
    }

    /**
     * Cập nhật time slot
     */
    @Transactional
    public WorkingTimeSlotResponse updateTimeSlot(Long slotId, UpdateWorkingTimeSlotRequest request) {
        WorkingTimeSlot timeSlot = workingTimeSlotRepository.findBySlotIdAndIsDeletedFalse(slotId)
                .orElseThrow(() -> new RuntimeException("Time slot not found with ID: " + slotId));

        // Validate time logic
        if (request.getStartTime().isAfter(request.getEndTime()) || 
            request.getStartTime().equals(request.getEndTime())) {
            throw new RuntimeException("Start time must be before end time");
        }

        // Kiểm tra tên ca làm việc trùng (trừ chính nó)
        if (workingTimeSlotRepository.existsByShiftNameAndSlotIdNot(request.getShiftName(), slotId)) {
            throw new RuntimeException("Shift name already exists: " + request.getShiftName());
        }

        // Check overlapping (exclude current slot)
        boolean hasOverlap = workingTimeSlotRepository.existsOverlappingSlotExcludingCurrent(
                slotId,
                request.getStartTime(),
                request.getEndTime()
        );

        if (hasOverlap) {
            throw new RuntimeException("Time slot overlaps with existing slot");
        }

        // Update fields
        timeSlot.setShiftName(request.getShiftName());
        timeSlot.setStartTime(request.getStartTime());
        timeSlot.setEndTime(request.getEndTime());
        timeSlot.setMaxPatients(request.getMaxPatients());
        timeSlot.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            timeSlot.setIsActive(request.getIsActive());
        }

        WorkingTimeSlot updatedTimeSlot = workingTimeSlotRepository.save(timeSlot);
        log.info("Updated time slot ID: {} - {}", slotId, updatedTimeSlot.getShiftName());

        return convertToResponse(updatedTimeSlot);
    }

    /**
     * Xóa time slot (soft delete)
     */
    @Transactional
    public void deleteTimeSlot(Long slotId) {
        WorkingTimeSlot timeSlot = workingTimeSlotRepository.findBySlotIdAndIsDeletedFalse(slotId)
                .orElseThrow(() -> new RuntimeException("Time slot not found with ID: " + slotId));

        timeSlot.setIsDeleted(true);
        workingTimeSlotRepository.save(timeSlot);
        log.info("Deleted time slot ID: {} - {}", slotId, timeSlot.getShiftName());
    }

    /**
     * Chuyển đổi entity sang response DTO
     */
    private WorkingTimeSlotResponse convertToResponse(WorkingTimeSlot timeSlot) {
        return WorkingTimeSlotResponse.builder()
                .slotId(timeSlot.getSlotId())
                .shiftName(timeSlot.getShiftName())
                .startTime(timeSlot.getStartTime())
                .endTime(timeSlot.getEndTime())
            .maxPatients(timeSlot.getMaxPatients())
                .description(timeSlot.getDescription())
                .isActive(timeSlot.getIsActive())
                .createdAt(timeSlot.getCreatedAt())
                .updatedAt(timeSlot.getUpdatedAt())
                .build();
    }
}
