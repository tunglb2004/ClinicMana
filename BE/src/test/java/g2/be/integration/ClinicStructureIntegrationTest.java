package g2.be.integration;

import g2.be.dto.request.CreateDepartmentRequest;
import g2.be.dto.request.CreateRoomRequest;
import g2.be.dto.response.DepartmentResponse;
import g2.be.dto.response.RoomResponse;
import g2.be.entity.Department;
import g2.be.entity.Room;
import g2.be.repository.DepartmentRepository;
import g2.be.repository.RoomRepository;
import g2.be.service.DepartmentService;
import g2.be.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "cloudinary.url=cloudinary://key:secret@test-cloud",
        "sepay.api-key=test-api-key",
        "sepay.bank=TESTBANK",
        "sepay.acc=1234567890",
        "spring.datasource.url=jdbc:h2:mem:clinic-structure-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class ClinicStructureIntegrationTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void createDepartmentAndRoomShouldPersistConsistently() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String deptName = "AUTO-DEPT-" + suffix;
        String roomId = "AR" + suffix.substring(0, 6);
        String roomName = "Auto Room " + suffix;

        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest(
                        deptName,
                        Department.DeptType.CLINICAL,
                        "Automated integration verification"
                )
        );

        RoomResponse room = roomService.createRoom(
                new CreateRoomRequest(
                        roomId,
                        roomName,
                        null,
                        "Test Floor",
                        department.getDeptId(),
                        "ACTIVE",
                        15
                )
        );

        Department persistedDepartment = departmentRepository.findByDeptIdAndIsDeletedFalse(department.getDeptId())
                .orElse(null);
        Room persistedRoom = roomRepository.findByRoomIdAndIsDeletedFalse(room.getRoomId())
                .orElse(null);

        assertNotNull(persistedDepartment);
        assertEquals(deptName, persistedDepartment.getDeptName());
        assertEquals(Department.DeptType.CLINICAL, persistedDepartment.getDeptType());
        assertTrue(Boolean.TRUE.equals(persistedDepartment.getIsActive()));
        assertTrue(Boolean.FALSE.equals(persistedDepartment.getIsDeleted()));

        assertNotNull(persistedRoom);
        assertEquals(roomName, persistedRoom.getRoomName());
        assertEquals("GENERAL", persistedRoom.getRoomType());
        assertEquals(department.getDeptId(), persistedRoom.getDepartment().getDeptId());
        assertEquals("ACTIVE", persistedRoom.getStatus());
        assertTrue(Boolean.FALSE.equals(persistedRoom.getIsDeleted()));
        assertTrue(roomRepository.findByDepartment_DeptIdAndIsDeletedFalse(department.getDeptId()).stream()
                .anyMatch(existingRoom -> room.getRoomId().equals(existingRoom.getRoomId())));
    }
}
