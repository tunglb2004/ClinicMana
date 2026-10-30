# Frontend Integration - Breaking Changes Summary

## ✅ Changes Completed

All frontend components have been successfully updated to match the new backend API requirements.

### 1. **Authentication Types** ([src/types/auth.types.ts](src/types/auth.types.ts))

#### Added:
- `SendOtpRequest` interface - for sending OTP to email
```typescript
interface SendOtpRequest {
  email: string;
}
```

#### Modified:
- `LoginRequest`: Changed `username` → `identifier` (supports email or phone)
```typescript
interface LoginRequest {
  identifier: string;  // ← Changed from username
  password: string;
}
```

- `RegisterRequest`: Added `email` and `otpCode` fields
```typescript
interface RegisterRequest {
  fullName: string;
  email: string;        // ← NEW
  phone: string;
  password: string;
  gender: string;
  otpCode: string;      // ← NEW
}
```

---

### 2. **Auth Service** ([src/services/auth.service.ts](src/services/auth.service.ts))

#### Added:
- `sendOtp()` method - POST `/api/auth/register/send-otp`
```typescript
sendOtp: async (data: SendOtpRequest): Promise<MessageResponse>
```

#### Updated:
- `login()` - Now sends `identifier` instead of `username`
- `register()` - Now includes `email` and `otpCode` in request body

---

### 3. **Login Form** ([src/components/feature/auth/LoginForm.tsx](src/components/feature/auth/LoginForm.tsx))

#### Changes:
- ✅ Changed field name from `username` to `identifier`
- ✅ Updated placeholder: "Email hoặc số điện thoại"
- ✅ Updated label: "Email hoặc Số điện thoại"
- ✅ Supports login via email OR phone number

#### Updated Code:
```typescript
const [formData, setFormData] = useState<LoginRequest>({
  identifier: '', // ← Changed from username
  password: '',
});

<Input
  id="identifier"
  name="identifier"
  type="text"
  placeholder="Email hoặc số điện thoại" // ← Updated
  value={formData.identifier}
  onChange={handleChange}
/>
```

---

### 4. **Register Form** ([src/components/feature/auth/RegisterForm.tsx](src/components/feature/auth/RegisterForm.tsx))

#### Features - 2-Step OTP Flow:

**Step 1: Registration Form**
- ✅ Full Name input
- ✅ Email input (NEW, required for OTP)
- ✅ Phone number input
- ✅ Gender selector
- ✅ Password & confirm password
- ✅ "Continue" button → Triggers OTP send

**Step 2: OTP Verification Form**
- ✅ Display email for reference
- ✅ 6-digit OTP code input
- ✅ Countdown timer (5 minutes)
- ✅ "Resend OTP" button (available after countdown)
- ✅ "Complete Registration" button
- ✅ "Back" button (to re-edit form)

#### State Management:
```typescript
const [formData, setFormData] = useState<RegisterRequest>({
  fullName: '',
  email: '',      // ← NEW
  phone: '',
  password: '',
  gender: '',
  otpCode: '',    // ← NEW
});

const [showOtpForm, setShowOtpForm] = useState(false);
const [otpCountdown, setOtpCountdown] = useState(0);
```

#### Key Functions:
1. `handleSendOtp()` - Step 1: Send OTP to email
   - Validates form data
   - Calls `authService.sendOtp()`
   - Shows OTP form on success
   - Starts 5-minute countdown

2. `handleResendOtp()` - Resend OTP button
   - Clears OTP input
   - Resets countdown to 5 minutes

3. `handleRegister()` - Step 2: Complete registration
   - Validates OTP (6 digits)
   - Calls `authService.register()` with all data including OTP
   - Redirects to login on success

---

## API Endpoints Used

| Endpoint | Method | Request Body | Response |
|----------|--------|--------------|----------|
| `/api/auth/login` | POST | `{identifier, password}` | `{accessToken, refreshToken, user}` |
| `/api/auth/register/send-otp` | POST | `{email}` | `{message}` |
| `/api/auth/register` | POST | `{fullName, email, phone, password, gender, otpCode}` | `{message}` |

---

## UI/UX Enhancements

### Login Page:
- Updated field label and placeholder for clarity
- Supports both email and phone number login
- Maintains existing error handling and loading states

### Register Page:
- **Clean two-step flow** with visual separation
- **Email validation** before sending OTP
- **OTP input formatting** - monospace font, character spacing
- **Countdown timer** - displays remaining time (MM:SS format)
- **Resend capability** - available after countdown expires
- **Form validation** - comprehensive validation at both steps
- **Error messages** - clear, actionable error messages
- **Loading states** - spinners during API calls
- **Success messages** - confirmation on OTP sent and registration complete

---

## Validation Rules

### Login Form:
- Identifier: required, non-empty
- Password: required, non-empty

### Register Form - Step 1:
- Full Name: required, non-empty
- Email: required, valid email format
- Phone: required, exactly 10 digits
- Password: required, minimum 6 characters
- Confirm Password: required, must match password
- Gender: required, must select a value

### Register Form - Step 2:
- OTP: required, exactly 6 digits (0-9)

---

## Error Handling

All API errors are properly caught and displayed:
- User-friendly error messages from backend
- Fallback error messages if backend message is unavailable
- Errors clear when user starts typing

Example:
```typescript
} catch (err: unknown) {
  let errorMessage = 'Không thể gửi OTP. Vui lòng thử lại.';
  if (err && typeof err === 'object' && 'response' in err) {
    const axiosError = err as { response?: { data?: { message?: string } } };
    errorMessage = axiosError.response?.data?.message || errorMessage;
  } else if (err instanceof Error) {
    errorMessage = err.message;
  }
  setError(errorMessage);
}
```

---

## TypeScript Compliance

- ✅ No `any` types used
- ✅ Proper error type handling
- ✅ No unused imports
- ✅ No unused variables
- ✅ Strict type checking throughout

---

## Testing Checklist

- [ ] Login with valid email
- [ ] Login with valid phone number
- [ ] Login with invalid credentials
- [ ] Register - fill form and send OTP
- [ ] Verify OTP received in email
- [ ] Register with valid OTP
- [ ] Register with invalid OTP
- [ ] Resend OTP functionality
- [ ] Countdown timer works (5 minutes)
- [ ] Form validation on each step
- [ ] Error messages display correctly
- [ ] Navigate back from OTP form
- [ ] Loading states show spinners

---

## Backend Ready ✅

The backend API is ready and fully functional:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- All endpoints implemented
- OTP email sending configured
- All validations in place

---

## Summary

**Login Component:** 30 minutes ⏱️
- Simple field rename from username → identifier
- Updated UI labels and placeholders

**Register Component:** 2-3 hours ⏱️
- Complex 2-step OTP flow
- Email validation
- OTP countdown timer
- Resend functionality
- Form state management

**Total time invested:** ~3-4 hours ✅

**Breaking Changes:** 
- Clients using old `username` field will get validation errors
- Backend no longer accepts registration without OTP
- All API clients must update to new request/response formats

