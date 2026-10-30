# Frontend API Response - Quick Reference Card

## 📦 Import Statement
```typescript
import { extractErrorMessage, extractErrorCode, isErrorCodeMatch } from '@/lib/errorHandler';
import { ErrorCode } from '@/types/api.types';
```

---

## ✅ Basic Error Handling (Most Common)

```typescript
try {
  const result = await someService.doSomething();
  // Use result directly - already unwrapped
} catch (error) {
  toast({ 
    variant: 'destructive',
    description: extractErrorMessage(error) // ✅ Always shows backend message
  });
}
```

---

## 🔍 Conditional Logic with Error Code

```typescript
try {
  await authService.login(data);
} catch (error) {
  const errorCode = extractErrorCode(error);
  
  if (errorCode === ErrorCode.LOGIN_ACCOUNT_LOCKED) {
    router.push('/contact-admin');
  }
  
  toast({ 
    variant: 'destructive',
    description: extractErrorMessage(error) 
  });
}
```

---

## 📝 Field Validation Errors

```typescript
import { hasFieldErrors, extractFieldErrors } from '@/lib/errorHandler';

try {
  await authService.register(formData);
} catch (error) {
  if (hasFieldErrors(error)) {
    const fields = extractFieldErrors(error);
    // fields = { email: 'Email đã tồn tại', phone: 'SĐT không hợp lệ' }
    Object.entries(fields).forEach(([field, msg]) => {
      setError(field, { message: msg });
    });
  } else {
    toast({ 
      variant: 'destructive',
      description: extractErrorMessage(error) 
    });
  }
}
```

---

## 🎯 Multiple Error Codes

```typescript
try {
  await appointmentService.create(data);
} catch (error) {
  const errorCode = extractErrorCode(error);
  
  switch (errorCode) {
    case ErrorCode.CREATE_APPOINTMENT_FULL:
      fetchAlternativeSlots();
      break;
    case ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS:
      formRef.current?.focus();
      break;
    default:
      // General error
      break;
  }
  
  toast({ 
    variant: 'destructive',
    description: extractErrorMessage(error) 
  });
}
```

---

## 🛠️ Helper Functions Reference

| Function | Returns | Use Case |
|----------|---------|----------|
| `extractErrorMessage(error)` | `string` | Get user-facing error message |
| `extractErrorCode(error)` | `ErrorCode \| null` | Get error code for logic |
| `isErrorCodeMatch(error, code)` | `boolean` | Check specific error code |
| `extractFieldErrors(error)` | `Record<string, string>` | Get field-level errors |
| `hasFieldErrors(error)` | `boolean` | Check if has field errors |

---

## 🚫 Common Mistakes to AVOID

### ❌ DON'T: Hardcode messages
```typescript
// ❌ WRONG
toast({ description: 'Đăng nhập thất bại' });
```

### ✅ DO: Use backend message
```typescript
// ✅ CORRECT
toast({ description: extractErrorMessage(error) });
```

---

### ❌ DON'T: Check message text
```typescript
// ❌ WRONG
if (error.response.data.message.includes('khóa')) {
  // logic
}
```

### ✅ DO: Check error code
```typescript
// ✅ CORRECT
if (isErrorCodeMatch(error, ErrorCode.LOGIN_ACCOUNT_LOCKED)) {
  // logic
}
```

---

### ❌ DON'T: Access nested data
```typescript
// ❌ WRONG (interceptor already unwrapped)
const user = response.data.data;
```

### ✅ DO: Access data directly
```typescript
// ✅ CORRECT
const user = response.data;
```

---

## 📋 Most Common Error Codes

```typescript
// Authentication
ErrorCode.LOGIN_INVALID_CREDENTIALS
ErrorCode.LOGIN_ACCOUNT_LOCKED
ErrorCode.REGISTER_EMAIL_EXISTS
ErrorCode.REGISTER_PHONE_EXISTS

// Password
ErrorCode.CHANGE_PASSWORD_INCORRECT
ErrorCode.FORGOT_PASSWORD_INVALID_OTP

// User Management
ErrorCode.USER_NOT_EXIST
ErrorCode.USER_ALREADY_LOCKED
ErrorCode.USER_DELETED

// Appointments
ErrorCode.CREATE_APPOINTMENT_FULL
ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS
ErrorCode.CANCEL_APPOINTMENT_NOT_ALLOWED

// Payment
ErrorCode.PAYMENT_FAILED
ErrorCode.PAYMENT_NO_PAYABLE_ITEMS

// Token
ErrorCode.REFRESH_TOKEN_EXPIRED
ErrorCode.REFRESH_TOKEN_INVALID

// OTP
ErrorCode.OTP_EXPIRED
ErrorCode.OTP_INVALID_OR_USED
```

**See full list:** [`src/types/api.types.ts`](../src/types/api.types.ts)

---

## 🎓 Complete Guide

For detailed examples and patterns, see:
- 📖 [FRONTEND_API_RESPONSE_GUIDE.md](../FRONTEND_API_RESPONSE_GUIDE.md)
- 💻 [EXAMPLES/login-example.tsx](login-example.tsx)
- 💻 [EXAMPLES/register-example.tsx](register-example.tsx)
- 💻 [EXAMPLES/service-pattern-example.ts](service-pattern-example.ts)

---

## ⚡ Remember

1. **Response is auto-unwrapped** - Access `response.data` directly
2. **Message from backend** - Use `extractErrorMessage(error)`
3. **Logic by ErrorCode** - Use `extractErrorCode(error)` or `isErrorCodeMatch()`
4. **Never hardcode messages** - Backend provides Vietnamese messages
5. **Field errors for forms** - Use `hasFieldErrors()` and `extractFieldErrors()`
