package g2.be.service;

import g2.be.dto.request.AssignMemberRequest;
import g2.be.dto.request.CreateDepartmentRequest;
import g2.be.dto.request.UpdateDepartmentRequest;
import g2.be.dto.response.DepartmentDetailResponse;
import g2.be.dto.response.DepartmentResponse;
import g2.be.dto.response.MessageResponse;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Nurse;
import g2.be.entity.Role;
import g2.be.entity.Room;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho DepartmentService đã được cập nhật theo logic nghiệp vụ mới.
 * Logic: Soft Delete, Chặn xóa khi có nhân sự, Sử dụng AppException và ErrorCode.
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private NurseRepository nurseRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private StaffScheduleRepository staffScheduleRepository;
    
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department testDepartment;
    private Doctor testDoctor;
    private Nurse testNurse;
    private User doctorUser;
    private User nurseUser;
    private Role doctorRole;

    @BeforeEach
    void setUp() {
        testDepartment = Department.builder()
                .deptId(1)
                .deptName("Khoa Nội tổng hợp")
                .deptType(Department.DeptType.CLINICAL)
                .description("Khoa khám và điều trị")
                .isActive(true)
                .isDeleted(false)
                .build();

        doctorRole = Role.builder().roleId(1).roleName("ROLE_DOCTOR").build();

        doctorUser = User.builder()
            .userId(2L)
            .fullName("Bác sĩ A")
            .roles(new HashSet<>())
            .build();
        nurseUser = User.builder().userId(3L).fullName("Y tá B").build();

        testDoctor = Doctor.builder().doctorId("D-001").user(doctorUser).build();
        testNurse = Nurse.builder().nurseId("N-001").user(nurseUser).build();
    }

    @Test
    void testGetAllDepartments_Success() {
        List<Department> departments = List.of(testDepartment);
        when(departmentRepository.findAllByIsDeletedFalseAndIsActiveTrueOrderByDeptIdDesc()).thenReturn(departments);
        when(doctorRepository.findByDepartment_DeptId(1)).thenReturn(List.of(testDoctor));
        when(nurseRepository.findByDepartment_DeptId(1)).thenReturn(List.of(testNurse));

        Map<String, Object> result = departmentService.getAllDepartments(null, 0, 10, false);

        assertNotNull(result);
        assertEquals(1L, result.get("totalItems"));
        verify(departmentRepository).findAllByIsDeletedFalseAndIsActiveTrueOrderByDeptIdDesc();
    }

    @Test
    void testGetDepartmentById_Success() {
        when(departmentRepository.findByDeptIdAndIsDeletedFalse(1)).thenReturn(Optional.of(testDepartment));

        DepartmentDetailResponse result = departmentService.getDepartmentById(1);

        assertNotNull(result);
        assertEquals("Khoa Nội tổng hợp", result.getDeptName());
    }

    @Test
    void testGetDepartmentById_NotFound() {
        when(departmentRepository.findByDeptIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            departmentService.getDepartmentById(999);
        });
        assertEquals(ErrorCode.DEPT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testCreateDepartment_Success() {
        CreateDepartmentRequest request = new CreateDepartmentRequest();
        request.setDeptName("Khoa Ngoại");
        request.setDeptType(Department.DeptType.CLINICAL);

        when(departmentRepository.existsByDeptNameAndIsDeletedFalse("Khoa Ngoại")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);

        DepartmentResponse result = departmentService.createDepartment(request);

        assertNotNull(result);
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void testDeleteDepartment_Success_WithSoftDelete() {
        // Arrange: Không có nhân sự, có 1 phòng trống
        Room emptyRoom = Room.builder().roomId("R1").roomName("Phòng 1").build();
        when(departmentRepository.findByDeptIdAndIsDeletedFalse(1)).thenReturn(Optional.of(testDepartment));
        when(doctorRepository.findByDepartment_DeptId(1)).thenReturn(Collections.emptyList());
        when(nurseRepository.findByDepartment_DeptId(1)).thenReturn(Collections.emptyList());
        when(roomRepository.findByDepartment_DeptIdAndIsDeletedFalse(1)).thenReturn(List.of(emptyRoom));
        when(doctorRepository.findByRoom_RoomId("R1")).thenReturn(Collections.emptyList());

        // Act
        MessageResponse result = departmentService.deleteDepartment(1);

        // Assert
        assertTrue(testDepartment.getIsDeleted());
        assertFalse(testDepartment.getIsActive());
        assertTrue(emptyRoom.getIsDeleted());
        verify(departmentRepository).save(testDepartment);
        verify(roomRepository).saveAll(anyList());
    }

    @Test
    void testDeleteDepartment_Fail_HasPersonnel() {
        // Arrange: Khoa vẫn còn bác sĩ
        when(departmentRepository.findByDeptIdAndIsDeletedFalse(1)).thenReturn(Optional.of(testDepartment));
        when(doctorRepository.findByDepartment_DeptId(1)).thenReturn(List.of(testDoctor));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            departmentService.deleteDepartment(1);
        });
        assertTrue(exception.getMessage().contains("nhân sự"));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void testUpdateDepartmentStatus_Deactivate_Fail_HasPersonnel() {
        when(departmentRepository.findByDeptIdAndIsDeletedFalse(1)).thenReturn(Optional.of(testDepartment));
        when(doctorRepository.findByDepartment_DeptId(1)).thenReturn(List.of(testDoctor));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            departmentService.updateDepartmentStatus(1, false);
        });
        assertTrue(exception.getMessage().contains("Không thể khóa khoa"));
    }

    @Test
    void testAssignMember_Doctor_Success() {
        AssignMemberRequest request = new AssignMemberRequest();
        request.setUserId(2L);

        when(departmentRepository.findByDeptIdAndIsDeletedFalse(1)).thenReturn(Optional.of(testDepartment));
        when(doctorRepository.findByUser_UserId(2L)).thenReturn(Optional.of(testDoctor));
        when(roleRepository.findByRoleName("ROLE_DOCTOR")).thenReturn(Optional.of(doctorRole));

        departmentService.assignMember(1, request);

        assertEquals(testDepartment, testDoctor.getDepartment());
        assertTrue(doctorUser.getRoles().contains(doctorRole));
        verify(doctorRepository).save(testDoctor);
        verify(userRepository).save(doctorUser);
    }

    @Test
    void testRemoveMember_Doctor_Fail_AlwaysRequiresDepartment() {
        // Mock logic block của service: không cho gỡ nếu không gán sang khoa khác
        when(departmentRepository.findByDeptIdAndIsDeletedFalse(1)).thenReturn(Optional.of(testDepartment));
        when(doctorRepository.findByUser_UserId(2L)).thenReturn(Optional.of(testDoctor));
        testDoctor.setDepartment(testDepartment);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            departmentService.removeMember(1, 2L);
        });

        // Verify không gọi save (không gỡ)
        verify(doctorRepository, never()).save(any());
    }
}
