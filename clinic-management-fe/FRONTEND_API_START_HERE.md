# 🎯 START HERE - Frontend API Response Update

**Last Updated:** February 12, 2026  
**Status:** ✅ Complete & Ready to Use

---

## 📖 Quick Navigation

### 🚀 For Quick Start
- **[QUICK REFERENCE CARD](EXAMPLES/QUICK_REFERENCE.md)** ⭐ Start here for copy-paste patterns

### 📚 For Complete Understanding  
- **[FRONTEND UPDATE SUMMARY](FRONTEND_UPDATE_SUMMARY.md)** - What changed & why
- **[COMPLETE GUIDE](FRONTEND_API_RESPONSE_GUIDE.md)** - Detailed documentation

### 💻 For Code Examples
- **[Login Example](EXAMPLES/login-example.tsx)** - Error handling with ErrorCode
- **[Register Example](EXAMPLES/register-example.tsx)** - Field validation errors
- **[Service Pattern](EXAMPLES/service-pattern-example.ts)** - Generic service template

---

## ⚡ TL;DR - What You Need to Know

### 1. Response Auto-Unwrapping ✅
```typescript
// Backend returns: { success: true, message: "...", data: {...}, error: null }
// You receive: {...} (the data field)

const user = await userService.getProfile();
// user is already unwrapped! No need for user.data.data
```

### 2. Error Messages ✅
```typescript
import { extractErrorMessage } from '@/lib/errorHandler';

try {
  await someService.doSomething();
} catch (error) {
  toast({ description: extractErrorMessage(error) }); // Shows backend message
}
```

### 3. Error Code Logic ✅
```typescript
import { extractErrorCode, ErrorCode } from '@/lib/errorHandler';

try {
  await authService.login(data);
} catch (error) {
  if (extractErrorCode(error) === ErrorCode.LOGIN_ACCOUNT_LOCKED) {
    router.push('/contact-admin'); // Special handling
  }
  toast({ description: extractErrorMessage(error) });
}
```

---

## 📦 What's Included

### Core Files (Updated)
| File | Status | Purpose |
|------|--------|---------|
| [`src/types/api.types.ts`](src/types/api.types.ts) | ✅ NEW | ApiResponse<T> interface & ErrorCode enum |
| [`src/lib/axios.ts`](src/lib/axios.ts) | ✅ UPDATED | Auto-unwrapping interceptor |
| [`src/lib/errorHandler.ts`](src/lib/errorHandler.ts) | ✅ UPDATED | Error extraction helpers |
| [`src/services/auth.service.ts`](src/services/auth.service.ts) | ✅ UPDATED | Documentation added |

### Documentation Files (New)
| File | Purpose |
|------|---------|
| [FRONTEND_UPDATE_SUMMARY.md](FRONTEND_UPDATE_SUMMARY.md) | High-level overview of changes |
| [FRONTEND_API_RESPONSE_GUIDE.md](FRONTEND_API_RESPONSE_GUIDE.md) | Complete implementation guide |
| [EXAMPLES/QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md) | Cheat sheet for daily use |
| [EXAMPLES/login-example.tsx](EXAMPLES/login-example.tsx) | Login component example |
| [EXAMPLES/register-example.tsx](EXAMPLES/register-example.tsx) | Registration with validation |
| [EXAMPLES/service-pattern-example.ts](EXAMPLES/service-pattern-example.ts) | Service template |

---

## 🎓 Learning Path

### For New Developers
1. ⭐ Read [QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md) (5 min)
2. Review [login-example.tsx](EXAMPLES/login-example.tsx) (5 min)
3. Copy patterns and adapt to your needs

### For Experienced Developers
1. Skim [FRONTEND_UPDATE_SUMMARY.md](FRONTEND_UPDATE_SUMMARY.md) (5 min)
2. Review migration checklist
3. Update components one by one

### For Team Leads
1. Read [FRONTEND_UPDATE_SUMMARY.md](FRONTEND_UPDATE_SUMMARY.md) (10 min)
2. Share [QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md) with team
3. Use [FRONTEND_API_RESPONSE_GUIDE.md](FRONTEND_API_RESPONSE_GUIDE.md) as reference

---

## 🔑 Key Concepts

### Concept 1: ApiResponse<T>
All backend APIs return:
```typescript
{
  success: boolean,      // true = OK, false = Error
  message: string,       // Vietnamese message for users
  data: T | null,        // Your actual data
  error: ErrorCode | null, // Error code for logic
  timestamp: string      // Response time
}
```

### Concept 2: Auto-Unwrapping
Axios interceptor extracts `data` field automatically:
```typescript
// Backend: { success: true, data: { id: 1, name: "John" } }
// You get: { id: 1, name: "John" }
```

