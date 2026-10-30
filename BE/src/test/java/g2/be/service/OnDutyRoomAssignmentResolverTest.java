package g2.be.service;

import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Room;
import g2.be.entity.Service;
import g2.be.entity.StaffSchedule;
import g2.be.entity.User;
import g2.be.entity.WorkingTimeSlot;
import g2.be.repository.DoctorRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.repository.WorkingTimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnDutyRoomAssignmentResolverTest {

    @Mock private StaffScheduleRepository staffScheduleRepository;
    @Mock private WorkingTimeSlotRepository workingTimeSlotRepository;
    @Mock private DoctorRepository doctorRepository;

    @InjectMocks
    private OnDutyRoomAssignmentResolver resolver;

    @Test
    void findAssignmentsForService_UsesServiceRoomsAndScheduledDoctorsOnly() {
        LocalDate workDate = LocalDate.of(2026, 4, 29);
        WorkingTimeSlot slot = WorkingTimeSlot.builder()
                .slotId(1L)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .isActive(true)
                .isDeleted(false)
                .build();
        Department paraclinicalDept = Department.builder()
                .deptId(2)
                .deptType(Department.DeptType.PARACLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        Room mappedScheduledRoom = Room.builder()
                .roomId("LAB-1")
                .roomName("Lab 1")
                .roomType("LAB")
                .department(paraclinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .build();
        Room mappedUnscheduledRoom = Room.builder()
                .roomId("LAB-2")
                .roomName("Lab 2")
                .roomType("LAB")
                .department(paraclinicalDept)
                .status("ACTIVE")
                .isDeleted(false)
                .build();
        Service service = Service.builder()
                .serviceId("S-X")
                .serviceName("Xet nghiem X")
                .price(BigDecimal.valueOf(100000))
                .department(paraclinicalDept)
                .rooms(List.of(mappedScheduledRoom, mappedUnscheduledRoom))
                .isActive(true)
                .isDeleted(false)
                .build();
        mappedScheduledRoom.setServices(List.of(service));
        mappedUnscheduledRoom.setServices(List.of(service));

        User doctorUser = User.builder().userId(10L).fullName("Bac si CLS").build();
        Doctor scheduledDoctor = Doctor.builder()
                .doctorId("DOC-CLS")
                .user(doctorUser)
                .department(paraclinicalDept)
                .build();
        StaffSchedule schedule = StaffSchedule.builder()
                .user(doctorUser)
                .room(mappedScheduledRoom)
                .slot(slot)
                .workDate(workDate)
                .build();

        when(staffScheduleRepository.findByRoom_RoomIdAndWorkDateOrderBySlot_StartTimeAsc("LAB-1", workDate))
                .thenReturn(List.of(schedule));
        when(staffScheduleRepository.findByRoom_RoomIdAndWorkDateOrderBySlot_StartTimeAsc("LAB-2", workDate))
                .thenReturn(List.of());
        when(doctorRepository.findByUser_UserId(10L)).thenReturn(Optional.of(scheduledDoctor));

        List<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> assignments =
                resolver.findAssignmentsForService(service, workDate, 1L);

        assertEquals(1, assignments.size());
        assertEquals("LAB-1", assignments.get(0).room().getRoomId());
        assertEquals("DOC-CLS", assignments.get(0).doctor().getDoctorId());
    }
}
