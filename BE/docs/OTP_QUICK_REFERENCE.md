# Quick Reference - OTP API

## API Endpoints

### 1. Send OTP (Gửi mã OTP)
```
POST http://localhost:8080/api/otp/send
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "type": "REGISTER"
}
```

**Success Response (200 OK):**
```json
{
  "message": "Mã OTP đã được gửi đến email: user@example.com",
  "email": "user@example.com",
  "expiresInMinutes": 5
}
```

---

### 2. Verify OTP (Xác thực mã OTP)
```
POST http://localhost:8080/api/otp/verify
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "otpCode": "123456",
  "type": "REGISTER"
}
```

**Success Response (200 OK):**
```json
{
  "message": "Xác thực OTP thành công"
}
```

**Error Response (400/500):**
```json
{
  "timestamp": "2026-01-31T15:46:29.123+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "OTP không hợp lệ hoặc đã được sử dụng",
  "path": "/api/otp/verify"
}
```

---

### 3. Resend OTP (Gửi lại mã OTP)
```
POST http://localhost:8080/api/otp/resend
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "type": "REGISTER"
}
```

## Token Types

| Type | Mô tả | Sử dụng cho |
|------|-------|-------------|
| `REGISTER` | Đăng ký tài khoản | Xác thực email khi đăng ký mới |
| `RESET_PASSWORD` | Đặt lại mật khẩu | Xác thực khi quên mật khẩu |

## Error Messages

| Lỗi | Nguyên nhân |
|-----|-------------|
| "OTP không hợp lệ hoặc đã được sử dụng" | Mã OTP sai hoặc đã dùng rồi |
| "OTP đã hết hạn. Vui lòng yêu cầu mã mới." | Quá 5 phút |
| "Loại OTP không hợp lệ. Chỉ chấp nhận: REGISTER hoặc RESET_PASSWORD" | Type sai định dạng |

## Testing với cURL

### Send OTP
```bash
curl -X POST http://localhost:8080/api/otp/send \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","type":"REGISTER"}'
```

### Verify OTP
```bash
curl -X POST http://localhost:8080/api/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","otpCode":"123456","type":"REGISTER"}'
```

## Testing với Postman

1. Import collection hoặc tạo request mới
2. Method: POST
3. URL: `http://localhost:8080/api/otp/send`
4. Headers: `Content-Type: application/json`
5. Body (raw, JSON):
```json
{
  "email": "your-email@example.com",
  "type": "REGISTER"
}
```
6. Send → Check console logs for OTP code
7. Copy OTP code
8. Create new request for verify
9. URL: `http://localhost:8080/api/otp/verify`
10. Body:
```json
{
  "email": "your-email@example.com",
  "otpCode": "YOUR_OTP_CODE",
  "type": "REGISTER"
}
```

## Database Queries

### Xem tất cả OTP
```sql
SELECT * FROM verification_tokens ORDER BY created_at DESC;
```

### Xem OTP chưa dùng
```sql
SELECT email, otp_code, type, expiry_date, created_at 
FROM verification_tokens 
WHERE is_used = FALSE 
  AND expiry_date > NOW()
ORDER BY created_at DESC;
```

### Xóa OTP hết hạn (manual cleanup)
```sql
DELETE FROM verification_tokens WHERE expiry_date < NOW();
```

## Service Methods Reference

```java
// Inject service
@Autowired
private VerificationTokenService verificationTokenService;

// 1. Create OTP
VerificationToken token = verificationTokenService.createOtpToken(
    "user@example.com", 
    VerificationToken.TokenType.REGISTER
);

// 2. Verify OTP (không đánh dấu đã dùng)
VerificationToken token = verificationTokenService.verifyOtp(
    "user@example.com", 
    "123456", 
    VerificationToken.TokenType.REGISTER
);

// 3. Verify và đánh dấu đã dùng (khuyên dùng)
VerificationToken token = verificationTokenService.verifyAndMarkAsUsed(
    "user@example.com", 
    "123456", 
    VerificationToken.TokenType.REGISTER
);

// 4. Kiểm tra có OTP hợp lệ không
boolean hasValid = verificationTokenService.hasValidOtp(
    "user@example.com", 
    VerificationToken.TokenType.REGISTER
);

// 5. Xóa OTP hết hạn
verificationTokenService.deleteExpiredTokens();
```

## Constants

```java
OTP_LENGTH = 6              // Độ dài mã OTP
OTP_EXPIRATION_MINUTES = 5  // Thời gian hết hạn (phút)
```

## Development Tips

1. **Xem OTP trong console:** 
   - Khi gọi `/api/otp/send`, mã OTP sẽ được in ra console
   - Format: `OTP Code for email@example.com: 123456`

2. **Test nhanh với Swagger:**
   - http://localhost:8080/swagger-ui.html
   - Tìm "OTP Verification" section
   - Try it out!

3. **Debug database:**
   - Check bảng `verification_tokens` sau mỗi request
   - Verify `is_used` và `expiry_date`

## Integration Examples

### Example 1: Register với OTP
```java
// Step 1: Send OTP
POST /api/otp/send
{
  "email": "newuser@example.com",
  "type": "REGISTER"
}

// Step 2: User nhập OTP từ email
// Step 3: Verify và Register
POST /api/auth/register
{
  "email": "newuser@example.com",
  "otpCode": "123456",
  "password": "securePassword123",
  "fullName": "John Doe",
  // ... other fields
}
// → Backend sẽ verify OTP trước khi tạo tài khoản
```

### Example 2: Reset Password với OTP
```java
// Step 1: Request OTP
POST /api/otp/send
{
  "email": "user@example.com",
  "type": "RESET_PASSWORD"
}

// Step 2: Verify OTP và reset password
POST /api/auth/reset-password
{
  "email": "user@example.com",
  "otpCode": "123456",
  "newPassword": "newSecurePassword123"
}
```
