# Frontend Update Summary - API Response Handling

**Date:** February 12, 2026  
**Status:** ✅ Complete  
**Backend Compatibility:** Standardized `ApiResponse<T>` format

---

## 📋 What Was Updated

### 1. Type Definitions ✅
**File:** [`src/types/api.types.ts`](src/types/api.types.ts) *(NEW)*

- ✅ Created `ApiResponse<T>` interface
- ✅ Created `ErrorCode` enum with 60+ error codes
- ✅ Added `isErrorCode()` type guard

### 2. Axios Configuration ✅
**File:** [`src/lib/axios.ts`](src/lib/axios.ts)

- ✅ Updated response interceptor to auto-unwrap `ApiResponse<T>` → `T`
- ✅ Added error code handling for token expiration
- ✅ Added error code handling for account locked
- ✅ Improved type safety with `ApiResponse<T>` generic

### 3. Error Handler ✅
**File:** [`src/lib/errorHandler.ts`](src/lib/errorHandler.ts)

- ✅ Updated `extractErrorMessage()` to work with new response format
- ✅ Added `extractErrorCode()` to get error enum
- ✅ Added `isErrorCodeMatch()` helper
- ✅ Updated `extractFieldErrors()` for new format
- ✅ Updated `isResponseSuccess()` for new format

### 4. Auth Service ✅
**File:** [`src/services/auth.service.ts`](src/services/auth.service.ts)

- ✅ Added documentation comments about auto-unwrapping
- ✅ Verified all methods work with new response format
- ✅ No breaking changes - services continue to work as before

---

## 📚 Documentation Created

### Main Guide
- **[FRONTEND_API_RESPONSE_GUIDE.md](FRONTEND_API_RESPONSE_GUIDE.md)** - Complete implementation guide with:
  - Response structure explanation
  - Auto-unwrapping mechanism
  - Error handling patterns
  - Best practices
  - Migration checklist
  - Troubleshooting guide

### Quick Reference
- **[EXAMPLES/QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md)** - One-page cheat sheet with:
  - Common patterns
  - Helper functions
  - Error codes
  - Do's and Don'ts

### Code Examples
- **[EXAMPLES/login-example.tsx](EXAMPLES/login-example.tsx)** - Login with error handling
- **[EXAMPLES/register-example.tsx](EXAMPLES/register-example.tsx)** - Registration with field validation
- **[EXAMPLES/service-pattern-example.ts](EXAMPLES/service-pattern-example.ts)** - Generic service template

---

## 🔄 How It Works

### Before (Old Format)
```typescript
// Backend returned:
{ code: 1001, success: false, message: "...", data: null }

// Frontend had to:
const response = await axiosClient.post('/auth/login', data);
if (response.data.success) {
  return response.data.data; // Manual unwrapping
}
```

### After (New Format)
```typescript
// Backend returns:
{ success: false, message: "...", data: null, error: "LOGIN_INVALID_CREDENTIALS", timestamp: "..." }

// Frontend receives (auto-unwrapped):
const response = await axiosClient.post('/auth/login', data);
return response.data; // ✅ Already unwrapped!
```

---

## 🎯 Key Features

### 1. Automatic Unwrapping
The axios response interceptor automatically extracts the `data` field from `ApiResponse<T>`, so services and components work with the actual data directly.

**Before:**
```typescript
const result = await axiosClient.get('/user/profile');
const profile = result.data.data; // Nested access
```

**After:**
```typescript
const result = await axiosClient.get('/user/profile');
const profile = result.data; // Direct access ✅
```

### 2. Backend Message Display
All error messages come from backend in Vietnamese. No need to hardcode messages in frontend.

```typescript
toast({ 
  variant: 'destructive',
  description: extractErrorMessage(error) // ✅ Backend message
});
```

### 3. Error Code Logic
Use `ErrorCode` enum for conditional logic instead of checking message strings.

```typescript
if (errorCode === ErrorCode.LOGIN_ACCOUNT_LOCKED) {
  router.push('/contact-admin');
}
```

### 4. Field Validation Support
Automatically extract field-level errors for form validation.

```typescript
if (hasFieldErrors(error)) {
  const fields = extractFieldErrors(error);
  // { email: 'Email đã tồn tại', phone: 'SĐT không hợp lệ' }
}
```

---

## 🚀 Usage Patterns

### Pattern 1: Simple Error Handling (90% of cases)
```typescript
try {
  const result = await someService.doSomething();
  // Use result...
} catch (error) {
  toast({ description: extractErrorMessage(error) });
}
```

### Pattern 2: Conditional Logic
```typescript
try {
  await authService.login(data);
} catch (error) {
  const errorCode = extractErrorCode(error);
  if (errorCode === ErrorCode.LOGIN_ACCOUNT_LOCKED) {
    // Special handling
  }
  toast({ description: extractErrorMessage(error) });
}
```

### Pattern 3: Form Validation
```typescript
try {
  await authService.register(formData);
} catch (error) {
  if (hasFieldErrors(error)) {
    const fields = extractFieldErrors(error);
    // Apply to form
  } else {
    toast({ description: extractErrorMessage(error) });
  }
}
```

