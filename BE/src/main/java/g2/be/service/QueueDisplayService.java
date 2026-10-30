package g2.be.service;

import g2.be.dto.response.LobbyDashboardResponse;
import g2.be.dto.response.RoomQueueBoardResponse;
import g2.be.entity.Appointment;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Invoice;
import g2.be.entity.Room;
import g2.be.entity.ServiceOrder;
import g2.be.entity.StaffSchedule;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.RoomRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QueueDisplayService {

    private static final int MAX_ROOM_SERVICE_ORDERS = 500;

    private final RoomRepository roomRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final OnDutyRoomAssignmentResolver onDutyRoomAssignmentResolver;

    @Transactional(readOnly = true)
    public LobbyDashboardResponse getLobbyDashboard() {
        List<Room> activeRooms = roomRepository.findByStatusAndIsDeletedFalse("ACTIVE").stream()
                .filter(this::isOperationalRoom)
                .toList();

        int totalWaiting = 0;
        int totalInProgress = 0;
        List<LobbyDashboardResponse.RoomOverview> roomOverviews = new ArrayList<>();

        for (Room room : activeRooms) {
            RoomQueueSnapshot snapshot = buildRoomQueueSnapshot(room);
            totalWaiting += snapshot.totalWaiting();
            totalInProgress += snapshot.totalInProgress();

            roomOverviews.add(LobbyDashboardResponse.RoomOverview.builder()
                    .roomId(room.getRoomId())
                    .roomName(room.getRoomName())
                    .roomType(room.getRoomType())
                    .location(room.getLocation())
                    .departmentName(room.getDepartment() != null ? room.getDepartment().getDeptName() : null)
                    .status(room.getStatus())
                    .currentQueueLength(snapshot.totalWaiting())
                    .baseAvgTime(snapshot.baseAvgTime())
                    .estimatedWaitMinutes(snapshot.estimatedWaitMinutes())
                    .doctorName(snapshot.doctorName())
                    .currentPatient(snapshot.currentPatient())
                    .build());
        }

        roomOverviews.sort(Comparator
                .comparing((LobbyDashboardResponse.RoomOverview room) -> room.getCurrentPatient() == null)
                .thenComparing((LobbyDashboardResponse.RoomOverview room) -> room.getCurrentQueueLength() == 0)
                .thenComparing(LobbyDashboardResponse.RoomOverview::getCurrentQueueLength, Comparator.reverseOrder())
                .thenComparing(LobbyDashboardResponse.RoomOverview::getRoomName, Comparator.nullsLast(String::compareToIgnoreCase)));

        return LobbyDashboardResponse.builder()
                .rooms(roomOverviews)
                .totalWaiting(totalWaiting)
                .totalInProgress(totalInProgress)
                .build();
    }

    @Transactional(readOnly = true)
    public RoomQueueBoardResponse getRoomQueueBoard(String roomId) {
        Room room = roomRepository.findByRoomIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay phong: " + roomId));
        if (!isOperationalRoom(room)) {
            throw new RuntimeException("Phong da bi khoa hoac xoa mem: " + roomId);
        }

        RoomQueueSnapshot snapshot = buildRoomQueueSnapshot(room);

        return RoomQueueBoardResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .roomType(room.getRoomType())
                .location(room.getLocation())
                .departmentName(room.getDepartment() != null ? room.getDepartment().getDeptName() : null)
                .doctorName(snapshot.doctorName())
                .currentPatient(snapshot.currentQueueEntry())
                .waitingList(snapshot.waitingList())
                .totalWaiting(snapshot.totalWaiting())
                .estimatedWaitMinutes(snapshot.estimatedWaitMinutes())
                .build();
    }

    private RoomQueueSnapshot buildRoomQueueSnapshot(Room room) {
        int avgTime = room.getBaseAvgTime() != null ? room.getBaseAvgTime() : 15;
        List<Appointment> queue = appointmentRepository.findActiveQueueByRoomId(
                room.getRoomId(),
                ClinicTime.today(),
                ClinicTime.now());
        List<ServiceOrder> serviceOrderQueue = resolveActiveServiceOrderQueue(room);

        Appointment currentAppointment = queue.stream()
                .filter(this::isInProgress)
                .findFirst()
                .orElse(null);
        int totalInProgress = (int) queue.stream().filter(this::isInProgress).count();
        List<Appointment> waitingAppointments = queue.stream()
                .filter(this::isWaiting)
                .toList();

        boolean roomBusy = currentAppointment != null;
        LobbyDashboardResponse.CurrentPatient currentPatient = toLobbyCurrentPatient(currentAppointment);
        RoomQueueBoardResponse.QueueEntry currentQueueEntry = toQueueEntry(currentAppointment, 0, 0, "IN_PROGRESS");

        List<RoomQueueBoardResponse.QueueEntry> waitingList = new ArrayList<>();
        for (int index = 0; index < waitingAppointments.size(); index++) {
            Appointment waitingAppointment = waitingAppointments.get(index);
            int position = index + 1;
            int estimatedWait = avgTime * (roomBusy ? position : Math.max(0, position - 1));
            waitingList.add(toQueueEntry(waitingAppointment, position, estimatedWait, "WAITING"));
        }

        for (int index = 0; index < serviceOrderQueue.size(); index++) {
            ServiceOrder serviceOrder = serviceOrderQueue.get(index);
            int position = waitingList.size() + 1;
            int estimatedWait = avgTime * (roomBusy ? position : Math.max(0, position - 1));
            RoomQueueBoardResponse.QueueEntry queueEntry = toQueueEntry(serviceOrder, position, estimatedWait, "WAITING");
            if (queueEntry != null) {
                waitingList.add(queueEntry);
            }
        }

        int totalWaiting = waitingList.size();
        int estimatedWaitMinutes = avgTime * (roomBusy ? totalWaiting : Math.max(0, totalWaiting - 1));

        return new RoomQueueSnapshot(
                resolveDisplayDoctorName(room, queue, serviceOrderQueue),
                avgTime,
                currentPatient,
                currentQueueEntry,
                waitingList,
                totalWaiting,
                totalInProgress,
                estimatedWaitMinutes);
    }

    private LobbyDashboardResponse.CurrentPatient toLobbyCurrentPatient(Appointment appointment) {
        if (appointment == null || appointment.getPatient() == null || appointment.getPatient().getUser() == null) {
            return null;
        }

        return LobbyDashboardResponse.CurrentPatient.builder()
                .appointmentId(appointment.getAppointmentId())
                .patientName(appointment.getPatient().getUser().getFullName())
                .patientId(appointment.getPatient().getPatientId())
                .build();
    }

    private RoomQueueBoardResponse.QueueEntry toQueueEntry(
            Appointment appointment,
            int queuePosition,
            int estimatedWaitMinutes,
            String status) {
        if (appointment == null || appointment.getPatient() == null || appointment.getPatient().getUser() == null) {
            return null;
        }

        return RoomQueueBoardResponse.QueueEntry.builder()
                .appointmentId(appointment.getAppointmentId())
                .patientName(appointment.getPatient().getUser().getFullName())
                .patientId(appointment.getPatient().getPatientId())
                .queuePosition(queuePosition)
                .priorityScore(appointment.getPriorityScore())
                .waitingSince(appointment.getWaitingSince())
                .estimatedWaitMinutes(estimatedWaitMinutes)
                .status(status)
                .build();
    }

    private RoomQueueBoardResponse.QueueEntry toQueueEntry(
            ServiceOrder serviceOrder,
            int queuePosition,
            int estimatedWaitMinutes,
            String status) {
        if (serviceOrder == null
                || serviceOrder.getMedicalRecord() == null
                || serviceOrder.getMedicalRecord().getPatient() == null
                || serviceOrder.getMedicalRecord().getPatient().getUser() == null) {
            return null;
        }

        Appointment appointment = serviceOrder.getMedicalRecord().getAppointment();
        String queueId = appointment != null && appointment.getAppointmentId() != null
                ? appointment.getAppointmentId()
                : serviceOrder.getOrderId();

        return RoomQueueBoardResponse.QueueEntry.builder()
                .appointmentId(queueId)
                .patientName(serviceOrder.getMedicalRecord().getPatient().getUser().getFullName())
                .patientId(serviceOrder.getMedicalRecord().getPatient().getPatientId())
                .queuePosition(queuePosition)
                .priorityScore(appointment != null ? appointment.getPriorityScore() : null)
                .waitingSince(resolveServiceOrderWaitingSince(serviceOrder))
                .estimatedWaitMinutes(estimatedWaitMinutes)
                .status(status)
                .build();
    }

    private LocalDateTime resolveServiceOrderWaitingSince(ServiceOrder serviceOrder) {
        if (serviceOrder == null) {
            return null;
        }
        String invoiceId = serviceOrder.getInvoiceId();
        if (invoiceId == null || invoiceId.isBlank()) {
            return serviceOrder.getCreatedAt();
        }
        return invoiceRepository.findById(invoiceId)
                .map(this::resolveInvoicePaidTime)
                .orElse(serviceOrder.getCreatedAt());
    }

    private LocalDateTime resolveInvoicePaidTime(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        if (invoice.getPaidAt() != null) {
            return invoice.getPaidAt();
        }
        return invoice.getCreatedAt();
    }

    private String resolveDisplayDoctorName(Room room, List<Appointment> queue, List<ServiceOrder> serviceOrderQueue) {
        Doctor scheduledDoctor = resolveScheduledDoctor(room.getRoomId());
        if (scheduledDoctor != null && scheduledDoctor.getUser() != null) {
            return scheduledDoctor.getUser().getFullName();
        }

        for (Appointment appointment : queue) {
            Doctor appointmentDoctor = appointment.getDoctor();
            if (appointmentDoctor != null && appointmentDoctor.getUser() != null) {
                return appointmentDoctor.getUser().getFullName();
            }

            Doctor paraclinicalDoctor = appointment.getParaclinicalDoctor();
            if (paraclinicalDoctor != null && paraclinicalDoctor.getUser() != null) {
                return paraclinicalDoctor.getUser().getFullName();
            }
        }

        for (ServiceOrder serviceOrder : serviceOrderQueue) {
            Doctor assignedDoctor = resolveAssignedDoctor(serviceOrder);
            if (assignedDoctor != null && assignedDoctor.getUser() != null) {
                return assignedDoctor.getUser().getFullName();
            }
        }

        return null;
    }

    private List<ServiceOrder> resolveActiveServiceOrderQueue(Room room) {
        List<ServiceOrder> candidateOrders = serviceOrderRepository
                .findByRoomIdsAndDate(
                        List.of(room.getRoomId()),
                        ClinicTime.today(),
                        ClinicTime.now(),
                        null,
                        PageRequest.of(0, MAX_ROOM_SERVICE_ORDERS))
                .getContent();

        List<ServiceOrder> sortedOrders = candidateOrders.stream()
                .filter(this::isQueueVisibleServiceOrder)
                .filter(order -> room.getRoomId().equals(resolveAssignedRoomId(order)))
                .filter(order -> !isAlreadyRepresentedByAppointmentQueue(order, room.getRoomId()))
                .toList();

        Map<String, ServiceOrder> uniqueOrders = new LinkedHashMap<>();
        for (ServiceOrder order : sortedOrders) {
            uniqueOrders.putIfAbsent(resolveServiceOrderQueueKey(order), order);
        }

        return new ArrayList<>(uniqueOrders.values());
    }

    private boolean isQueueVisibleServiceOrder(ServiceOrder serviceOrder) {
        return serviceOrder != null
                && !"CANCELLED".equalsIgnoreCase(serviceOrder.getStatus())
                && !"COMPLETED".equalsIgnoreCase(serviceOrder.getStatus())
                && "PAID".equalsIgnoreCase(serviceOrder.getStatus());
    }

    private boolean isAlreadyRepresentedByAppointmentQueue(ServiceOrder serviceOrder, String roomId) {
        Appointment appointment = resolveAppointment(serviceOrder);
        if (appointment == null || appointment.getAppointmentDate() == null) {
            return false;
        }

        return ClinicTime.today().equals(appointment.getAppointmentDate())
                && roomId.equals(resolvePrimaryAppointmentRoomId(appointment))
                && (isWaiting(appointment) || isInProgress(appointment));
    }

    private String resolveServiceOrderQueueKey(ServiceOrder serviceOrder) {
        Appointment appointment = resolveAppointment(serviceOrder);
        if (appointment != null && appointment.getAppointmentId() != null && !appointment.getAppointmentId().isBlank()) {
            return "APT:" + appointment.getAppointmentId();
        }

        if (serviceOrder.getMedicalRecord() != null
                && serviceOrder.getMedicalRecord().getRecordId() != null
                && !serviceOrder.getMedicalRecord().getRecordId().isBlank()) {
            return "REC:" + serviceOrder.getMedicalRecord().getRecordId();
        }

        return "ORD:" + serviceOrder.getOrderId();
    }

    private String resolveAssignedRoomId(ServiceOrder serviceOrder) {
        Room assignedRoom = resolveAssignedRoom(serviceOrder);
        return assignedRoom != null ? assignedRoom.getRoomId() : null;
    }

    private Room resolveAssignedRoom(ServiceOrder serviceOrder) {
        if (serviceOrder == null || serviceOrder.getService() == null) {
            return null;
        }

        Appointment appointment = resolveAppointment(serviceOrder);
        if (roomCanReceiveService(serviceOrder.getAssignedRoom(), serviceOrder.getService(), appointment)) {
            return serviceOrder.getAssignedRoom();
        }

        if (appointment != null) {
            if (roomCanReceiveService(appointment.getParaclinicalRoom(), serviceOrder.getService(), appointment)) {
                return appointment.getParaclinicalRoom();
            }

            if (roomCanReceiveService(appointment.getRoom(), serviceOrder.getService(), appointment)) {
                return appointment.getRoom();
            }
        }

        return null;
    }

    private Doctor resolveAssignedDoctor(ServiceOrder serviceOrder) {
        if (serviceOrder == null || serviceOrder.getService() == null) {
            return null;
        }

        Room assignedRoom = resolveAssignedRoom(serviceOrder);
        return findOnDutyAssignmentForRoom(
                serviceOrder.getService(),
                assignedRoom,
                resolveAppointment(serviceOrder))
                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::doctor)
                .orElse(null);
    }

    private boolean roomCanReceiveService(Room room, g2.be.entity.Service service, Appointment appointment) {
        return findOnDutyAssignmentForRoom(service, room, appointment).isPresent();
    }

    private Optional<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> findOnDutyAssignmentForRoom(
            g2.be.entity.Service service,
            Room room,
            Appointment appointment) {
        if (service == null || room == null || room.getRoomId() == null) {
            return Optional.empty();
        }

        if (appointment != null) {
            return onDutyRoomAssignmentResolver.findAssignmentForRoomAt(
                    service,
                    room,
                    appointment.getAppointmentDate(),
                    appointment.getAppointmentTime());
        }

        return onDutyRoomAssignmentResolver.findCurrentAssignmentForRoom(service, room, null, null);
    }

    private Appointment resolveAppointment(ServiceOrder serviceOrder) {
        return serviceOrder != null && serviceOrder.getMedicalRecord() != null
                ? serviceOrder.getMedicalRecord().getAppointment()
                : null;
    }

    private String resolvePrimaryAppointmentRoomId(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        if (appointment.getRoom() != null && appointment.getRoom().getRoomId() != null) {
            return appointment.getRoom().getRoomId();
        }

        List<g2.be.entity.Service> schedulingServices = extractSchedulingServices(appointment.getServices());
        if (schedulingServices.isEmpty()) {
            return null;
        }

        return onDutyRoomAssignmentResolver
                .findAssignmentsForServicesAt(
                        schedulingServices,
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime(),
                        true)
                .stream()
                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::room)
                .filter(Objects::nonNull)
                .map(Room::getRoomId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<g2.be.entity.Service> extractSchedulingServices(List<g2.be.entity.Service> services) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }

        List<g2.be.entity.Service> clinicalServices = services.stream()
                .filter(service -> service.getDepartment() != null
                        && service.getDepartment().getDeptType() == Department.DeptType.CLINICAL)
                .toList();
        if (!clinicalServices.isEmpty()) {
            return clinicalServices;
        }

        return services.stream()
                .filter(service -> service.getDepartment() != null
                        && service.getDepartment().getDeptType() == Department.DeptType.PARACLINICAL)
                .toList();
    }

    private Doctor resolveScheduledDoctor(String roomId) {
        List<StaffSchedule> schedules = staffScheduleRepository
                .findByRoom_RoomIdAndWorkDateOrderBySlot_StartTimeAsc(roomId, ClinicTime.today());
        if (schedules.isEmpty()) {
            return null;
        }

        java.time.LocalTime now = ClinicTime.now().toLocalTime();
        Doctor currentShiftDoctor = schedules.stream()
            .filter(schedule -> isCurrentShiftSchedule(now, schedule))
            .map(this::doctorFromSchedule)
            .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (currentShiftDoctor != null) {
            return currentShiftDoctor;
        }

        return null;
    }

    private boolean isCurrentShiftSchedule(java.time.LocalTime now, StaffSchedule schedule) {
        return schedule != null
                && schedule.getSlot() != null
                && schedule.getSlot().getStartTime() != null
                && schedule.getSlot().getEndTime() != null
                && !now.isBefore(schedule.getSlot().getStartTime())
                && now.isBefore(schedule.getSlot().getEndTime());
    }

    private Doctor doctorFromSchedule(StaffSchedule schedule) {
        if (schedule == null || schedule.getUser() == null) {
            return null;
        }

        return schedule.getUser().getDoctor();
    }

    private boolean isWaiting(Appointment appointment) {
        return appointment != null && "WAITING".equalsIgnoreCase(appointment.getStatus());
    }

    private boolean isInProgress(Appointment appointment) {
        return appointment != null && "IN_PROGRESS".equalsIgnoreCase(appointment.getStatus());
    }

    private boolean isOperationalRoom(Room room) {
        return room != null
                && !Boolean.TRUE.equals(room.getIsDeleted())
                && "ACTIVE".equalsIgnoreCase(room.getStatus())
                && room.getDepartment() != null
                && !Boolean.TRUE.equals(room.getDepartment().getIsDeleted())
                && Boolean.TRUE.equals(room.getDepartment().getIsActive());
    }

    private record RoomQueueSnapshot(
            String doctorName,
            int baseAvgTime,
            LobbyDashboardResponse.CurrentPatient currentPatient,
            RoomQueueBoardResponse.QueueEntry currentQueueEntry,
            List<RoomQueueBoardResponse.QueueEntry> waitingList,
            int totalWaiting,
            int totalInProgress,
            int estimatedWaitMinutes) {
    }
}
