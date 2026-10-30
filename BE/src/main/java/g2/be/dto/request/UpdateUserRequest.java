package g2.be.dto.request;

import java.time.LocalDate;
import java.util.Set;

import g2.be.entity.User.Gender;
import g2.be.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để nhận dữ liệu khi Admin sửa thông tin tài khoản nhân viên.
 * Dùng trong API: PUT /api/admin/users/{id}
 * 
 * Lưu ý: Tất cả các field đều optional (chỉ cập nhật field được gửi lên)
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    /**
     * Email mới - tùy chọn
     */
    @Email(message = "Email không hợp lệ")
    @Pattern(regexp = ValidationPatterns.EMAIL, message = "Email phai dung dinh dang, dai 6-30 ky tu va khong co dau cham lien tiep")
    private String email;

    /**
     * Họ tên đầy đủ - tùy chọn
     */
    @Size(max = 100, message = "Họ tên không được quá 100 ký tự")
    @Pattern(regexp = ValidationPatterns.FULL_NAME, message = "Ho ten khong duoc chua so hoac ky tu dac biet")
    private String fullName;

    /**
     * Số CCCD - tùy chọn, 9-20 ký tự
     */
    @Size(max = 12, message = "CCCD khong duoc vuot qua 12 ky tu")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_CCCD, message = "CCCD phai gom dung 12 chu so")
    private String cccd;

    /**
     * Giới tính - tùy chọn (MALE, FEMALE, OTHER)
     */
    private Gender gender;

    /**
     * Số điện thoại - tùy chọn, 10-15 chữ số
     */
    @Pattern(regexp = ValidationPatterns.OPTIONAL_PHONE, message = "So dien thoai phai bat dau bang 0 va co 10-11 chu so")
    private String phoneNumber;

    /**
     * URL ảnh đại diện - tùy chọn
     */
    private String avatarUrl;

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
     * Danh sách tên Role mới - tùy chọn
     * Nếu gửi lên sẽ thay thế toàn bộ roles hiện tại
     * VD: ["ROLE_DOCTOR"] hoặc ["ROLE_NURSE", "ROLE_RECEPTIONIST"]
     */
    private Set<String> roleNames;

    /**
     * Trạng thái active - tùy chọn
     * true = mở khóa, false = khóa tài khoản
     */
    private Boolean isActive;
}
