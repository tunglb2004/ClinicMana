package g2.be.service;

import g2.be.dto.request.AssignMemberRequest;
import g2.be.dto.request.CreateRoomRequest;
import g2.be.dto.request.UpdateRoomRequest;
import g2.be.dto.response.MemberResponse;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.RoomDetailResponse;
import g2.be.dto.response.RoomResponse;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Room;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.DepartmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.RoomRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.util.DoctorDepartmentClassifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final StaffScheduleRepository staffScheduleRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getAllRooms(Integer deptId, String keyword, int page, int size) {
        return getAllRooms(deptId, keyword, page, size, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllRooms(Integer deptId, String keyword, int page, int size, boolean includeInactive) {
        List<Room> rooms = deptId != null
                ? roomRepository.findByDepartment_DeptIdAndIsDeletedFalseOrderByRoomIdAsc(deptId)
                : roomRepository.findAllByIsDeletedFalseOrderByRoomIdAsc();

        if (!includeInactive) {
            rooms = rooms.stream()
                    .filter(this::isOperationalRoom)
                    .toList();
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null) {
            rooms = rooms.stream()
                    .filter(room -> matchesKeyword(room, normalizedKeyword))
                    .toList();
        }

        List<RoomResponse> responses = rooms.stream()
                .map(this::convertToRoomResponse)
                .toList();

        return buildPagedResponse(responses, page, size, "rooms");
    }

    @Transactional(readOnly = true)
    public RoomDetailResponse getRoomById(String roomId) {
        Room room = findVisibleRoom(roomId);

        List<MemberResponse> doctors = doctorRepository.findByRoom_RoomId(roomId).stream()
                .map(this::convertDoctorToMember)
                .toList();

        return RoomDetailResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .roomType(room.getRoomType())
                .location(room.getLocation())
                .deptId(room.getDepartment() != null ? room.getDepartment().getDeptId() : null)
                .deptName(room.getDepartment() != null ? room.getDepartment().getDeptName() : null)
                .doctors(doctors)
                .status(room.getStatus())
                .baseAvgTime(room.getBaseAvgTime())
                .currentQueueLength(room.getCurrentQueueLength())
                .build();
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        String roomId = request.getRoomId().trim();
        String roomName = request.getRoomName().trim();

        if (roomRepository.existsById(roomId)) {
            throw new RuntimeException("Ma phong '" + roomId + "' da ton tai");
        }
        if (roomRepository.existsByRoomNameAndIsDeletedFalse(roomName)) {
            throw new RuntimeException("Ten phong '" + roomName + "' da ton tai");
        }
        if (request.getDeptId() == null) {
            throw new RuntimeException("Khong the tao phong khi chua gan khoa.");
        }

        Department department = findDepartmentForRoom(request.getDeptId(), true);
        Room room = Room.builder()
                .roomId(roomId)
                .roomName(roomName)
                .roomType(resolveCanonicalRoomType(department))
                .location(request.getLocation())
                .department(department)
                .status(normalizeStatus(request.getStatus(), "ACTIVE"))
                .isDeleted(false)
                .baseAvgTime(request.getBaseAvgTime() != null ? request.getBaseAvgTime() : 15)
                .currentQueueLength(0)
                .build();

        return convertToRoomResponse(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse updateRoom(String roomId, UpdateRoomRequest request) {
        Room room = findVisibleRoom(roomId);
        Department targetDepartment = room.getDepartment();

        if (request.getRoomName() != null && !request.getRoomName().trim().isEmpty()) {
            String roomName = request.getRoomName().trim();
            if (roomRepository.existsByRoomNameAndIsDeletedFalseAndRoomIdNot(roomName, roomId)) {
                throw new RuntimeException("Ten phong '" + roomName + "' da ton tai");
            }
            room.setRoomName(roomName);
        }

        if (request.getLocation() != null) {
            room.setLocation(request.getLocation());
        }

        if (request.getDeptId() != null) {
            targetDepartment = findDepartmentForRoom(request.getDeptId(), true);
        }

        if (targetDepartment == null) {
            throw new RuntimeException("Phong phai luon thuoc mot khoa hop le.");
        }

        if (request.getDeptId() != null && room.getDepartment() != null
                && !room.getDepartment().getDeptId().equals(targetDepartment.getDeptId())) {
            if (!doctorRepository.findByRoom_RoomId(roomId).isEmpty()) {
                throw new RuntimeException(
                        "Khong the doi khoa cua phong dang co bac si. Vui long go bac si khoi phong truoc.");
            }
            if (!staffScheduleRepository.findByRoom_RoomId(roomId).isEmpty()) {
                throw new RuntimeException(
                        "Khong the doi khoa cua phong dang co lich truc gan phong. Vui long go lich truc khoi phong truoc.");
            }
        }

        room.setDepartment(targetDepartment);
        room.setRoomType(resolveCanonicalRoomType(targetDepartment));

        if (request.getStatus() != null) {
            room.setStatus(normalizeStatus(request.getStatus(), room.getStatus()));
        }
        if (request.getBaseAvgTime() != null) {
            room.setBaseAvgTime(request.getBaseAvgTime());
        }

        return convertToRoomResponse(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse updateRoomStatus(String roomId, String status) {
        Room room = findVisibleRoom(roomId);
        String newStatus = normalizeStatus(status, room.getStatus());
        
        if ("INACTIVE".equals(newStatus)) {
            boolean hasDoctors = !doctorRepository.findByRoom_RoomId(roomId).isEmpty();
            boolean hasSchedules = !staffScheduleRepository.findByRoom_RoomId(roomId).isEmpty();
            if (hasDoctors || hasSchedules) {
                throw new RuntimeException("Không thể khóa phòng khi vẫn còn bác sĩ phụ trách hoặc có lịch trực tại phòng.");
            }
        }
        
        room.setStatus(newStatus);
        return convertToRoomResponse(roomRepository.save(room));
    }

    @Transactional
    public MessageResponse deleteRoom(String roomId) {
        Room room = findVisibleRoom(roomId);
        
        boolean hasDoctors = !doctorRepository.findByRoom_RoomId(roomId).isEmpty();
        boolean hasSchedules = !staffScheduleRepository.findByRoom_RoomId(roomId).isEmpty();
        if (hasDoctors || hasSchedules) {
            throw new RuntimeException("Không thể xóa phòng do vẫn còn bác sĩ phụ trách hoặc có lịch trực liên quan. Vui lòng gỡ phân công trước khi xóa.");
        }

        room.setStatus("INACTIVE");
        room.setIsDeleted(true);
        roomRepository.save(room);
        return new MessageResponse("Xoa phong '" + room.getRoomName() + "' thanh cong");
    }

    @Transactional
    public MessageResponse assignMember(String roomId, AssignMemberRequest request) {
        Room room = findAssignableRoom(roomId);

        Doctor doctor = doctorRepository.findByUser_UserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "Khong tim thay bac si voi userId: " + request.getUserId()
                                + ". Chi bac si moi co the duoc gan vao phong."));

        validateDoctorRoomCompatibility(doctor, room);
        doctor.setRoom(room);
        doctorRepository.save(doctor);

        return new MessageResponse("Da gan bac si '" + doctor.getUser().getFullName()
                + "' vao phong '" + room.getRoomName() + "'");
    }

    @Transactional
    public MessageResponse removeMember(String roomId, Long userId) {
        findVisibleRoom(roomId);

        Doctor doctor = doctorRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay bac si voi userId: " + userId));

        if (doctor.getRoom() == null || !doctor.getRoom().getRoomId().equals(roomId)) {
            throw new RuntimeException("Bac si nay khong thuoc phong nay");
        }

        doctor.setRoom(null);
        doctorRepository.save(doctor);

        return new MessageResponse("Da go bac si '" + doctor.getUser().getFullName() + "' khoi phong");
    }

    private RoomResponse convertToRoomResponse(Room room) {
        int doctorCount = doctorRepository.findByRoom_RoomId(room.getRoomId()).size();

        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .roomType(room.getRoomType())
                .location(room.getLocation())
                .deptId(room.getDepartment() != null ? room.getDepartment().getDeptId() : null)
                .deptName(room.getDepartment() != null ? room.getDepartment().getDeptName() : null)
                .doctorCount(doctorCount)
                .status(room.getStatus())
                .baseAvgTime(room.getBaseAvgTime())
                .currentQueueLength(room.getCurrentQueueLength())
                .build();
    }

    private void validateDoctorRoomCompatibility(Doctor doctor, Room room) {
        if (doctor.getDepartment() == null) {
            throw new RuntimeException("Bac si phai duoc gan khoa truoc khi gan vao phong.");
        }
        if (room.getDepartment() == null) {
            throw new RuntimeException("Phong phai thuoc mot khoa hop le truoc khi gan bac si.");
        }
        if (!Boolean.TRUE.equals(doctor.getDepartment().getIsActive())
                || Boolean.TRUE.equals(doctor.getDepartment().getIsDeleted())) {
            throw new RuntimeException("Khoa cua bac si dang bi khoa hoac da xoa mem.");
        }
        if (!doctor.getDepartment().getDeptId().equals(room.getDepartment().getDeptId())) {
            throw new RuntimeException("Bac si chi duoc gan vao phong thuoc dung khoa cua minh.");
        }

        boolean generalRoom = isGeneralRoom(room);
        boolean paraclinicalDoctor = isParaclinicalDoctor(doctor);

        if (generalRoom && paraclinicalDoctor) {
            throw new RuntimeException("Phong '" + room.getRoomName()
                    + "' la phong GENERAL nen khong the gan bac si can lam sang.");
        }

        if (!generalRoom && !paraclinicalDoctor) {
            throw new RuntimeException("Phong '" + room.getRoomName()
                    + "' khong phai phong GENERAL nen khong the gan bac si lam sang.");
        }
    }

    private boolean isParaclinicalDoctor(Doctor doctor) {
        return DoctorDepartmentClassifier.isParaclinical(doctor);
    }

    private String resolveCanonicalRoomType(Department department) {
        if (department == null || department.getDeptType() == null) {
            throw new RuntimeException("Khong the xac dinh loai phong khi khoa chua hop le.");
        }

        return switch (department.getDeptType()) {
            case CLINICAL -> "GENERAL";
            case PARACLINICAL -> "PARACLINICAL";
            case ADMINISTRATIVE -> "ADMINISTRATIVE";
            case PHARMACY -> "PHARMACY";
        };
    }

    private boolean isGeneralRoom(Room room) {
        String roomType = Optional.ofNullable(room.getRoomType()).orElse("").toUpperCase(Locale.ROOT);
        String roomId = Optional.ofNullable(room.getRoomId()).orElse("").toUpperCase(Locale.ROOT);
        String roomName = Optional.ofNullable(room.getRoomName()).orElse("").toLowerCase(Locale.ROOT);

        if (roomType.contains("GENERAL")
                || roomId.startsWith("DK")
                || roomName.contains("da khoa")
                || roomName.contains("general")) {
            return true;
        }

        if (room.getDepartment() != null && room.getDepartment().getDeptType() != null) {
            return room.getDepartment().getDeptType() == Department.DeptType.CLINICAL;
        }

        return false;
    }

    private MemberResponse convertDoctorToMember(Doctor doctor) {
        return MemberResponse.builder()
                .userId(doctor.getUser().getUserId())
                .fullName(doctor.getUser().getFullName())
                .staffId(doctor.getUser().getStaffId())
                .role("DOCTOR")
                .specialization(doctor.getSpecialization())
                .phoneNumber(doctor.getUser().getPhoneNumber())
                .email(doctor.getUser().getEmail())
                .build();
    }

    private Room findVisibleRoom(String roomId) {
        return roomRepository.findByRoomIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Room findAssignableRoom(String roomId) {
        Room room = findVisibleRoom(roomId);
        if (!isOperationalRoom(room)) {
            throw new RuntimeException("Khong the thao tac voi phong dang khoa hoac da bi vo hieu hoa.");
        }
        return room;
    }

    private Department findDepartmentForRoom(Integer deptId, boolean requireActive) {
        Department department = departmentRepository.findByDeptIdAndIsDeletedFalse(deptId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPT_NOT_FOUND));
        if (requireActive && !Boolean.TRUE.equals(department.getIsActive())) {
            throw new RuntimeException("Khong the gan phong vao khoa dang khoa.");
        }
        return department;
    }

    private String normalizeStatus(String status, String fallback) {
        String resolved = (status == null || status.isBlank()) ? fallback : status.trim();
        String normalized = resolved.toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new RuntimeException("Trang thai phong phai la ACTIVE hoac INACTIVE.");
        }
        return normalized;
    }

    private boolean isOperationalRoom(Room room) {
        if (room == null || Boolean.TRUE.equals(room.getIsDeleted())) {
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(room.getStatus())) {
            return false;
        }
        Department department = room.getDepartment();
        return department != null
                && !Boolean.TRUE.equals(department.getIsDeleted())
                && Boolean.TRUE.equals(department.getIsActive());
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesKeyword(Room room, String keyword) {
        return (room.getRoomId() != null && room.getRoomId().toLowerCase(Locale.ROOT).contains(keyword))
                || (room.getRoomName() != null && room.getRoomName().toLowerCase(Locale.ROOT).contains(keyword));
    }

    private <T> Map<String, Object> buildPagedResponse(List<T> items, int page, int size, String key) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : Math.max(items.size(), 1);
        int fromIndex = Math.min(safePage * safeSize, items.size());
        int toIndex = Math.min(fromIndex + safeSize, items.size());
        int totalPages = items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / safeSize);

        Map<String, Object> response = new HashMap<>();
        response.put(key, items.subList(fromIndex, toIndex));
        response.put("currentPage", safePage);
        response.put("totalItems", (long) items.size());
        response.put("totalPages", totalPages);
        return response;
    }
}
