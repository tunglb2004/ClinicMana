package g2.be.dto.response;

import g2.be.entity.ServiceOrder;
import g2.be.util.ServiceOrderResultFiles;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServiceOrderResponse {
    private String orderId;
    private String recordId;
    private String serviceId;
    private String serviceName;
    private BigDecimal price;
    private String technicianId;
    private String resultNote;
    private String resultFileUrl;
    private List<String> resultFileUrls;
    private String indicationNote;
    private String status;
    private String orderType;
    private String invoiceId;
    private String paymentStatus;
    private boolean unpaid;
    private LocalDateTime createdAt;
    private String assignedRoomId;
    private String assignedRoomName;
    private String patientId;
    private String patientName;
    private String appointmentId;
    
    public static ServiceOrderResponse fromEntity(ServiceOrder entity) {
        return fromEntity(entity, entity.getInvoiceId() == null || entity.getInvoiceId().isBlank() ? "UNPAID" : null);
    }

    public static ServiceOrderResponse fromEntity(ServiceOrder entity, String paymentStatus) {
        List<String> resultFileUrls = ServiceOrderResultFiles.parse(entity.getResultFileUrl());
        String resolvedPaymentStatus = paymentStatus;
        boolean cancelled = "CANCELLED".equalsIgnoreCase(entity.getStatus());
        boolean unpaid = entity.getInvoiceId() == null || entity.getInvoiceId().isBlank();
        if (cancelled) {
            resolvedPaymentStatus = "CANCELLED";
            unpaid = false;
        } else if (resolvedPaymentStatus != null) {
            unpaid = "UNPAID".equalsIgnoreCase(resolvedPaymentStatus) || "PENDING".equalsIgnoreCase(resolvedPaymentStatus);
        }

        var builder = ServiceOrderResponse.builder()
            .orderId(entity.getOrderId())
            .recordId(entity.getMedicalRecord().getRecordId())
            .serviceId(entity.getService().getServiceId())
            .serviceName(entity.getService().getServiceName())
            .price(entity.getService().getPrice())
            .technicianId(entity.getTechnicianId())
            .resultNote(entity.getResultNote())
            .resultFileUrl(ServiceOrderResultFiles.latest(entity.getResultFileUrl()))
            .resultFileUrls(resultFileUrls)
            .indicationNote(entity.getIndicationNote())
            .status(entity.getStatus())
            .orderType(entity.getOrderType())
            .invoiceId(entity.getInvoiceId())
            .paymentStatus(resolvedPaymentStatus)
            .unpaid(unpaid)
            .createdAt(entity.getCreatedAt());

        if (entity.getMedicalRecord().getPatient() != null) {
            builder.patientId(entity.getMedicalRecord().getPatient().getPatientId());
            if (entity.getMedicalRecord().getPatient().getUser() != null) {
                builder.patientName(entity.getMedicalRecord().getPatient().getUser().getFullName());
            }
        }
        if (entity.getMedicalRecord().getAppointment() != null) {
            builder.appointmentId(entity.getMedicalRecord().getAppointment().getAppointmentId());
        }
        if (entity.getAssignedRoom() != null) {
            builder.assignedRoomId(entity.getAssignedRoom().getRoomId());
            builder.assignedRoomName(entity.getAssignedRoom().getRoomName());
        }

        return builder.build();
    }
}
