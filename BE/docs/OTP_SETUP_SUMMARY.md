# OTP Verification System - Setup Summary

## ✅ Hoàn thành

Hệ thống xác thực OTP đã được tích hợp thành công vào dự án!

## 📋 Các file đã tạo

### 1. **Entity Layer**
- `src/main/java/g2/be/entity/VerificationToken.java`
  - Entity cho bảng `verification_tokens`
  - Chứa: id, email, otpCode, expiryDate, type, isUsed, createdAt
  - Enum TokenType: REGISTER, RESET_PASSWORD

### 2. **Repository Layer**
- `src/main/java/g2/be/repository/VerificationTokenRepository.java`
  - Interface JPA Repository
  - Methods: findByEmailAndOtpCodeAndTypeAndIsUsed, findByEmailAndType, deleteExpiredTokens, deleteByEmailAndType

### 3. **Service Layer**
- `src/main/java/g2/be/service/VerificationTokenService.java`
  - generateOtpCode() - Tạo mã 6 chữ số ngẫu nhiên
  - createOtpToken() - Tạo và lưu OTP mới
  - verifyOtp() - Xác thực OTP
  - markAsUsed() - Đánh dấu đã sử dụng
  - verifyAndMarkAsUsed() - Xác thực và đánh dấu (kết hợp)
  - deleteExpiredTokens() - Xóa OTP hết hạn
  - hasValidOtp() - Kiểm tra OTP còn hiệu lực

### 4. **Controller Layer**
- `src/main/java/g2/be/controller/OtpController.java`
  - POST /api/otp/send - Gửi OTP
  - POST /api/otp/verify - Xác thực OTP
  - POST /api/otp/resend - Gửi lại OTP

### 5. **DTO Layer**
- `src/main/java/g2/be/dto/request/SendOtpRequest.java`
- `src/main/java/g2/be/dto/request/VerifyOtpRequest.java`
- `src/main/java/g2/be/dto/response/OtpResponse.java`

### 6. **Configuration**
- `src/main/java/g2/be/config/SecurityConfig.java` (Đã cập nhật)
  - Thêm `/api/otp/**` vào whitelist (không cần authentication)

### 7. **Documentation**
- `docs/OTP_VERIFICATION.md` - Tài liệu chi tiết về OTP system
- `docs/OTP_SETUP_SUMMARY.md` - File này

## 🔧 Cấu hình

### Database
- Bảng `verification_tokens` sẽ được Hibernate tự động tạo khi khởi động
- Không cần chạy SQL script thủ công
- Kiểm tra: `spring.jpa.hibernate.ddl-auto=update` trong application.properties

### Security
- OTP endpoints đã được thêm vào whitelist
- Không cần JWT token để gọi API OTP

## 🚀 Cách sử dụng

### 1. Khởi động ứng dụng
```bash
mvn spring-boot:run
```

### 2. Truy cập Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 3. Test API OTP

#### Gửi OTP
```bash
curl -X POST http://localhost:8080/api/otp/send \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "type": "REGISTER"
  }'
```

**Response:**
```json
{
  "message": "Mã OTP đã được gửi đến email: test@example.com",
  "email": "test@example.com",
  "expiresInMinutes": 5
}
```

**Lưu ý:** Trong development, mã OTP sẽ được in ra console. Kiểm tra logs để lấy mã.

#### Xác thực OTP
```bash
curl -X POST http://localhost:8080/api/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "otpCode": "123456",
    "type": "REGISTER"
  }'
```

## 📊 Luồng hoạt động

```
1. User yêu cầu gửi OTP
   ↓
2. System tạo mã 6 chữ số ngẫu nhiên
   ↓
3. Lưu vào database với expiry_date = now + 5 phút
   ↓
4. [TODO] Gửi email cho user
   ↓
5. User nhập OTP
   ↓
6. System xác thực:
   - Email đúng?
   - OTP đúng?
   - Type đúng?
   - Chưa được sử dụng?
   - Chưa hết hạn?
   ↓
7. Nếu hợp lệ: Đánh dấu is_used = true
   ↓
8. Return success
```

