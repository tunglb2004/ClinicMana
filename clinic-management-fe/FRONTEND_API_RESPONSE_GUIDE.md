# Frontend API Response Handling Guide

## Overview

Backend đã được refactor để trả về định dạng phản hồi chuẩn. Frontend đã được cập nhật để xử lý tự động.

---

## 1. Cấu trúc Response Chuẩn

Mọi API trả về định dạng thống nhất:

```typescript
interface ApiResponse<T> {
  success: boolean;       // true: Thành công, false: Thất bại
  message: string;        // Thông báo tiếng Việt cho User
  data: T | null;         // Dữ liệu trả về
  error: ErrorCode | null; // Mã lỗi (Enum) để xử lý logic
  timestamp: string;      // Thời gian phản hồi
}
```

---

## 2. Automatic Unwrapping (Tự động Giải Nén)

**Axios Interceptor đã tự động unwrap response:**

```typescript
// Backend trả về:
{
  "success": true,
  "message": "Đăng nhập thành công.",
  "data": { "token": "...", "username": "nguyenvana" },
  "error": null,
  "timestamp": "2026-02-12T18:00:00"
}

// Frontend nhận được (response.data):
{ "token": "...", "username": "nguyenvana" }
```

**Bạn không cần kiểm tra `response.data.data` nữa!**

---

## 3. Cách Sử Dụng Trong Service

### ✅ Đúng (Recommended)

```typescript
import axiosClient from '@/lib/axios';

export const userService = {
  getProfile: async (): Promise<UserProfile> => {
    const response = await axiosClient.get<UserProfile>('/user/profile');
    return response.data; // ✅ Đã được unwrap tự động
  },
  
  updateProfile: async (data: UpdateProfileRequest): Promise<MessageResponse> => {
    const response = await axiosClient.put<MessageResponse>('/user/profile', data);
    return response.data; // ✅ Đã được unwrap tự động
  },
};
```

### ❌ Sai (Không cần thiết)

```typescript
// ❌ KHÔNG CẦN LÀM NHƯ NÀY
const response = await axiosClient.get('/user/profile');
if (response.data.success) {
  return response.data.data; // ❌ response.data đã là data rồi!
}
```

---

## 4. Xử Lý Lỗi Trong Component

### A. Hiển Thị Message

```typescript
import { extractErrorMessage } from '@/lib/errorHandler';
import { toast } from '@/hooks/use-toast';

try {
  const result = await authService.login({ identifier, password });
  toast({ description: 'Đăng nhập thành công' });
} catch (error) {
  // ✅ Hiển thị message từ backend
  toast({ 
    variant: 'destructive',
    description: extractErrorMessage(error) 
  });
}
```

### B. Xử Lý Logic Theo Error Code

```typescript
import { extractErrorCode, extractErrorMessage, isErrorCodeMatch } from '@/lib/errorHandler';
import { ErrorCode } from '@/types/api.types';

try {
  await authService.login({ identifier, password });
} catch (error) {
  const errorCode = extractErrorCode(error);
  
  // ✅ Check theo error code (KHÔNG check theo message text)
  if (errorCode === ErrorCode.LOGIN_ACCOUNT_LOCKED) {
    // Xử lý logic đặc biệt cho tài khoản bị khóa
    router.push('/contact-admin');
  } else if (errorCode === ErrorCode.LOGIN_INVALID_CREDENTIALS) {
    // Xử lý logic sai thông tin đăng nhập
    setAttempts(prev => prev + 1);
  }
  
  // Hiển thị message cho user
  toast({ 
    variant: 'destructive',
    description: extractErrorMessage(error) 
  });
}
```

### C. Xử Lý Lỗi Validation (Field Errors)

```typescript
import { extractFieldErrors, hasFieldErrors } from '@/lib/errorHandler';

try {
  await authService.register(formData);
} catch (error) {
  if (hasFieldErrors(error)) {
    // Xử lý lỗi từng field
    const fieldErrors = extractFieldErrors(error);
    
    // Ví dụ với React Hook Form
    Object.entries(fieldErrors).forEach(([field, message]) => {
      setError(field as keyof FormData, { 
        type: 'manual', 
        message 
      });
    });
  } else {
    // Lỗi chung
    toast({ 
      variant: 'destructive',
      description: extractErrorMessage(error) 
    });
  }
}
```

---

## 5. Helper Functions

### Available Helpers

