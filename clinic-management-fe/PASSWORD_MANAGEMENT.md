# Password Management Features - Implementation Guide

## Overview
This document describes the implementation of three new authentication features:
1. **Reset Password** - Allow users to reset forgotten passwords using OTP
2. **Change Password** - Allow logged-in users to change their password
3. **Get Current User Info** - Retrieve detailed information about the logged-in user

## Implementation Details

### 1. API Endpoints

#### Reset Password (`POST /api/auth/reset-password`)
- **Purpose**: Reset password for users who forgot their password
- **Authentication**: Not required (public endpoint)
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "otpCode": "123456",
    "newPassword": "NewSecurePassword123"
  }
  ```
- **Response**: `{ "message": "Mật khẩu đã được đặt lại thành công" }`

**Flow**:
1. User enters email → System sends OTP to email
2. User receives OTP and enters it
3. User sets new password
4. System validates and updates password

#### Change Password (`POST /api/auth/change-password`)
- **Purpose**: Allow logged-in users to change their password
- **Authentication**: Required (Bearer Token)
- **Request Body**:
  ```json
  {
    "currentPassword": "OldPassword123",
    "newPassword": "NewPassword456",
    "confirmationPassword": "NewPassword456"
  }
  ```
- **Response**: `{ "message": "Mật khẩu đã được thay đổi thành công" }`

**Security Note**: 
- Current password is verified before allowing change
- New password must be different from current password
- Minimum 8 characters required

#### Get Current User (`GET /api/auth/me`)
- **Purpose**: Retrieve detailed information about the authenticated user
- **Authentication**: Required (Bearer Token)
- **Response**:
  ```json
  {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "cccd": "123456789",
    "gender": "male",
    "phoneNumber": "+84912345678",
    "avatarUrl": "https://example.com/avatar.jpg",
    "roles": ["PATIENT"],
    "profile": {}
  }
  ```

### 2. Frontend Components

#### ResetPasswordForm (`src/components/feature/auth/ResetPasswordForm.tsx`)
**Features**:
- Multi-step form (Email → OTP → New Password)
- Progress indicator showing current step
- OTP expiration countdown timer
- Password validation (minimum 8 characters)
- Password confirmation matching
- Error and success messages
- Back navigation between steps

**Usage**:
```tsx
import ResetPasswordForm from '@/components/feature/auth/ResetPasswordForm';

export default function ResetPasswordPage() {
  return <ResetPasswordForm />;
}
```

#### ChangePasswordForm (`src/components/feature/auth/ChangePasswordForm.tsx`)
**Features**:
- Single form for changing password
- Password visibility toggle for all fields
- Current password verification
- New password validation (minimum 8 characters)
- Password confirmation matching
- Prevents using same password as current
- Error and success messages
- Redirects to dashboard after success

**Usage**:
```tsx
import ChangePasswordForm from '@/components/feature/auth/ChangePasswordForm';

export default function ChangePasswordPage() {
  return <ChangePasswordForm />;
}
```

### 3. Service Layer (`src/services/auth.service.ts`)

New service methods added:

```typescript
// Reset password with OTP
resetPassword: async (data: ResetPasswordRequest): Promise<MessageResponse>

// Change password for logged-in user
changePassword: async (data: ChangePasswordRequest): Promise<MessageResponse>

// Get current user info (already existed, enhanced)
getCurrentUser: async (): Promise<UserInfoResponse>
```

### 4. Type Definitions (`src/types/auth.types.ts`)

```typescript
export interface ResetPasswordRequest {
  email: string;
  otpCode: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmationPassword: string;
}

