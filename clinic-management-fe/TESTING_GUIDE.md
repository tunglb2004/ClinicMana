# 🚀 Quick Start - Testing the Changes

## Files Modified

1. **[src/types/auth.types.ts](src/types/auth.types.ts)** - Type definitions
   - Added `SendOtpRequest`
   - Modified `LoginRequest` (username → identifier)
   - Modified `RegisterRequest` (added email, otpCode)

2. **[src/services/auth.service.ts](src/services/auth.service.ts)** - API service
   - Added `sendOtp()` method
   - Updated `login()` and `register()` methods

3. **[src/components/feature/auth/LoginForm.tsx](src/components/feature/auth/LoginForm.tsx)** - Login page
   - Changed username field to identifier
   - Updated placeholder and label text

4. **[src/components/feature/auth/RegisterForm.tsx](src/components/feature/auth/RegisterForm.tsx)** - Register page
   - Implemented 2-step OTP flow
   - Added email field and OTP verification form
   - Added countdown timer (5 minutes)
   - Added resend OTP functionality

---

## How to Test

### 1. Test Login Page

**URL:** `http://localhost:3000/login`

#### Test Case 1: Login with Email
```
Email/Phone: user@example.com
Password: password123
Expected: Logs in if credentials are valid
```

#### Test Case 2: Login with Phone
```
Email/Phone: 0912345678
Password: password123
Expected: Logs in if credentials are valid
```

#### Test Case 3: Invalid Credentials
```
Email/Phone: invalid@email.com
Password: wrongpassword
Expected: Error message displayed
```

---

### 2. Test Register Page - Step 1

**URL:** `http://localhost:3000/register`

#### Fill Form:
```
Full Name:      John Doe
Email:          john@example.com
Phone:          0912345678
Gender:         Nam/Nữ/Khác
Password:       password123
Confirm:        password123
```

#### Click "Tiếp tục" Button
- Should validate form
- Should call POST `/api/auth/register/send-otp`
- Should show success message
- Should display OTP verification form

---

### 3. Test Register Page - Step 2

#### Verify OTP Received:
- Check email inbox for OTP code (6 digits)
- Example: `123456`

#### Enter OTP:
```
OTP Code:       123456
```

#### Click "Hoàn tất đăng ký" Button
- Should validate OTP (6 digits)
- Should call POST `/api/auth/register`
- Should show success message
- Should redirect to `/login` after 2 seconds

---

### 4. Test Resend OTP

#### Steps:
1. Fill register form and send OTP
2. Before countdown expires, wait for it to finish
3. Click "Gửi lại mã OTP" button
4. Should receive new OTP in email
5. Countdown resets to 5 minutes

---

### 5. Test Back Navigation

#### Steps:
1. Fill register form and send OTP
2. Click "Quay lại" button
3. Should return to Step 1 form
4. Form data should be preserved (optional, based on requirements)

---

## Backend Testing with Swagger

**URL:** `http://localhost:8080/swagger-ui.html`

### Test Endpoints:

#### 1. Send OTP
```
POST /api/auth/register/send-otp
Request Body:
{
  "email": "test@example.com"
}

Expected Response:
{
  "code": 200,
  "message": "OTP sent successfully",
  "result": null
}
```

#### 2. Register with OTP
```
POST /api/auth/register
Request Body:
{
  "fullName": "John Doe",
  "email": "test@example.com",
  "phone": "0912345678",
  "password": "password123",
  "gender": "MALE",
  "otpCode": "123456"
}

Expected Response:
{
  "code": 201,
  "message": "Registration successful",
  "result": null
}
```

#### 3. Login
```
POST /api/auth/login
Request Body:
{
  "identifier": "test@example.com",  // or phone number
  "password": "password123"
}

Expected Response:
{
  "code": 200,
  "message": "Login successful",
  "result": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "user": {
      "id": 1,
      "fullName": "John Doe",
      "roles": ["PATIENT"]
    }
  }
}
```

---

## Common Issues & Solutions

### Issue 1: "Email không hợp lệ" error
**Solution:** Make sure email format is correct (e.g., user@example.com)

### Issue 2: "Số điện thoại phải có 10 chữ số"
**Solution:** Enter exactly 10 digits, e.g., 0912345678

### Issue 3: OTP not received
**Solution:** 
- Check spam/junk folder
- Verify email is correct
- Try resend OTP button

### Issue 4: "Mã OTP phải gồm 6 chữ số"
**Solution:** OTP must be exactly 6 numbers, no spaces or special characters

### Issue 5: "Mật khẩu xác nhận không khớp"
**Solution:** Ensure both password fields have identical values

### Issue 6: Login with wrong identifier format
**Solution:** Use either valid email (user@example.com) or phone (0912345678)

---

## Development Notes

### State Management:
- Using React `useState` for form state
- Email state is shared between OTP send and register calls

### Timer Implementation:
- Uses `useEffect` with cleanup to manage countdown
- Countdown stops at 0, allows resend
- Format: MM:SS (e.g., 04:45 for 4 minutes 45 seconds)

### Error Handling:
- Properly typed with `unknown` type
- Graceful fallback for missing backend messages
- User-friendly Vietnamese error messages

### Validation:
- Form validation before API calls
- OTP must be exactly 6 digits
- Email format validation
- Phone number length validation (10 digits)

---

## Next Steps

1. ✅ Frontend components updated
2. ✅ API service updated
3. ✅ Types updated
4. ⏳ Test all functionality
5. ⏳ Deploy to staging/production
6. ⏳ Monitor for errors

---

## Support

For issues or questions:
1. Check this guide first
2. Review [FRONTEND_CHANGES_SUMMARY.md](FRONTEND_CHANGES_SUMMARY.md)
3. Test with Swagger UI
4. Check browser console for errors
5. Review network tab in DevTools

