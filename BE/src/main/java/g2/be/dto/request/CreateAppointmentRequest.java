package g2.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppointmentRequest {

    private String patientId;

    @Valid
    private CreatePatientRequest newPatient;

    private String doctorId; // Doctor Code (e.g. BS-001)
    private String paraclinicalDoctorId;
    private String roomId;

    @NotEmpty(message = "Vui long chon it nhat mot dich vu")
    private List<String> serviceIds;

    @NotNull(message = "Ngay kham khong duoc de trong")
    private LocalDate appointmentDate;

    @NotNull(message = "Gio kham khong duoc de trong")
    private LocalTime appointmentTime;

    private String status;

    @NotBlank(message = "Ly do kham khong duoc de trong")
    @Size(max = 1000, message = "Ly do kham khong duoc vuot qua 1000 ky tu")
    private String notes;

    private String followUpSourceRecordId;
}
