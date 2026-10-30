package g2.be.service;

import g2.be.dto.request.AssignMemberRequest;
import g2.be.dto.request.CreateRoomRequest;
import g2.be.dto.request.UpdateRoomRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.RoomDetailResponse;
import g2.be.dto.response.RoomResponse;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.DoctorType;
import g2.be.entity.Room;
import g2.be.entity.StaffSchedule;
import g2.be.entity.User;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.DepartmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.RoomRepository;
import g2.be.repository.StaffScheduleRepository;
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
 * Unit test cho RoomService đã được cập nhật.
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private StaffScheduleRepository staffScheduleRepository;

    @InjectMocks
    private RoomService roomService;

    private Department testDepartment;
    private Room testRoom;
    private Doctor testDoctor;
    private User doctorUser;

    @BeforeEach
    void setUp() {
        testDepartment = Department.builder()
                .deptId(1)
                .deptName("Khoa Nội")
                .deptType(Department.DeptType.CLINICAL) // Sẽ sinh ra roomType GENERAL
                .build();

        testRoom = Room.builder()
                .roomId("P-001")
                .roomName("Phòng 1")
                .status("ACTIVE")
                .isDeleted(false)
                .department(testDepartment)
                .build();

        doctorUser = User.builder()
            .userId(10L)
            .fullName("Bác sĩ phòng khám")
            .staffId("ST-001")
            .phoneNumber("0901234567")
            .email("doctor@test.com")
            .build();

        testDoctor = Doctor.builder()
            .doctorId("D-001")
            .user(doctorUser)
            .department(testDepartment)
            .doctorType(DoctorType.CLINICAL)
            .build();
    }

    @Test
    void testGetRoomById_Success() {
        when(roomRepository.findByRoomIdAndIsDeletedFalse("P-001")).thenReturn(Optional.of(testRoom));
        when(doctorRepository.findByRoom_RoomId("P-001")).thenReturn(List.of(testDoctor));

        RoomDetailResponse result = roomService.getRoomById("P-001");

        assertNotNull(result);
        assertEquals("P-001", result.getRoomId());
    }

    @Test
    void testGetRoomById_NotFound() {
        when(roomRepository.findByRoomIdAndIsDeletedFalse("P-999")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            roomService.getRoomById("P-999");
        });
        assertEquals(ErrorCode.ROOM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testCreateRoom_Success_WithDerivedRoomType() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomId("P-002");
        request.setRoomName("Phòng 2");
        request.setDeptId(1);
        request.setRoomType("OLD_TYPE"); // Sẽ bị ghi đè thành GENERAL

        when(roomRepository.existsById("P-002")).thenReturn(false);
        when(roomRepository.existsByRoomNameAndIsDeletedFalse("Phòng 2")).thenReturn(false);
        when(departmentRepository.findByDeptIdAndIsDeletedFalse(1)).thenReturn(Optional.of(testDepartment));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse result = roomService.createRoom(request);

        assertNotNull(result);
        assertEquals("GENERAL", result.getRoomType()); // Được gán theo Khoa CLINICAL
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void testDeleteRoom_Success_SoftDelete() {
        when(roomRepository.findByRoomIdAndIsDeletedFalse("P-001")).thenReturn(Optional.of(testRoom));
        when(doctorRepository.findByRoom_RoomId("P-001")).thenReturn(Collections.emptyList());
        when(staffScheduleRepository.findByRoom_RoomId("P-001")).thenReturn(Collections.emptyList());

        MessageResponse result = roomService.deleteRoom("P-001");

        assertTrue(testRoom.getIsDeleted());
        assertEquals("INACTIVE", testRoom.getStatus());
        verify(roomRepository).save(testRoom);
    }

    @Test
    void testDeleteRoom_Fail_HasSchedule() {
        when(roomRepository.findByRoomIdAndIsDeletedFalse("P-001")).thenReturn(Optional.of(testRoom));
        when(doctorRepository.findByRoom_RoomId("P-001")).thenReturn(Collections.emptyList());
        when(staffScheduleRepository.findByRoom_RoomId("P-001")).thenReturn(List.of(new StaffSchedule())); // Mock có lịch trực

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            roomService.deleteRoom("P-001");
        });
        assertTrue(exception.getMessage().contains("lịch trực"));
        verify(roomRepository, never()).save(testRoom);
    }

    @Test
    void testUpdateRoomStatus_Deactivate_Fail_HasDoctors() {
        when(roomRepository.findByRoomIdAndIsDeletedFalse("P-001")).thenReturn(Optional.of(testRoom));
        when(doctorRepository.findByRoom_RoomId("P-001")).thenReturn(List.of(testDoctor));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            roomService.updateRoomStatus("P-001", "INACTIVE");
        });
        assertTrue(exception.getMessage().contains("vẫn còn bác sĩ"));
    }
}
