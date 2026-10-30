package g2.be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import g2.be.entity.Invoice;
import g2.be.entity.Room;
import g2.be.entity.ServiceOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialDetailResponse {
    private String invoiceId;
    private String cashierId;

    // Patient info
    private String patientId;
    private String patientName;
    private String patientPhone;
    private String patientEmail;

    // Appointment info
    private String appointmentId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String doctorId;
    private String doctorName;
    private String departmentName;
    private String roomId;
    private String roomName;

    // Service orders
    private List<ServiceOrderInfo> serviceOrders;

    // Payment info
    private BigDecimal totalServiceFee;
    private BigDecimal insurancePaid;
    private BigDecimal customerPaid;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    // Receipt / E-Invoice info
    private String receiptNumber;
    private String eInvoiceCode;
    private String eInvoiceUrl;
    private String receiptStatus;
    private String receiptErrorMessage;
    private LocalDateTime receiptIssuedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceOrderInfo {
        private String orderId;
        private String serviceId;
        private String serviceName;
        private BigDecimal price;
        private String status;
        private LocalDateTime createdAt;
        private String assignedRoomId;
        private String assignedRoomName;
    }

    public static FinancialDetailResponse fromEntity(Invoice invoice, List<ServiceOrder> orders) {
        FinancialDetailResponseBuilder builder = FinancialDetailResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .cashierId(invoice.getCashierId())
                .totalServiceFee(invoice.getTotalServiceFee())
                .insurancePaid(invoice.getInsurancePaid())
                .customerPaid(invoice.getCustomerPaid())
                .paymentStatus(invoice.getPaymentStatus())
                .paymentMethod(invoice.getPaymentMethod())
                .transactionId(invoice.getTransactionId())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .receiptNumber(invoice.getReceiptNumber())
                .eInvoiceCode(invoice.getEInvoiceCode())
                .eInvoiceUrl(invoice.getEInvoiceUrl())
                .receiptStatus(invoice.getReceiptStatus())
                .receiptErrorMessage(invoice.getReceiptErrorMessage())
                .receiptIssuedAt(invoice.getReceiptIssuedAt());
        
        var appointment = invoice.getMedicalRecord() != null ? invoice.getMedicalRecord().getAppointment() : null;

        String deptName = null;
        String rId = null;
        String rName = null;

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
                }
            }

            if (appt != null) {
                builder.appointmentId(appt.getAppointmentId());
                builder.appointmentDate(appt.getAppointmentDate());
                builder.appointmentTime(appt.getAppointmentTime());
                if (appt.getRoom() != null) {
                    if (appt.getRoom().getDepartment() != null) {
                        deptName = appt.getRoom().getDepartment().getDeptName();
                    }
                    rId = appt.getRoom().getRoomId();
                    rName = appt.getRoom().getRoomName();
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
                if (rId == null && doctor.getRoom() != null) {
                    rId = doctor.getRoom().getRoomId();
                    rName = doctor.getRoom().getRoomName();
                }
            }
        }

        if (orders != null) {
            // Fallback: derive department and room from service→room mapping
            if (deptName == null || rId == null) {
                for (ServiceOrder order : orders) {
                    Room bestRoom = resolveAssignedRoom(order, appointment);
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

            List<ServiceOrderInfo> serviceOrderInfos = orders.stream()
                    .map(order -> {
                        var infoBuilder = ServiceOrderInfo.builder()
                            .orderId(order.getOrderId())
                            .serviceId(order.getService() != null ? order.getService().getServiceId() : null)
                            .serviceName(order.getService() != null ? order.getService().getServiceName() : null)
                            .price(order.getService() != null ? order.getService().getPrice() : null)
                            .status(order.getStatus())
                            .createdAt(order.getCreatedAt());
                        Room bestRoom = resolveAssignedRoom(order, appointment);
                        if (bestRoom != null) {
                            infoBuilder.assignedRoomId(bestRoom.getRoomId());
                            infoBuilder.assignedRoomName(bestRoom.getRoomName());
                        }
                        return infoBuilder.build();
                    })
                    .toList();
            builder.serviceOrders(serviceOrderInfos);
        }

        builder.departmentName(deptName);
        builder.roomId(rId);
        builder.roomName(rName);

        return builder.build();
    }

    static Room resolveAssignedRoom(ServiceOrder order, g2.be.entity.Appointment appointment) {
        if (order != null && order.getAssignedRoom() != null
                && roomSupportsService(order.getAssignedRoom(), order.getService())) {
            return order.getAssignedRoom();
        }

        Room appointmentRoom = resolveAppointmentServiceRoom(order, appointment);
        if (appointmentRoom != null) {
            return appointmentRoom;
        }
        return order != null ? pickBestRoom(order.getService()) : null;
    }

    private static Room resolveAppointmentServiceRoom(ServiceOrder order, g2.be.entity.Appointment appointment) {
        if (order == null || order.getService() == null || appointment == null || appointment.getServices() == null) {
            return null;
        }

        String orderServiceId = order.getService().getServiceId();
        if (orderServiceId == null) {
            return null;
        }

        boolean matchesAppointmentService = appointment.getServices().stream()
                .anyMatch(service -> orderServiceId.equals(service.getServiceId()));
        if (!matchesAppointmentService) {
            return null;
        }

        if (isParaclinicalService(order.getService())) {
            // Appointment chỉ lưu 1 paraclinicalRoom cho tất cả dịch vụ cận lâm sàng.
            // Nếu phòng đó hỗ trợ dịch vụ này → dùng, nếu không → trả null để
            // fallback sang pickBestRoom() tìm phòng đúng cho từng dịch vụ riêng.
            Room paraclinicalRoom = resolveParaclinicalRoom(appointment);
            if (roomSupportsService(paraclinicalRoom, order.getService())) {
                return paraclinicalRoom;
            }
            // Không dùng appointment.getRoom() hay doctor.getRoom() cho dịch vụ cận lâm sàng
            // vì đó là phòng lâm sàng, không liên quan. Trả null để pickBestRoom xử lý.
            return null;
        }

        if (appointment.getRoom() != null) {
            return appointment.getRoom();
        }

        if (appointment.getDoctor() != null && appointment.getDoctor().getRoom() != null) {
            return appointment.getDoctor().getRoom();
        }

        return null;
    }

    private static Room resolveParaclinicalRoom(g2.be.entity.Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        if (appointment.getParaclinicalRoom() != null) {
            return appointment.getParaclinicalRoom();
        }

        if (appointment.getParaclinicalDoctor() != null && appointment.getParaclinicalDoctor().getRoom() != null) {
            return appointment.getParaclinicalDoctor().getRoom();
        }

        return null;
    }

    private static boolean isParaclinicalService(g2.be.entity.Service service) {
        return service != null
                && service.getDepartment() != null
                && service.getDepartment().getDeptType() == g2.be.entity.Department.DeptType.PARACLINICAL;
    }

    private static boolean roomSupportsService(Room room, g2.be.entity.Service service) {
        if (room == null || service == null || service.getServiceId() == null || room.getServices() == null) {
            return false;
        }

        return room.getServices().stream()
                .anyMatch(mapped -> service.getServiceId().equals(mapped.getServiceId()));
    }

    /**
     * Pick the best room for a service: prefer ACTIVE rooms with doctors, but fall back to any ACTIVE mapped room.
     */
    static Room pickBestRoom(g2.be.entity.Service service) {
        if (service == null || service.getRooms() == null) return null;
        List<Room> active = service.getRooms().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .toList();
        if (active.isEmpty()) return null;
        List<Room> preferred = active.stream()
                .filter(r -> r.getDoctors() != null && !r.getDoctors().isEmpty())
                .toList();
        List<Room> candidates = preferred.isEmpty() ? active : preferred;
        // Prefer SPECIALTY, then LAB, then GENERAL
        for (String type : List.of("SPECIALTY", "LAB", "GENERAL")) {
            var match = candidates.stream().filter(r -> type.equals(r.getRoomType())).findFirst();
            if (match.isPresent()) return match.get();
        }
        return candidates.get(0);
    }
}
