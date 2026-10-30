package g2.be.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import g2.be.entity.User.Gender;
import g2.be.validation.ValidationPatterns;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePatientRequest {

    @NotBlank(message = "Email khong duoc de trong")
    @Pattern(regexp = ValidationPatterns.EMAIL, message = "Email phai dung dinh dang, dai 6-30 ky tu va khong co dau cham lien tiep")
    private String email;

    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(max = 100, message = "Ho ten khong duoc vuot qua 100 ky tu")
    @Pattern(regexp = ValidationPatterns.FULL_NAME, message = "Ho ten khong duoc chua so hoac ky tu dac biet")
    private String fullName;

    @NotBlank(message = "So dien thoai khong duoc de trong")
    @Pattern(regexp = ValidationPatterns.PHONE, message = "So dien thoai phai bat dau bang 0 va co 10-11 chu so")
    private String phoneNumber;

    @Pattern(regexp = ValidationPatterns.OPTIONAL_CCCD, message = "CCCD phai gom dung 12 chu so")
    private String cccd;

    private Gender gender;

    @Past(message = "Ngay sinh phai nam trong qua khu")
    private LocalDate dateOfBirth;

    @Size(max = 5, message = "Nhom mau khong duoc vuot qua 5 ky tu")
    private String bloodType;

    @Size(max = 50, message = "So BHYT khong duoc vuot qua 50 ky tu")
    private String healthInsuranceNumber;

    private String allergies;

    private String address;

    @Digits(integer = 3, fraction = 2, message = "Chieu cao khong hop le")
    @DecimalMin(value = "0.01", message = "Chieu cao phai lon hon 0")
    @DecimalMax(value = "300.00", message = "Chieu cao khong duoc vuot qua 300 cm")
    private BigDecimal height;

    @Digits(integer = 3, fraction = 2, message = "Can nang khong hop le")
    @DecimalMin(value = "0.01", message = "Can nang phai lon hon 0")
    @DecimalMax(value = "500.00", message = "Can nang khong duoc vuot qua 500 kg")
    private BigDecimal weight;

    private String backgroundDisease;

    @Size(max = 100, message = "Ten nguoi lien he khong duoc vuot qua 100 ky tu")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_FULL_NAME, message = "Ten nguoi lien he khong duoc chua so hoac ky tu dac biet")
    private String emergencyContactName;

    @Pattern(regexp = ValidationPatterns.OPTIONAL_PHONE, message = "So dien thoai lien he phai bat dau bang 0 va co 10-11 chu so")
    private String emergencyContactPhone;

    @Size(max = 50, message = "Moi quan he khong duoc vuot qua 50 ky tu")
    private String emergencyContactRelationship;
}
