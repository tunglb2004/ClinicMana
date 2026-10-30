package g2.be.dto.response;

import g2.be.entity.Appointment;
import g2.be.entity.Doctor;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Room;
import g2.be.entity.ServiceOrder;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDetailResponse {

    // ── Appointment info ──────────────────────────────────────────────────────
    private String appointmentId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String status;
    private Integer queueNumber;
    private String notes;
    private String cancelReason;
    private LocalDateTime createdAt;
    private String visitType;
    private String followUpSourceRecordId;

    // ── Patient info ──────────────────────────────────────────────────────────
    private String patientId;
    private String patientName;
    private String patientPhone;
    private LocalDate patientDateOfBirth;
    private String patientBloodType;
    private String patientHealthInsuranceNumber;
    private String patientAllergies;
    private String patientBackgroundDisease;

    // ── Doctor info ───────────────────────────────────────────────────────────
    private Long doctorId;
    private String doctorCode;
    private String doctorName;
    private String departmentName;
    private String roomId;
    private String roomName;
    private String specialization;
    private String degree;

    // ── Services ─────────────────────────────────────────────────────────────
    private List<ServiceInfo> services;

    // ── Medical record (if exists) ───────────────────────────────────────────
    private MedicalRecordInfo medicalRecord;
    private MedicalRecordInfo followUpSourceRecord;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceInfo {
        private String serviceId;
        private String serviceName;
        private BigDecimal price;
        private String departmentType;
        private String assignedRoomId;
        private String assignedRoomName;
        private String assignedDoctorId;
        private String assignedDoctorName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicalRecordInfo {
        private String recordId;
        private String symptoms;
        private String diagnosis;
        private String doctorNotes;
        private String recordStatus;
        private LocalDateTime recordCreatedAt;
        private String visitType;
        private String parentRecordId;
        private String rootRecordId;
    }

        public static AppointmentDetailResponse fromEntity(
            Appointment appointment,
            MedicalRecord medicalRecord,
            List<ServiceOrder> serviceOrders,
            Map<String, Doctor> techniciansById) {
        Map<String, ServiceOrder> serviceOrdersByServiceId = serviceOrders == null
            ? Map.of()
            : serviceOrders.stream()
                .filter(order -> order.getService() != null && order.getService().getServiceId() != null)
                .filter(order -> order.getStatus() == null || !"CANCELLED".equalsIgnoreCase(order.getStatus()))
                .collect(Collectors.toMap(
                    order -> order.getService().getServiceId(),
                    order -> order,
                    (first, ignored) -> first));
        List<ServiceInfo> serviceInfos = buildServiceInfos(
                appointment,
                serviceOrders,
                serviceOrdersByServiceId,
                techniciansById);

        AppointmentDetailResponseBuilder builder = AppointmentDetailResponse.builder()
                // Appointment
                .appointmentId(appointment.getAppointmentId())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .queueNumber(appointment.getQueueNumber())
                .notes(appointment.getNotes())
                .cancelReason(appointment.getCancelReason())
                .createdAt(appointment.getCreatedAt())
                .visitType(appointment.getVisitType())
                .followUpSourceRecordId(appointment.getFollowUpSourceRecord() != null
                        ? appointment.getFollowUpSourceRecord().getRecordId()
                        : null)
                // Patient
                .patientId(appointment.getPatient().getPatientId())
                .patientName(appointment.getPatient().getUser().getFullName())
                .patientPhone(appointment.getPatient().getUser().getPhoneNumber())
                .patientDateOfBirth(appointment.getPatient().getDateOfBirth())
                .patientBloodType(appointment.getPatient().getBloodType())
                .patientHealthInsuranceNumber(appointment.getPatient().getHealthInsuranceNumber())
                .patientAllergies(appointment.getPatient().getAllergies())
                .patientBackgroundDisease(appointment.getPatient().getBackgroundDisease())
                // Services
                .services(serviceInfos.isEmpty() ? null : serviceInfos);

        // Doctor (optional)
        String deptName = null;
        String rId = null;
        String rName = null;
        if (appointment.getDoctor() != null) {
            var doctor = appointment.getDoctor();
            builder.doctorId(doctor.getUser().getUserId())
                    .doctorCode(doctor.getDoctorId())
                    .doctorName(doctor.getUser().getFullName())
                    .specialization(doctor.getSpecialization())
                    .degree(doctor.getDegree());
            if (doctor.getDepartment() != null) {
                deptName = doctor.getDepartment().getDeptName();
            }
        }

        if (appointment.getRoom() != null) {
            if (deptName == null && appointment.getRoom().getDepartment() != null) {
                deptName = appointment.getRoom().getDepartment().getDeptName();
            }
            rId = appointment.getRoom().getRoomId();
            rName = appointment.getRoom().getRoomName();
        } else if (appointment.getParaclinicalRoom() != null) {
            if (deptName == null && appointment.getParaclinicalRoom().getDepartment() != null) {
                deptName = appointment.getParaclinicalRoom().getDepartment().getDeptName();
            }
            rId = appointment.getParaclinicalRoom().getRoomId();
            rName = appointment.getParaclinicalRoom().getRoomName();
        } else if (appointment.getDoctor() != null && appointment.getDoctor().getRoom() != null) {
            var doctor = appointment.getDoctor();
            if (doctor.getRoom() != null) {
                rId = doctor.getRoom().getRoomId();
                rName = doctor.getRoom().getRoomName();
            }
        }

        // Fallback: derive department and room from appointment services→rooms
        if ((deptName == null || rId == null) && appointment.getServices() != null) {
            for (var svc : appointment.getServices()) {
                Room bestRoom = FinancialDetailResponse.pickBestRoom(svc);
                if (bestRoom != null) {
                    if (deptName == null && bestRoom.getDepartment() != null) {
                        deptName = bestRoom.getDepartment().getDeptName();
                    }
                    if (rId == null) {
                        rId = bestRoom.getRoomId();
                        rName = bestRoom.getRoomName();
                    }
                }
                if (deptName != null && rId != null) break;
            }
        }
        builder.departmentName(deptName);
        builder.roomId(rId);
        builder.roomName(rName);

        builder.medicalRecord(toMedicalRecordInfo(medicalRecord));
        builder.followUpSourceRecord(toMedicalRecordInfo(appointment.getFollowUpSourceRecord()));

        return builder.build();
    }

    private static List<ServiceInfo> buildServiceInfos(
            Appointment appointment,
            List<ServiceOrder> serviceOrders,
            Map<String, ServiceOrder> serviceOrdersByServiceId,
            Map<String, Doctor> techniciansById) {
        Map<String, g2.be.entity.Service> servicesById = new java.util.LinkedHashMap<>();
        if (appointment.getServices() != null) {
            appointment.getServices().stream()
                    .filter(service -> service != null && service.getServiceId() != null)
                    .forEach(service -> servicesById.putIfAbsent(service.getServiceId(), service));
        }

        if (serviceOrders != null) {
            serviceOrders.stream()
                    .filter(order -> order != null
                            && (order.getStatus() == null || !"CANCELLED".equalsIgnoreCase(order.getStatus()))
                            && order.getService() != null
                            && order.getService().getServiceId() != null)
                    .forEach(order -> servicesById.putIfAbsent(order.getService().getServiceId(), order.getService()));
        }

        return servicesById.values().stream()
                .map(service -> toServiceInfo(
                        appointment,
                        service,
                        serviceOrdersByServiceId.get(service.getServiceId()),
                        techniciansById))
                .collect(Collectors.toList());
    }

    private static ServiceInfo toServiceInfo(
            Appointment appointment,
            g2.be.entity.Service service,
            ServiceOrder serviceOrder,
            Map<String, Doctor> techniciansById) {
        ServiceInfo.ServiceInfoBuilder svcBuilder = ServiceInfo.builder()
                .serviceId(service.getServiceId())
                .serviceName(service.getServiceName())
                .price(service.getPrice())
                .departmentType(service.getDepartment() != null
                        && service.getDepartment().getDeptType() != null
                        ? service.getDepartment().getDeptType().name()
                        : null);

        Doctor assignedDoctor = resolveAssignedDoctor(
                appointment,
                service,
                serviceOrder,
                techniciansById);
        if (assignedDoctor != null) {
            svcBuilder.assignedDoctorId(assignedDoctor.getDoctorId());
            if (assignedDoctor.getUser() != null) {
                svcBuilder.assignedDoctorName(assignedDoctor.getUser().getFullName());
            }
        }

        Room bestRoom = resolveAssignedRoom(
                appointment,
                service,
                serviceOrder,
                techniciansById);
        if (bestRoom != null) {
            svcBuilder.assignedRoomId(bestRoom.getRoomId());
            svcBuilder.assignedRoomName(bestRoom.getRoomName());
        }

        return svcBuilder.build();
    }

    private static MedicalRecordInfo toMedicalRecordInfo(MedicalRecord medicalRecord) {
        if (medicalRecord == null) {
            return null;
        }

        return MedicalRecordInfo.builder()
                .recordId(medicalRecord.getRecordId())
                .symptoms(medicalRecord.getSymptoms())
                .diagnosis(medicalRecord.getDiagnosis())
                .doctorNotes(medicalRecord.getDoctorNotes())
                .recordStatus(medicalRecord.getStatus())
                .recordCreatedAt(medicalRecord.getCreatedAt())
                .visitType(medicalRecord.getVisitType())
                .parentRecordId(medicalRecord.getParentRecord() != null
                        ? medicalRecord.getParentRecord().getRecordId()
                        : null)
                .rootRecordId(medicalRecord.getRootRecord() != null
                        ? medicalRecord.getRootRecord().getRecordId()
                        : null)
                .build();
    }

    private static Room resolveAssignedRoom(
            Appointment appointment,
            g2.be.entity.Service service,
            ServiceOrder serviceOrder,
            Map<String, Doctor> techniciansById) {
        if (appointment == null || service == null) {
            return null;
        }

        if (serviceOrder != null) {
            Room serviceOrderRoom = FinancialDetailResponse.resolveAssignedRoom(serviceOrder, appointment);
            if (serviceOrderRoom != null) {
                return serviceOrderRoom;
            }
        }

        Doctor technician = resolveAssignedTechnician(serviceOrder, techniciansById);
        Room technicianRoom = resolveDoctorRoom(technician);
        if (roomSupportsService(technicianRoom, service)) {
            return technicianRoom;
        }

        if (service.getDepartment() != null
                && service.getDepartment().getDeptType() == g2.be.entity.Department.DeptType.PARACLINICAL) {
            if (roomSupportsService(appointment.getParaclinicalRoom(), service)) {
                return appointment.getParaclinicalRoom();
            }
            if (roomSupportsService(resolveDoctorRoom(appointment.getParaclinicalDoctor()), service)) {
                return resolveDoctorRoom(appointment.getParaclinicalDoctor());
            }
            if (roomSupportsService(appointment.getRoom(), service)) {
                return appointment.getRoom();
            }
            if (roomSupportsService(resolveDoctorRoom(appointment.getDoctor()), service)) {
                return resolveDoctorRoom(appointment.getDoctor());
            }
        }

        if (appointment.getRoom() != null) {
            return appointment.getRoom();
        }

        if (appointment.getDoctor() != null && appointment.getDoctor().getRoom() != null) {
            return appointment.getDoctor().getRoom();
        }

        return FinancialDetailResponse.pickBestRoom(service);
    }

    private static Doctor resolveAssignedDoctor(
            Appointment appointment,
            g2.be.entity.Service service,
            ServiceOrder serviceOrder,
            Map<String, Doctor> techniciansById) {
        if (appointment == null || service == null) {
            return null;
        }

        Doctor technician = resolveAssignedTechnician(serviceOrder, techniciansById);
        if (doctorSupportsService(technician, service)) {
            return technician;
        }

        if (service.getDepartment() != null
                && service.getDepartment().getDeptType() == g2.be.entity.Department.DeptType.PARACLINICAL) {
            if (doctorSupportsService(appointment.getParaclinicalDoctor(), service)) {
                return appointment.getParaclinicalDoctor();
            }
            if (doctorSupportsService(appointment.getDoctor(), service)) {
                return appointment.getDoctor();
            }
            return null;
        }

        if (appointment.getDoctor() != null) {
            return appointment.getDoctor();
        }

        return appointment.getParaclinicalDoctor();
    }

    private static Doctor resolveAssignedTechnician(ServiceOrder serviceOrder, Map<String, Doctor> techniciansById) {
        if (serviceOrder == null
                || serviceOrder.getTechnicianId() == null
                || serviceOrder.getTechnicianId().isBlank()
                || techniciansById == null) {
            return null;
        }

        return techniciansById.get(serviceOrder.getTechnicianId());
    }

    private static Room resolveDoctorRoom(Doctor doctor) {
        return doctor != null ? doctor.getRoom() : null;
    }

    private static boolean doctorSupportsService(Doctor doctor, g2.be.entity.Service service) {
        return roomSupportsService(resolveDoctorRoom(doctor), service);
    }

    private static boolean roomSupportsService(Room room, g2.be.entity.Service service) {
        if (room == null || service == null || service.getServiceId() == null || room.getServices() == null) {
            return false;
        }

        return room.getServices().stream()
                .anyMatch(mapped -> service.getServiceId().equals(mapped.getServiceId()));
    }
}
