package g2.be.dto.request;

import g2.be.entity.User;
import g2.be.validation.ValidationPatterns;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Ho ten khong duoc vuot qua 100 ky tu")
    @Pattern(regexp = ValidationPatterns.FULL_NAME, message = "Ho ten khong duoc chua so hoac ky tu dac biet")
    private String fullName;

    @Pattern(regexp = ValidationPatterns.OPTIONAL_CCCD, message = "CCCD phai gom dung 12 chu so")
    private String cccd;

    private User.Gender gender;

    @Pattern(regexp = ValidationPatterns.OPTIONAL_PHONE, message = "So dien thoai phai bat dau bang 0 va co 10-11 chu so")
    private String phoneNumber;

    private String avatarUrl;

    // Patient specific fields
    private LocalDate dateOfBirth;

    @Size(max = 5)
    private String bloodType;

    @Size(max = 50)
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

    @Pattern(regexp = ValidationPatterns.OPTIONAL_PHONE, message = "SDT nguoi lien he phai bat dau bang 0 va co 10-11 chu so")
    private String emergencyContactPhone;

    @Size(max = 50, message = "Moi quan he khong duoc vuot qua 50 ky tu")
    private String emergencyContactRelationship;

    // Doctor specific fields
    @Size(max = 50)
    private String licenseNumber;

    @Size(max = 100)
    private String specialization;

    @Size(max = 100)
    private String degree;

    private LocalDate certificationIssueDate;

    @Size(max = 150)
    private String certificationPlace;

    @Size(max = 255)
    private String certificationFileUrl;

    @Size(max = 150)
    private String trainingPlace;

    @Size(max = 4)
    private String graduationYear;

    @Size(max = 255)
    private String degreeFileUrl;

    private Integer experienceYears;

    @Size(max = 255)
    private String previousWorkplace;

    private String bio;

    @Size(max = 100)
    private String position;
}
