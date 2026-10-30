package g2.be.service;

import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Room;
import g2.be.entity.Service;
import g2.be.entity.StaffSchedule;
import g2.be.entity.WorkingTimeSlot;
import g2.be.repository.DoctorRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.repository.WorkingTimeSlotRepository;
import g2.be.util.ClinicTime;
import g2.be.util.DoctorDepartmentClassifier;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class OnDutyRoomAssignmentResolver {

    private final StaffScheduleRepository staffScheduleRepository;
    private final WorkingTimeSlotRepository workingTimeSlotRepository;
    private final DoctorRepository doctorRepository;

    public record RoomDoctorAssignment(Room room, Doctor doctor) {
    }

    private record ScheduleContext(LocalDate workDate, WorkingTimeSlot slot) {
    }

    public List<RoomDoctorAssignment> findCurrentAssignmentsForService(
            Service service,
            LocalDate requestedDate,
            Long requestedSlotId) {
        return resolveCurrentDefaultContext(requestedDate, requestedSlotId)
                .map(context -> findAssignmentsForService(service, context.workDate(), context.slot().getSlotId()))
                .orElse(List.of());
    }

    public List<RoomDoctorAssignment> findCurrentAssignmentsForServices(
            List<Service> services,
            LocalDate requestedDate,
            Long requestedSlotId,
            boolean requireAllServices) {
        return resolveCurrentDefaultContext(requestedDate, requestedSlotId)
                .map(context -> findAssignmentsForServices(
                        services,
                        context.workDate(),
                        context.slot().getSlotId(),
                        requireAllServices))
                .orElse(List.of());
    }

    public List<RoomDoctorAssignment> findAssignmentsForServiceAt(
            Service service,
            LocalDate workDate,
            LocalTime appointmentTime) {
        return resolveContextAt(workDate, appointmentTime)
                .map(context -> findAssignmentsForService(service, context.workDate(), context.slot().getSlotId()))
                .orElse(List.of());
    }

    public List<RoomDoctorAssignment> findAssignmentsForServicesAt(
            List<Service> services,
            LocalDate workDate,
            LocalTime appointmentTime,
            boolean requireAllServices) {
        return resolveContextAt(workDate, appointmentTime)
                .map(context -> findAssignmentsForServices(
                        services,
                        context.workDate(),
                        context.slot().getSlotId(),
                        requireAllServices))
                .orElse(List.of());
    }

    public List<RoomDoctorAssignment> findAssignmentsForService(
            Service service,
            LocalDate workDate,
            Long slotId) {
        return findAssignmentsForServices(
                service == null ? List.of() : List.of(service),
                workDate,
                slotId,
                true);
    }

    public List<RoomDoctorAssignment> findAssignmentsForServices(
            List<Service> services,
            LocalDate workDate,
            Long slotId,
            boolean requireAllServices) {
        List<Service> requestedServices = normalizeServices(services);
        if (requestedServices.isEmpty() || workDate == null || slotId == null) {
            return List.of();
        }

        List<Room> sourceRooms = sourceRoomsForServices(requestedServices, requireAllServices);
        if (sourceRooms.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, RoomDoctorAssignment> assignmentsByRoom = new LinkedHashMap<>();
        for (Room room : sourceRooms) {
            if (!isOperationalRoom(room)
                    || !roomMatchesServiceDepartments(room, requestedServices)
                    || !roomSupportsServices(room, requestedServices, requireAllServices)) {
                continue;
            }

            staffScheduleRepository.findByRoom_RoomIdAndWorkDateOrderBySlot_StartTimeAsc(
                            room.getRoomId(),
                            workDate)
                    .stream()
                    .filter(schedule -> schedule.getSlot() != null
                            && slotId.equals(schedule.getSlot().getSlotId()))
                    .map(this::resolveScheduleDoctor)
                    .flatMap(Optional::stream)
                    .filter(doctor -> doctorMatchesServices(doctor, requestedServices))
                    .findFirst()
                    .ifPresent(doctor -> assignmentsByRoom.putIfAbsent(
                            room.getRoomId(),
                            new RoomDoctorAssignment(room, doctor)));
        }

        return new ArrayList<>(assignmentsByRoom.values());
    }

    public Optional<RoomDoctorAssignment> findAssignmentForRoomAt(
            Service service,
            Room room,
            LocalDate workDate,
            LocalTime appointmentTime) {
        if (room == null || room.getRoomId() == null) {
            return Optional.empty();
        }

        return findAssignmentsForServiceAt(service, workDate, appointmentTime).stream()
                .filter(assignment -> assignment.room() != null
                        && room.getRoomId().equals(assignment.room().getRoomId()))
                .findFirst();
    }

    public Optional<RoomDoctorAssignment> findCurrentAssignmentForRoom(
            Service service,
            Room room,
            LocalDate requestedDate,
            Long requestedSlotId) {
        if (room == null || room.getRoomId() == null) {
            return Optional.empty();
        }

        return findCurrentAssignmentsForService(service, requestedDate, requestedSlotId).stream()
                .filter(assignment -> assignment.room() != null
                        && room.getRoomId().equals(assignment.room().getRoomId()))
                .findFirst();
    }

    public Optional<WorkingTimeSlot> findSlotByAppointmentTime(LocalTime appointmentTime) {
        if (appointmentTime == null) {
            return Optional.empty();
        }

        Optional<WorkingTimeSlot> exactSlot =
                workingTimeSlotRepository.findByStartTimeAndIsActiveTrueAndIsDeletedFalse(appointmentTime);
        if (exactSlot.isPresent()) {
            return exactSlot;
        }

        return workingTimeSlotRepository.findActiveSlotsContainingTime(appointmentTime)
                .stream()
                .findFirst();
    }

    private Optional<ScheduleContext> resolveCurrentDefaultContext(LocalDate requestedDate, Long requestedSlotId) {
        if (requestedDate == null || requestedSlotId == null) {
            LocalDate today = ClinicTime.today();
            return findSlotByAppointmentTime(ClinicTime.now().toLocalTime())
                    .map(slot -> new ScheduleContext(today, slot));
        }

        return workingTimeSlotRepository.findBySlotIdAndIsDeletedFalse(requestedSlotId)
                .filter(slot -> Boolean.TRUE.equals(slot.getIsActive()))
                .map(slot -> new ScheduleContext(requestedDate, slot));
    }

    private Optional<ScheduleContext> resolveContextAt(LocalDate workDate, LocalTime appointmentTime) {
        if (workDate == null || appointmentTime == null) {
            return Optional.empty();
        }

        return findSlotByAppointmentTime(appointmentTime)
                .map(slot -> new ScheduleContext(workDate, slot));
    }

    private Optional<Doctor> resolveScheduleDoctor(StaffSchedule schedule) {
        if (schedule == null || schedule.getUser() == null || schedule.getUser().getUserId() == null) {
            return Optional.empty();
        }

        if (schedule.getUser().getDoctor() != null) {
            return Optional.of(schedule.getUser().getDoctor());
        }

        return doctorRepository.findByUser_UserId(schedule.getUser().getUserId());
    }

    private List<Service> normalizeServices(List<Service> services) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }

        return services.stream()
                .filter(service -> service != null
                        && service.getServiceId() != null
                        && service.getDepartment() != null
                        && service.getDepartment().getDeptType() != null
                        && !Boolean.TRUE.equals(service.getIsDeleted())
                        && Boolean.TRUE.equals(service.getIsActive()))
                .distinct()
                .toList();
    }

    private List<Room> sourceRoomsForServices(List<Service> services, boolean requireAllServices) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, Room> roomsById = new LinkedHashMap<>();
        Service firstService = services.get(0);
        if (firstService.getRooms() != null) {
            firstService.getRooms().stream()
                    .filter(room -> room != null && room.getRoomId() != null)
                    .forEach(room -> roomsById.putIfAbsent(room.getRoomId(), room));
        }

        return roomsById.values().stream()
                .filter(room -> roomSupportsServices(room, services, requireAllServices))
                .toList();
    }

    private boolean roomSupportsServices(Room room, List<Service> services, boolean requireAllServices) {
        if (room == null || services == null || services.isEmpty()) {
            return false;
        }

        return requireAllServices
                ? services.stream().allMatch(service -> roomSupportsService(room, service))
                : services.stream().anyMatch(service -> roomSupportsService(room, service));
    }

    private boolean roomSupportsService(Room room, Service service) {
        if (room == null || service == null || service.getServiceId() == null || room.getRoomId() == null) {
            return false;
        }

        if (service.getRooms() != null
                && service.getRooms().stream()
                .filter(Objects::nonNull)
                .anyMatch(candidate -> room.getRoomId().equals(candidate.getRoomId()))) {
            return true;
        }

        return room.getServices() != null
                && room.getServices().stream()
                .filter(Objects::nonNull)
                .anyMatch(mapped -> service.getServiceId().equals(mapped.getServiceId()));
    }

    private boolean roomMatchesServiceDepartments(Room room, List<Service> services) {
        if (room == null || room.getDepartment() == null || room.getDepartment().getDeptType() == null) {
            return false;
        }

        Department.DeptType roomDeptType = room.getDepartment().getDeptType();
        return services.stream()
                .allMatch(service -> service.getDepartment() != null
                        && service.getDepartment().getDeptType() == roomDeptType);
    }

    private boolean doctorMatchesServices(Doctor doctor, List<Service> services) {
        if (doctor == null || services == null || services.isEmpty()) {
            return false;
        }

        return services.stream().allMatch(service -> {
            Department.DeptType serviceDeptType = service.getDepartment() != null
                    ? service.getDepartment().getDeptType()
                    : null;
            return DoctorDepartmentClassifier.matchesDeptType(doctor, serviceDeptType);
        });
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