```typescript
// 1. Lấy error message (luôn có giá trị)
extractErrorMessage(error: unknown): string

// 2. Lấy error code (có thể null)
extractErrorCode(error: unknown): ErrorCode | null

// 3. Check error code cụ thể
isErrorCodeMatch(error: unknown, code: ErrorCode): boolean

// 4. Lấy field errors (cho validation)
extractFieldErrors(error: unknown): Record<string, string>

// 5. Check có field errors không
hasFieldErrors(error: unknown): boolean

// 6. Lấy success message
extractSuccessMessage(response: ApiResponse): string
```

### Examples

```typescript
import { 
  extractErrorCode, 
  extractErrorMessage, 
  isErrorCodeMatch 
} from '@/lib/errorHandler';
import { ErrorCode } from '@/types/api.types';

// Example 1: Simple error handling
try {
  await someService.doSomething();
} catch (error) {
  console.error(extractErrorMessage(error));
}

// Example 2: Conditional logic based on error code
try {
  await authService.changePassword(data);
} catch (error) {
  if (isErrorCodeMatch(error, ErrorCode.CHANGE_PASSWORD_INCORRECT)) {
    // Mật khẩu cũ không đúng
    setOldPasswordError('Mật khẩu hiện tại không đúng');
  }
  toast({ variant: 'destructive', description: extractErrorMessage(error) });
}

// Example 3: Multiple error codes
try {
  await appointmentService.create(data);
} catch (error) {
  const errorCode = extractErrorCode(error);
  
  switch (errorCode) {
    case ErrorCode.CREATE_APPOINTMENT_FULL:
      // Khung giờ đã đầy - hiển thị khung giờ khác
      fetchAvailableSlots();
      break;
    case ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS:
      // Thiếu thông tin - focus vào form
      formRef.current?.focus();
      break;
    default:
      // Lỗi chung
      break;
  }
  
  toast({ variant: 'destructive', description: extractErrorMessage(error) });
}
```

---

## 6. Error Codes Reference

### Authentication (1001-1009)
- `LOGIN_INVALID_CREDENTIALS` - Sai thông tin đăng nhập
- `LOGIN_ACCOUNT_LOCKED` - Tài khoản bị khóa
- `LOGIN_SUCCESS` - Đăng nhập thành công
- `REGISTER_EMAIL_EXISTS` - Email đã tồn tại
- `REGISTER_PHONE_EXISTS` - SĐT đã tồn tại
- `FORGOT_PASSWORD_OTP_SENT` - Đã gửi OTP
- `FORGOT_PASSWORD_INVALID_OTP` - OTP không hợp lệ
- `CHANGE_PASSWORD_INCORRECT` - Mật khẩu cũ sai
- `CHANGE_PASSWORD_SUCCESS` - Đổi mật khẩu thành công

### User Management (1010-1015, 1049-1062)
- `UPDATE_PROFILE_MISSING_FIELDS` - Thiếu thông tin
- `UPDATE_PROFILE_SUCCESS` - Cập nhật thành công
- `USER_NOT_EXIST` - User không tồn tại
- `USER_DELETED` - User đã xóa
- `USER_ALREADY_LOCKED` - User đã bị khóa
- `DELETE_USER_SUCCESS` - Xóa thành công
- `LOCK_USER_SUCCESS` - Khóa thành công
- `UNLOCK_USER_SUCCESS` - Mở khóa thành công

### Appointments (1020-1026)
- `CREATE_APPOINTMENT_MISSING_FIELDS` - Thiếu thông tin đặt lịch
- `CREATE_APPOINTMENT_FULL` - Khung giờ đã đầy
- `CREATE_APPOINTMENT_SUCCESS` - Đặt lịch thành công
- `RESCHEDULE_SUCCESS` - Dời lịch thành công
- `CANCEL_APPOINTMENT_NOT_ALLOWED` - Không thể hủy
- `CANCEL_APPOINTMENT_SUCCESS` - Hủy lịch thành công

### Payment (1027-1030)
- `PAYMENT_NO_PAYABLE_ITEMS` - Không có khoản thanh toán
- `PAYMENT_INSURANCE_APPLIED` - Đã áp dụng BHYT
- `PAYMENT_FAILED` - Thanh toán thất bại
- `PAYMENT_SUCCESS` - Thanh toán thành công

