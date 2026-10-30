package g2.be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnpaidRecordResponse {
    private String recordId;
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String phoneNumber;
    private String healthInsuranceNumber;
    private String departmentName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Long unpaidServiceCount;
    private BigDecimal totalServiceFee;
    private LocalDateTime createdAt;
}
