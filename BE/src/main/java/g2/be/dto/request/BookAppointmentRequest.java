package g2.be.dto.request;

import g2.be.validation.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class BookAppointmentRequest {

    @NotEmpty(message = "Vui long chon it nhat mot dich vu")
    private List<String> serviceIds;

    @NotNull(message = "Ngay kham khong duoc de trong")
    private LocalDate appointmentDate;

    @NotNull(message = "Gio kham khong duoc de trong")
    private LocalTime appointmentTime;

    @NotBlank(message = "Ly do kham khong duoc de trong")
    @Size(max = 1000, message = "Ly do kham khong duoc vuot qua 1000 ky tu")
    private String notes;

    private String followUpSourceRecordId;

    /** Only populated when booking for a relative */
    @Valid
    private RelativeInfo relative;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelativeInfo {
        private String patientId;

        @NotBlank(message = "Ho ten nguoi than khong duoc de trong")
        @Size(max = 100, message = "Ho ten nguoi than khong duoc vuot qua 100 ky tu")
        @Pattern(regexp = ValidationPatterns.FULL_NAME, message = "Ho ten nguoi than khong duoc chua so hoac ky tu dac biet")
        private String fullName;

        @NotBlank(message = "So dien thoai nguoi than khong duoc de trong")
        @Pattern(regexp = ValidationPatterns.PHONE, message = "So dien thoai nguoi than phai bat dau bang 0 va co 10-11 chu so")
        private String phoneNumber;

        @NotNull(message = "Ngay sinh nguoi than khong duoc de trong")
        @Past(message = "Ngay sinh nguoi than phai nam trong qua khu")
        private LocalDate dateOfBirth;

        @NotBlank(message = "Moi quan he khong duoc de trong")
        @Size(max = 50, message = "Moi quan he khong duoc vuot qua 50 ky tu")
        private String relationship;

        @Pattern(regexp = ValidationPatterns.OPTIONAL_EMAIL, message = "Email nguoi than khong hop le")
        private String email;

        @Pattern(regexp = ValidationPatterns.OPTIONAL_CCCD, message = "CCCD nguoi than phai gom dung 12 chu so")
        private String cccd;
    }
}
