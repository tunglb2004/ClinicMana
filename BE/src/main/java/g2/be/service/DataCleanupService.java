package g2.be.service;

import g2.be.dto.request.DataCleanupExecuteRequest;
import g2.be.dto.response.DataCleanupExecutionResponse;
import g2.be.dto.response.DataCleanupIssue;
import g2.be.dto.response.DataCleanupReport;
import g2.be.entity.Appointment;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.DoctorType;
import g2.be.entity.Nurse;
import g2.be.entity.Role;
import g2.be.entity.Room;
import g2.be.entity.StaffSchedule;
import g2.be.entity.User;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.DepartmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.NurseRepository;
import g2.be.repository.RoleRepository;
import g2.be.repository.RoomRepository;
import g2.be.repository.StaffScheduleRepository;
import g2.be.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DataCleanupService {

    private static final String CLEANUP_CONFIRMATION_TEXT = "EXECUTE_DATA_CLEANUP";

    private final AppointmentRepository appointmentRepository;
    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final DoctorRepository doctorRepository;
    private final NurseRepository nurseRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Transactional
    public DataCleanupReport audit() {
        return buildAuditReport();
    }

    @Transactional
    public DataCleanupExecutionResponse executeCleanup() {
        DataCleanupReport before = buildAuditReport();
        List<String> actions = new ArrayList<>();

        normalizeRooms(actions);
        normalizeServiceRoomMappings(actions);
        normalizeDoctors(actions);
        normalizeSchedules(actions);
        normalizeAppointments(actions);

        DataCleanupReport after = buildAuditReport();

        after.getNotes().add("service_room đã được dọn theo department.dept_type; appointment còn hiệu lực cũng được gán lại room/doctor nếu lệch.");

        return DataCleanupExecutionResponse.builder()
                .before(before)
                .after(after)
                .actions(actions)
                .build();
    }

    public void validateExecutionRequest(DataCleanupExecuteRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.getConfirmExecution())) {
            throw new RuntimeException("Cleanup dữ liệu yêu cầu confirmExecution=true trước khi thực thi.");
        }

        if (!CLEANUP_CONFIRMATION_TEXT.equals(request.getConfirmationText())) {
            throw new RuntimeException("confirmationText không hợp lệ. Giá trị bắt buộc: " + CLEANUP_CONFIRMATION_TEXT);
        }

        if (request.getExpectedIssueCount() != null) {
            int currentIssueCount = buildAuditReport().getCounts().getOrDefault("totalIssues", 0);
            if (!request.getExpectedIssueCount().equals(currentIssueCount)) {
                throw new RuntimeException(
                        "Số issue hiện tại đã thay đổi sau lần audit trước. Hãy gọi /audit lại trước khi execute cleanup.");
            }
        }
    }

    private DataCleanupReport buildAuditReport() {
        List<Room> rooms = roomRepository.findAll();
        List<Doctor> doctors = doctorRepository.findAll();
        List<Nurse> nurses = nurseRepository.findAll();
        List<StaffSchedule> schedules = staffScheduleRepository.findAll();
        List<Appointment> appointments = appointmentRepository.findAll();
        Map<Integer, Department> departmentsById = buildDepartmentMap();

        Map<Long, Doctor> doctorByUserId = new HashMap<>();
        for (Doctor doctor : doctors) {
            if (doctor.getUser() != null && doctor.getUser().getUserId() != null) {
                doctorByUserId.put(doctor.getUser().getUserId(), doctor);
            }
        }

        Map<Long, Nurse> nurseByUserId = new HashMap<>();
        for (Nurse nurse : nurses) {
            if (nurse.getUser() != null && nurse.getUser().getUserId() != null) {
                nurseByUserId.put(nurse.getUser().getUserId(), nurse);
            }
        }

        Map<Long, List<StaffSchedule>> schedulesByUserId = buildSchedulesByUserId(schedules);

        List<DataCleanupIssue> issues = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();

        int roomsNeedingNormalization = 0;
        int roomsAmbiguous = 0;
        for (Room room : rooms) {
            String canonicalRoomType = determineCanonicalRoomType(room);
            if (canonicalRoomType == null) {
                if (room.getDepartment() == null || room.getDepartment().getDeptType() == null) {
                    roomsAmbiguous++;
                    issues.add(issue("ROOM", "Room", room.getRoomId(),
                            describeRoom(room),
                            "Department phải tồn tại để dùng làm nguồn sự thật",
                            "Manual review"));
                }
                continue;
            }

            if (!Objects.equals(normalizeText(room.getRoomType()), normalizeText(canonicalRoomType))) {
                roomsNeedingNormalization++;
                issues.add(issue("ROOM", "Room", room.getRoomId(),
                        describeRoom(room),
                        "roomType=" + canonicalRoomType,
                        "Update room_type theo department"));
            }
        }

        int serviceRoomMappingsAudited = 0;
        int serviceRoomMappingsNeedingCleanup = 0;
        int serviceRoomMappingsAmbiguous = 0;
        for (Room room : rooms) {
            for (g2.be.entity.Service service : room.getServices()) {
                serviceRoomMappingsAudited++;

                if (isServiceRoomCompatible(room, service)) {
                    continue;
                }

                if (!canEvaluateServiceRoomMapping(room, service)) {
                    serviceRoomMappingsAmbiguous++;
                    issues.add(issue("SERVICE_ROOM", "RoomServiceMapping",
                            room.getRoomId() + "::" + safe(service != null ? service.getServiceId() : null),
                            describeServiceRoomMapping(room, service),
                            "Room và service phải có department.dept_type hợp lệ trước khi giữ mapping",
                            "Manual review"));
                    continue;
                }

                serviceRoomMappingsNeedingCleanup++;
                issues.add(issue("SERVICE_ROOM", "RoomServiceMapping",
                        room.getRoomId() + "::" + safe(service.getServiceId()),
                        describeServiceRoomMapping(room, service),
                        "room.department.dept_type phải trùng service.department.dept_type",
                        "Remove stale service_room mapping"));
            }
        }

        int doctorsNeedingTypeNormalization = 0;
        int doctorsNeedingRoleNormalization = 0;
        int doctorsWithInvalidRoom = 0;
        int doctorsAmbiguous = 0;
        int doctorsNeedingDepartmentBackfill = 0;
        for (Doctor doctor : doctors) {
            Department effectiveDepartment = resolveEffectiveDoctorDepartment(doctor, schedulesByUserId, departmentsById);
            DoctorType canonicalDoctorType = determineCanonicalDoctorType(effectiveDepartment);
            if (canonicalDoctorType == null) {
                doctorsAmbiguous++;
                issues.add(issue("DOCTOR", "Doctor", doctor.getDoctorId(),
                        describeDoctor(doctor),
                        "Doctor phải thuộc khoa CLINICAL hoặc PARACLINICAL",
                        "Manual review"));
                continue;
            }

            if (doctor.getDepartment() == null && effectiveDepartment != null) {
                doctorsNeedingDepartmentBackfill++;
                issues.add(issue("DOCTOR_DEPARTMENT", "Doctor", doctor.getDoctorId(),
                        describeDoctor(doctor),
                        "deptId=" + effectiveDepartment.getDeptId(),
                        "Backfill doctor.dept_id từ room/schedule history"));
            }

            if (doctor.getDoctorType() != canonicalDoctorType) {
                doctorsNeedingTypeNormalization++;
                issues.add(issue("DOCTOR", "Doctor", doctor.getDoctorId(),
                        describeDoctor(doctor),
                        "doctorType=" + canonicalDoctorType.name(),
                        "Update doctor_type theo department"));
            }

            if (!hasCanonicalDoctorRole(doctor.getUser(), canonicalDoctorType)) {
                doctorsNeedingRoleNormalization++;
                issues.add(issue("USER_ROLE", "User", doctor.getUser().getUserId().toString(),
                        describeUserRoles(doctor.getUser()),
                        canonicalDoctorType == DoctorType.SPECIALIST ? "ROLE_SPECIALISTDOCTOR" : "ROLE_DOCTOR",
                        "Sync user_roles theo doctor_type chuẩn"));
            }

            if (doctor.getRoom() != null && !isDoctorCompatibleWithRoom(doctor, doctor.getRoom(), canonicalDoctorType)) {
                doctorsWithInvalidRoom++;
                issues.add(issue("DOCTOR_ROOM", "Doctor", doctor.getDoctorId(),
                        "roomId=" + doctor.getRoom().getRoomId(),
                        "room compatible với department của bác sĩ",
                        "Clear doctors.room_id"));
            }
        }

        int schedulesWithInvalidRoom = 0;
        int schedulesWithLegacyDeptMismatch = 0;
        int schedulesWithLegacyDateMismatch = 0;
        int schedulesWithLegacyTimeMismatch = 0;
        int schedulesAmbiguous = 0;

        for (StaffSchedule schedule : schedules) {
            if (schedule.getRoom() == null) {
                checkLegacyFields(schedule, issues);
                if (needsLegacyDeptFix(schedule, doctorByUserId, nurseByUserId)) schedulesWithLegacyDeptMismatch++;
                if (!Objects.equals(schedule.getLegacyDate(), schedule.getWorkDate())) schedulesWithLegacyDateMismatch++;
                if (needsLegacyTimeFix(schedule)) schedulesWithLegacyTimeMismatch++;
                continue;
            }

            Doctor doctor = schedule.getUser() != null ? doctorByUserId.get(schedule.getUser().getUserId()) : null;
            Nurse nurse = schedule.getUser() != null ? nurseByUserId.get(schedule.getUser().getUserId()) : null;

            if (doctor == null && nurse == null) {
                schedulesAmbiguous++;
                issues.add(issue("SCHEDULE", "StaffSchedule", schedule.getScheduleId().toString(),
                        describeSchedule(schedule),
                        "Schedule phải thuộc doctor hoặc nurse profile hợp lệ",
                        "Manual review"));
            } else if (doctor != null) {
                DoctorType canonicalDoctorType = determineCanonicalDoctorType(doctor.getDepartment());
                if (canonicalDoctorType == null || !isDoctorCompatibleWithRoom(doctor, schedule.getRoom(), canonicalDoctorType)) {
                    schedulesWithInvalidRoom++;
                    issues.add(issue("SCHEDULE_ROOM", "StaffSchedule", schedule.getScheduleId().toString(),
                            describeSchedule(schedule),
                            "room compatible với doctor.department",
                            "Clear staff_schedules.room_id"));
                }
            } else if (!isNurseCompatibleWithRoom(nurse, schedule.getRoom())) {
                schedulesWithInvalidRoom++;
                issues.add(issue("SCHEDULE_ROOM", "StaffSchedule", schedule.getScheduleId().toString(),
                        describeSchedule(schedule),
                        "room.department phải khớp nurse.department",
                        "Clear staff_schedules.room_id"));
            }

            if (needsLegacyDeptFix(schedule, doctorByUserId, nurseByUserId)) {
                schedulesWithLegacyDeptMismatch++;
                issues.add(issue("SCHEDULE_LEGACY", "StaffSchedule", schedule.getScheduleId().toString(),
                        describeSchedule(schedule) + ", legacyDeptId=" + schedule.getLegacyDeptId(),
                        "legacyDeptId đồng bộ với department hiện tại",
                        "Update legacy dept/date/time fields"));
            }
            if (!Objects.equals(schedule.getLegacyDate(), schedule.getWorkDate())) {
                schedulesWithLegacyDateMismatch++;
            }
            if (needsLegacyTimeFix(schedule)) {
                schedulesWithLegacyTimeMismatch++;
            }
        }

        int appointmentsEligibleForNormalization = 0;
        int appointmentsWithAssignmentDrift = 0;
        int appointmentsAmbiguous = 0;
        for (Appointment appointment : appointments) {
            if (!shouldNormalizeAppointment(appointment)) {
                continue;
            }

            appointmentsEligibleForNormalization++;
            PaymentService.AppointmentAssignment expectedAssignment = paymentService.resolveAppointmentAssignment(appointment);
            if (!hasAppointmentAssignmentMismatch(appointment, expectedAssignment)) {
                continue;
            }

            if (!canSafelyApplyAppointmentAssignment(appointment, expectedAssignment)) {
                appointmentsAmbiguous++;
                issues.add(issue("APPOINTMENT_ASSIGNMENT", "Appointment", appointment.getAppointmentId(),
                        describeAppointmentAssignment(appointment),
                        "Cần tìm được room tương thích đầy đủ cho từng nhóm service trước khi ghi cleanup",
                        "Manual review"));
                continue;
            }

            appointmentsWithAssignmentDrift++;
            issues.add(issue("APPOINTMENT_ASSIGNMENT", "Appointment", appointment.getAppointmentId(),
                    describeAppointmentAssignment(appointment),
                    describeAppointmentAssignment(expectedAssignment),
                    "Reassign appointment room/doctor theo service_room + department"));
        }

        counts.put("roomsAudited", rooms.size());
        counts.put("roomsNeedingNormalization", roomsNeedingNormalization);
        counts.put("roomsAmbiguous", roomsAmbiguous);
        counts.put("serviceRoomMappingsAudited", serviceRoomMappingsAudited);
        counts.put("serviceRoomMappingsNeedingCleanup", serviceRoomMappingsNeedingCleanup);
        counts.put("serviceRoomMappingsAmbiguous", serviceRoomMappingsAmbiguous);
        counts.put("doctorsAudited", doctors.size());
        counts.put("doctorsNeedingTypeNormalization", doctorsNeedingTypeNormalization);
        counts.put("doctorsNeedingDepartmentBackfill", doctorsNeedingDepartmentBackfill);
        counts.put("doctorsNeedingRoleNormalization", doctorsNeedingRoleNormalization);
        counts.put("doctorsWithInvalidRoom", doctorsWithInvalidRoom);
        counts.put("doctorsAmbiguous", doctorsAmbiguous);
        counts.put("nursesAudited", nurses.size());
        counts.put("schedulesAudited", schedules.size());
        counts.put("schedulesWithInvalidRoom", schedulesWithInvalidRoom);
        counts.put("schedulesWithLegacyDeptMismatch", schedulesWithLegacyDeptMismatch);
        counts.put("schedulesWithLegacyDateMismatch", schedulesWithLegacyDateMismatch);
        counts.put("schedulesWithLegacyTimeMismatch", schedulesWithLegacyTimeMismatch);
        counts.put("schedulesAmbiguous", schedulesAmbiguous);
        counts.put("appointmentsAudited", appointments.size());
        counts.put("appointmentsEligibleForNormalization", appointmentsEligibleForNormalization);
        counts.put("appointmentsWithAssignmentDrift", appointmentsWithAssignmentDrift);
        counts.put("appointmentsAmbiguous", appointmentsAmbiguous);
        counts.put("totalIssues", issues.size());

        List<String> notes = new ArrayList<>();
        notes.add("Department là nguồn sự thật cho cleanup này.");
        notes.add("Clinical rooms được chuẩn hóa thành room_type = GENERAL.");
        notes.add("Paraclinical/Admin/Pharmacy rooms giữ room_type hiện có nếu đã không phải GENERAL; nếu đang GENERAL hoặc để trống sẽ được đổi theo department.");
        notes.add("service_room mapping lệch khoa sẽ bị gỡ theo department.dept_type.");
        notes.add("Appointments chưa hoàn tất và còn services sẽ được gán lại doctor/room nếu assignment hiện tại lệch.");
        notes.add("Service orders đã thanh toán có thể lưu assigned_room_id chính thức; cleanup chỉ nên dùng cho dữ liệu cấu trúc/phân công ngoại lệ.");

        return DataCleanupReport.builder()
                .counts(counts)
                .issues(issues)
                .notes(notes)
                .build();
    }

    private void normalizeRooms(List<String> actions) {
        List<Room> changedRooms = new ArrayList<>();
        for (Room room : roomRepository.findAll()) {
            String canonicalRoomType = determineCanonicalRoomType(room);
            if (canonicalRoomType == null) {
                continue;
            }

            if (!Objects.equals(normalizeText(room.getRoomType()), normalizeText(canonicalRoomType))) {
                actions.add("Room " + room.getRoomId() + ": room_type '" + safe(room.getRoomType())
                        + "' -> '" + canonicalRoomType + "'");
                room.setRoomType(canonicalRoomType);
                changedRooms.add(room);
            }
        }

        if (!changedRooms.isEmpty()) {
            roomRepository.saveAll(changedRooms);
        }
    }

    private void normalizeServiceRoomMappings(List<String> actions) {
        List<Room> changedRooms = new ArrayList<>();

        for (Room room : roomRepository.findAll()) {
            if (room.getServices() == null || room.getServices().isEmpty()) {
                continue;
            }

            List<String> invalidServiceIds = room.getServices().stream()
                    .filter(service -> canEvaluateServiceRoomMapping(room, service) && !isServiceRoomCompatible(room, service))
                    .map(g2.be.entity.Service::getServiceId)
                    .filter(Objects::nonNull)
                    .toList();

            if (invalidServiceIds.isEmpty()) {
                continue;
            }

            room.getServices().removeIf(service -> {
                boolean remove = service != null && invalidServiceIds.contains(service.getServiceId());
                if (remove) {
                    actions.add("Room " + room.getRoomId() + ": remove stale service mapping '" + service.getServiceId()
                            + "' (roomDept=" + safe(room.getDepartment() != null ? room.getDepartment().getDeptType() : null)
                            + ", serviceDept=" + safe(service.getDepartment() != null ? service.getDepartment().getDeptType() : null) + ")");
                }
                return remove;
            });
            changedRooms.add(room);
        }

        if (!changedRooms.isEmpty()) {
            roomRepository.saveAll(changedRooms);
        }
    }

    private void normalizeDoctors(List<String> actions) {
        Role clinicalRole = roleRepository.findByRoleName("ROLE_DOCTOR")
                .orElseThrow(() -> new RuntimeException("Thiếu role ROLE_DOCTOR"));
        Role specialistRole = roleRepository.findByRoleName("ROLE_SPECIALISTDOCTOR")
                .orElseThrow(() -> new RuntimeException("Thiếu role ROLE_SPECIALISTDOCTOR"));
        Map<Integer, Department> departmentsById = buildDepartmentMap();
        Map<Long, List<StaffSchedule>> schedulesByUserId = buildSchedulesByUserId(staffScheduleRepository.findAll());

        List<Doctor> changedDoctors = new ArrayList<>();
        List<User> changedUsers = new ArrayList<>();

        for (Doctor doctor : doctorRepository.findAll()) {
            Department effectiveDepartment = resolveEffectiveDoctorDepartment(doctor, schedulesByUserId, departmentsById);
            DoctorType canonicalDoctorType = determineCanonicalDoctorType(effectiveDepartment);
            if (canonicalDoctorType == null) {
                continue;
            }

            boolean doctorChanged = false;
            boolean userChanged = false;

            if (doctor.getDepartment() == null && effectiveDepartment != null) {
                actions.add("Doctor " + doctor.getDoctorId() + ": backfill dept_id -> " + effectiveDepartment.getDeptId());
                doctor.setDepartment(effectiveDepartment);
                doctorChanged = true;
            }

            if (doctor.getDoctorType() != canonicalDoctorType) {
                actions.add("Doctor " + doctor.getDoctorId() + ": doctor_type '" + safe(doctor.getDoctorType())
                        + "' -> '" + canonicalDoctorType.name() + "'");
                doctor.setDoctorType(canonicalDoctorType);
                doctorChanged = true;
            }

            if (doctor.getRoom() != null && !isDoctorCompatibleWithRoom(doctor, doctor.getRoom(), canonicalDoctorType)) {
                actions.add("Doctor " + doctor.getDoctorId() + ": clear incompatible room '" + doctor.getRoom().getRoomId() + "'");
                doctor.setRoom(null);
                doctorChanged = true;
            }

            User user = doctor.getUser();
            if (user != null) {
                Set<Role> roles = user.getRoles();
                if (canonicalDoctorType == DoctorType.SPECIALIST) {
                    boolean removedClinical = roles.removeIf(role -> "ROLE_DOCTOR".equals(role.getRoleName())
                            || "ROLE_CLINICALDOCTOR".equals(role.getRoleName())
                            || "ROLE_CLINICAL_DOCTOR".equals(role.getRoleName()));
                    boolean addedSpecialist = roles.stream().noneMatch(role -> "ROLE_SPECIALISTDOCTOR".equals(role.getRoleName()));
                    if (addedSpecialist) {
                        roles.add(specialistRole);
                    }
                    if (removedClinical || addedSpecialist) {
                        actions.add("User " + user.getUserId() + ": sync roles to ROLE_SPECIALISTDOCTOR");
                        userChanged = true;
                    }
                } else {
                    boolean removedSpecialist = roles.removeIf(role -> "ROLE_SPECIALISTDOCTOR".equals(role.getRoleName())
                            || "ROLE_SPECIALIST_DOCTOR".equals(role.getRoleName()));
                    boolean addedClinical = roles.stream().noneMatch(role -> "ROLE_DOCTOR".equals(role.getRoleName()));
                    if (addedClinical) {
                        roles.add(clinicalRole);
                    }
                    if (removedSpecialist || addedClinical) {
                        actions.add("User " + user.getUserId() + ": sync roles to ROLE_DOCTOR");
                        userChanged = true;
                    }
                }
            }

            if (doctorChanged) {
                changedDoctors.add(doctor);
            }
            if (userChanged && user != null) {
                changedUsers.add(user);
            }
        }

        if (!changedDoctors.isEmpty()) {
            doctorRepository.saveAll(changedDoctors);
        }
        if (!changedUsers.isEmpty()) {
            userRepository.saveAll(changedUsers);
        }
    }

    private void normalizeSchedules(List<String> actions) {
        Map<Long, Doctor> doctorByUserId = new HashMap<>();
        for (Doctor doctor : doctorRepository.findAll()) {
            if (doctor.getUser() != null && doctor.getUser().getUserId() != null) {
                doctorByUserId.put(doctor.getUser().getUserId(), doctor);
            }
        }

        Map<Long, Nurse> nurseByUserId = new HashMap<>();
        for (Nurse nurse : nurseRepository.findAll()) {
            if (nurse.getUser() != null && nurse.getUser().getUserId() != null) {
                nurseByUserId.put(nurse.getUser().getUserId(), nurse);
            }
        }

        List<StaffSchedule> changedSchedules = new ArrayList<>();
        for (StaffSchedule schedule : staffScheduleRepository.findAll()) {
            boolean changed = false;
            Doctor doctor = schedule.getUser() != null ? doctorByUserId.get(schedule.getUser().getUserId()) : null;
            Nurse nurse = schedule.getUser() != null ? nurseByUserId.get(schedule.getUser().getUserId()) : null;

            if (schedule.getRoom() != null) {
                boolean roomCompatible;
                if (doctor != null) {
                    DoctorType canonicalDoctorType = determineCanonicalDoctorType(doctor.getDepartment());
                    roomCompatible = canonicalDoctorType != null && isDoctorCompatibleWithRoom(doctor, schedule.getRoom(), canonicalDoctorType);
                } else if (nurse != null) {
                    roomCompatible = isNurseCompatibleWithRoom(nurse, schedule.getRoom());
                } else {
                    roomCompatible = false;
                }

                if (!roomCompatible) {
                    actions.add("Schedule " + schedule.getScheduleId() + ": clear incompatible room '" + schedule.getRoom().getRoomId() + "'");
                    schedule.setRoom(null);
                    changed = true;
                }
            }

            if (!Objects.equals(schedule.getLegacyDate(), schedule.getWorkDate())) {
                schedule.setLegacyDate(schedule.getWorkDate());
                changed = true;
                actions.add("Schedule " + schedule.getScheduleId() + ": sync legacy date");
            }

            if (schedule.getSlot() != null) {
                LocalTime startTime = schedule.getSlot().getStartTime();
                LocalTime endTime = schedule.getSlot().getEndTime();
                if (!Objects.equals(schedule.getLegacyStartTime(), startTime)) {
                    schedule.setLegacyStartTime(startTime);
                    changed = true;
                    actions.add("Schedule " + schedule.getScheduleId() + ": sync legacy start_time");
                }
                if (!Objects.equals(schedule.getLegacyEndTime(), endTime)) {
                    schedule.setLegacyEndTime(endTime);
                    changed = true;
                    actions.add("Schedule " + schedule.getScheduleId() + ": sync legacy end_time");
                }
            }

            Integer expectedLegacyDeptId = determineExpectedLegacyDeptId(doctor, nurse);
            if (expectedLegacyDeptId != null && !Objects.equals(schedule.getLegacyDeptId(), expectedLegacyDeptId)) {
                schedule.setLegacyDeptId(expectedLegacyDeptId);
                changed = true;
                actions.add("Schedule " + schedule.getScheduleId() + ": sync legacy dept_id -> " + expectedLegacyDeptId);
            }

            if (changed) {
                changedSchedules.add(schedule);
            }
        }

        if (!changedSchedules.isEmpty()) {
            staffScheduleRepository.saveAll(changedSchedules);
        }
    }

    private void normalizeAppointments(List<String> actions) {
        List<Appointment> changedAppointments = new ArrayList<>();

        for (Appointment appointment : appointmentRepository.findAll()) {
            if (!shouldNormalizeAppointment(appointment)) {
                continue;
            }

            PaymentService.AppointmentAssignment expectedAssignment = paymentService.resolveAppointmentAssignment(appointment);
            if (!hasAppointmentAssignmentMismatch(appointment, expectedAssignment)) {
                continue;
            }

            if (!canSafelyApplyAppointmentAssignment(appointment, expectedAssignment)) {
                actions.add("Appointment " + appointment.getAppointmentId()
                        + ": skip reassignment because no fully compatible room could be resolved");
                continue;
            }

            String before = describeAppointmentAssignment(appointment);
            paymentService.reassignAppointmentParticipants(appointment);
            String after = describeAppointmentAssignment(appointment);

            if (!Objects.equals(before, after)) {
                actions.add("Appointment " + appointment.getAppointmentId() + ": " + before + " -> " + after);
                changedAppointments.add(appointment);
            }
        }

        if (!changedAppointments.isEmpty()) {
            appointmentRepository.saveAll(changedAppointments);
        }
    }

    private boolean hasCanonicalDoctorRole(User user, DoctorType doctorType) {
        if (user == null) {
            return false;
        }

        boolean hasDoctorRole = user.getRoles().stream().anyMatch(role -> "ROLE_DOCTOR".equals(role.getRoleName()));
        boolean hasSpecialistRole = user.getRoles().stream().anyMatch(role -> "ROLE_SPECIALISTDOCTOR".equals(role.getRoleName()));

        if (doctorType == DoctorType.SPECIALIST) {
            return hasSpecialistRole && !hasDoctorRole;
        }

        return hasDoctorRole && !hasSpecialistRole;
    }

    private boolean isDoctorCompatibleWithRoom(Doctor doctor, Room room, DoctorType canonicalDoctorType) {
        if (doctor == null || room == null || canonicalDoctorType == null) {
            return false;
        }

        Department department = doctor.getDepartment();
        if (department == null || room.getDepartment() == null) {
            return false;
        }

        if (!Objects.equals(department.getDeptId(), room.getDepartment().getDeptId())) {
            return false;
        }

        boolean generalRoom = isGeneralRoom(room);
        return canonicalDoctorType == DoctorType.SPECIALIST ? !generalRoom : generalRoom;
    }

    private boolean isNurseCompatibleWithRoom(Nurse nurse, Room room) {
        if (nurse == null || room == null || nurse.getDepartment() == null || room.getDepartment() == null) {
            return false;
        }
        return Objects.equals(nurse.getDepartment().getDeptId(), room.getDepartment().getDeptId());
    }

    private DoctorType determineCanonicalDoctorType(Department department) {
        if (department == null || department.getDeptType() == null) {
            return null;
        }

        if (department.getDeptType() == Department.DeptType.CLINICAL) {
            return DoctorType.CLINICAL;
        }
        if (department.getDeptType() == Department.DeptType.PARACLINICAL) {
            return DoctorType.SPECIALIST;
        }
        return null;
    }

    private String determineCanonicalRoomType(Room room) {
        if (room == null || room.getDepartment() == null || room.getDepartment().getDeptType() == null) {
            return null;
        }

        Department.DeptType deptType = room.getDepartment().getDeptType();
        String currentRoomType = normalizeText(room.getRoomType());

        if (deptType == Department.DeptType.CLINICAL) {
            return "GENERAL";
        }

        if (deptType == Department.DeptType.PARACLINICAL) {
            return isBlank(currentRoomType) || "GENERAL".equals(currentRoomType) ? "PARACLINICAL" : room.getRoomType();
        }

        if (deptType == Department.DeptType.ADMINISTRATIVE || deptType == Department.DeptType.PHARMACY) {
            return isBlank(currentRoomType) || "GENERAL".equals(currentRoomType) ? deptType.name() : room.getRoomType();
        }

        return room.getRoomType();
    }

    private boolean isGeneralRoom(Room room) {
        String roomType = normalizeText(room.getRoomType());
        String roomId = normalizeText(room.getRoomId());
        String roomName = Optional.ofNullable(room.getRoomName()).orElse("").toLowerCase(Locale.ROOT);

        return "GENERAL".equals(roomType)
                || roomId.startsWith("DK")
                || roomName.contains("đa khoa")
                || roomName.contains("general");
    }

    private boolean canEvaluateServiceRoomMapping(Room room, g2.be.entity.Service service) {
        return room != null
                && service != null
                && room.getDepartment() != null
                && room.getDepartment().getDeptType() != null
                && service.getDepartment() != null
                && service.getDepartment().getDeptType() != null;
    }

    private boolean isServiceRoomCompatible(Room room, g2.be.entity.Service service) {
        if (!canEvaluateServiceRoomMapping(room, service)) {
            return false;
        }

        Department.DeptType roomDeptType = room.getDepartment().getDeptType();
        Department.DeptType serviceDeptType = service.getDepartment().getDeptType();
        return (roomDeptType == Department.DeptType.CLINICAL || roomDeptType == Department.DeptType.PARACLINICAL)
                && roomDeptType == serviceDeptType;
    }

    private boolean shouldNormalizeAppointment(Appointment appointment) {
        if (appointment == null || appointment.getServices() == null || appointment.getServices().isEmpty()) {
            return false;
        }

        String status = normalizeText(appointment.getStatus());
        return !Set.of("CANCELLED", "COMPLETED").contains(status);
    }

    private boolean canSafelyApplyAppointmentAssignment(Appointment appointment,
                                                        PaymentService.AppointmentAssignment assignment) {
        List<g2.be.entity.Service> clinicalServices = extractAppointmentServicesByType(
                appointment,
                Department.DeptType.CLINICAL);
        List<g2.be.entity.Service> paraclinicalServices = extractAppointmentServicesByType(
                appointment,
                Department.DeptType.PARACLINICAL);

        if (!clinicalServices.isEmpty() && !roomSupportsAllServices(assignment.room(), clinicalServices)) {
            return false;
        }

        if (paraclinicalServices.isEmpty()) {
            return true;
        }

        Room primaryParaclinicalRoom = clinicalServices.isEmpty()
                ? assignment.room()
                : assignment.paraclinicalRoom();
        if (primaryParaclinicalRoom == null) {
            return false;
        }

        return paraclinicalServices.stream()
                .allMatch(service -> roomSupportsService(primaryParaclinicalRoom, service)
                        || hasOperationalMappedRoom(service));
    }

    private boolean hasAppointmentAssignmentMismatch(Appointment appointment,
                                                     PaymentService.AppointmentAssignment assignment) {
        return !Objects.equals(resolveDoctorId(appointment.getDoctor()), resolveDoctorId(assignment.doctor()))
                || !Objects.equals(resolveRoomId(appointment.getRoom()), resolveRoomId(assignment.room()))
                || !Objects.equals(resolveDoctorId(appointment.getParaclinicalDoctor()), resolveDoctorId(assignment.paraclinicalDoctor()))
                || !Objects.equals(resolveRoomId(appointment.getParaclinicalRoom()), resolveRoomId(assignment.paraclinicalRoom()));
    }

    private boolean hasAppointmentServiceType(Appointment appointment, Department.DeptType deptType) {
        return appointment.getServices().stream()
                .map(g2.be.entity.Service::getDepartment)
                .filter(Objects::nonNull)
                .map(Department::getDeptType)
                .anyMatch(type -> type == deptType);
    }

    private List<g2.be.entity.Service> extractAppointmentServicesByType(Appointment appointment,
                                                                        Department.DeptType deptType) {
        if (appointment == null || appointment.getServices() == null) {
            return List.of();
        }

        return appointment.getServices().stream()
                .filter(service -> service != null
                        && service.getDepartment() != null
                        && service.getDepartment().getDeptType() == deptType)
                .toList();
    }

    private boolean roomSupportsAllServices(Room room, List<g2.be.entity.Service> services) {
        if (room == null || services == null || services.isEmpty()) {
            return false;
        }

        return services.stream().allMatch(service -> roomSupportsService(room, service));
    }

    private boolean roomSupportsService(Room room, g2.be.entity.Service service) {
        if (room == null || service == null || service.getServiceId() == null || room.getServices() == null) {
            return false;
        }

        return room.getServices().stream()
                .anyMatch(mapped -> mapped != null && service.getServiceId().equals(mapped.getServiceId()));
    }

    private boolean hasOperationalMappedRoom(g2.be.entity.Service service) {
        return service != null
                && service.getRooms() != null
                && service.getRooms().stream().anyMatch(this::isOperationalRoom);
    }

    private boolean isOperationalRoom(Room room) {
        return room != null
                && !Boolean.TRUE.equals(room.getIsDeleted())
                && "ACTIVE".equalsIgnoreCase(room.getStatus())
                && room.getDepartment() != null
                && !Boolean.TRUE.equals(room.getDepartment().getIsDeleted())
                && Boolean.TRUE.equals(room.getDepartment().getIsActive());
    }

    private String resolveDoctorId(Doctor doctor) {
        return doctor != null ? doctor.getDoctorId() : null;
    }

    private String resolveRoomId(Room room) {
        return room != null ? room.getRoomId() : null;
    }

    private boolean needsLegacyDeptFix(StaffSchedule schedule,
                                       Map<Long, Doctor> doctorByUserId,
                                       Map<Long, Nurse> nurseByUserId) {
        if (schedule.getUser() == null) {
            return false;
        }

        Doctor doctor = doctorByUserId.get(schedule.getUser().getUserId());
        Nurse nurse = nurseByUserId.get(schedule.getUser().getUserId());
        Integer expectedLegacyDeptId = determineExpectedLegacyDeptId(doctor, nurse);
        return expectedLegacyDeptId != null && !Objects.equals(schedule.getLegacyDeptId(), expectedLegacyDeptId);
    }

    private Integer determineExpectedLegacyDeptId(Doctor doctor, Nurse nurse) {
        if (doctor != null && doctor.getDepartment() != null) {
            return doctor.getDepartment().getDeptId();
        }
        if (nurse != null && nurse.getDepartment() != null) {
            return nurse.getDepartment().getDeptId();
        }
        return null;
    }

    private boolean needsLegacyTimeFix(StaffSchedule schedule) {
        if (schedule.getSlot() == null) {
            return false;
        }
        return !Objects.equals(schedule.getLegacyStartTime(), schedule.getSlot().getStartTime())
                || !Objects.equals(schedule.getLegacyEndTime(), schedule.getSlot().getEndTime());
    }

    private void checkLegacyFields(StaffSchedule schedule, List<DataCleanupIssue> issues) {
        if (!Objects.equals(schedule.getLegacyDate(), schedule.getWorkDate())) {
            issues.add(issue("SCHEDULE_LEGACY", "StaffSchedule", schedule.getScheduleId().toString(),
                    describeSchedule(schedule) + ", legacyDate=" + schedule.getLegacyDate(),
                    "legacyDate=" + schedule.getWorkDate(),
                    "Update legacy date"));
        }
        if (needsLegacyTimeFix(schedule)) {
            issues.add(issue("SCHEDULE_LEGACY", "StaffSchedule", schedule.getScheduleId().toString(),
                    describeSchedule(schedule),
                    "legacy start/end time đồng bộ với slot",
                    "Update legacy start_time/end_time"));
        }
    }

    private DataCleanupIssue issue(String category,
                                   String entityType,
                                   String entityId,
                                   String currentValue,
                                   String expectedValue,
                                   String action) {
        return DataCleanupIssue.builder()
                .category(category)
                .entityType(entityType)
                .entityId(entityId)
                .currentValue(currentValue)
                .expectedValue(expectedValue)
                .action(action)
                .build();
    }

    private String describeRoom(Room room) {
        return "roomType=" + safe(room.getRoomType())
                + ", deptId=" + (room.getDepartment() != null ? room.getDepartment().getDeptId() : null)
                + ", deptType=" + (room.getDepartment() != null ? safe(room.getDepartment().getDeptType()) : null);
    }

    private String describeDoctor(Doctor doctor) {
        return "doctorType=" + safe(doctor.getDoctorType())
                + ", deptId=" + (doctor.getDepartment() != null ? doctor.getDepartment().getDeptId() : null)
                + ", roomId=" + (doctor.getRoom() != null ? doctor.getRoom().getRoomId() : null);
    }

    private String describeSchedule(StaffSchedule schedule) {
        return "userId=" + (schedule.getUser() != null ? schedule.getUser().getUserId() : null)
                + ", roomId=" + (schedule.getRoom() != null ? schedule.getRoom().getRoomId() : null)
                + ", workDate=" + schedule.getWorkDate()
                + ", slotId=" + (schedule.getSlot() != null ? schedule.getSlot().getSlotId() : null);
    }

    private String describeServiceRoomMapping(Room room, g2.be.entity.Service service) {
        return "roomId=" + safe(room != null ? room.getRoomId() : null)
                + ", roomDeptType=" + safe(room != null && room.getDepartment() != null ? room.getDepartment().getDeptType() : null)
                + ", serviceId=" + safe(service != null ? service.getServiceId() : null)
                + ", serviceDeptType=" + safe(service != null && service.getDepartment() != null ? service.getDepartment().getDeptType() : null);
    }

    private String describeAppointmentAssignment(Appointment appointment) {
        return "doctor=" + safe(resolveDoctorId(appointment.getDoctor()))
                + ", room=" + safe(resolveRoomId(appointment.getRoom()))
                + ", paraclinicalDoctor=" + safe(resolveDoctorId(appointment.getParaclinicalDoctor()))
                + ", paraclinicalRoom=" + safe(resolveRoomId(appointment.getParaclinicalRoom()));
    }

    private String describeAppointmentAssignment(PaymentService.AppointmentAssignment assignment) {
        return "doctor=" + safe(resolveDoctorId(assignment.doctor()))
                + ", room=" + safe(resolveRoomId(assignment.room()))
                + ", paraclinicalDoctor=" + safe(resolveDoctorId(assignment.paraclinicalDoctor()))
                + ", paraclinicalRoom=" + safe(resolveRoomId(assignment.paraclinicalRoom()));
    }

    private String describeUserRoles(User user) {
        if (user == null) {
            return "roles=[]";
        }
        return user.getRoles().stream()
                .map(Role::getRoleName)
                .sorted()
                .toList()
                .toString();
    }

    private String normalizeText(String value) {
        return Optional.ofNullable(value)
                .map(text -> text.trim().toUpperCase(Locale.ROOT))
                .orElse("");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Map<Integer, Department> buildDepartmentMap() {
        Map<Integer, Department> departmentsById = new HashMap<>();
        for (Department department : departmentRepository.findAll()) {
            departmentsById.put(department.getDeptId(), department);
        }
        return departmentsById;
    }

    private Map<Long, List<StaffSchedule>> buildSchedulesByUserId(List<StaffSchedule> schedules) {
        Map<Long, List<StaffSchedule>> schedulesByUserId = new HashMap<>();
        for (StaffSchedule schedule : schedules) {
            if (schedule.getUser() == null || schedule.getUser().getUserId() == null) {
                continue;
            }
            schedulesByUserId.computeIfAbsent(schedule.getUser().getUserId(), ignored -> new ArrayList<>())
                    .add(schedule);
        }
        return schedulesByUserId;
    }

    private Department resolveEffectiveDoctorDepartment(Doctor doctor,
                                                       Map<Long, List<StaffSchedule>> schedulesByUserId,
                                                       Map<Integer, Department> departmentsById) {
        if (doctor.getDepartment() != null) {
            return doctor.getDepartment();
        }

        Map<Integer, Integer> deptFrequency = new HashMap<>();
        if (doctor.getRoom() != null && doctor.getRoom().getDepartment() != null && doctor.getRoom().getDepartment().getDeptId() != null) {
            deptFrequency.merge(doctor.getRoom().getDepartment().getDeptId(), 1, Integer::sum);
        }

        List<StaffSchedule> schedules = doctor.getUser() != null
                ? schedulesByUserId.getOrDefault(doctor.getUser().getUserId(), List.of())
                : List.of();
        for (StaffSchedule schedule : schedules) {
            if (schedule.getRoom() != null && schedule.getRoom().getDepartment() != null && schedule.getRoom().getDepartment().getDeptId() != null) {
                deptFrequency.merge(schedule.getRoom().getDepartment().getDeptId(), 1, Integer::sum);
                continue;
            }
            if (schedule.getLegacyDeptId() != null) {
                deptFrequency.merge(schedule.getLegacyDeptId(), 1, Integer::sum);
            }
        }

        if (deptFrequency.size() != 1) {
            return null;
        }

        Integer deptId = deptFrequency.keySet().iterator().next();
        Department department = departmentsById.get(deptId);
        if (department == null) {
            return null;
        }

        return determineCanonicalDoctorType(department) != null ? department : null;
    }

    private String safe(Object value) {
        return value == null ? "null" : value.toString();
    }
}