## ⚠️ Lưu ý quan trọng

### 1. Email Service chưa được tích hợp
- Hiện tại mã OTP chỉ được in ra console
- Cần tích hợp JavaMailSender để gửi email thực
- Xem hướng dẫn trong file `OTP_VERIFICATION.md`

### 2. Rate Limiting chưa có
- Nên thêm giới hạn số lần gửi OTP (3 lần/15 phút)
- Tránh spam và lạm dụng

### 3. Production Checklist
- [ ] Tích hợp email service
- [ ] Thêm rate limiting
- [ ] Thêm validation cho DTOs
- [ ] Bật HTTPS
- [ ] Thêm logging và monitoring
- [ ] Scheduled task xóa OTP hết hạn
- [ ] Xem xét hash OTP trong database

## 🧪 Testing

### Kiểm tra database
```sql
-- Xem tất cả OTP tokens
SELECT * FROM verification_tokens ORDER BY created_at DESC;

-- Xem OTP chưa sử dụng
SELECT * FROM verification_tokens WHERE is_used = FALSE;

-- Xem OTP hết hạn
SELECT * FROM verification_tokens WHERE expiry_date < NOW();
```

### Test cases cần kiểm tra
1. ✅ Gửi OTP thành công
2. ✅ Xác thực OTP hợp lệ
3. ✅ OTP hết hạn → Error
4. ✅ OTP đã sử dụng → Error
5. ✅ OTP sai → Error
6. ✅ Email sai → Error
7. ✅ Type không hợp lệ → Error
8. ✅ Gửi OTP mới → OTP cũ bị xóa

## 📝 Các bước tiếp theo

### Ưu tiên cao
1. **Tích hợp Email Service**
   - Thêm dependency: spring-boot-starter-mail
   - Cấu hình SMTP
   - Tạo EmailService
   - Cập nhật OtpController để gửi email

2. **Tích hợp vào luồng đăng ký**
   - Sửa endpoint /api/auth/register
   - Yêu cầu xác thực OTP trước khi tạo tài khoản
   - Tạo endpoint /api/auth/register/send-otp

### Ưu tiên trung bình
3. **Tích hợp vào luồng reset password**
   - Tạo endpoint /api/auth/forgot-password
   - Gửi OTP khi user quên mật khẩu
   - Tạo endpoint /api/auth/reset-password (yêu cầu OTP)

4. **Thêm Validation**
   - @Email validation
   - @NotBlank validation
   - @Pattern validation cho OTP

### Ưu tiên thấp
5. **Rate Limiting**
   - Sử dụng Bucket4j hoặc Redis
   - Giới hạn 3 lần gửi OTP/15 phút

6. **Scheduled Tasks**
   - Tự động xóa OTP hết hạn mỗi giờ
   - @EnableScheduling trong main application

## 📞 Hỗ trợ

Nếu gặp vấn đề, kiểm tra:
1. **Logs** - Xem terminal khi khởi động ứng dụng
2. **Database** - Kiểm tra bảng verification_tokens đã được tạo chưa
3. **Swagger UI** - Test trực tiếp các endpoint
4. **Console** - Xem mã OTP được in ra (trong development)

## ✨ Tóm tắt

Hệ thống OTP đã sẵn sàng sử dụng! Bạn có thể:
- ✅ Tạo và lưu mã OTP
- ✅ Xác thực OTP
- ✅ Quản lý thời gian hết hạn
- ✅ Đánh dấu OTP đã sử dụng
- ✅ Test qua Swagger UI

**Điều duy nhất còn thiếu:** Gửi email thực tế (hiện tại chỉ in ra console).

Chúc bạn phát triển thành công! 🎉
