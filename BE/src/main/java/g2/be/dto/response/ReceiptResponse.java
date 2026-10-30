package g2.be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import g2.be.entity.Invoice;
import g2.be.entity.Room;
import g2.be.entity.ServiceOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptResponse {

    // Receipt / E-Invoice info
    private String invoiceId;
    private String receiptNumber;
    private String eInvoiceCode;
    private String eInvoiceUrl;
    private String receiptStatus;
    private String receiptErrorMessage;
    private LocalDateTime receiptIssuedAt;

    // Patient info
    private String patientId;
    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private String patientAddress;
    private LocalDate patientDateOfBirth;
    private String patientCccd;
    private String healthInsuranceNumber;

    // Appointment info
    private String appointmentId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

    // Doctor info
    private String doctorId;
    private String doctorName;
    private String departmentName;
    private String roomId;
    private String roomName;

    // Service items
    private List<ServiceItem> services;

    // Payment summary
    private BigDecimal totalServiceFee;
    private BigDecimal insurancePaid;
    private BigDecimal customerPaid;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    // Cashier info
    private String cashierId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceItem {
        private String serviceId;
        private String serviceName;
        private BigDecimal price;
        private String status;
        private String assignedRoomId;
        private String assignedRoomName;
    }

    public static ReceiptResponse fromEntity(Invoice invoice, List<ServiceOrder> orders) {
        ReceiptResponseBuilder builder = ReceiptResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .receiptNumber(invoice.getReceiptNumber())
                .eInvoiceCode(invoice.getEInvoiceCode())
                .eInvoiceUrl(invoice.getEInvoiceUrl())
                .receiptStatus(invoice.getReceiptStatus())
                .receiptErrorMessage(invoice.getReceiptErrorMessage())
                .receiptIssuedAt(invoice.getReceiptIssuedAt())
                .cashierId(invoice.getCashierId())
                .totalServiceFee(invoice.getTotalServiceFee())
                .insurancePaid(invoice.getInsurancePaid())
                .customerPaid(invoice.getCustomerPaid())
                .paymentStatus(invoice.getPaymentStatus())
                .paymentMethod(invoice.getPaymentMethod())
                .transactionId(invoice.getTransactionId())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt());

            var appointment = invoice.getMedicalRecord() != null ? invoice.getMedicalRecord().getAppointment() : null;

            String deptName = null;
            String receiptRoomId = null;
            String receiptRoomName = null;

        if (invoice.getMedicalRecord() != null) {
            var record = invoice.getMedicalRecord();
            var appt = record.getAppointment();
            var doctor = appt != null && appt.getDoctor() != null ? appt.getDoctor() : record.getDoctor();

            if (record.getPatient() != null) {
                builder.patientId(record.getPatient().getPatientId());
                if (record.getPatient().getUser() != null) {
                    builder.patientName(record.getPatient().getUser().getFullName());
                    builder.patientPhone(record.getPatient().getUser().getPhoneNumber());
                    builder.patientEmail(record.getPatient().getUser().getEmail());
                    builder.patientCccd(record.getPatient().getUser().getCccd());
                }
                builder.patientAddress(record.getPatient().getAddress());
                builder.patientDateOfBirth(record.getPatient().getDateOfBirth());
                builder.healthInsuranceNumber(record.getPatient().getHealthInsuranceNumber());
            }

            if (appt != null) {
                builder.appointmentId(appt.getAppointmentId());
                builder.appointmentDate(appt.getAppointmentDate());
                builder.appointmentTime(appt.getAppointmentTime());
                if (appt.getRoom() != null) {
                    if (appt.getRoom().getDepartment() != null) {
                        deptName = appt.getRoom().getDepartment().getDeptName();
                    }
                    receiptRoomId = appt.getRoom().getRoomId();
                    receiptRoomName = appt.getRoom().getRoomName();
                }
            }

            if (doctor != null) {
                builder.doctorId(doctor.getDoctorId());
                if (doctor.getUser() != null) {
                    builder.doctorName(doctor.getUser().getFullName());
                }
                if (deptName == null && doctor.getDepartment() != null) {
                    deptName = doctor.getDepartment().getDeptName();
                }
                if (receiptRoomId == null && doctor.getRoom() != null) {
                    receiptRoomId = doctor.getRoom().getRoomId();
                    receiptRoomName = doctor.getRoom().getRoomName();
                }
            }
        }

        if (orders != null) {
            if (deptName == null || receiptRoomId == null) {
                for (ServiceOrder order : orders) {
                    Room bestRoom = FinancialDetailResponse.resolveAssignedRoom(order, appointment);
                    if (bestRoom != null) {
                        if (deptName == null && bestRoom.getDepartment() != null) {
                            deptName = bestRoom.getDepartment().getDeptName();
                        }
                        if (receiptRoomId == null) {
                            receiptRoomId = bestRoom.getRoomId();
                            receiptRoomName = bestRoom.getRoomName();
                        }
                    }
                    if (deptName != null && receiptRoomId != null) {
                        break;
                    }
                }
            }

            List<ServiceItem> serviceItems = orders.stream()
                    .map(order -> {
                        var itemBuilder = ServiceItem.builder()
                                .serviceId(order.getService() != null ? order.getService().getServiceId() : null)
                                .serviceName(order.getService() != null ? order.getService().getServiceName() : null)
                                .price(order.getService() != null ? order.getService().getPrice() : null)
                                .status(order.getStatus());

                            Room bestRoom = FinancialDetailResponse.resolveAssignedRoom(order, appointment);
                        if (bestRoom != null) {
                            itemBuilder.assignedRoomId(bestRoom.getRoomId());
                            itemBuilder.assignedRoomName(bestRoom.getRoomName());
                        }

                        return itemBuilder.build();
                    })
                    .collect(Collectors.toList());
            builder.services(serviceItems);
        }

        builder.departmentName(deptName);
        builder.roomId(receiptRoomId);
        builder.roomName(receiptRoomName);

        return builder.build();
    }
}
