package g2.be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Interface-based projection cho native SQL query có GROUP_CONCAT.
 * Column alias trong SQL phải khớp với getter name (camelCase).
 */
public interface UnpaidRecordProjection {
    String getRecordId();
    String getAppointmentId();
    String getPatientId();
    String getPatientName();
    String getPhoneNumber();
    String getHealthInsuranceNumber();
    String getDepartmentName();
    LocalDate getAppointmentDate();
    LocalTime getAppointmentTime();
    Long getUnpaidServiceCount();
    BigDecimal getTotalServiceFee();
    LocalDateTime getCreatedAt();
    String getServiceSummary();
}
