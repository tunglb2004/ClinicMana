package g2.be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import g2.be.entity.Invoice;
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
public class FinancialResponse {
    private String invoiceId;
    private String recordId;       // Medical Record ID — dùng cho supplement-payment
    private String patientId;
    private String patientName;
    private String appointmentId;
    private BigDecimal totalServiceFee;
    private BigDecimal insurancePaid;
    private BigDecimal customerPaid;
    private String paymentStatus;
    private String paymentMethod;
    private String receiptNumber;
    private String receiptStatus;
    private LocalDateTime receiptIssuedAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String cashierId;

    private String departmentName;
    private String healthInsuranceNumber;
    private String serviceSummary;

    public static FinancialResponse fromEntity(Invoice invoice) {
        FinancialResponseBuilder builder = FinancialResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .totalServiceFee(invoice.getTotalServiceFee())
                .insurancePaid(invoice.getInsurancePaid())
                .customerPaid(invoice.getCustomerPaid())
                .paymentStatus(invoice.getPaymentStatus())
                .paymentMethod(invoice.getPaymentMethod())
                .receiptNumber(invoice.getReceiptNumber())
                .receiptStatus(invoice.getReceiptStatus())
                .receiptIssuedAt(invoice.getReceiptIssuedAt())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .cashierId(invoice.getCashierId());

        if (invoice.getMedicalRecord() != null) {
            if (invoice.getMedicalRecord().getPatient() != null) {
                builder.patientId(invoice.getMedicalRecord().getPatient().getPatientId());
                builder.healthInsuranceNumber(invoice.getMedicalRecord().getPatient().getHealthInsuranceNumber());
                if (invoice.getMedicalRecord().getPatient().getUser() != null) {
                    builder.patientName(invoice.getMedicalRecord().getPatient().getUser().getFullName());
                }
            }
            if (invoice.getMedicalRecord().getAppointment() != null) {
                builder.appointmentId(invoice.getMedicalRecord().getAppointment().getAppointmentId());
            }
            if (invoice.getMedicalRecord().getDoctor() != null && invoice.getMedicalRecord().getDoctor().getDepartment() != null) {
                builder.departmentName(invoice.getMedicalRecord().getDoctor().getDepartment().getDeptName());
            }
        }

        return builder.build();
    }
}
