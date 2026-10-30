package g2.be.service;

import g2.be.dto.request.AssignMemberRequest;
import g2.be.dto.request.CreateDepartmentRequest;
import g2.be.dto.request.UpdateDepartmentRequest;
import g2.be.dto.response.DepartmentDetailResponse;
import g2.be.dto.response.DepartmentResponse;
import g2.be.dto.response.MemberResponse;
import g2.be.dto.response.MessageResponse;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.DoctorType;
import g2.be.entity.Nurse;
import g2.be.entity.Role;
import g2.be.entity.Room;
import g2.be.entity.StaffSchedule;
import g2.be.entity.User;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.DepartmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.NurseRepository;
import g2.be.repository.RoleRepository;
import g2.be.repository.RoomRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final NurseRepository nurseRepository;
    private final RoomRepository roomRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getAllDepartments(String keyword, int page, int size) {
        return getAllDepartments(keyword, page, size, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllDepartments(String keyword, int page, int size, boolean includeInactive) {
        List<Department> departments = includeInactive
                ? departmentRepository.findAllByIsDeletedFalseOrderByDeptIdDesc()
                : departmentRepository.findAllByIsDeletedFalseAndIsActiveTrueOrderByDeptIdDesc();

        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null) {
            departments = departments.stream()
                    .filter(department -> containsIgnoreCase(department.getDeptName(), normalizedKeyword))
                    .toList();
        }

        List<DepartmentResponse> responses = departments.stream()
                .map(this::convertToDepartmentResponse)
                .toList();

        return buildPagedResponse(responses, page, size, "departments");
    }

    @Transactional(readOnly = true)
    public DepartmentDetailResponse getDepartmentById(Integer deptId) {
        Department department = findVisibleDepartment(deptId);

        List<MemberResponse> doctors = doctorRepository.findByDepartment_DeptId(deptId).stream()
                .map(this::convertDoctorToMember)
                .toList();

        List<MemberResponse> nurses = nurseRepository.findByDepartment_DeptId(deptId).stream()
                .map(this::convertNurseToMember)
                .toList();

        return DepartmentDetailResponse.builder()
                .deptId(department.getDeptId())
                .deptName(department.getDeptName())
                .deptType(department.getDeptType().name())
                .description(department.getDescription())
                .isActive(department.getIsActive())
                .doctors(doctors)
                .nurses(nurses)
                .build();
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String deptName = request.getDeptName().trim();
        if (departmentRepository.existsByDeptNameAndIsDeletedFalse(deptName)) {
            throw new AppException(ErrorCode.DEPT_NAME_EXISTS);
        }

        Department department = Department.builder()
                .deptName(deptName)
                .deptType(request.getDeptType())
                .description(request.getDescription())
                .isActive(true)
                .isDeleted(false)
                .build();

        return convertToDepartmentResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse updateDepartment(Integer deptId, UpdateDepartmentRequest request) {
        Department department = findVisibleDepartment(deptId);

        if (request.getDeptName() != null && !request.getDeptName().trim().isEmpty()) {
            String deptName = request.getDeptName().trim();
            if (departmentRepository.existsByDeptNameAndIsDeletedFalseAndDeptIdNot(deptName, deptId)) {
                throw new AppException(ErrorCode.DEPT_NAME_EXISTS);
            }
            department.setDeptName(deptName);
        }

        if (request.getDeptType() != null && request.getDeptType() != department.getDeptType()) {
            boolean hasMembers = !doctorRepository.findByDepartment_DeptId(deptId).isEmpty()
                    || !nurseRepository.findByDepartment_DeptId(deptId).isEmpty();
            boolean hasRooms = !roomRepository.findByDepartment_DeptIdAndIsDeletedFalse(deptId).isEmpty();
            if (hasMembers || hasRooms) {
                throw new RuntimeException(
                        "Khong the doi loai khoa khi khoa dang co nhan su hoac phong. Vui long chuyen het du lieu phu thuoc truoc.");
            }
            department.setDeptType(request.getDeptType());
        }

        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }

        return convertToDepartmentResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse updateDepartmentStatus(Integer deptId, Boolean isActive) {
        Department department = findVisibleDepartment(deptId);
        
        if (Boolean.FALSE.equals(isActive)) {
            boolean hasDoctors = !doctorRepository.findByDepartment_DeptId(deptId).isEmpty();
            boolean hasNurses = !nurseRepository.findByDepartment_DeptId(deptId).isEmpty();
            if (hasDoctors || hasNurses) {
                throw new RuntimeException("Không thể khóa khoa khi vẫn còn nhân sự (Bác sĩ/Y tá) đang công tác tại khoa.");
            }
        }
        
        department.setIsActive(Boolean.TRUE.equals(isActive));
        return convertToDepartmentResponse(departmentRepository.save(department));
    }

    @Transactional
    public MessageResponse deleteDepartment(Integer deptId) {
        Department department = findVisibleDepartment(deptId);
        
        // Kiểm tra nhân sự trực tiếp thuộc khoa
        boolean hasDoctors = !doctorRepository.findByDepartment_DeptId(deptId).isEmpty();
        boolean hasNurses = !nurseRepository.findByDepartment_DeptId(deptId).isEmpty();
        if (hasDoctors || hasNurses) {
            throw new RuntimeException("Không thể xóa khoa do vẫn còn nhân sự hoạt động. Vui lòng chuyển nhân sự sang khoa khác trước khi xóa.");
        }

        List<Room> rooms = roomRepository.findByDepartment_DeptIdAndIsDeletedFalse(deptId);
        
        // Kiểm tra xem có phòng nào thuộc khoa đang có nhân sự không
        for (Room room : rooms) {
            if (!doctorRepository.findByRoom_RoomId(room.getRoomId()).isEmpty()) {
                throw new RuntimeException("Không thể xóa khoa khi phòng '" + room.getRoomName() + "' vẫn còn bác sĩ phụ trách.");
            }
        }

        department.setIsActive(false);
        department.setIsDeleted(true);

        for (Room room : rooms) {
            room.setStatus("INACTIVE");
            room.setIsDeleted(true);
        }
        if (!rooms.isEmpty()) {
            roomRepository.saveAll(rooms);
        }

        departmentRepository.save(department);
        return new MessageResponse(ErrorCode.DELETE_DEPT_SUCCESS.getMessage());
    }

    @Transactional
    public MessageResponse assignMember(Integer deptId, AssignMemberRequest request) {
        Department department = findAssignableDepartment(deptId);

        var doctorOpt = doctorRepository.findByUser_UserId(request.getUserId());
        if (doctorOpt.isPresent()) {
            Doctor doctor = doctorOpt.get();
            doctor.setDepartment(department);
            doctor.setDoctorType(resolveDoctorTypeFromDepartment(department));
            syncDoctorRoles(doctor.getUser(), doctor.getDoctorType());
            clearDoctorRoomIfIncompatible(doctor);
            doctorRepository.save(doctor);
            userRepository.save(doctor.getUser());
            syncSchedulesForDepartmentChange(doctor.getUser().getUserId(), department);
            return new MessageResponse("Da gan bac si '" + doctor.getUser().getFullName()
                    + "' vao khoa '" + department.getDeptName() + "'");
        }

        var nurseOpt = nurseRepository.findByUser_UserId(request.getUserId());
        if (nurseOpt.isPresent()) {
            Nurse nurse = nurseOpt.get();
            nurse.setDepartment(department);
            nurseRepository.save(nurse);
            syncSchedulesForDepartmentChange(nurse.getUser().getUserId(), department);
            return new MessageResponse("Da gan y ta '" + nurse.getUser().getFullName()
                    + "' vao khoa '" + department.getDeptName() + "'");
        }

        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @Transactional
    public MessageResponse removeMember(Integer deptId, Long userId) {
        findVisibleDepartment(deptId);

        if (!staffScheduleRepository.findByUser_UserId(userId).isEmpty()) {
            throw new RuntimeException(
                    "Khong the go nhan su khoi khoa khi con lich truc. Vui long chuyen khoa hoac xoa lich truc truoc.");
        }

        var doctorOpt = doctorRepository.findByUser_UserId(userId);
        if (doctorOpt.isPresent()) {
            Doctor doctor = doctorOpt.get();
            if (doctor.getDepartment() == null || !doctor.getDepartment().getDeptId().equals(deptId)) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
            if (doctor.getRoom() != null) {
                throw new RuntimeException(
                        "Khong the go bac si khoi khoa khi con gan phong. Vui long go phong hoac chuyen khoa truoc.");
            }
            throw new RuntimeException(
                    "Khong the de bac si khong thuoc khoa nao. Vui long dung API gan sang khoa khac thay vi go khoi khoa.");
        }

        var nurseOpt = nurseRepository.findByUser_UserId(userId);
        if (nurseOpt.isPresent()) {
            Nurse nurse = nurseOpt.get();
            if (nurse.getDepartment() == null || !nurse.getDepartment().getDeptId().equals(deptId)) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
            throw new RuntimeException(
                    "Khong the de y ta khong thuoc khoa nao. Vui long dung API gan sang khoa khac thay vi go khoi khoa.");
        }

        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    private DepartmentResponse convertToDepartmentResponse(Department department) {
        int doctorCount = doctorRepository.findByDepartment_DeptId(department.getDeptId()).size();
        int nurseCount = nurseRepository.findByDepartment_DeptId(department.getDeptId()).size();

        return DepartmentResponse.builder()
                .deptId(department.getDeptId())
                .deptName(department.getDeptName())
                .deptType(department.getDeptType().name())
                .description(department.getDescription())
                .isActive(department.getIsActive())
                .doctorCount(doctorCount)
                .nurseCount(nurseCount)
                .build();
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

    private MemberResponse convertNurseToMember(Nurse nurse) {
        return MemberResponse.builder()
                .userId(nurse.getUser().getUserId())
                .fullName(nurse.getUser().getFullName())
                .staffId(nurse.getUser().getStaffId())
                .role("NURSE")
                .specialization(null)
                .phoneNumber(nurse.getUser().getPhoneNumber())
                .email(nurse.getUser().getEmail())
                .build();
    }

    private DoctorType resolveDoctorTypeFromDepartment(Department department) {
        if (department.getDeptType() == Department.DeptType.CLINICAL) {
            return DoctorType.CLINICAL;
        }
        if (department.getDeptType() == Department.DeptType.PARACLINICAL) {
            return DoctorType.SPECIALIST;
        }
        throw new RuntimeException("Khoa nay khong phu hop de gan cho bac si.");
    }

    private void syncDoctorRoles(User user, DoctorType doctorType) {
        Set<Role> roles = user.getRoles() == null ? new HashSet<>() : user.getRoles();
        user.setRoles(roles);

        roles.removeIf(role -> {
            String roleName = role.getRoleName();
            return "ROLE_DOCTOR".equals(roleName)
                    || "ROLE_CLINICALDOCTOR".equals(roleName)
                    || "ROLE_CLINICAL_DOCTOR".equals(roleName)
                    || "ROLE_SPECIALISTDOCTOR".equals(roleName)
                    || "ROLE_SPECIALIST_DOCTOR".equals(roleName);
        });

        String targetRole = doctorType == DoctorType.SPECIALIST ? "ROLE_SPECIALISTDOCTOR" : "ROLE_DOCTOR";
        Role doctorRole = roleRepository.findByRoleName(targetRole)
                .orElseThrow(() -> new RuntimeException("Thieu role: " + targetRole));
        roles.add(doctorRole);
    }

    private void clearDoctorRoomIfIncompatible(Doctor doctor) {
        Room room = doctor.getRoom();
        if (room == null || room.getDepartment() == null || doctor.getDepartment() == null) {
            return;
        }

        boolean sameDepartment = room.getDepartment().getDeptId().equals(doctor.getDepartment().getDeptId());
        boolean roomVisible = !Boolean.TRUE.equals(room.getIsDeleted());
        boolean roomActive = "ACTIVE".equalsIgnoreCase(room.getStatus());
        boolean roomDepartmentActive = !Boolean.TRUE.equals(room.getDepartment().getIsDeleted())
                && Boolean.TRUE.equals(room.getDepartment().getIsActive());

        if (!sameDepartment || !roomVisible || !roomActive || !roomDepartmentActive) {
            doctor.setRoom(null);
        }
    }

    private void syncSchedulesForDepartmentChange(Long userId, Department department) {
        List<StaffSchedule> schedules = staffScheduleRepository.findByUser_UserId(userId);
        if (schedules.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (StaffSchedule schedule : schedules) {
            if (schedule.getRoom() != null && schedule.getRoom().getDepartment() != null
                    && !schedule.getRoom().getDepartment().getDeptId().equals(department.getDeptId())) {
                schedule.setRoom(null);
                changed = true;
            }

            if (!department.getDeptId().equals(schedule.getLegacyDeptId())) {
                schedule.setLegacyDeptId(department.getDeptId());
                changed = true;
            }
        }

        if (changed) {
            staffScheduleRepository.saveAll(schedules);
        }
    }

    private Department findVisibleDepartment(Integer deptId) {
        return departmentRepository.findByDeptIdAndIsDeletedFalse(deptId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPT_NOT_FOUND));
    }

    private Department findAssignableDepartment(Integer deptId) {
        Department department = findVisibleDepartment(deptId);
        if (!Boolean.TRUE.equals(department.getIsActive())) {
            throw new RuntimeException("Khong the thao tac voi khoa dang khoa.");
        }
        return department;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
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
