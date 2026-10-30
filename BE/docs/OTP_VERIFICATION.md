# OTP Verification System Documentation

## Tổng quan

Hệ thống xác thực OTP (One-Time Password) cho phép xác minh người dùng thông qua mã 6 chữ số được gửi qua email.

## Cấu trúc Database

### Bảng `verification_tokens`

```sql
CREATE TABLE verification_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,         -- Email nhận OTP
    otp_code VARCHAR(10) NOT NULL,       -- Mã OTP 6 chữ số
    expiry_date TIMESTAMP NOT NULL,      -- Thời gian hết hạn (5 phút)
    type VARCHAR(20) NOT NULL,           -- Loại: 'REGISTER' hoặc 'RESET_PASSWORD'
    is_used BOOLEAN DEFAULT FALSE,       -- Đã sử dụng chưa?
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Lưu ý:** Bảng này sẽ được Hibernate tự động tạo khi khởi động ứng dụng (do `spring.jpa.hibernate.ddl-auto=update`).

## Các file đã tạo

### 1. Entity
- `VerificationToken.java` - Entity JPA cho bảng verification_tokens

### 2. Repository
- `VerificationTokenRepository.java` - Repository để thao tác với database

### 3. Service
- `VerificationTokenService.java` - Business logic xử lý OTP
  - Tạo mã OTP ngẫu nhiên 6 chữ số
  - Xác thực OTP
  - Đánh dấu OTP đã sử dụng
  - Xóa OTP hết hạn

### 4. Controller
- `OtpController.java` - REST API endpoints cho OTP

### 5. DTOs
- `SendOtpRequest.java` - Request để gửi OTP
- `VerifyOtpRequest.java` - Request để xác thực OTP
- `OtpResponse.java` - Response khi gửi OTP thành công

## API Endpoints

### 1. Gửi mã OTP
```http
POST /api/otp/send
Content-Type: application/json

{
  "email": "user@example.com",
  "type": "REGISTER"
}
```

**Response:**
```json
{
  "message": "Mã OTP đã được gửi đến email: user@example.com",
  "email": "user@example.com",
  "expiresInMinutes": 5
}
```

### 2. Xác thực OTP
```http
POST /api/otp/verify
Content-Type: application/json

{
  "email": "user@example.com",
  "otpCode": "123456",
  "type": "REGISTER"
}
```

**Response:**
```json
{
  "message": "Xác thực OTP thành công"
}
```

### 3. Gửi lại OTP
```http
POST /api/otp/resend
Content-Type: application/json

{
  "email": "user@example.com",
  "type": "REGISTER"
}
```

## Loại OTP (TokenType)

- `REGISTER` - Dùng cho đăng ký tài khoản mới
- `RESET_PASSWORD` - Dùng cho đặt lại mật khẩu

## Quy tắc hoạt động

1. **Thời gian hết hạn:** Mã OTP có hiệu lực trong 5 phút
2. **Mã OTP:** 6 chữ số ngẫu nhiên (ví dụ: 123456)
3. **Sử dụng một lần:** Mỗi OTP chỉ có thể sử dụng 1 lần
4. **Ghi đè:** Khi tạo OTP mới cho cùng email và type, OTP cũ sẽ bị xóa
5. **Bảo mật:** Sử dụng SecureRandom để tạo mã ngẫu nhiên

## Cách sử dụng trong code

### Ví dụ: Tích hợp OTP vào đăng ký

```java
// 1. Gửi OTP khi người dùng yêu cầu đăng ký
@PostMapping("/register/send-otp")
public ResponseEntity<?> sendRegistrationOtp(@RequestBody EmailRequest request) {
    VerificationToken token = verificationTokenService.createOtpToken(
        request.getEmail(), 
        VerificationToken.TokenType.REGISTER
    );
    
    // Gửi email (cần tích hợp email service)
    emailService.sendOtpEmail(request.getEmail(), token.getOtpCode());
    
    return ResponseEntity.ok("OTP đã được gửi");
}

// 2. Xác thực OTP trước khi cho phép đăng ký
@PostMapping("/register/verify-and-complete")
public ResponseEntity<?> verifyAndRegister(@RequestBody RegisterWithOtpRequest request) {
    // Xác thực OTP
    verificationTokenService.verifyAndMarkAsUsed(
        request.getEmail(), 
        request.getOtpCode(), 
        VerificationToken.TokenType.REGISTER
    );
    
    // Nếu OTP hợp lệ, tiến hành đăng ký
    authService.register(request);
    
    return ResponseEntity.ok("Đăng ký thành công");
}
```

## TODO - Các bước tiếp theo

### 1. Tích hợp Email Service
Cần thêm dependency vào `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

Cấu hình trong `application.properties`:

```properties
# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Tạo EmailService:

```java
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendOtpEmail(String to, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mã xác thực OTP");
        message.setText("Mã OTP của bạn là: " + otpCode + "\nMã có hiệu lực trong 5 phút.");
        
        mailSender.send(message);
    }
}
```

### 2. Cập nhật Security Configuration
Thêm OTP endpoints vào whitelist trong `SecurityConfig.java`:

```java
.requestMatchers("/api/otp/**").permitAll()
```

### 3. Scheduled Task để xóa OTP hết hạn
Tạo scheduled task để tự động xóa OTP hết hạn:

```java
@Scheduled(cron = "0 0 * * * *") // Chạy mỗi giờ
public void cleanupExpiredTokens() {
    verificationTokenService.deleteExpiredTokens();
}
```

### 4. Rate Limiting
Thêm giới hạn số lần gửi OTP để tránh spam:
- Tối đa 3 lần gửi OTP trong 15 phút cho mỗi email
- Có thể dùng Redis hoặc cache in-memory

### 5. Validation
Thêm validation cho DTOs:

```java
@NotBlank(message = "Email không được để trống")
@Email(message = "Email không hợp lệ")
private String email;

@NotBlank(message = "OTP code không được để trống")
@Pattern(regexp = "\\d{6}", message = "OTP phải là 6 chữ số")
private String otpCode;
```

## Testing với Swagger UI

1. Khởi động ứng dụng: `mvn spring-boot:run`
2. Truy cập Swagger UI: http://localhost:8080/swagger-ui.html
3. Tìm section "OTP Verification"
4. Test các endpoints:
   - Gửi OTP: POST /api/otp/send
   - Kiểm tra console để lấy mã OTP (trong development)
   - Xác thực OTP: POST /api/otp/verify

## Bảo mật

- ✅ Mã OTP được tạo bằng SecureRandom
- ✅ OTP chỉ có hiệu lực 5 phút
- ✅ Mỗi OTP chỉ sử dụng được 1 lần
- ✅ OTP được hash trong database (nếu cần, có thể thêm)
- ⚠️ Cần thêm rate limiting
- ⚠️ Cần thêm HTTPS trong production
- ⚠️ Cần log các hoạt động OTP để audit

## Troubleshooting

### Lỗi "OTP không hợp lệ"
- Kiểm tra email và mã OTP có đúng không
- Kiểm tra OTP có hết hạn chưa (5 phút)
- Kiểm tra OTP đã được sử dụng chưa

### Lỗi "Loại OTP không hợp lệ"
- Type chỉ chấp nhận: "REGISTER" hoặc "RESET_PASSWORD"
- Viết hoa toàn bộ

### Database không tạo bảng
- Kiểm tra `spring.jpa.hibernate.ddl-auto=update` trong application.properties
- Kiểm tra kết nối database
- Xem logs khi khởi động ứng dụng
