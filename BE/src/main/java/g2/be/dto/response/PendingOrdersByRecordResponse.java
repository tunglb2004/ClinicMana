package g2.be.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class PendingOrdersByRecordResponse {
    private String recordId;
    private String patientId;
    private String patientName;
    private String phoneNumber;
    private String healthInsuranceNumber;
    private String doctorName;
    private String departmentName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private List<ServiceOrderResponse> orders;
    private BigDecimal totalAmount;
}
