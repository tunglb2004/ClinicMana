# Hướng dẫn Update Frontend (API Response Handling)

Backend đã được refactor để trả về định dạng phản hồi chuẩn và thống nhất mã lỗi. Dưới đây là hướng dẫn chi tiết để Frontend cập nhật logic xử lý.

## 1. Cấu trúc Response chuẩn

Mọi API (GET, POST, PUT, DELETE) đều trả về định dạng JSON thống nhất sau:

```json
{
  "success": boolean,       // true: Thành công, false: Thất bại
  "message": "String",      // Thông báo (Tiếng Việt) để hiển thị cho User
  "data": Object | null,    // Dữ liệu trả về (nếu có)
  "error": "String" | null, // Mã lỗi (Enum Name) để xử lý logic (nếu success = false)
  "timestamp": "Date"       // Thời gian phản hồi
}
```

### Ví dụ Success
```json
{
  "success": true,
  "message": "Đăng nhập thành công.",
  "data": {
    "token": "eyJhbGciOi...",
    "username": "nguyenvana"
  },
  "error": null,
  "timestamp": "2026-02-12T18:00:00"
}
```

### Ví dụ Error
```json
{
  "success": false,
  "message": "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị hệ thống.",
  "data": null,
  "error": "LOGIN_ACCOUNT_LOCKED", // Dùng mã này để check logic
  "timestamp": "2026-02-12T18:00:00"
}
```

---

## 2. Logic Xử Lý Frontend

### Xử lý chung (Interceptor / Global Handler)

1.  **Luôn kiểm tra `success`**:
    *   Nếu `success == true`: Hiển thị `message` (nếu cần, ví dụ Toast Success) và lấy `data`.
    *   Nếu `success == false`: Hiển thị `message` (Toast Error) và xử lý theo `error` code (nếu cần).

2.  **Không hardcode message ở Frontend**:
    *   Backend đã trả về message chuẩn tiếng Việt. Hãy hiển thị trực tiếp `response.message`.

3.  **Xử lý Logic theo mã lỗi (`error`)**:
    *   Không check theo `message` text (vì text có thể đổi).
    *   Check theo `error` field (Enum Name).
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
    FORGOT_PASSWORD_INVALID_OTP(1007, "OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.", HttpStatus.BAD_REQUEST),

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
    ACCOUNT_DUPLICATE_EMAIL_CCCD(1014, "Email/CCCD đã tồn tại.", HttpStatus.BAD_REQUEST),

    // 09. Lock/Unlock Account (User confirmation usually handled in FE, but backend
    // might need success msg?)
    LOCK_UNLOCK_CONFIRM(1015, "Bạn có chắc chắn muốn thay đổi trạng thái tài khoản này không?", HttpStatus.OK),

    // 10. Delete Account/Item
    DELETE_CONFIRM(1016, "Bạn có chắc chắn muốn xóa mục này không? Thao tác có thể không thể hoàn tác.", HttpStatus.OK),

    // 11. Department/Room/Service
    DEPT_NAME_EXISTS(1017, "Tên đã tồn tại.", HttpStatus.BAD_REQUEST),

    // 12. Work Shift
    WORK_SHIFT_OVERLAP(1018, "Khung giờ ca làm bị trùng với ca làm hiện có.", HttpStatus.BAD_REQUEST),

    // 13. Patient Search
    PATIENT_SEARCH_NO_DATA(1019, "Không tìm thấy hồ sơ bệnh nhân phù hợp.", HttpStatus.NOT_FOUND),

    // 14. Create Appointment
    CREATE_APPOINTMENT_MISSING_FIELDS(1020, "Vui lòng điền đầy đủ các trường bắt buộc trước khi đặt lịch.",
            HttpStatus.BAD_REQUEST),
    CREATE_APPOINTMENT_FULL(1021, "Khung giờ đã đầy. Vui lòng chọn khung giờ khác.", HttpStatus.BAD_REQUEST),
    CREATE_APPOINTMENT_SUCCESS(1022, "Đặt lịch khám thành công.", HttpStatus.CREATED),

    // 15. Reschedule Appointment
    RESCHEDULE_CONFIRM(1023, "Bạn có chắc chắn muốn dời lịch khám này không?", HttpStatus.OK),
    RESCHEDULE_SUCCESS(1024, "Dời lịch khám thành công.", HttpStatus.OK),

    // 16. Cancel Appointment
    CANCEL_APPOINTMENT_NOT_ALLOWED(1025, "Bạn không thể hủy lịch khám này do chính sách hủy lịch của phòng khám.",
            HttpStatus.BAD_REQUEST),
    CANCEL_APPOINTMENT_SUCCESS(1026, "Hủy lịch khám thành công.", HttpStatus.OK),

    // 17. Payment
    PAYMENT_NO_PAYABLE_ITEMS(1027, "Không tìm thấy khoản cần thanh toán cho lượt khám này.", HttpStatus.NOT_FOUND),
    PAYMENT_INSURANCE_APPLIED(1028, "Đã áp dụng giảm trừ BHYT. Tổng tiền cần thanh toán đã được cập nhật.",
            HttpStatus.OK),
    PAYMENT_FAILED(1029, "Thanh toán thất bại. Vui lòng thử lại hoặc chọn phương thức khác.", HttpStatus.BAD_REQUEST),
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
    UPLOAD_FILE_UNSUPPORTED_TYPE(1036, "Định dạng tệp không được hỗ trợ. Vui lòng tải lên PDF/JPG/PNG (theo cấu hình).",
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
    USER_NOT_EXIST(1049, "Người dùng không tồn tại", HttpStatus.NOT_FOUND), // Added based on codebase analysis
    EMAIL_NOT_EXIST(1050, "Email không tồn tại trong hệ thống.", HttpStatus.NOT_FOUND), // Added based on codebase
                                                                                        // analysis
    PASSWORD_CONFIRMATION_INCORRECT(1051, "Mật khẩu mới và xác nhận mật khẩu không khớp", HttpStatus.BAD_REQUEST), // Added
                                                                                                                   // based
                                                                                                                   // on
                                                                                                                   // codebase
                                                                                                                   // analysis
    UPDATE_EMAIL_SUCCESS(1052, "Cập nhật email thành công.", HttpStatus.OK), // Added based on codebase analysis
    REGISTER_SUCCESS(1053, "Đăng ký thành công.", HttpStatus.OK), // Added based on codebase analysis

    // Additional ErrorCodes for refactoring
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
    ;