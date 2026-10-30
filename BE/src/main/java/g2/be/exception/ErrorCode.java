package g2.be.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
        // 01. Login
        LOGIN_INVALID_CREDENTIALS(1001, "Đăng nhập thất bại. Vui lòng kiểm tra lại email/tên đăng nhập hoặc mật khẩu.",
                        HttpStatus.BAD_REQUEST),
        LOGIN_ACCOUNT_LOCKED(1002, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị hệ thống.",
                        HttpStatus.FORBIDDEN),
        LOGIN_SUCCESS(1003, "Đăng nhập thành công.", HttpStatus.OK),

        // 02. Register
        REGISTER_EMAIL_EXISTS(1004, "Email này đã được sử dụng.", HttpStatus.BAD_REQUEST),
        REGISTER_PHONE_EXISTS(1005, "Số điện thoại này đã được sử dụng.", HttpStatus.BAD_REQUEST),

        // 03. Forgot Password
        FORGOT_PASSWORD_OTP_SENT(1006, "Mã OTP đã được gửi đến email của bạn.", HttpStatus.OK),
        FORGOT_PASSWORD_INVALID_OTP(1007, "OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.",
                        HttpStatus.BAD_REQUEST),

        // 04. Change Password
        CHANGE_PASSWORD_INCORRECT(1008, "Mật khẩu hiện tại không đúng.", HttpStatus.BAD_REQUEST),
        CHANGE_PASSWORD_SUCCESS(1009, "Cập nhật mật khẩu thành công.", HttpStatus.OK),

        // 05. Update Profile
        UPDATE_PROFILE_MISSING_FIELDS(1010, "Vui lòng nhập đầy đủ thông tin bắt buộc.", HttpStatus.BAD_REQUEST),
        UPDATE_PROFILE_SUCCESS(1011, "Cập nhật hồ sơ thành công.", HttpStatus.OK),

        // 06. Staff Account List
        STAFF_LIST_NO_DATA(1012, "Không tìm thấy tài khoản nhân sự.", HttpStatus.NOT_FOUND),

        // 07. Create Staff Account
        CREATE_STAFF_SUCCESS(1013, "Tạo tài khoản nhân sự thành công.", HttpStatus.CREATED),

        // 08. Create/Update Account
        ACCOUNT_DUPLICATE_EMAIL_CCCD(1014, "CCCD đã tồn tại.", HttpStatus.BAD_REQUEST),

        // 09. Lock/Unlock Account
        LOCK_UNLOCK_CONFIRM(1015, "Bạn có chắc chắn muốn thay đổi trạng thái tài khoản này không?", HttpStatus.OK),

        // 10. Delete Account/Item
        DELETE_CONFIRM(1016, "Bạn có chắc chắn muốn xóa mục này không? Thao tác có thể không thể hoàn tác.",
                        HttpStatus.OK),

        // 11. Department/Room/Service
        DEPT_NAME_EXISTS(1017, "Tên khoa đã tồn tại.", HttpStatus.BAD_REQUEST),
        DEPT_NOT_FOUND(1071, "Không tìm thấy khoa.", HttpStatus.NOT_FOUND),
        DEPT_HAS_MEMBERS(1072, "Không thể xóa khoa vì còn nhân sự (bác sĩ/y tá) thuộc khoa này.",
                        HttpStatus.BAD_REQUEST),
        DEPT_HAS_ROOMS(1076,
                        "Không thể xóa khoa vì còn phòng thuộc khoa này. Vui lòng xóa hoặc chuyển các phòng trước.",
                        HttpStatus.BAD_REQUEST),
        CREATE_DEPT_SUCCESS(1073, "Tạo khoa thành công.", HttpStatus.CREATED),
        UPDATE_DEPT_SUCCESS(1074, "Cập nhật khoa thành công.", HttpStatus.OK),
        DELETE_DEPT_SUCCESS(1075, "Xóa khoa thành công.", HttpStatus.OK),

        // 12. Work Shift
        WORK_SHIFT_OVERLAP(1018, "Khung giờ ca làm bị trùng với ca làm hiện có.", HttpStatus.BAD_REQUEST),

        // 13. Patient Search
        PATIENT_SEARCH_NO_DATA(1019, "Không tìm thấy hồ sơ bệnh nhân phù hợp.", HttpStatus.NOT_FOUND),

        // 14. Create Appointment
        CREATE_APPOINTMENT_MISSING_FIELDS(1020, "Vui lòng điền đầy đủ các trường bắt buộc trước khi đặt lịch.",
                        HttpStatus.BAD_REQUEST),
        CREATE_APPOINTMENT_FULL(1021, "Khung giờ đã đầy. Vui lòng chọn khung giờ khác.", HttpStatus.BAD_REQUEST),
        CREATE_APPOINTMENT_TIME_EXPIRED(1124, "Ca kham da qua gio. Vui long chon ca kham khac.", HttpStatus.BAD_REQUEST),
        CREATE_APPOINTMENT_SUCCESS(1022, "Đặt lịch khám thành công.", HttpStatus.CREATED),

        // 15. Reschedule Appointment
        RESCHEDULE_CONFIRM(1023, "Bạn có chắc chắn muốn dời lịch khám này không?", HttpStatus.OK),
        RESCHEDULE_SUCCESS(1024, "Dời lịch khám thành công.", HttpStatus.OK),

        // 16. Cancel Appointment
        CANCEL_APPOINTMENT_NOT_ALLOWED(1025, "Bạn không thể hủy lịch khám này do chính sách hủy lịch của phòng khám.",
                        HttpStatus.BAD_REQUEST),
        CANCEL_APPOINTMENT_SUCCESS(1026, "Hủy lịch khám thành công.", HttpStatus.OK),
        CANCEL_APPOINTMENT_ALREADY_CANCELLED(1125, "Lịch khám này đã được hủy trước đó.", HttpStatus.BAD_REQUEST),
        CANCEL_APPOINTMENT_STATUS_NOT_ALLOWED(1126,
                        "Chỉ có thể hủy lịch chưa thanh toán và chưa bắt đầu khám.", HttpStatus.BAD_REQUEST),
        CANCEL_APPOINTMENT_ACCESS_DENIED(1127,
                        "Bạn chỉ có thể hủy lịch của mình hoặc lịch do bạn đặt hộ.", HttpStatus.FORBIDDEN),

        // 17. Payment
        PAYMENT_NO_PAYABLE_ITEMS(1027, "Không tìm thấy khoản cần thanh toán cho lượt khám này.", HttpStatus.NOT_FOUND),
        PAYMENT_INSURANCE_APPLIED(1028, "Đã áp dụng giảm trừ BHYT. Tổng tiền cần thanh toán đã được cập nhật.",
                        HttpStatus.OK),
        PAYMENT_FAILED(1029, "Thanh toán thất bại. Vui lòng thử lại hoặc chọn phương thức khác.",
                        HttpStatus.BAD_REQUEST),
        PAYMENT_SUCCESS(1030, "Thanh toán thành công.", HttpStatus.OK),

        // 18. Queue Number
        QUEUE_NUMBER_ISSUED(1031, "Đã cấp số thứ tự cho bệnh nhân.", HttpStatus.OK),

        // 19. Visit Schedule
        VISIT_SCHEDULE_GENERATED(1032, "Đã tạo lộ trình khám và gán phòng/khung giờ thành công.", HttpStatus.OK),

        // 20. Invoice/Itinerary
        DOWNLOAD_SUCCESS(1033, "Tải hóa đơn/lộ trình thành công.", HttpStatus.OK),

        // 21. Daily Patient List
        DAILY_LIST_EMPTY(1034, "Hôm nay không có bệnh nhân được xếp lịch.", HttpStatus.OK),

        // 22. Status Update
        VISIT_STATUS_UPDATED(1035, "Cập nhật trạng thái lượt khám thành công.", HttpStatus.OK),

        // 23. Upload File
        UPLOAD_FILE_UNSUPPORTED_TYPE(1036,
                        "Định dạng tệp không được hỗ trợ. Vui lòng tải lên PDF/JPG/PNG (theo cấu hình).",
                        HttpStatus.BAD_REQUEST),
        UPLOAD_FILE_SIZE_EXCEEDS(1037, "Dung lượng tệp vượt quá giới hạn cho phép.", HttpStatus.BAD_REQUEST),
        UPLOAD_FILE_SUCCESS(1038, "Tải tệp lên thành công.", HttpStatus.OK),

        // 24. Update Exam Result
        UPDATE_RESULT_MISSING_FIELDS(1039, "Vui lòng nhập đầy đủ thông tin bắt buộc trước khi lưu.",
                        HttpStatus.BAD_REQUEST),
        UPDATE_RESULT_SUCCESS(1040, "Cập nhật kết quả khám thành công.", HttpStatus.OK),

        // 25. Specialist Results
        SPECIALIST_RESULTS_LOADED(1045, "Đã tải kết quả khám chuyên khoa thành công.", HttpStatus.OK),

        // 26. Feedback
        FEEDBACK_NO_DATA(1046, "Không tìm thấy phản hồi.", HttpStatus.NOT_FOUND),
        SUBMIT_REVIEW_SUCCESS(1047, "Cảm ơn bạn! Đánh giá của bạn đã được gửi.", HttpStatus.OK),
        RESPOND_FEEDBACK_SUCCESS(1048, "Đã đăng phản hồi thành công.", HttpStatus.OK),

        // Generic
        UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
        USER_NOT_EXIST(1049, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
        EMAIL_NOT_EXIST(1050, "Email không tồn tại trong hệ thống.", HttpStatus.NOT_FOUND),
        PASSWORD_CONFIRMATION_INCORRECT(1051, "Mật khẩu mới và xác nhận mật khẩu không khớp", HttpStatus.BAD_REQUEST),
        UPDATE_EMAIL_SUCCESS(1052, "Cập nhật email thành công.", HttpStatus.OK),
        REGISTER_SUCCESS(1053, "Đăng ký thành công.", HttpStatus.OK),

        // Additional ErrorCodes
        USERNAME_EXISTS(1054, "Tên đăng nhập đã tồn tại.", HttpStatus.BAD_REQUEST),
        ROLE_NOT_FOUND(1055, "Vai trò không tồn tại.", HttpStatus.NOT_FOUND),
        USER_DELETED(1056, "Người dùng đã bị xóa.", HttpStatus.BAD_REQUEST),
        USER_ALREADY_DELETED(1057, "Người dùng đã bị xóa trước đó.", HttpStatus.BAD_REQUEST),
        USER_ALREADY_LOCKED(1058, "Tài khoản đã bị khóa trước đó.", HttpStatus.BAD_REQUEST),
        USER_NOT_LOCKED(1059, "Tài khoản đang hoạt động bình thường.", HttpStatus.BAD_REQUEST),
        DELETE_USER_SUCCESS(1060, "Xóa người dùng thành công.", HttpStatus.OK),
        LOCK_USER_SUCCESS(1061, "Khóa tài khoản thành công.", HttpStatus.OK),
        UNLOCK_USER_SUCCESS(1062, "Mở khóa tài khoản thành công.", HttpStatus.OK),
        REFRESH_TOKEN_EXPIRED(1063, "Refresh token đã hết hạn. Vui lòng đăng nhập lại.", HttpStatus.FORBIDDEN),
        REFRESH_TOKEN_INVALID(1064, "Refresh token không hợp lệ", HttpStatus.BAD_REQUEST),
        EMAIL_SEND_FAILED(1065, "Không thể gửi email. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR),
        INVALID_OTP_TYPE(1066, "Loại OTP không hợp lệ.", HttpStatus.BAD_REQUEST),
        GOOGLE_TOKEN_FAILED(1067, "Lỗi kết nối OAuth2.", HttpStatus.INTERNAL_SERVER_ERROR),
        OTP_EXPIRED(1068, "OTP đã hết hạn. Vui lòng yêu cầu mã mới.", HttpStatus.BAD_REQUEST),
        OTP_INVALID_OR_USED(1069, "OTP không hợp lệ hoặc đã được sử dụng", HttpStatus.BAD_REQUEST),
        VERIFY_OTP_SUCCESS(1070, "Xác thực OTP thành công", HttpStatus.OK),
        SERVICE_NOT_FOUND(1077, "Dịch vụ không tồn tại.", HttpStatus.NOT_FOUND),
        APPOINTMENT_NOT_FOUND(1078, "Lịch hẹn không tồn tại.", HttpStatus.NOT_FOUND),
        PATIENT_NOT_FOUND(1079, "Hồ sơ bệnh nhân không tồn tại.", HttpStatus.NOT_FOUND),
        DOCTOR_NOT_FOUND(1080, "Bác sĩ không tồn tại.", HttpStatus.NOT_FOUND),
        PAYMENT_ALREADY_PAID(1081, "Dịch vụ này đã được thanh toán.", HttpStatus.BAD_REQUEST),
        DOCTOR_SCHEDULE_CONFLICT(1082, "Bác sĩ đã có lịch hẹn vào thời gian này.", HttpStatus.CONFLICT),

        // SePay / Webhook
        INVOICE_NOT_FOUND(1083, "Hóa đơn không tồn tại.", HttpStatus.NOT_FOUND),
        UNAUTHENTICATED(1084, "Xác thực thất bại.", HttpStatus.UNAUTHORIZED),
        INVALID_KEY(1085, "Dữ liệu không hợp lệ.", HttpStatus.BAD_REQUEST),
        
        // 27. Medical Record & Prescription (Dải 1086 - 1092)
        MEDICAL_RECORD_NOT_FOUND(1086, "Hồ sơ bệnh án không tồn tại.", HttpStatus.NOT_FOUND),
        PRESCRIPTION_NOT_FOUND(1087, "Đơn thuốc không tồn tại.", HttpStatus.NOT_FOUND),
        MEDICAL_RECORD_NOT_EXAMINING(1088, "Hồ sơ bệnh án không trong trạng thái đang khám.", HttpStatus.BAD_REQUEST),
        INVALID_STATUS_TRANSITION(1089, "Chuyển đổi trạng thái không hợp lệ.", HttpStatus.BAD_REQUEST),
        PRESCRIPTION_REQUIRED_FOR_CONFIRM(1090, "Không thể xác nhận kết quả khi chưa có đơn thuốc.", HttpStatus.BAD_REQUEST),
        MEDICAL_RECORD_ALREADY_FINISHED(1091, "Hồ sơ bệnh án đã kết thúc, không thể chỉnh sửa.", HttpStatus.BAD_REQUEST),
        NURSE_DOCTOR_ASSIGNMENT_EXISTS(1092, "Y tá đã được gán theo dõi bác sĩ này vào ngày đã chọn.", HttpStatus.BAD_REQUEST),
        ROOM_NOT_FOUND(1093, "Không tìm thấy phòng.", HttpStatus.NOT_FOUND),

        // Financial & Reviews (Đã Re-index dải 1100 - 1112)
        INSURANCE_EXPIRED(1100, "Thẻ BHYT đã hết hạn. Vui lòng gia hạn thẻ trước khi sử dụng.", HttpStatus.BAD_REQUEST),
        INSURANCE_NOT_FOUND(1101, "Không tìm thấy thông tin BHYT với CCCD đã cung cấp.", HttpStatus.NOT_FOUND),
        REVIEW_NOT_FOUND(1102, "Đánh giá không tồn tại.", HttpStatus.NOT_FOUND),
        REVIEW_ALREADY_EXISTS(1103, "Bạn đã đánh giá cho lịch hẹn này.", HttpStatus.BAD_REQUEST),
        REVIEW_INVALID_RATING(1104, "Điểm đánh giá phải từ 1 đến 5.", HttpStatus.BAD_REQUEST),
        REVIEW_ALREADY_REPLIED(1105, "Đánh giá này đã được phản hồi.", HttpStatus.BAD_REQUEST),
        RECEIPT_ALREADY_ISSUED(1106, "Phiếu thu đã được xuất cho hóa đơn này.", HttpStatus.BAD_REQUEST),
        RECEIPT_UNPAID_INVOICE(1107, "Chỉ có thể xuất phiếu thu cho hóa đơn đã thanh toán.", HttpStatus.BAD_REQUEST),
        INVOICE_ACCESS_DENIED(1108, "Bạn không có quyền truy cập hóa đơn này.", HttpStatus.FORBIDDEN),
        ORDER_NOT_FOUND(1109, "Chỉ định dịch vụ không tồn tại.", HttpStatus.NOT_FOUND),
        RECORD_NOT_FOUND(1110, "Hồ sơ bệnh án không tồn tại.", HttpStatus.NOT_FOUND),
        NO_EXISTING_ORDER(1111, "Chưa có chỉ định ban đầu. Vui lòng tạo chỉ định lần đầu trước khi bổ sung.", HttpStatus.BAD_REQUEST),
        NO_SUPPLEMENT_ORDERS(1112, "Không có chỉ định bổ sung nào chưa thanh toán.", HttpStatus.BAD_REQUEST),

        // Priority Queue (Assigned 1113 - 1115 to avoid conflict with Financial)
        DOCTOR_BUSY(1113, "Bác sĩ đang khám bệnh nhân khác. Vui lòng hoàn tất ca hiện tại trước.", HttpStatus.CONFLICT),
        NO_WAITING_PATIENT(1114, "Không có bệnh nhân nào đang chờ trong hàng đợi.", HttpStatus.NOT_FOUND),
        INVALID_APPOINTMENT_STATUS(1115, "Trạng thái lịch hẹn không hợp lệ cho thao tác này.", HttpStatus.BAD_REQUEST),
        ORDER_ALREADY_CANCELLED(1116, "Chỉ định này đã được hủy trước đó.", HttpStatus.BAD_REQUEST),
        ORDER_CANCEL_NOT_ALLOWED(1117, "Chỉ được hủy chỉ định chưa thanh toán và chưa có kết quả.", HttpStatus.BAD_REQUEST),
        INVOICE_CANCELLED(1118, "Hóa đơn liên quan đã được hủy vì không còn dịch vụ chờ thanh toán.", HttpStatus.OK),
        RECEIPT_ISSUANCE_IN_PROGRESS(1119, "Phiếu thu/hóa đơn điện tử đang được xử lý. Vui lòng thử lại sau.", HttpStatus.CONFLICT),
        APPOINTMENT_HAS_UNPAID_INVOICES(1120, "Không thể hoàn tất khám khi còn hóa đơn chưa thanh toán.", HttpStatus.BAD_REQUEST),
        RECEIPT_APPOINTMENT_NOT_COMPLETED(1121, "Chỉ có thể in/phát hành hóa đơn điện tử khi ca khám đã hoàn tất.", HttpStatus.BAD_REQUEST)
        , FOLLOW_UP_RECORD_NOT_ELIGIBLE(1122, "Ho so tai kham khong hop le.", HttpStatus.BAD_REQUEST)
        , APPOINTMENT_ALREADY_PAID_CANNOT_CANCEL(1123, "Lịch hẹn đã thanh toán, không thể hủy. Vui lòng xử lý hoàn/hủy hóa đơn bằng nghiệp vụ riêng.", HttpStatus.BAD_REQUEST)
        , APPOINTMENT_HAS_UNFINISHED_PARACLINICAL_ORDERS(1124, "Không thể hoàn tất khám khi còn dịch vụ cận lâm sàng chưa có kết quả.", HttpStatus.BAD_REQUEST)
        , CASH_HANDOVER_NOT_FOUND(1128, "Khong tim thay phieu ban giao tien mat.", HttpStatus.NOT_FOUND)
        , CASH_HANDOVER_INVALID_AMOUNT(1129, "So tien ban giao khong hop le.", HttpStatus.BAD_REQUEST)
        , CASH_HANDOVER_AMOUNT_EXCEEDS_REMAINING(1130, "So tien ban giao vuot qua tien mat con lai.", HttpStatus.BAD_REQUEST)
        , CASH_HANDOVER_SELF_TRANSFER(1131, "Khong the ban giao tien mat cho chinh minh.", HttpStatus.BAD_REQUEST)
        , CASH_HANDOVER_ACCESS_DENIED(1132, "Ban khong co quyen thao tac voi phieu ban giao nay.", HttpStatus.FORBIDDEN)
        , CASH_HANDOVER_INVALID_STATUS(1133, "Phieu ban giao khong o trang thai cho xu ly.", HttpStatus.BAD_REQUEST)
        , CASHIER_NOT_FOUND(1134, "Thu ngan nhan ban giao khong hop le.", HttpStatus.NOT_FOUND)
        ;

        private int code;
        private String message;
        private HttpStatusCode statusCode;

        ErrorCode(int code, String message, HttpStatusCode statusCode) {
                this.code = code;
                this.message = message;
                this.statusCode = statusCode;
        }
}
