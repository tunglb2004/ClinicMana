package g2.be.service;

import g2.be.dto.request.ServiceRoomRequest;
import g2.be.dto.response.MessageResponse;
import g2.be.dto.response.ServiceRoomResponse;
import g2.be.entity.Department;
import g2.be.entity.Room;
import g2.be.entity.Service;
import g2.be.repository.RoomRepository;
import g2.be.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceRoomService {

    private final ServiceRepository serviceRepository;
    private final RoomRepository roomRepository;
    private final OnDutyRoomAssignmentResolver onDutyRoomAssignmentResolver;

    @Transactional
    public ServiceRoomResponse assignServicesToRoom(String roomId, ServiceRoomRequest request) {
        Room room = findVisibleRoom(roomId);
        validateRoomCanReceiveServices(room);
        requireReplaceConfirmation(room, request);

        room.getServices().clear();

        for (String serviceId : request.getServiceIds()) {
            Service service = serviceRepository.findByServiceIdAndIsDeletedFalse(serviceId)
                    .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu voi ma: " + serviceId));
            validateServiceRoomCompatibility(service, room);
            room.getServices().add(service);
        }

        roomRepository.save(room);
        return buildResponse(room);
    }

    @Transactional(readOnly = true)
    public ServiceRoomResponse getServicesByRoom(String roomId) {
        return buildResponse(findVisibleRoom(roomId));
    }

    @Transactional(readOnly = true)
    public List<ServiceRoomResponse> getRoomsByService(String serviceId) {
        return getRoomsByService(serviceId, null, null);
    }

    @Transactional(readOnly = true)
    public List<ServiceRoomResponse> getRoomsByService(String serviceId, LocalDate date, Long slotId) {
        Service service = serviceRepository.findByServiceIdAndIsDeletedFalse(serviceId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu voi ma: " + serviceId));

        return onDutyRoomAssignmentResolver.findCurrentAssignmentsForService(service, date, slotId)
                .stream()
                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::room)
                .map(this::buildResponse)
                .toList();
    }

    @Transactional
    public MessageResponse removeServiceFromRoom(String roomId, String serviceId) {
        Room room = findVisibleRoom(roomId);
        Service service = serviceRepository.findByServiceIdAndIsDeletedFalse(serviceId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu voi ma: " + serviceId));

        boolean removed = room.getServices().removeIf(item -> item.getServiceId().equals(serviceId));
        if (!removed) {
            throw new RuntimeException(
                    "Khong tim thay mapping dich vu '" + serviceId + "' trong phong '" + roomId + "'");
        }

        roomRepository.save(room);
        return new MessageResponse("Da go dich vu '" + service.getServiceName()
                + "' khoi phong '" + room.getRoomName() + "'");
    }

    @Transactional(readOnly = true)
    public List<ServiceRoomResponse> getAllMappings() {
        return roomRepository.findAllByIsDeletedFalseOrderByRoomIdAsc().stream()
                .filter(this::isOperationalRoom)
                .map(this::buildResponse)
                .toList();
    }

    private void requireReplaceConfirmation(Room room, ServiceRoomRequest request) {
        Set<String> currentServiceIds = room.getServices().stream()
                .filter(service -> !Boolean.TRUE.equals(service.getIsDeleted()))
                .map(Service::getServiceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> requestedServiceIds = new LinkedHashSet<>(request.getServiceIds());

        if (!currentServiceIds.isEmpty()
                && !currentServiceIds.equals(requestedServiceIds)
                && !Boolean.TRUE.equals(request.getConfirmReplaceAll())) {
            throw new RuntimeException(
                    "API gan dich vu-phong se thay the toan bo mapping hien tai. Gui confirmReplaceAll=true de xac nhan.");
        }
    }

    private ServiceRoomResponse buildResponse(Room room) {
        List<ServiceRoomResponse.ServiceInfo> services = room.getServices().stream()
                .filter(service -> !Boolean.TRUE.equals(service.getIsDeleted()))
                .map(service -> ServiceRoomResponse.ServiceInfo.builder()
                        .serviceId(service.getServiceId())
                        .serviceName(service.getServiceName())
                        .build())
                .toList();

        return ServiceRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .services(services)
                .build();
    }

    private void validateRoomCanReceiveServices(Room room) {
        if (!isOperationalRoom(room)) {
            throw new RuntimeException("Phong da bi khoa hoac xoa mem.");
        }
        if (room.getDepartment() == null || room.getDepartment().getDeptType() == null) {
            throw new RuntimeException("Phong phai thuoc khoa hop le truoc khi gan dich vu.");
        }

        Department.DeptType deptType = room.getDepartment().getDeptType();
        if (deptType != Department.DeptType.CLINICAL && deptType != Department.DeptType.PARACLINICAL) {
            throw new RuntimeException("Chi phong thuoc khoa CLINICAL hoac PARACLINICAL moi duoc gan dich vu kham.");
        }
    }

    private void validateServiceRoomCompatibility(Service service, Room room) {
        if (service.getDepartment() == null) {
            throw new RuntimeException("Dich vu '" + service.getServiceName() + "' chua duoc gan vao khoa nao.");
        }
        if (Boolean.TRUE.equals(service.getIsDeleted())) {
            throw new RuntimeException("Dich vu '" + service.getServiceName() + "' da bi xoa mem.");
        }

        Department.DeptType serviceDeptType = service.getDepartment().getDeptType();
        Department.DeptType roomDeptType = room.getDepartment().getDeptType();

        if (roomDeptType == Department.DeptType.CLINICAL && serviceDeptType != Department.DeptType.CLINICAL) {
            throw new RuntimeException("Phong '" + room.getRoomName()
                    + "' thuoc khoa CLINICAL nen khong the gan dich vu khac lam sang '" + service.getServiceName() + "'.");
        }

        if (roomDeptType == Department.DeptType.PARACLINICAL && serviceDeptType != Department.DeptType.PARACLINICAL) {
            throw new RuntimeException("Phong '" + room.getRoomName()
                    + "' thuoc khoa PARACLINICAL nen khong the gan dich vu khac can lam sang '" + service.getServiceName() + "'.");
        }
    }

    private Room findVisibleRoom(String roomId) {
        return roomRepository.findByRoomIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay phong voi ma: " + roomId));
    }

    private boolean isOperationalRoom(Room room) {
        return room != null
                && !Boolean.TRUE.equals(room.getIsDeleted())
                && "ACTIVE".equalsIgnoreCase(room.getStatus())
                && room.getDepartment() != null
                && !Boolean.TRUE.equals(room.getDepartment().getIsDeleted())
                && Boolean.TRUE.equals(room.getDepartment().getIsActive());
    }

}
