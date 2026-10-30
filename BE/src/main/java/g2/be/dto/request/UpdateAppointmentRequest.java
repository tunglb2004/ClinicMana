package g2.be.dto.request;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAppointmentRequest {
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String status;
    private String doctorId;
    private String paraclinicalDoctorId;
    private String roomId;
    private List<String> serviceIds; // Changed from single serviceId
}