export interface UserInfoResponse {
  id: number;
  username: string;
  email?: string;
  fullName: string;
  cccd?: string;
  gender?: string;
  phoneNumber?: string;
  avatarUrl?: string;
  roles: string[];
  profile?: Record<string, unknown>;
}
```

### 5. Pages Created

- **`app/reset-password/page.tsx`** - Reset password page (public route)
- **`app/change-password/page.tsx`** - Change password page (protected route)

## Security Considerations

### Password Reset Flow
1. ✓ Email verification via OTP required
2. ✓ OTP has expiration time (configurable, default per backend)
3. ✓ OTP is single-use
4. ✓ New password must meet requirements (8+ characters)
5. ✓ Rate limiting recommended on backend

### Change Password Flow
1. ✓ User must be authenticated (Bearer token required)
2. ✓ Current password must be verified
3. ✓ New password different from current password
4. ✓ Minimum 8 character requirement
5. ✓ Password confirmation must match

### Best Practices Followed
- **No hardcoded URLs**: All API calls use `process.env.NEXT_PUBLIC_API_URL`
- **Service Layer**: All API logic in `src/services/`
- **Reusable Components**: Forms are self-contained and reusable
- **Error Handling**: Comprehensive error messages with user feedback
- **User Feedback**: Success messages and loading states
- **Client Validation**: Form validation before submission
- **Token Management**: Automatic token handling via axios interceptors

## Usage Instructions

### For Users - Reset Forgotten Password
1. Navigate to `/reset-password`
2. Enter email address
3. Click "Gửi mã OTP"
4. Check email for 6-digit OTP code
5. Enter OTP code on the form
6. Set new password (minimum 8 characters)
7. Confirm password matches
8. Click "Đặt lại mật khẩu"
9. Redirected to login page on success

### For Users - Change Password (Logged In)
1. Navigate to `/change-password`
2. Enter current password
3. Enter new password (minimum 8 characters)
4. Confirm new password matches
5. Click "Cập nhật mật khẩu"
6. Redirected to dashboard on success

### For Developers - Update Current User Info

In any component, use the auth store to get current user:

```typescript
import { useAuthStore } from '@/store/auth.store';

export function MyComponent() {
  const { user, refreshUserInfo } = useAuthStore();

  // On component mount
  useEffect(() => {
    refreshUserInfo(); // Fetch latest user info from /api/auth/me
  }, []);

  return <div>{user?.fullName}</div>;
}
```

## Integration with Existing Features

### Login Form
- Added "Quên mật khẩu?" (Forgot password?) link next to password field
- Links to `/reset-password` page

### Auth Store
- `getCurrentUser()` method maps `UserInfoResponse` to `UserInfo`
- `refreshUserInfo()` automatically handles 401 responses

### Navigation
- Protected routes can check authentication status
- Redirect to login if not authenticated

## Environment Variables

Ensure these are set in `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## Testing Checklist

- [ ] Reset password flow works end-to-end
- [ ] OTP expiration timer displays correctly
- [ ] Password validation prevents weak passwords
- [ ] Change password flow works for logged-in users
- [ ] Error messages display correctly
- [ ] Success redirects to appropriate pages
- [ ] Forms reset after successful submission
- [ ] Password visibility toggle works
- [ ] Responsive design on mobile
- [ ] API calls use correct endpoint paths
- [ ] Authorization headers sent with protected endpoints
- [ ] User info retrieved correctly via `/api/auth/me`

## Future Enhancements

1. **Two-Factor Authentication (2FA)**
   - Add TOTP support for additional security

2. **Password Strength Indicator**
   - Real-time feedback on password strength

3. **Password History**
   - Prevent reusing recent passwords

4. **Session Management**
   - Force logout on password change
   - Show active sessions

5. **Email Notifications**
   - Notify user of password changes
   - Confirm password reset attempts

6. **Rate Limiting UI**
   - Display rate limit warnings
   - Countdown for retry attempts

## File Structure

```
src/
├── components/feature/auth/
│   ├── LoginForm.tsx (updated with reset password link)
│   ├── ResetPasswordForm.tsx (new)
│   └── ChangePasswordForm.tsx (new)
├── services/
│   └── auth.service.ts (updated with new methods)
├── types/
│   └── auth.types.ts (updated with new types)
└── store/
    └── auth.store.ts (unchanged, already supports getCurrentUser)

app/
├── reset-password/
│   └── page.tsx (new)
└── change-password/
    └── page.tsx (new)
```

## Related Documentation

- [Authentication System](../AUTH_README.md)
- [API Integration](../DOCUMENTATION_INDEX.md)
- [Type System](../src/types/auth.types.ts)
- [Service Layer](../src/services/auth.service.ts)

---

**Last Updated**: February 3, 2026
**Version**: 1.0
**Status**: Ready for Production