---

## ✅ Migration Checklist

When updating existing components:

- [ ] Import helpers from `@/lib/errorHandler`
- [ ] Import `ErrorCode` from `@/types/api.types`
- [ ] Replace hardcoded messages with `extractErrorMessage(error)`
- [ ] Replace string checking with `ErrorCode` enum
- [ ] Remove manual unwrapping if any (`response.data.data` → `response.data`)
- [ ] Add field error handling for forms
- [ ] Test error scenarios

---

## 🛠️ Helper Functions

All available in [`src/lib/errorHandler.ts`](src/lib/errorHandler.ts):

| Function | Purpose |
|----------|---------|
| `extractErrorMessage(error)` | Get user-facing error message |
| `extractErrorCode(error)` | Get ErrorCode enum for logic |
| `isErrorCodeMatch(error, code)` | Check specific error code |
| `extractFieldErrors(error)` | Get field-level validation errors |
| `hasFieldErrors(error)` | Check if has field errors |
| `extractSuccessMessage(response)` | Get success message |
| `isResponseSuccess(response)` | Check if response is successful |

---

## 📊 Error Codes Coverage

**Total Error Codes:** 60+

### Categories:
- **Authentication (9 codes):** Login, Register, Password
- **User Management (14 codes):** CRUD, Lock/Unlock, Delete
- **Appointments (7 codes):** Create, Reschedule, Cancel
- **Payment (4 codes):** Payment flow, Insurance
- **Token Management (2 codes):** Refresh token handling
- **OTP Management (4 codes):** OTP validation
- **File Upload (3 codes):** Validation, Size limits
- **General (20+ codes):** Various features

**Full list:** [`src/types/api.types.ts`](src/types/api.types.ts)

---

## 🔒 Backward Compatibility

✅ **All existing services continue to work without changes!**

The axios interceptor handles unwrapping transparently, so:
- Services don't need to be updated (but documentation added)
- Existing error handling continues to work
- New error handling patterns are available for new code

---

## 🧪 Testing Recommendations

### Unit Tests
```typescript
// Test error message extraction
const error = mockAxiosError({ 
  response: { 
    data: { 
      success: false, 
      message: 'Test error', 
      error: ErrorCode.LOGIN_INVALID_CREDENTIALS 
    } 
  } 
});
expect(extractErrorMessage(error)).toBe('Test error');
expect(extractErrorCode(error)).toBe(ErrorCode.LOGIN_INVALID_CREDENTIALS);
```

### Integration Tests
- Test login with wrong credentials → Show backend message
- Test register with existing email → Show field error
- Test account locked → Redirect to contact page
- Test token expiration → Auto refresh

---

## 📖 Documentation Links

| Document | Purpose |
|----------|---------|
| [FRONTEND_API_RESPONSE_GUIDE.md](FRONTEND_API_RESPONSE_GUIDE.md) | Complete guide with examples |
| [EXAMPLES/QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md) | Quick cheat sheet |
| [EXAMPLES/login-example.tsx](EXAMPLES/login-example.tsx) | Login implementation |
| [EXAMPLES/register-example.tsx](EXAMPLES/register-example.tsx) | Registration with validation |
| [EXAMPLES/service-pattern-example.ts](EXAMPLES/service-pattern-example.ts) | Service template |

---

## 🎓 Training & Onboarding

### For New Developers
1. Read [QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md)
2. Review code examples in `EXAMPLES/`
3. Practice with Pattern 1 (Simple Error Handling)

### For Existing Developers
1. Review [Migration Checklist](#-migration-checklist)
2. Update one component at a time
3. Follow patterns in examples

---

## 🚨 Important Reminders

1. **✅ DO:** Use `extractErrorMessage(error)` for all user-facing messages
2. **✅ DO:** Use `ErrorCode` enum for conditional logic
3. **✅ DO:** Access `response.data` directly (already unwrapped)
4. **❌ DON'T:** Hardcode messages in frontend
5. **❌ DON'T:** Check message strings for logic
6. **❌ DON'T:** Access `response.data.data` (no longer needed)

---

## 📞 Support

If you encounter issues:
1. Check [Troubleshooting section](FRONTEND_API_RESPONSE_GUIDE.md#10-troubleshooting) in main guide
2. Review code examples in `EXAMPLES/`
3. Verify backend endpoint returns correct format
4. Check `ErrorCode` enum is up-to-date with backend

---

## ✨ Benefits

1. **Type Safety** - Full TypeScript support with generics
2. **Consistency** - All APIs follow same pattern
3. **Simplicity** - Auto-unwrapping reduces boilerplate
4. **Maintainability** - Centralized error handling
5. **UX** - Vietnamese messages from backend
6. **Logic Separation** - ErrorCode enum for business logic
7. **Form Support** - Field-level validation errors

---

**Status:** ✅ Ready for Production  
**Next Steps:** Start updating components following the patterns in examples
