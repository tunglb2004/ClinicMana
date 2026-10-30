package g2.be.dto.request;

import java.time.LocalDate;
import java.util.Set;

import g2.be.entity.User.Gender;
import g2.be.validation.ValidationPatterns;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để nhận dữ liệu khi Admin tạo tài khoản nhân viên mới.
 * Dùng trong API: POST /api/admin/users
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    /**
     * Tên đăng nhập - bắt buộc, unique, 3-50 ký tự
     */
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    private String username;

    /**
     * Email - bắt buộc, unique, phải đúng format email
     */
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Pattern(regexp = ValidationPatterns.EMAIL, message = "Email phai dung dinh dang, dai 6-30 ky tu va khong co dau cham lien tiep")
    private String email;

    /**
     * Họ tên đầy đủ - bắt buộc
     */
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Ho ten khong duoc vuot qua 100 ky tu")
    @Pattern(regexp = ValidationPatterns.FULL_NAME, message = "Ho ten khong duoc chua so hoac ky tu dac biet")
    private String fullName;

    /**
     * Số CCCD - tùy chọn, 12 ký tự
     */
    @Size(max = 20, message = "CCCD không được quá 20 ký tự")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_CCCD, message = "CCCD phai gom dung 12 chu so")
    private String cccd;

    /**
     * Giới tính - tùy chọn (MALE, FEMALE, OTHER)
     */
    private Gender gender;

    /**
     * Số điện thoại - tùy chọn
     */
    @Size(max = 15, message = "Số điện thoại không được quá 15 ký tự")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_PHONE, message = "So dien thoai phai bat dau bang 0 va co 10-11 chu so")
    private String phoneNumber;

    private String specialization;

    private String position;

    private String degree;

    private String licenseNumber;

    private LocalDate licenseDate;

    private String licensePlace;

    private String school;

    private String graduationYear;

    private Integer experienceYears;

    private String previousWorkplace;

    private String bio;

    private String supportSkills;

    /**
     * Danh sách tên Role cần gán - bắt buộc ít nhất 1 role
     * VD: ["DOCTOR"] hoặc ["NURSE", "RECEPTIONIST"]
     */
    @NotEmpty(message = "Phải chọn ít nhất 1 role")
    private Set<String> roleNames;

    public String getAvatarUrl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