### Token & OTP (1063-1070)
- `REFRESH_TOKEN_EXPIRED` - Refresh token hết hạn
- `REFRESH_TOKEN_INVALID` - Refresh token không hợp lệ
- `OTP_EXPIRED` - OTP hết hạn
- `OTP_INVALID_OR_USED` - OTP không hợp lệ/đã dùng
- `VERIFY_OTP_SUCCESS` - Xác thực OTP thành công

**Xem đầy đủ tại:** [`src/types/api.types.ts`](src/types/api.types.ts)

---

## 7. Best Practices

### ✅ DO

1. **Luôn hiển thị message từ backend**
   ```typescript
   toast({ description: extractErrorMessage(error) });
   ```

2. **Xử lý logic theo error code**
   ```typescript
   if (errorCode === ErrorCode.LOGIN_ACCOUNT_LOCKED) {
     router.push('/contact-admin');
   }
   ```

3. **Xử lý field errors cho form validation**
   ```typescript
   if (hasFieldErrors(error)) {
     const fields = extractFieldErrors(error);
     // Apply to form
   }
   ```

### ❌ DON'T

1. **KHÔNG hardcode message ở frontend**
   ```typescript
   // ❌ SAI
   toast({ description: 'Đăng nhập thất bại' });
   
   // ✅ ĐÚNG
   toast({ description: extractErrorMessage(error) });
   ```

2. **KHÔNG check message text**
   ```typescript
   // ❌ SAI
   if (error.response.data.message.includes('khóa')) {
     // logic
   }
   
   // ✅ ĐÚNG
   if (isErrorCodeMatch(error, ErrorCode.LOGIN_ACCOUNT_LOCKED)) {
     // logic
   }
   ```

3. **KHÔNG truy cập response.data.data**
   ```typescript
   // ❌ SAI (interceptor đã unwrap rồi)
   const user = response.data.data;
   
   // ✅ ĐÚNG
   const user = response.data;
   ```

---

## 8. Migration Checklist

Khi update code cũ sang format mới:

- [ ] Import các helper từ `@/lib/errorHandler`
- [ ] Import `ErrorCode` từ `@/types/api.types`
- [ ] Thay thế hardcoded message bằng `extractErrorMessage(error)`
- [ ] Thay thế string checking bằng `ErrorCode` enum
- [ ] Xóa logic unwrap thủ công (nếu có `response.data.data`)
- [ ] Thêm field error handling cho form (nếu có validation)
- [ ] Test các trường hợp error khác nhau

---

## 9. Testing

### Manual Testing

```typescript
// 1. Test success case
const response = await authService.login({ identifier: 'test@example.com', password: '123456' });
console.log('Login success:', response); // Should be unwrapped data

// 2. Test error case
try {
  await authService.login({ identifier: 'test@example.com', password: 'wrong' });
} catch (error) {
  console.log('Error message:', extractErrorMessage(error));
  console.log('Error code:', extractErrorCode(error));
}

// 3. Test field errors
try {
  await authService.register({ email: 'invalid', password: '123' });
} catch (error) {
  if (hasFieldErrors(error)) {
    console.log('Field errors:', extractFieldErrors(error));
  }
}
```

---

## 10. Troubleshooting

### Issue: Response không đúng cấu trúc

**Nguyên nhân:** Backend chưa update hết endpoints  
**Giải pháp:** Check backend endpoint có trả về `{ success, message, data, error, timestamp }` không

### Issue: Error code không khớp

**Nguyên nhân:** Frontend enum chưa sync với backend  
**Giải pháp:** Copy ErrorCode mới nhất từ backend vào [`src/types/api.types.ts`](src/types/api.types.ts)

### Issue: Message không hiển thị tiếng Việt

**Nguyên nhân:** Backend trả về message bằng tiếng Anh  
**Giải pháp:** Báo team backend cập nhật message tiếng Việt

---

## Summary

1. ✅ **Axios Interceptor tự động unwrap** → `response.data` là data thật
2. ✅ **Hiển thị message từ backend** → Dùng `extractErrorMessage(error)`
3. ✅ **Xử lý logic theo ErrorCode** → Dùng `extractErrorCode(error)` hoặc `isErrorCodeMatch(error, code)`
4. ✅ **Không hardcode message** → Backend đã có message tiếng Việt chuẩn
5. ✅ **Xử lý field errors** → Dùng `hasFieldErrors(error)` và `extractFieldErrors(error)`

**Tất cả code mẫu và helper đã sẵn sàng sử dụng!**
