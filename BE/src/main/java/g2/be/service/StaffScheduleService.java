package g2.be.service;

import g2.be.dto.request.AssignNurseToDoctorRequest;
import g2.be.dto.request.CreateStaffScheduleRequest;
import g2.be.dto.request.UpdateStaffScheduleRequest;
import g2.be.dto.response.*;
import g2.be.entity.*;
import g2.be.repository.*;
import g2.be.util.ClinicTime;
import g2.be.util.DoctorDepartmentClassifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xá»­ lÃ½ logic nghiá»‡p vá»¥ cho quáº£n lÃ½ lá»‹ch lÃ m viá»‡c nhÃ¢n viÃªn.
 * Bao gá»“m: Xem danh sÃ¡ch, chi tiáº¿t, gÃ¡n ca, sá»­a, xÃ³a, gÃ¡n y tÃ¡ theo bÃ¡c sÄ©.
 * 
 * Quy táº¯c nghiá»‡p vá»¥:
 * - Má»—i nhÃ¢n viÃªn chá»‰ Ä‘Æ°á»£c gÃ¡n 1 láº§n vÃ o 1 ca trong 1 ngÃ y
 * - Chá»‰ Doctor vÃ  Nurse má»›i Ä‘Æ°á»£c gÃ¡n ca
 * - Y tÃ¡ cÃ³ thá»ƒ theo dÃµi nhiá»u bÃ¡c sÄ©
 * 
 * @author Nam
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class StaffScheduleService {

    private final StaffScheduleRepository staffScheduleRepository;
    private final NurseDoctorAssignmentRepository nurseDoctorAssignmentRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final NurseRepository nurseRepository;
    private final WorkingTimeSlotRepository workingTimeSlotRepository;
    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final OnDutyRoomAssignmentResolver onDutyRoomAssignmentResolver;

    // ==================== DANH SÃCH Lá»ŠCH LÃ€M VIá»†C ====================

    /**
     * Láº¥y danh sÃ¡ch lá»‹ch lÃ m viá»‡c theo ngÃ y, cÃ³ thá»ƒ filter theo khoa.
     * 
     * @param date   - NgÃ y cáº§n xem (máº·c Ä‘á»‹nh hÃ´m nay)
     * @param deptId - Filter theo khoa (null = táº¥t cáº£)
     * @param page   - Sá»‘ trang
     * @param size   - Sá»‘ lÆ°á»£ng má»—i trang
     * @return Map chá»©a danh sÃ¡ch lá»‹ch + phÃ¢n trang
     */
    public Map<String, Object> getStaffSchedules(LocalDate date, Integer deptId, int page, int size) {
        LocalDate queryDate = (date != null) ? date : ClinicTime.today();
        Pageable pageable = PageRequest.of(page, size, Sort.by("slot.startTime").ascending());

        Page<StaffSchedule> schedulePage;

        if (deptId != null) {
            schedulePage = staffScheduleRepository.findByWorkDateAndDeptId(queryDate, deptId, pageable);
        } else {
            schedulePage = staffScheduleRepository.findByWorkDate(queryDate, pageable);
        }

        List<StaffScheduleResponse> schedules = schedulePage.getContent().stream()
                .map(this::convertToScheduleResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("schedules", schedules);
        response.put("currentPage", schedulePage.getNumber());
        response.put("totalItems", schedulePage.getTotalElements());
        response.put("totalPages", schedulePage.getTotalPages());
        response.put("date", queryDate);

        return response;
    }

    // ==================== CHI TIáº¾T Lá»ŠCH LÃ€M VIá»†C NHÃ‚N VIÃŠN ====================

    /**
     * Láº¥y chi tiáº¿t lá»‹ch lÃ m viá»‡c cá»§a 1 nhÃ¢n viÃªn trong khoáº£ng thá»i gian.
     * 
     * @param userId   - ID nhÃ¢n viÃªn
     * @param fromDate - Tá»« ngÃ y
     * @param toDate   - Äáº¿n ngÃ y
     * @return StaffScheduleDetailResponse
     */
    public StaffScheduleDetailResponse getStaffScheduleDetail(Long userId, LocalDate fromDate, LocalDate toDate) {
        // Kiá»ƒm tra user tá»“n táº¡i
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + userId));

        // XÃ¡c Ä‘á»‹nh role vÃ  khoa
        String role;
        String deptName = null;
        var doctorOpt = doctorRepository.findByUser_UserId(userId);
        var nurseOpt = nurseRepository.findByUser_UserId(userId);

        if (doctorOpt.isPresent()) {
            role = "DOCTOR";
            if (doctorOpt.get().getDepartment() != null) {
                deptName = doctorOpt.get().getDepartment().getDeptName();
            }
        } else if (nurseOpt.isPresent()) {
            role = "NURSE";
            if (nurseOpt.get().getDepartment() != null) {
                deptName = nurseOpt.get().getDepartment().getDeptName();
            }
        } else {
            throw new RuntimeException("User không phải bác sĩ hoặc y tá");
        }

        // Láº¥y lá»‹ch trong khoáº£ng ngÃ y
        LocalDate from = (fromDate != null) ? fromDate : ClinicTime.today();
        LocalDate to = (toDate != null) ? toDate : from.plusDays(7);

        List<StaffScheduleResponse> schedules = staffScheduleRepository
                .findByUser_UserIdAndWorkDateBetween(userId, from, to).stream()
                .map(this::convertToScheduleResponse)
                .collect(Collectors.toList());

        return StaffScheduleDetailResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .staffId(user.getStaffId())
                .role(role)
                .deptName(deptName)
                .schedules(schedules)
                .build();
    }

    // ==================== GÃN NHÃ‚N Sá»° VÃ€O CA ====================

    /**
     * GÃ¡n nhÃ¢n viÃªn vÃ o ca lÃ m viá»‡c theo ngÃ y.
     * 
     * @param request - userId, slotId, workDate, note
     * @return StaffScheduleResponse
     */
    public StaffScheduleResponse createStaffSchedule(CreateStaffScheduleRequest request) {
        // Kiá»ƒm tra user tá»“n táº¡i
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + request.getUserId()));

        // Kiá»ƒm tra user pháº£i lÃ  Doctor hoáº·c Nurse
        Optional<Doctor> doctorOpt = doctorRepository.findByUser_UserId(request.getUserId());
        Optional<Nurse> nurseOpt = nurseRepository.findByUser_UserId(request.getUserId());
        boolean isDoctor = doctorOpt.isPresent();
        boolean isNurse = nurseOpt.isPresent();
        if (!isDoctor && !isNurse) {
            throw new RuntimeException("Chỉ có thể gán lịch cho bác sĩ hoặc y tá.");
        }

        // Kiá»ƒm tra ca lÃ m viá»‡c tá»“n táº¡i
        WorkingTimeSlot slot = workingTimeSlotRepository.findBySlotIdAndIsDeletedFalse(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc với ID: " + request.getSlotId()));

        // Kiá»ƒm tra trÃ¹ng lá»‹ch (cÃ¹ng user + cÃ¹ng ca + cÃ¹ng ngÃ y)
        if (staffScheduleRepository.existsByUser_UserIdAndSlot_SlotIdAndWorkDate(
                request.getUserId(), request.getSlotId(), request.getWorkDate())) {
            throw new RuntimeException("Nhân viên đã được gán vào ca '" + slot.getShiftName()
                    + "' ngày " + request.getWorkDate());
        }

        // TÃ¬m phÃ²ng náº¿u cÃ³ roomId
        Room room = null;
        if (request.getRoomId() != null && !request.getRoomId().isBlank()) {
            room = roomRepository.findByRoomIdAndIsDeletedFalse(request.getRoomId())
                    .filter(this::isOperationalRoom)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng đang hoạt động với mã: "
                            + request.getRoomId()));
        }

        Integer legacyDeptId = resolveDeptIdForLegacySchema(doctorOpt, nurseOpt, room);
        if (legacyDeptId == null) {
            throw new RuntimeException("Không tìm thấy khoa để gán lịch cho nhân viên. Vui lòng gán khoa trước.");
        }

        if (room != null && doctorOpt.isPresent()) {
            validateDoctorRoomCompatibility(doctorOpt.get(), room);
        }
        if (room != null && nurseOpt.isPresent()) {
            validateNurseRoomCompatibility(nurseOpt.get(), room);
        }

        // Kiá»ƒm tra phÃ²ng Ä‘Ã£ cÃ³ bÃ¡c sÄ©/y tÃ¡ khÃ¡c trong cÃ¹ng ca + ngÃ y chÆ°a
        if (room != null) {
            if (isDoctor) {
                long doctorCount = staffScheduleRepository.countDoctorsInRoomSlotDate(
                        room.getRoomId(), request.getSlotId(), request.getWorkDate());
                if (doctorCount >= 1) {
                    throw new RuntimeException("Phòng '" + room.getRoomName() + "' đã có bác sĩ được gán trong ca này. Mỗi phòng chỉ được gán tối đa 1 bác sĩ/ca.");
                }
            }
            if (isNurse) {
                long nurseCount = staffScheduleRepository.countNursesInRoomSlotDate(
                        room.getRoomId(), request.getSlotId(), request.getWorkDate());
                if (nurseCount >= 1) {
                    throw new RuntimeException("Phòng '" + room.getRoomName() + "' đã có y tá được gán trong ca này. Mỗi phòng chỉ được gán tối đa 1 y tá/ca.");
                }
            }
        }

        // Táº¡o lá»‹ch
        StaffSchedule schedule = StaffSchedule.builder()
                .user(user)
                .slot(slot)
                .workDate(request.getWorkDate())
                .legacyDeptId(legacyDeptId)
                .room(room)
                .note(request.getNote())
                .build();

        StaffSchedule saved = staffScheduleRepository.save(schedule);
        return convertToScheduleResponse(saved);
    }

    // ==================== Cáº¬P NHáº¬T Lá»ŠCH LÃ€M VIá»†C ====================

    /**
     * Cáº­p nháº­t lá»‹ch lÃ m viá»‡c nhÃ¢n viÃªn.
     * 
     * @param scheduleId - ID lá»‹ch cáº§n cáº­p nháº­t
     * @param request    - slotId, workDate, note
     * @return StaffScheduleResponse
     */
    @Transactional
    public StaffScheduleResponse updateStaffSchedule(Long scheduleId, UpdateStaffScheduleRequest request) {
        StaffSchedule schedule = staffScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc với ID: " + scheduleId));

        User targetUser = schedule.getUser();
        if (request.getUserId() != null && !request.getUserId().equals(schedule.getUser().getUserId())) {
            targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + request.getUserId()));
        }

        Optional<Doctor> targetDoctorOpt = doctorRepository.findByUser_UserId(targetUser.getUserId());
        Optional<Nurse> targetNurseOpt = nurseRepository.findByUser_UserId(targetUser.getUserId());
        boolean targetIsDoctor = targetDoctorOpt.isPresent();
        boolean targetIsNurse = targetNurseOpt.isPresent();
        if (!targetIsDoctor && !targetIsNurse) {
            throw new RuntimeException("Chỉ có thể gán lịch cho bác sĩ hoặc y tá.");
        }

        WorkingTimeSlot targetSlot = schedule.getSlot();
        if (request.getSlotId() != null) {
            targetSlot = workingTimeSlotRepository.findBySlotIdAndIsDeletedFalse(request.getSlotId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc với ID: " + request.getSlotId()));
        }

        LocalDate targetWorkDate = request.getWorkDate() != null ? request.getWorkDate() : schedule.getWorkDate();

        if (staffScheduleRepository.existsByUser_UserIdAndSlot_SlotIdAndWorkDateAndScheduleIdNot(
                targetUser.getUserId(), targetSlot.getSlotId(), targetWorkDate, scheduleId)) {
            throw new RuntimeException("Nhân viên đã được gán vào ca này trong ngày này.");
        }

        Room targetRoom = schedule.getRoom();
        if (request.getRoomId() != null) {
            if (request.getRoomId().isBlank()) {
                targetRoom = null;
            } else {
                targetRoom = roomRepository.findByRoomIdAndIsDeletedFalse(request.getRoomId())
                        .filter(this::isOperationalRoom)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng đang hoạt động với mã: "
                                + request.getRoomId()));
            }
        }

        if (targetRoom != null && targetDoctorOpt.isPresent()) {
            validateDoctorRoomCompatibility(targetDoctorOpt.get(), targetRoom);
        }
        if (targetRoom != null && targetNurseOpt.isPresent()) {
            validateNurseRoomCompatibility(targetNurseOpt.get(), targetRoom);
        }

        if (targetRoom != null) {
            boolean sameRoomSlotDate = isSameRoomSlotDate(schedule, targetRoom, targetSlot.getSlotId(), targetWorkDate);
            boolean currentIsDoctor = doctorRepository.findByUser_UserId(schedule.getUser().getUserId()).isPresent();
            boolean currentIsNurse = nurseRepository.findByUser_UserId(schedule.getUser().getUserId()).isPresent();

            if (targetIsDoctor) {
                long doctorCount = staffScheduleRepository.countDoctorsInRoomSlotDate(
                        targetRoom.getRoomId(), targetSlot.getSlotId(), targetWorkDate);
                long effective = sameRoomSlotDate && currentIsDoctor ? doctorCount - 1 : doctorCount;
                if (effective >= 1) {
                    throw new RuntimeException("Phòng '" + targetRoom.getRoomName()
                            + "' đã có bác sĩ được gán trong ca này. Mỗi phòng chỉ được gán tối đa 1 bác sĩ/ca.");
                }
            }

            if (targetIsNurse) {
                long nurseCount = staffScheduleRepository.countNursesInRoomSlotDate(
                        targetRoom.getRoomId(), targetSlot.getSlotId(), targetWorkDate);
                long effective = sameRoomSlotDate && currentIsNurse ? nurseCount - 1 : nurseCount;
                if (effective >= 1) {
                    throw new RuntimeException("Phòng '" + targetRoom.getRoomName()
                            + "' đã có y tá được gán trong ca này. Mỗi phòng chỉ được gán tối đa 1 y tá/ca.");
                }
            }
        }

        schedule.setUser(targetUser);
        schedule.setSlot(targetSlot);
        schedule.setWorkDate(targetWorkDate);
        schedule.setRoom(targetRoom);
        if (request.getNote() != null) {
            schedule.setNote(request.getNote());
        }

        if (schedule.getLegacyDeptId() == null || request.getUserId() != null || request.getRoomId() != null) {
            Integer legacyDeptId = resolveDeptIdForLegacySchema(targetDoctorOpt, targetNurseOpt, targetRoom);
            if (legacyDeptId == null) {
                throw new RuntimeException("Không tìm thấy khoa để cập nhật lịch cho nhân viên.");
            }
            schedule.setLegacyDeptId(legacyDeptId);
        }

        StaffSchedule saved = staffScheduleRepository.save(schedule);
        return convertToScheduleResponse(saved);
    }

    // ==================== XÃ“A Lá»ŠCH LÃ€M VIá»†C ====================

    /**
     * XÃ³a lá»‹ch lÃ m viá»‡c (hard delete).
     * 
     * @param scheduleId - ID lá»‹ch cáº§n xÃ³a
     * @return MessageResponse
     */
    @Transactional
    public MessageResponse deleteStaffSchedule(Long scheduleId) {
        StaffSchedule schedule = staffScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc với ID: " + scheduleId));

        String info = schedule.getUser().getFullName() + " - " + schedule.getSlot().getShiftName()
                + " ngày " + schedule.getWorkDate();

        List<StaffSchedule> duplicateAssignments = staffScheduleRepository.findByUser_UserIdAndWorkDateAndSlot_SlotId(
                schedule.getUser().getUserId(), schedule.getWorkDate(), schedule.getSlot().getSlotId());
        if (duplicateAssignments.isEmpty()) {
            staffScheduleRepository.delete(schedule);
        } else {
            staffScheduleRepository.deleteAll(duplicateAssignments);
        }
        return new MessageResponse("Đã xóa lịch làm việc: " + info);
    }

    // ==================== GÃN Y TÃ THEO DÃ•I BÃC SÄ¨ ====================

    /**
     * GÃ¡n y tÃ¡ theo dÃµi bÃ¡c sÄ©.
     * 
     * @param request - nurseUserId, doctorUserId, effectiveDate
     * @return NurseDoctorAssignmentResponse
     */
    public NurseDoctorAssignmentResponse assignNurseToDoctor(AssignNurseToDoctorRequest request) {
        // TÃ¬m y tÃ¡
        Nurse nurse = nurseRepository.findByUser_UserId(request.getNurseUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy y tá với userId: " + request.getNurseUserId()));

        // TÃ¬m bÃ¡c sÄ©
        Doctor doctor = doctorRepository.findByUser_UserId(request.getDoctorUserId())
                .orElseThrow(
                        () -> new RuntimeException("Không tìm thấy bác sĩ với userId: " + request.getDoctorUserId()));

        // Kiá»ƒm tra trÃ¹ng (y tÃ¡ Ä‘Ã£ theo dÃµi bÃ¡c sÄ© nÃ y chÆ°a)
        if (nurseDoctorAssignmentRepository.existsByNurse_NurseIdAndDoctor_DoctorIdAndIsActiveTrue(
                nurse.getNurseId(), doctor.getDoctorId())) {
            throw new RuntimeException("Y tá '" + nurse.getUser().getFullName()
                    + "' đang theo dõi bác sĩ '" + doctor.getUser().getFullName() + "'");
        }

        // Táº¡o assignment
        NurseDoctorAssignment assignment = NurseDoctorAssignment.builder()
                .nurse(nurse)
                .doctor(doctor)
            .effectiveDate(request.getEffectiveDate() != null ? request.getEffectiveDate() : ClinicTime.today())
                .endDate(request.getEndDate())
                .isActive(true)
                .build();

        NurseDoctorAssignment saved = nurseDoctorAssignmentRepository.save(assignment);
        return convertToNurseDoctorResponse(saved);
    }

    /**
     * Gá»¡ y tÃ¡ khá»i bÃ¡c sÄ© (deactivate, khÃ´ng xÃ³a).
     */
    public MessageResponse removeNurseFromDoctor(Long nurseUserId, Long doctorUserId) {
        Nurse nurse = nurseRepository.findByUser_UserId(nurseUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy y tá với userId: " + nurseUserId));

        Doctor doctor = doctorRepository.findByUser_UserId(doctorUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ với userId: " + doctorUserId));

        NurseDoctorAssignment assignment = nurseDoctorAssignmentRepository
                .findByNurse_NurseIdAndDoctor_DoctorIdAndIsActiveTrue(nurse.getNurseId(), doctor.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Y tá này không đang theo dõi bác sĩ này"));

        assignment.setIsActive(false);
        assignment.setEndDate(ClinicTime.today());
        nurseDoctorAssignmentRepository.save(assignment);

        return new MessageResponse("Đã gỡ y tá '" + nurse.getUser().getFullName()
                + "' khỏi bác sĩ '" + doctor.getUser().getFullName() + "'");
    }

    /**
     * Láº¥y danh sÃ¡ch y tÃ¡ Ä‘ang theo dÃµi 1 bÃ¡c sÄ©.
     */
    public List<NurseDoctorAssignmentResponse> getNursesFollowingDoctor(Long doctorUserId) {
        Doctor doctor = doctorRepository.findByUser_UserId(doctorUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ với userId: " + doctorUserId));

        return nurseDoctorAssignmentRepository.findByDoctor_DoctorIdAndIsActiveTrue(doctor.getDoctorId())
                .stream()
                .map(this::convertToNurseDoctorResponse)
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================

    private boolean isSameRoomSlotDate(StaffSchedule schedule, Room room, Long slotId, LocalDate workDate) {
        return schedule.getRoom() != null
                && room != null
                && schedule.getRoom().getRoomId().equals(room.getRoomId())
                && schedule.getSlot() != null
                && Objects.equals(schedule.getSlot().getSlotId(), slotId)
                && Objects.equals(schedule.getWorkDate(), workDate);
    }

    private Integer resolveDeptIdForLegacySchema(Optional<Doctor> doctorOpt, Optional<Nurse> nurseOpt, Room room) {
        Integer deptId = doctorOpt
                .map(Doctor::getDepartment)
                .map(Department::getDeptId)
                .orElse(null);

        if (deptId != null) {
            return deptId;
        }

        deptId = nurseOpt
                .map(Nurse::getDepartment)
                .map(Department::getDeptId)
                .orElse(null);

        if (deptId != null) {
            return deptId;
        }

        deptId = Optional.ofNullable(room)
                .filter(this::isOperationalRoom)
                .map(Room::getDepartment)
                .map(Department::getDeptId)
                .orElse(null);

        if (deptId != null) {
            return deptId;
        }

        return departmentRepository.findAllByIsDeletedFalseAndIsActiveTrueOrderByDeptIdDesc()
                .stream()
                .findFirst()
                .map(Department::getDeptId)
                .orElse(null);
    }

    // ==================== PHÃ’NG LÃ‚M SÃ€NG + BÃC SÄ¨ TRá»°C ====================

    /**
     * Láº¥y danh sÃ¡ch phÃ²ng lÃ¢m sÃ ng cÃ³ bÃ¡c sÄ© trá»±c theo staff_schedules.
     * - CÃ³ slotId + date: láº¥y Ä‘Ãºng ca/ngÃ y Ä‘Ã³.
     * - KhÃ´ng cÃ³: láº¥y ca hiá»‡n táº¡i trong ngÃ y hiá»‡n táº¡i.
     */
    public List<Map<String, Object>> getClinicalRooms(Long slotId, LocalDate date) {
        return getClinicalRooms(slotId, date, null);
    }

    public List<Map<String, Object>> getClinicalRooms(Long slotId, LocalDate date, List<String> serviceIds) {
        List<Service> requestedServices = loadRequestedServices(serviceIds);
        if (serviceIds != null && !serviceIds.isEmpty() && requestedServices.isEmpty()) {
            return List.of();
        }

        Department.DeptType deptType = resolveRequestedDeptType(requestedServices);
        if (!requestedServices.isEmpty()) {
            List<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> assignments =
                    slotId != null && date != null
                            ? onDutyRoomAssignmentResolver.findAssignmentsForServices(
                                    requestedServices,
                                    date,
                                    slotId,
                                    true)
                            : onDutyRoomAssignmentResolver.findCurrentAssignmentsForServices(
                                    requestedServices,
                                    date,
                                    slotId,
                                    true);

            return buildRoomDoctorItems(assignments, deptType);
        }

        Long effectiveSlotId = slotId;
        LocalDate effectiveDate = date;
        if (effectiveSlotId == null || effectiveDate == null) {
            effectiveDate = ClinicTime.today();
            effectiveSlotId = onDutyRoomAssignmentResolver
                    .findSlotByAppointmentTime(ClinicTime.now().toLocalTime())
                    .map(WorkingTimeSlot::getSlotId)
                    .orElse(null);
        }
        if (effectiveSlotId == null) {
            return List.of();
        }
        return getClinicalRoomsBySchedule(effectiveSlotId, effectiveDate, deptType, requestedServices);
    }

    /**
     * Láº¥y phÃ²ng cÃ³ bÃ¡c sÄ© trá»±c theo ca + ngÃ y tá»« staff_schedules.room_id.
     */
    private List<Map<String, Object>> getClinicalRoomsBySchedule(
            Long slotId,
            LocalDate date,
            Department.DeptType deptType,
            List<Service> requestedServices) {
        List<StaffSchedule> schedules = staffScheduleRepository.findBySlot_SlotIdAndWorkDate(slotId, date);

        List<Map<String, Object>> result = new ArrayList<>();
        for (StaffSchedule schedule : schedules) {
            // Chá»‰ láº¥y báº£n ghi cÃ³ room_id
            if (schedule.getRoom() == null) continue;
            if (!isOperationalRoom(schedule.getRoom())) continue;
            if (!matchesRequestedDeptType(schedule.getRoom(), deptType)) continue;
            if (!roomSupportsRequestedServices(schedule.getRoom(), requestedServices)) continue;

            // Chá»‰ láº¥y bÃ¡c sÄ© lÃ¢m sÃ ng (khÃ´ng láº¥y y tÃ¡, khÃ´ng láº¥y bÃ¡c sÄ© CLS)
            var doctorOpt = doctorRepository.findByUser_UserId(schedule.getUser().getUserId());
            if (doctorOpt.isEmpty()) continue;

            Doctor doctor = doctorOpt.get();
            if (!matchesRequestedDeptType(doctor, deptType)) continue;

            Room room = schedule.getRoom();

            Map<String, Object> item = new HashMap<>();
            item.put("roomId", room.getRoomId());
            item.put("roomName", room.getRoomName());
            item.put("doctorId", doctor.getDoctorId());
            item.put("doctorName", schedule.getUser().getFullName());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildRoomDoctorItems(
            List<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> assignments,
            Department.DeptType deptType) {
        if (assignments == null || assignments.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seenRoomIds = new LinkedHashSet<>();
        for (OnDutyRoomAssignmentResolver.RoomDoctorAssignment assignment : assignments) {
            Room room = assignment.room();
            Doctor doctor = assignment.doctor();
            if (room == null || doctor == null || room.getRoomId() == null) {
                continue;
            }
            if (!matchesRequestedDeptType(room, deptType) || !matchesRequestedDeptType(doctor, deptType)) {
                continue;
            }
            if (!seenRoomIds.add(room.getRoomId())) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("roomId", room.getRoomId());
            item.put("roomName", room.getRoomName());
            item.put("doctorId", doctor.getDoctorId());
            item.put("doctorName", doctor.getUser() != null ? doctor.getUser().getFullName() : null);
            result.add(item);
        }
        return result;
    }

    private List<Service> loadRequestedServices(List<String> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return List.of();
        }

        List<String> normalizedServiceIds = serviceIds.stream()
                .filter(serviceId -> serviceId != null && !serviceId.isBlank())
                .distinct()
                .toList();

        if (normalizedServiceIds.isEmpty()) {
            return List.of();
        }

        return serviceRepository.findAllByServiceIdInAndIsDeletedFalseAndIsActiveTrue(normalizedServiceIds);
    }

    private Department.DeptType resolveRequestedDeptType(List<Service> services) {
        if (services == null || services.isEmpty()) {
            return Department.DeptType.CLINICAL;
        }

        boolean hasClinical = services.stream()
                .anyMatch(service -> service.getDepartment() != null
                        && service.getDepartment().getDeptType() == Department.DeptType.CLINICAL);
        if (hasClinical) {
            return Department.DeptType.CLINICAL;
        }

        boolean hasParaclinical = services.stream()
                .anyMatch(service -> service.getDepartment() != null
                        && service.getDepartment().getDeptType() == Department.DeptType.PARACLINICAL);
        if (hasParaclinical) {
            return Department.DeptType.PARACLINICAL;
        }

        return Department.DeptType.CLINICAL;
    }

    private boolean roomSupportsRequestedServices(Room room, List<Service> requestedServices) {
        if (room == null) {
            return false;
        }

        if (requestedServices == null || requestedServices.isEmpty()) {
            return true;
        }

        return requestedServices.stream().allMatch(service -> service.getRooms() != null
                && service.getRooms().stream().anyMatch(candidate -> room.getRoomId().equals(candidate.getRoomId())));
    }

    private boolean matchesRequestedDeptType(Room room, Department.DeptType deptType) {
        return room != null
                && room.getDepartment() != null
                && room.getDepartment().getDeptType() == deptType;
    }

    private boolean matchesRequestedDeptType(Doctor doctor, Department.DeptType deptType) {
        if (doctor == null) {
            return false;
        }
        return DoctorDepartmentClassifier.matchesDeptType(doctor, deptType);
    }

    private void validateDoctorRoomCompatibility(Doctor doctor, Room room) {
        if (!isOperationalRoom(room)) {
            throw new RuntimeException("Phòng đã bị khóa hoặc xóa mềm.");
        }
        if (doctor.getDepartment() == null) {
            throw new RuntimeException("Bác sĩ phải được gán khoa trước khi xếp lịch vào phòng.");
        }
        if (room.getDepartment() == null) {
            throw new RuntimeException("Phòng phải thuộc khoa hợp lệ trước khi xếp lịch.");
        }
        if (!doctor.getDepartment().getDeptId().equals(room.getDepartment().getDeptId())) {
            throw new RuntimeException("Bác sĩ chỉ được xếp vào phòng thuộc đúng khoa của mình.");
        }

        boolean generalRoom = isGeneralRoom(room);
        boolean paraclinicalDoctor = isParaclinicalDoctor(doctor);

        if (generalRoom && paraclinicalDoctor) {
            throw new RuntimeException("Phòng '" + room.getRoomName()
                    + "' là phòng GENERAL nên không thể gán bác sĩ cận lâm sàng.");
        }

        if (!generalRoom && !paraclinicalDoctor) {
            throw new RuntimeException("Phòng '" + room.getRoomName()
                    + "' không phải phòng GENERAL nên không thể gán bác sĩ lâm sàng.");
        }
    }

    private void validateNurseRoomCompatibility(Nurse nurse, Room room) {
        if (!isOperationalRoom(room)) {
            throw new RuntimeException("Phòng đã bị khóa hoặc xóa mềm.");
        }
        if (room.getDepartment() == null) {
            throw new RuntimeException("Phòng phải thuộc khoa hợp lệ trước khi xếp lịch.");
        }
    }

    private boolean isOperationalRoom(Room room) {
        return room != null
                && !Boolean.TRUE.equals(room.getIsDeleted())
                && "ACTIVE".equalsIgnoreCase(room.getStatus())
                && room.getDepartment() != null
                && !Boolean.TRUE.equals(room.getDepartment().getIsDeleted())
                && Boolean.TRUE.equals(room.getDepartment().getIsActive());
    }

    private boolean isParaclinicalDoctor(Doctor doctor) {
        return DoctorDepartmentClassifier.isParaclinical(doctor);
    }

    private boolean isGeneralRoom(Room room) {
        String roomType = Optional.ofNullable(room.getRoomType()).orElse("").toUpperCase(Locale.ROOT);
        String roomId = Optional.ofNullable(room.getRoomId()).orElse("").toUpperCase(Locale.ROOT);
        String roomName = Optional.ofNullable(room.getRoomName()).orElse("").toLowerCase(Locale.ROOT);

        if (roomType.contains("GENERAL")
                || roomId.startsWith("DK")
                || roomName.contains("đa khoa")
                || roomName.contains("general")) {
            return true;
        }

        if (room.getDepartment() != null && room.getDepartment().getDeptType() != null) {
            return room.getDepartment().getDeptType() == Department.DeptType.CLINICAL;
        }

        return false;
    }

    /**
     * Convert StaffSchedule entity â†’ StaffScheduleResponse DTO
     */
    private StaffScheduleResponse convertToScheduleResponse(StaffSchedule schedule) {
        // XÃ¡c Ä‘á»‹nh role + khoa
        String role = "UNKNOWN";
        String deptName = null;
        var doctorOpt = doctorRepository.findByUser_UserId(schedule.getUser().getUserId());
        if (doctorOpt.isPresent()) {
            role = "DOCTOR";
            if (doctorOpt.get().getDepartment() != null) {
                deptName = doctorOpt.get().getDepartment().getDeptName();
            }
        } else {
            var nurseOpt = nurseRepository.findByUser_UserId(schedule.getUser().getUserId());
            if (nurseOpt.isPresent()) {
                role = "NURSE";
                if (nurseOpt.get().getDepartment() != null) {
                    deptName = nurseOpt.get().getDepartment().getDeptName();
                }
            }
        }

        return StaffScheduleResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .userId(schedule.getUser().getUserId())
                .fullName(schedule.getUser().getFullName())
                .staffId(schedule.getUser().getStaffId())
                .role(role)
                .slotId(schedule.getSlot().getSlotId())
                .shiftName(schedule.getSlot().getShiftName())
                .startTime(schedule.getSlot().getStartTime())
                .endTime(schedule.getSlot().getEndTime())
                .workDate(schedule.getWorkDate())
                .roomId(schedule.getRoom() != null ? schedule.getRoom().getRoomId() : null)
                .roomName(schedule.getRoom() != null ? schedule.getRoom().getRoomName() : null)
                .note(schedule.getNote())
                .deptName(deptName)
                .build();
    }

    /**
     * Convert NurseDoctorAssignment entity â†’ NurseDoctorAssignmentResponse DTO
     */
    private NurseDoctorAssignmentResponse convertToNurseDoctorResponse(NurseDoctorAssignment assignment) {
        return NurseDoctorAssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .nurseUserId(assignment.getNurse().getUser().getUserId())
                .nurseFullName(assignment.getNurse().getUser().getFullName())
                .nurseStaffId(assignment.getNurse().getUser().getStaffId())
                .doctorUserId(assignment.getDoctor().getUser().getUserId())
                .doctorFullName(assignment.getDoctor().getUser().getFullName())
                .doctorStaffId(assignment.getDoctor().getUser().getStaffId())
                .doctorSpecialization(assignment.getDoctor().getSpecialization())
                .effectiveDate(assignment.getEffectiveDate())
                .endDate(assignment.getEndDate())
                .isActive(assignment.getIsActive())
                .build();
    }
}


