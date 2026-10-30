package g2.be.service;

import g2.be.dto.response.ServiceRoomResponse;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Room;
import g2.be.entity.Service;
import g2.be.repository.RoomRepository;
import g2.be.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRoomServiceTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private OnDutyRoomAssignmentResolver onDutyRoomAssignmentResolver;

    @InjectMocks
    private ServiceRoomService serviceRoomService;

    @Test
    void getRoomsByService_ReturnsOnlyRoomsWithOnDutyDoctorForRequestedService() {
        LocalDate workDate = LocalDate.of(2026, 4, 29);
        Department paraclinicalDept = Department.builder()
                .deptId(2)
                .deptType(Department.DeptType.PARACLINICAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        Room onDutyRoom = Room.builder()
                .roomId("LAB-1")
                .roomName("Lab 1")
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
                .rooms(List.of(onDutyRoom))
                .isActive(true)
                .isDeleted(false)
                .build();
        onDutyRoom.setServices(List.of(service));
        Doctor doctor = Doctor.builder()
                .doctorId("DOC-1")
                .department(paraclinicalDept)
                .build();

        when(serviceRepository.findByServiceIdAndIsDeletedFalse("S-X")).thenReturn(Optional.of(service));
        when(onDutyRoomAssignmentResolver.findCurrentAssignmentsForService(service, workDate, 1L))
                .thenReturn(List.of(new OnDutyRoomAssignmentResolver.RoomDoctorAssignment(onDutyRoom, doctor)));

        List<ServiceRoomResponse> result = serviceRoomService.getRoomsByService("S-X", workDate, 1L);

        assertEquals(1, result.size());
        assertEquals("LAB-1", result.get(0).getRoomId());
        assertEquals("S-X", result.get(0).getServices().get(0).getServiceId());
    }
}
