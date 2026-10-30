# 🔄 Frontend Response Structure Update

**Date**: February 3, 2026  
**Status**: ✅ Completed

## 📋 Overview

Frontend đã được cập nhật hoàn toàn để phù hợp với **Backend Response Structure mới**:

### ❌ Cũ (Old):
```typescript
// Response trả về flat structure
{
  accessToken: "...",
  refreshToken: "...",
  user: { ... }
}
```

### ✅ Mới (New):
```typescript
// Response wrapped trong ApiResponse<T>
{
  code: 200,
  success: true,
  message: "Đăng nhập thành công",
  data: {
    accessToken: "...",
    refreshToken: "...",
    user: { ... }
  }
}
```

---

## 🎯 Key Changes

### 1. **New Type Definition** ([src/types/auth.types.ts](src/types/auth.types.ts))

```typescript
export interface ApiResponse<T = unknown> {
  code: number;
  success: boolean;
  message: string;
  data: T;
}
```

### 2. **Updated Service Layer** ([src/services/auth.service.ts](src/services/auth.service.ts))

**All methods now unwrap `ApiResponse` and return data directly:**

```typescript
// ✅ LOGIN - Returns LoginResponse
login: async (data: LoginRequest): Promise<LoginResponse> => {
  const response = await axiosClient.post<ApiResponse<LoginResponse>>('/auth/login', data);
  return response.data.data;  // Extract data from ApiResponse
}

// ✅ SEND OTP - Returns MessageResponse
sendOtp: async (data: SendOtpRequest): Promise<MessageResponse> => {
  const response = await axiosClient.post<ApiResponse<null>>('/auth/register/send-otp', data);
  return { message: response.data.message };  // Extract message from ApiResponse
}

// ✅ GET CURRENT USER - Returns UserInfoResponse
getCurrentUser: async (): Promise<UserInfoResponse> => {
  const response = await axiosClient.get<ApiResponse<UserInfoResponse>>('/auth/me');
  return response.data.data;  // Extract data from ApiResponse
}
```

### 3. **Updated Axios Interceptor** ([src/lib/axios.ts](src/lib/axios.ts))

**Refresh Token now extracts from `response.data.data`:**

```typescript
// Call refresh token API
const response = await axios.post(`${baseURL}/auth/refresh-token`, {
  refreshToken,
});

// Backend returns: { code, success, message, data: { accessToken, refreshToken } }
// Extract tokens from response.data.data
const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data.data;
```

### 4. **Updated Error Handler** ([src/lib/errorHandler.ts](src/lib/errorHandler.ts))

**Updated to handle new structure:**

```typescript
interface BackendErrorResponse {
  code?: number;
  success?: boolean;
  message?: string;
  data?: Record<string, string> | null;  // Field errors in data
}

// Extract error message from response.data.message
extractErrorMessage(error) => error.response?.data?.message

// Extract field errors from response.data.data
extractFieldErrors(error) => error.response?.data?.data

// New utility
isResponseSuccess(response) => response.success === true
```

### 5. **Updated Components**

All auth forms now work seamlessly with the new structure:

- ✅ [LoginForm.tsx](src/components/feature/auth/LoginForm.tsx)
- ✅ [RegisterForm.tsx](src/components/feature/auth/RegisterForm.tsx)
- ✅ [ChangePasswordForm.tsx](src/components/feature/auth/ChangePasswordForm.tsx)
- ✅ [ResetPasswordForm.tsx](src/components/feature/auth/ResetPasswordForm.tsx)
- ✅ [ProfilePage.tsx](app/profile/page.tsx)

---

## 📊 Response Types by Endpoint

| Endpoint | Response Type | Data Type | Access Pattern |
|----------|--------------|-----------|----------------|
| `POST /auth/login` | `ApiResponse<LoginResponse>` | `{ accessToken, refreshToken, user }` | `response.data.data` |
| `POST /auth/register/send-otp` | `ApiResponse<null>` | `null` | `response.data.message` |
| `POST /auth/register` | `ApiResponse<null>` | `null` | `response.data.message` |
| `POST /auth/refresh-token` | `ApiResponse<RefreshTokenResponse>` | `{ accessToken, refreshToken }` | `response.data.data` |
| `GET /auth/me` | `ApiResponse<UserInfoResponse>` | `{ id, fullName, roles, ... }` | `response.data.data` |
| `POST /auth/reset-password` | `ApiResponse<null>` | `null` | `response.data.message` |
| `POST /auth/change-password` | `ApiResponse<null>` | `null` | `response.data.message` |
| `POST /auth/logout` | `ApiResponse<null>` | `null` | `response.data.message` |

---

## 🎨 Usage Examples

### Success Case
```typescript
try {
  const response = await authService.login(credentials);
  // response is already unwrapped: { accessToken, refreshToken, user }
  setTokens(response.accessToken, response.refreshToken);
  setUser(response.user);
} catch (error) {
  // Error message from backend
  setError(extractErrorMessage(error));
}
```

### Error Handling
```typescript
catch (error) {
  // 1. Check for field errors (validation)
  if (hasFieldErrors(error)) {
    setFieldErrors(extractFieldErrors(error));
    setError('Vui lòng kiểm tra lại thông tin');
  } else {
    // 2. General error message
    setError(extractErrorMessage(error));
  }
}
```

### Message Response
```typescript
try {
  const response = await authService.sendOtp(data);
  // response = { message: "OTP đã được gửi đến email của bạn" }
  setSuccess(extractSuccessMessage(response));
} catch (error) {
  setError(extractErrorMessage(error));
}
```

---

## ✅ Benefits

1. **Type Safety**: Full TypeScript support với `ApiResponse<T>`
2. **Consistency**: Tất cả responses đều follow cùng structure
3. **Message Handling**: Backend messages được ưu tiên 100%
4. **Error Handling**: Standardized error extraction
5. **Clean Code**: Service layer unwraps responses, components nhận data trực tiếp

---

## 🔍 Verification

Run the application và test các flows:

```bash
npm run dev
```

Test cases:
- ✅ Login với credentials hợp lệ
- ✅ Login với credentials không hợp lệ (check error message từ backend)
- ✅ Register với OTP (check success message từ backend)
- ✅ Field validation errors (check field-level messages)
- ✅ Change password (check success/error messages)
- ✅ Reset password (check OTP messages)
- ✅ Get user profile (check data extraction)

---

## 📝 Migration Notes

Nếu tạo service mới, follow pattern:

```typescript
// For endpoints returning data
method: async (data): Promise<DataType> => {
  const response = await axiosClient.post<ApiResponse<DataType>>('/endpoint', data);
  return response.data.data;  // Extract data
}

// For endpoints returning only message
method: async (data): Promise<MessageResponse> => {
  const response = await axiosClient.post<ApiResponse<null>>('/endpoint', data);
  return { message: response.data.message };  // Extract message
}
```

---

## 🎯 Summary

**Frontend is now 100% compatible with Backend's ApiResponse structure!**

- ✅ All API calls access via `.data.data` or `.data.message`
- ✅ Error messages always come from backend
- ✅ Success messages always come from backend
- ✅ Field validation errors properly extracted
- ✅ TypeScript type safety maintained
- ✅ Clean separation: service unwraps, components use data directly