### Concept 3: ErrorCode Enum
Use enum for logic, NOT message strings:
```typescript
// ✅ GOOD
if (errorCode === ErrorCode.LOGIN_ACCOUNT_LOCKED) { ... }

// ❌ BAD
if (error.message.includes('khóa')) { ... }
```

---

## 📋 Common Tasks

### Task: Create a New Service
1. Copy pattern from [service-pattern-example.ts](EXAMPLES/service-pattern-example.ts)
2. Define your types
3. Use `axiosClient` from `@/lib/axios`
4. Return `response.data` (already unwrapped)

### Task: Handle Form Validation Errors
1. Use pattern from [register-example.tsx](EXAMPLES/register-example.tsx)
2. Check `hasFieldErrors(error)`
3. Extract with `extractFieldErrors(error)`
4. Apply to form fields

### Task: Show Error Messages
```typescript
import { extractErrorMessage } from '@/lib/errorHandler';

toast({ 
  variant: 'destructive',
  description: extractErrorMessage(error) 
});
```

### Task: Add Conditional Logic
```typescript
import { extractErrorCode, ErrorCode } from '@/lib/errorHandler';

const errorCode = extractErrorCode(error);
if (errorCode === ErrorCode.SPECIFIC_ERROR) {
  // Your logic
}
```

---

## ✅ Pre-flight Checklist

Before you start coding:
- [ ] Read [QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md)
- [ ] Understand auto-unwrapping concept
- [ ] Know how to use `extractErrorMessage()`
- [ ] Know how to use `extractErrorCode()`
- [ ] Have [ErrorCode enum](src/types/api.types.ts) bookmarked

---

## 🚫 Common Mistakes

### Mistake 1: Manual Unwrapping
```typescript
// ❌ WRONG
const user = response.data.data;

// ✅ CORRECT
const user = response.data;
```

### Mistake 2: Hardcoded Messages
```typescript
// ❌ WRONG
toast({ description: 'Đăng nhập thất bại' });

// ✅ CORRECT
toast({ description: extractErrorMessage(error) });
```

### Mistake 3: String Checking
```typescript
// ❌ WRONG
if (error.message.includes('khóa')) { ... }

// ✅ CORRECT
if (isErrorCodeMatch(error, ErrorCode.LOGIN_ACCOUNT_LOCKED)) { ... }
```

---

## 🛠️ Essential Imports

Copy this to your components:
```typescript
// For services
import axiosClient from '@/lib/axios';

// For error handling
import { 
  extractErrorMessage, 
  extractErrorCode, 
  isErrorCodeMatch,
  hasFieldErrors,
  extractFieldErrors 
} from '@/lib/errorHandler';

// For error codes
import { ErrorCode } from '@/types/api.types';

// For toast notifications (if using shadcn)
import { toast } from '@/hooks/use-toast';
```

---

## 📊 Stats

- **Files Updated:** 4
- **Files Created:** 7
- **Error Codes:** 60+
- **Code Examples:** 3
- **Documentation Pages:** 4
- **TypeScript Errors:** 0 ✅

---

## 🎯 Next Steps

1. **Read:** [QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md)
2. **Understand:** Auto-unwrapping & ErrorCode concepts
3. **Practice:** Update one component using examples
4. **Adopt:** Use patterns for all new components
5. **Migrate:** Update existing components gradually

---

## 📞 Getting Help

### If Something Doesn't Work
1. Check [Troubleshooting section](FRONTEND_API_RESPONSE_GUIDE.md#10-troubleshooting)
2. Verify backend returns correct format
3. Check ErrorCode enum matches backend
4. Review examples in `EXAMPLES/` folder

### If You Need Clarification
1. Read [Complete Guide](FRONTEND_API_RESPONSE_GUIDE.md)
2. Study code examples
3. Ask team lead

---

## 🌟 Quick Wins

After reading this guide, you can:
- ✅ Handle errors with backend messages automatically
- ✅ Use ErrorCode for conditional logic
- ✅ Access response data directly (unwrapped)
- ✅ Handle field validation errors
- ✅ Create new services following the pattern

---

## 📚 All Documentation

| Priority | Document | Read Time |
|----------|----------|-----------|
| ⭐⭐⭐ | [QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md) | 5 min |
| ⭐⭐ | [FRONTEND_UPDATE_SUMMARY.md](FRONTEND_UPDATE_SUMMARY.md) | 10 min |
| ⭐ | [FRONTEND_API_RESPONSE_GUIDE.md](FRONTEND_API_RESPONSE_GUIDE.md) | 20 min |
| 💻 | [login-example.tsx](EXAMPLES/login-example.tsx) | 5 min |
| 💻 | [register-example.tsx](EXAMPLES/register-example.tsx) | 5 min |
| 💻 | [service-pattern-example.ts](EXAMPLES/service-pattern-example.ts) | 5 min |

---

**🚀 Ready to start? Go to [QUICK_REFERENCE.md](EXAMPLES/QUICK_REFERENCE.md)**
