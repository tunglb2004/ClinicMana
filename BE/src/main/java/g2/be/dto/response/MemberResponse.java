package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response thông tin nhân sự (dùng chung cho Doctor và Nurse).
 * Hiển thị trong chi tiết khoa/phòng khi liệt kê danh sách member.
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {

    // ID user trong bảng users
    private Long userId;

    // Họ tên đầy đủ
    private String fullName;

    // Mã nhân viên (ví dụ: NV-XXXXXXXX)
    private String staffId;

    // Vai trò: "DOCTOR" hoặc "NURSE"
    private String role;

    // Chuyên khoa (chỉ có ở Doctor, Nurse sẽ null)
    private String specialization;

    // Số điện thoại
    private String phoneNumber;

    // Email
    private String email;
}
