package g2.be.dto.response;

import lombok.*;

import java.util.List;
import java.util.Set;

/**
 * DTO trả về danh sách tài khoản nhân viên với phân trang.
 * Dùng trong API: GET /api/admin/users
 * 
 * @author Nam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserListResponse {

    /**
     * Danh sách users (thông tin rút gọn)
     */
    private List<UserSummary> users;

    /**
     * Tổng số users trong database (sau khi filter)
     */
    private long totalElements;

    /**
     * Tổng số trang
     */
    private int totalPages;

    /**
     * Trang hiện tại (bắt đầu từ 0)
     */
    private int currentPage;

    /**
     * Số lượng users mỗi trang
     */
    private int pageSize;

    /**
     * Inner class: Thông tin rút gọn của 1 user trong danh sách
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummary {

        /** ID của user */
        private Long userId;

        /** Mã nhân viên (VD: NV-A1B2C3D4) */
        private String staffId;

        /** Mã bệnh nhân (VD: BN-XXXXXX) */
        private String patientId;

        /** Tên đăng nhập */
        private String username;

        /** Email */
        private String email;

        /** Họ tên đầy đủ */
        private String fullName;

        /** Số điện thoại */
        private String phoneNumber;

        /** Số CCCD */
        private String cccd;

        /** Trạng thái: true = Active, false = Locked */
        private Boolean isActive;

        /** Danh sách tên roles */
        private Set<String> roles;

        /** Mã bác sĩ từ bảng doctors (VD: BS-001), chỉ có khi user có ROLE_DOCTOR */
        private String doctorId;

        /** Mã phòng bác sĩ được phân công */
        private String roomId;

        /** Mã khoa hiệu lực của bác sĩ */
        private Integer deptId;

        /** Tên khoa hiệu lực của bác sĩ */
        private String deptName;

        /** Loại khoa hiệu lực của bác sĩ */
        private String deptType;
    }
}
