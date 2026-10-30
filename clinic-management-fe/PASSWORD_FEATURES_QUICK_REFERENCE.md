# Password Management Features - Quick Reference

## Features Implemented ✓

### 1. Reset Password (`/reset-password`)
- **Route**: `/reset-password`
- **Type**: Public (no authentication required)
- **Steps**:
  1. Enter email → OTP sent
  2. Enter OTP from email
  3. Enter new password (min 8 chars)
  4. Redirects to login on success
  
**File**: [ResetPasswordForm.tsx](src/components/feature/auth/ResetPasswordForm.tsx)

### 2. Change Password (`/change-password`)
- **Route**: `/change-password`
- **Type**: Protected (requires login)
- **Requirements**:
  - Current password
  - New password (min 8 chars, must differ from current)
  - Password confirmation
  
**File**: [ChangePasswordForm.tsx](src/components/feature/auth/ChangePasswordForm.tsx)

### 3. Get Current User (`/api/auth/me`)
- **Service Method**: `authService.getCurrentUser()`
- **Returns**: Full user profile with roles, email, phone, etc.
- **Auto-integrated**: Zustand store can fetch via `refreshUserInfo()`

**File**: [auth.service.ts](src/services/auth.service.ts)

## API Endpoints Used

```
POST /api/auth/reset-password
{
  "email": "user@email.com",
  "otpCode": "123456",
  "newPassword": "NewPass123"
}

POST /api/auth/change-password
{
  "currentPassword": "oldpass",
  "newPassword": "newpass",
  "confirmationPassword": "newpass"
}

GET /api/auth/me
(requires Authorization: Bearer token)
```

## Key Features

✓ **Multi-step form** with progress indicator (reset password)
✓ **OTP countdown timer** (automatic expiration display)
✓ **Password visibility toggle** (show/hide passwords)
✓ **Validation** (min 8 chars, password match check)
✓ **Error handling** with user-friendly messages
✓ **Success feedback** with auto-redirect
✓ **Responsive design** (mobile-friendly)
✓ **Security** (no hardcoded URLs, proper token handling)

## How to Use

### Reset Password Link
Added to login form - click "Quên mật khẩu?" next to password field

### Direct Links
- Reset: `http://yoursite.com/reset-password`
- Change: `http://yoursite.com/change-password`

### In Code
```typescript
// Service
import { authService } from '@/services/auth.service';

const result = await authService.resetPassword({
  email: 'user@email.com',
  otpCode: '123456',
  newPassword: 'NewPassword123'
});

const result = await authService.changePassword({
  currentPassword: 'OldPass',
  newPassword: 'NewPass',
  confirmationPassword: 'NewPass'
});

// Store - Get current user
const { user, refreshUserInfo } = useAuthStore();
await refreshUserInfo(); // Calls /api/auth/me
```

## Type Definitions

```typescript
// Request types
type ResetPasswordRequest = {
  email: string;
  otpCode: string;
  newPassword: string;
};

type ChangePasswordRequest = {
  currentPassword: string;
  newPassword: string;
  confirmationPassword: string;
};

// Response type
type UserInfoResponse = {
  id: number;
  username: string;
  email?: string;
  fullName: string;
  cccd?: string;
  gender?: string;
  phoneNumber?: string;
  avatarUrl?: string;
  roles: string[];
  profile?: object;
};

type MessageResponse = {
  message: string;
};
```

## Files Modified/Created

### Created
- [app/reset-password/page.tsx](app/reset-password/page.tsx)
- [app/change-password/page.tsx](app/change-password/page.tsx)
- [src/components/feature/auth/ResetPasswordForm.tsx](src/components/feature/auth/ResetPasswordForm.tsx)
- [src/components/feature/auth/ChangePasswordForm.tsx](src/components/feature/auth/ChangePasswordForm.tsx)
- [PASSWORD_MANAGEMENT.md](PASSWORD_MANAGEMENT.md) - Full documentation

### Modified
- [src/types/auth.types.ts](src/types/auth.types.ts) - Added new request types
- [src/services/auth.service.ts](src/services/auth.service.ts) - Added new methods
- [src/components/feature/auth/LoginForm.tsx](src/components/feature/auth/LoginForm.tsx) - Added reset password link

### Unchanged (Already Support)
- [src/store/auth.store.ts](src/store/auth.store.ts) - Already has `getCurrentUser()` and `refreshUserInfo()`
- [src/lib/axios.ts](src/lib/axios.ts) - Proper interceptors already in place

## Component Architecture

Following MANDATORY RULES:
- ✓ No hardcoded URLs (uses `process.env.NEXT_PUBLIC_API_URL`)
- ✓ API calls in service layer (`src/services/`)
- ✓ Components use `'use client'` (has hooks)
- ✓ Proper error handling & user feedback
- ✓ TypeScript types for all requests/responses
- ✓ Zustand for state management

## Testing Scenarios

### Reset Password
1. Go to `/reset-password`
2. Enter valid email
3. Check backend logs/email for OTP
4. Enter OTP (format: 123456)
5. Set new password
6. Should see success message and redirect to login

### Change Password
1. Login first
2. Go to `/change-password`
3. Enter current password
4. Enter new password (different, min 8 chars)
5. Confirm password
6. Should see success message and redirect to dashboard

### Get User Info
```typescript
// In any component
const { user } = useAuthStore();
console.log(user); // Full user object from last login

// Or refresh from server
const { refreshUserInfo } = useAuthStore();
await refreshUserInfo(); // Calls GET /api/auth/me
```

## Troubleshooting

### OTP Not Sent
- Check backend OTP service is running
- Verify email configuration
- Check email spam folder

### Password Change Failed
- Ensure current password is correct
- Check new password meets requirements (8+ chars, different from current)
- Verify authorization token is valid

### User Info Not Loading
- Ensure user is authenticated
- Check token is not expired
- Verify `/api/auth/me` endpoint is accessible

## Next Steps (Optional)

1. **Add rate limiting** to password reset (backend)
2. **Email notifications** when password changes
3. **Password strength indicator** in form
4. **Session invalidation** on password change
5. **2FA support** for enhanced security

---
**Status**: ✓ Ready to Use
**Version**: 1.0
**Date**: February 3, 2026
