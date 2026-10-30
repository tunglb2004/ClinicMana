package g2.be.dto.request;

import g2.be.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(max = 100, message = "Ho ten khong duoc vuot qua 100 ky tu")
    @Pattern(regexp = ValidationPatterns.FULL_NAME, message = "Ho ten khong duoc chua so hoac ky tu dac biet")
    private String fullName;

    @NotBlank(message = "Email khong duoc de trong")
    @Pattern(regexp = ValidationPatterns.EMAIL, message = "Email phai dung dinh dang, dai 6-30 ky tu va khong co dau cham lien tiep")
    private String email;

    @NotBlank(message = "So dien thoai khong duoc de trong")
    @Pattern(regexp = ValidationPatterns.PHONE, message = "So dien thoai phai bat dau bang 0 va co 10-11 chu so")
    private String phone;

    @NotBlank(message = "CCCD khong duoc de trong")
    @Pattern(regexp = ValidationPatterns.CCCD, message = "CCCD phai gom dung 12 chu so")
    private String cccd;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Pattern(regexp = ValidationPatterns.PASSWORD, message = "Mat khau phai co 8-100 ky tu, gom chu hoa, chu thuong, so va ky tu dac biet")
    private String password;

    private String gender; // "MALE", "FEMALE", "OTHER"

    @NotBlank(message = "OTP khong duoc de trong")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP phai gom 6 chu so")
    private String otpCode;
}
