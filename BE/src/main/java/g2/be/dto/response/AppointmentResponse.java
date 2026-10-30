package g2.be.dto.response;

import g2.be.entity.Appointment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private String appointmentId;
    private String patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String departmentName;
    private List<ServiceInfo> services;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String status;
    private Integer queueNumber;
    private Integer priorityScore;
    private LocalDateTime waitingSince;
    private String notes;
    private String cancelReason;
    private String bookedByPatientId;
    private LocalDateTime createdAt;
    private String recordId;
    private String paymentStatus;
    private String visitType;
    private String followUpSourceRecordId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceInfo {
        private String serviceId;
        private String serviceName;
        private BigDecimal price;
    }

    public static AppointmentResponse fromEntity(Appointment appointment) {
        return AppointmentResponse.builder()
                .appointmentId(appointment.getAppointmentId())
                .patientId(appointment.getPatient().getPatientId())
                .patientName(appointment.getPatient().getUser().getFullName())
                .doctorId(appointment.getDoctor() != null ? appointment.getDoctor().getUser().getUserId() : null)
                .doctorName(appointment.getDoctor() != null ? appointment.getDoctor().getUser().getFullName() : null)
                .departmentName(appointment.getDoctor() != null && appointment.getDoctor().getDepartment() != null
                        ? appointment.getDoctor().getDepartment().getDeptName()
                        : null)
                .services(appointment.getServices() != null && !appointment.getServices().isEmpty()
                        ? appointment.getServices().stream()
                                .map(service -> ServiceInfo.builder()
                                        .serviceId(service.getServiceId())
                                        .serviceName(service.getServiceName())
                                        .price(service.getPrice())
                                        .build())
                                .collect(Collectors.toList())
                        : null)
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .queueNumber(appointment.getQueueNumber())
                .priorityScore(appointment.getPriorityScore())
                .waitingSince(appointment.getWaitingSince())
                .notes(appointment.getNotes())
                .cancelReason(appointment.getCancelReason())
                .bookedByPatientId(appointment.getBookedByPatientId())
                .createdAt(appointment.getCreatedAt())
                .visitType(appointment.getVisitType())
                .followUpSourceRecordId(appointment.getFollowUpSourceRecord() != null
                        ? appointment.getFollowUpSourceRecord().getRecordId()
                        : null)
                .build();
    }

    public static AppointmentResponse fromEntity(Appointment appointment, String recordId) {
        AppointmentResponse r = fromEntity(appointment);
        r.setRecordId(recordId);
        return r;
    }
}
