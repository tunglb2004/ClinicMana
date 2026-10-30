# Clinic Management - Technology & Architecture Documentation

## 1. Tech Stack Overview

### Frontend
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **UI Components**: Shadcn/UI
- **State Management**: Zustand
- **HTTP Client**: Axios (via axiosClient)
- **Runtime**: Node.js 18+
- **Form Management**: React Hook Form
- **Validation**: Zod

## 2. Project Structure

```
clinic-management-fe/
├── app/                              # Next.js App Router
│   ├── globals.css                   # Global styles
│   ├── layout.tsx                    # Root layout
│   ├── page.tsx                      # Home page
│   ├── dashboard/                    # Dashboard pages
│   │   ├── layout.tsx                # Dashboard layout
│   │   ├── page.tsx                  # Dashboard home
│   │   ├── admin/                    # Admin role pages
│   │   ├── clinical-doctor/          # Clinical doctor role pages
│   │   ├── specialist-doctor/        # Specialist doctor role pages
│   │   ├── manager/                  # Manager role pages
│   │   ├── nurse/                    # Nurse role pages
│   │   ├── patient/                  # Patient role pages
│   │   └── receptionist/             # Receptionist role pages
│   ├── login/                        # Login page
│   ├── register/                     # Registration page
│   └── unauthorized/                 # Unauthorized access page
│
├── src/
│   ├── components/                   # React components
│   │   ├── feature/                  # Feature-specific components
│   │   │   └── auth/                 # Authentication components
│   │   │       ├── LoginForm.tsx
│   │   │       └── RegisterForm.tsx
│   │   ├── layout/                   # Layout components
│   │   │   └── ProtectedRoute.tsx    # Role-based protected route
│   │   └── ui/                       # Reusable UI components (Shadcn/UI)
│   │       ├── button.tsx
│   │       ├── card.tsx
│   │       ├── input.tsx
│   │       ├── label.tsx
│   │       └── select.tsx
│   │
│   ├── hooks/                        # Custom React hooks
│   │   └── useAuth.ts                # Authentication hook
│   │
│   ├── lib/                          # Utility functions & configurations
│   │   ├── axios.ts                  # Configured axios instance (axiosClient)
│   │   ├── roleRedirect.ts           # Role-based redirect logic
│   │   ├── utils.ts                  # General utilities
│   │   └── validations/              # Validation schemas
│   │
│   ├── services/                     # Business logic & API calls
│   │   └── auth.service.ts           # Authentication API service
│   │
│   ├── store/                        # Zustand state management
│   │   └── auth.store.ts             # Auth global state
│   │
│   ├── types/                        # TypeScript type definitions
│   │   └── auth.types.ts             # Auth-related types
│   │
│   └── middleware.ts                 # Next.js middleware
│
├── public/                           # Static assets
├── package.json                      # Project dependencies
├── tsconfig.json                     # TypeScript configuration
├── next.config.ts                    # Next.js configuration
├── tailwind.config.ts                # Tailwind CSS configuration
├── eslint.config.mjs                 # ESLint configuration
├── postcss.config.mjs                # PostCSS configuration
└── components.json                   # Shadcn/UI configuration

```

## 3. Architecture Patterns

### Frontend Architecture

#### 3.1 Component Hierarchy
```
App (Root)
  ├── Server Components (Default)
  │   ├── Layout Components
  │   └── Page Components
  └── Client Components ('use client')
      ├── Interactive Components
      └── Components using Hooks
```

#### 3.2 Data Flow
```
UI Component
    ↓
Custom Hook (useAuth)
    ↓
Zustand Store (auth.store)
    ↓
Service Layer (auth.service)
    ↓
Axios Client (lib/axios)
    ↓
Backend API
```

#### 3.3 Authentication Flow (Frontend)
```
1. User navigates to /login
2. LoginForm component displays login form
3. User submits credentials
4. Form validation via Zod schema
5. auth.service.login() called with credentials
6. axiosClient sends POST request to /auth/login endpoint
7. Response received with JWT token
8. Token stored in auth.store (Zustand)
9. useAuth hook provides auth state to components
10. Redirect to dashboard (role-based via ProtectedRoute)
11. Subsequent requests include JWT in Authorization header
```

#### 3.4 Protected Route Logic
```
ProtectedRoute Component receives:
  - User from auth.store
  - User role
  - Required roles for page

If user authenticated:
  - If user.role in required roles → Render page
  - If user.role NOT in required roles → Redirect to /unauthorized

If not authenticated:
  - Redirect to /login
```

## 4. Key Frontend Technologies & Packages

| Package | Version | Purpose |
|---------|---------|---------|
| next | 14.x | React framework with file-based routing |
| react | 19.x | UI library |
| typescript | 5.x | Type safety |
| tailwindcss | 3.x | Utility-first CSS |
| shadcn/ui | Latest | Pre-built accessible UI components |
| zustand | Latest | Lightweight state management |
| axios | Latest | HTTP client |
| eslint | Latest | Code quality |
| @hookform/resolvers | Latest | Form validation integration |
| zod | Latest | Schema validation |
| react-hot-toast | Latest | Toast notifications |
| next-auth (optional) | Latest | Advanced authentication |

## 5. Development Conventions

### 5.1 Frontend Rules (MANDATORY)

#### API Communication
- ✅ DO: Use `axiosClient` from `@/lib/axios`
- ❌ DON'T: Use raw `fetch` or direct `axios` imports
- ❌ DON'T: Hardcode URLs like `http://localhost:8080`
- ✅ DO: Use environment variable `process.env.NEXT_PUBLIC_API_URL`

#### Component Architecture
- ✅ Default to Server Components
- ✅ Add `'use client'` only when using hooks or event listeners
- ✅ Place business logic in `src/services/`
- ❌ DON'T: Put API calls directly in components

#### State Management
- ✅ Use Zustand for global state (User, Cart, Settings)
- ❌ DON'T: Use prop drilling (> 2 levels)
- ✅ Use Context API for deeply nested component trees

#### Form Handling
- ✅ Use React Hook Form with Zod validation
- ✅ Display validation errors clearly to users
- ✅ Show loading states during submission
- ❌ DON'T: Validate only on backend

## 6. Frontend Environment Configuration

```env
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_API_TIMEOUT=30000

# App Configuration
NEXT_PUBLIC_APP_NAME=Clinic Management System
NEXT_PUBLIC_APP_VERSION=1.0.0

# Feature Flags
NEXT_PUBLIC_ENABLE_NOTIFICATIONS=true
NEXT_PUBLIC_ENABLE_DARK_MODE=true

# JWT Configuration (Frontend storage)
NEXT_PUBLIC_JWT_KEY=clinic_auth_token
```

## 7. File Organization Rules

### When Creating New Files
- **Components**: Place in `src/components/feature/[feature-name]/`
- **Services**: Always place API logic in `src/services/`
- **Hooks**: Custom hooks in `src/hooks/`
- **Types**: TypeScript definitions in `src/types/`
- **Stores**: Zustand stores in `src/store/`
- **Pages**: Route files in `app/[route]/`
- **Utils**: Utility functions in `src/lib/`

### Naming Conventions
- **Components**: PascalCase (e.g., `LoginForm.tsx`)
- **Services**: camelCase with `.service.ts` suffix (e.g., `auth.service.ts`)
- **Stores**: camelCase with `.store.ts` suffix (e.g., `auth.store.ts`)
- **Hooks**: camelCase with `use` prefix (e.g., `useAuth.ts`)
- **Types**: PascalCase with `.types.ts` suffix (e.g., `auth.types.ts`)
- **Files**: Always use `.tsx` for components, `.ts` for logic

## 8. Database Schema Patterns

### Soft Delete Fields (in all tables)
```sql
ALTER TABLE table_name ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE table_name ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE table_name ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE table_name ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
```

### Query Pattern
```sql
SELECT * FROM table_name WHERE is_deleted = FALSE AND is_active = TRUE;
```

## 8. Authentication & Authorization Flow

### Roles Supported (Frontend)
1. **Admin** - System administrator, full access to all features
2. **Doctor** - Access to patient records, medical features
3. **Manager** - Department/clinic management features
4. **Nurse** - Nursing operations, patient care features
5. **Patient** - Patient portal, personal health records
6. **Pharmacist** - Pharmacy operations, medication management
7. **Receptionist** - Reception desk operations, scheduling

### Protected Routes Implementation
```typescript
// app/dashboard/[role]/page.tsx structure
- Each role has its own dashboard
- ProtectedRoute component wraps pages
- Validates user authentication
- Checks if user.role matches page role
- Redirects to /unauthorized if access denied
```

### Token Management
- JWT token stored in Zustand store (in-memory)
- Optional: Persist to localStorage (handle security carefully)
- Token included in all API requests via axios interceptor
- Token refresh handled in axiosClient
- Logout clears token from store and localStorage

### Authentication State
```typescript
type AuthStore = {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  setUser: (user: User) => void;
}
```

## 9. Development Workflow

### Prerequisites
- Node.js 18+
- npm or yarn
- Git
- Code editor (VS Code recommended)
- Chrome/Firefox browser for testing

### Getting Started
```bash
# Install dependencies
npm install

# Create .env.local file
cp .env.example .env.local

# Start development server
npm run dev

# Open browser
# http://localhost:3000
```

### Available Scripts
```bash
# Development
npm run dev          # Start dev server with hot reload

# Production
npm run build        # Build for production
npm start            # Start production server

# Code Quality
npm run lint         # Run ESLint
npm run lint:fix     # Fix linting issues

# Type Checking
npm run typecheck    # Check TypeScript errors
```

### Code Quality
- Use ESLint for code linting
- Follow Prettier formatting
- Write meaningful commit messages
- Create branches for features: `feature/feature-name`
- Create branches for fixes: `bugfix/bug-name`
- Keep commits atomic and descriptive

## 10. Common Frontend Patterns

### Using axiosClient
```typescript
import axiosClient from '@/lib/axios';

// GET request
const response = await axiosClient.get('/users/profile');

// POST request
const response = await axiosClient.post('/auth/login', {
  email: 'user@example.com',
  password: 'password'
});

// PUT/PATCH request
const response = await axiosClient.put('/users/profile', {
  name: 'New Name'
});

// With error handling
try {
  const response = await axiosClient.get('/data');
  console.log(response.data);
} catch (error) {
  console.error(error.response?.data?.message || 'Error');
}
```

### Using Zustand Store
```typescript
import useAuthStore from '@/store/auth.store';

const AuthComponent = () => {
  // Access state
  const user = useAuthStore((state) => state.user);
  const token = useAuthStore((state) => state.token);
  
  // Access methods
  const logout = useAuthStore((state) => state.logout);
  const login = useAuthStore((state) => state.login);
  
  return (
    <div>
      <p>Welcome, {user?.name}</p>
      <button onClick={logout}>Logout</button>
    </div>
  );
};
```

### Creating a Service
```typescript
// src/services/user.service.ts
import axiosClient from '@/lib/axios';

export const userService = {
  async getProfile() {
    return axiosClient.get('/users/profile');
  },
  
  async updateProfile(data: ProfileUpdateData) {
    return axiosClient.put('/users/profile', data);
  },

  async getUsers(page: number, limit: number) {
    return axiosClient.get('/users', {
      params: { page, limit }
    });
  }
};
```

### Creating a Custom Hook
```typescript
// src/hooks/useUser.ts
'use client';

import { useEffect, useState } from 'react';
import { userService } from '@/services/user.service';

export const useUser = (userId: string) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const response = await userService.getUser(userId);
        setUser(response.data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, [userId]);

  return { user, loading, error };
};
```

### Server vs Client Components
```typescript
// ✅ Server Component (default)
// Renders on server, no interactivity needed
export default function Page() {
  return <div>Server rendered</div>;
}

// ✅ Client Component (with hooks)
// Required when using useState, useEffect, etc.
'use client';
import { useState } from 'react';

export default function InteractiveComponent() {
  const [count, setCount] = useState(0);
  return (
    <button onClick={() => setCount(count + 1)}>
      Count: {count}
    </button>
  );
}
```

### Form with Validation
```typescript
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { authService } from '@/services/auth.service';

const loginSchema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(6, 'Password too short'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginForm() {
  const { register, handleSubmit, formState: { errors } } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await authService.login(data);
      // Redirect or show success
    } catch (error) {
      // Show error message
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('email')} placeholder="Email" />
      {errors.email && <span>{errors.email.message}</span>}
      
      <input {...register('password')} type="password" placeholder="Password" />
      {errors.password && <span>{errors.password.message}</span>}
      
      <button type="submit">Login</button>
    </form>
  );
}
```

### Protected Route Component
```typescript
'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import useAuthStore from '@/store/auth.store';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRoles?: string[];
}

export default function ProtectedRoute({
  children,
  requiredRoles = [],
}: ProtectedRouteProps) {
  const router = useRouter();
  const { user, isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }

    if (requiredRoles.length > 0 && !requiredRoles.includes(user?.role || '')) {
      router.push('/unauthorized');
    }
  }, [isAuthenticated, user, requiredRoles, router]);

  if (!isAuthenticated) return null;

  return <>{children}</>;
}
```

## 11. Performance & Best Practices

### Frontend Optimization
- Use dynamic imports for large components
  ```typescript
  const HeavyComponent = dynamic(() => import('@/components/Heavy'), {
    loading: () => <p>Loading...</p>,
  });
  ```
- Implement image optimization with Next.js Image
  ```typescript
  import Image from 'next/image';
  <Image src="/image.jpg" alt="desc" width={400} height={300} />
  ```
- Lazy load non-critical components
- Use React.memo for expensive components
- Optimize bundle size with tree-shaking
- Use CSS modules for component-scoped styles
- Leverage Next.js built-in optimizations (automatic splitting, etc.)

### API Optimization
- Implement request/response caching
- Use pagination for large datasets
- Debounce API calls from user input
  ```typescript
  const debouncedSearch = useMemo(
    () => debounce((query) => searchService.search(query), 300),
    []
  );
  ```
- Cancel previous requests when component unmounts
  ```typescript
  useEffect(() => {
    const controller = new AbortController();
    fetchData(controller.signal);
    return () => controller.abort();
  }, []);
  ```

### State Management Optimization
- Don't store entire API responses in Zustand
- Use selectors to minimize re-renders
- Implement proper cleanup in effects
- Use useCallback for function stability

## 12. Security Considerations

### Frontend Security Best Practices
- Never hardcode sensitive data (API keys, secrets)
- Never store sensitive data in localStorage
- Use httpOnly cookies for JWT tokens (if possible)
- Validate all user inputs on both client and server
- Use HTTPS in production
- Implement CSRF protection for forms
- Sanitize HTML content if displaying user-generated content
- Use Content Security Policy (CSP) headers
- Implement proper error handling (don't expose stack traces)

### Authentication Security
- Store JWT in secure location (preferably httpOnly cookie)
- Implement token refresh mechanism
- Set appropriate token expiration times
- Validate token signature and expiration
- Clear tokens on logout
- Use HTTPS for all API communication

### Data Protection
- Don't log sensitive information
- Implement proper access controls
- Use environment variables for configuration
- Regular security audits
- Keep dependencies updated

## 13. Troubleshooting Guide

### Common Issues

| Issue | Solution |
|-------|----------|
| API requests fail with CORS error | Check `process.env.NEXT_PUBLIC_API_URL` and backend CORS headers |
| 'use client' errors | Only use 'use client' in components with hooks/interactivity |
| Tailwind styles not applying | Ensure path in tailwind.config.ts includes your files |
| State not persisting on refresh | Implement localStorage persistence or backend session |
| Components not re-rendering | Check Zustand selectors, use shallow comparison |
| Build fails with TypeScript errors | Run `npm run typecheck`, fix type errors |
| Axios interceptor not working | Check interceptor setup in lib/axios.ts |
| Authentication loop | Verify token validation logic in ProtectedRoute |
| Image optimization warnings | Use Next.js Image component, specify width/height |
| Slow page loads | Use dynamic imports, lazy load components |

### Debugging Tips
```typescript
// Debug Zustand state
import useAuthStore from '@/store/auth.store';

useAuthStore.subscribe((state) => console.log('Auth state:', state));

// Debug API calls
// Enable in lib/axios.ts for request/response logging
axiosClient.interceptors.request.use((config) => {
  console.log('Request:', config);
  return config;
});

// Debug component re-renders
console.log('Component rendered at', new Date().toISOString());
```

## 14. Documentation & Resources

### Frontend Documentation
- [Next.js Documentation](https://nextjs.org/docs)
- [React Documentation](https://react.dev)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Tailwind CSS](https://tailwindcss.com/docs)
- [Shadcn/UI Components](https://ui.shadcn.com/)
- [Zustand State Manager](https://github.com/pmndrs/zustand)
- [React Hook Form](https://react-hook-form.com/)
- [Zod Validation](https://zod.dev/)

### Learning Resources
- Next.js 14 App Router Guide
- TypeScript Best Practices
- React Hooks Deep Dive
- Zustand State Management Patterns
- Performance Optimization Techniques

### Tools & Extensions (Recommended)
- VS Code: ESLint, Prettier, TypeScript Vue Plugin
- Chrome DevTools: React Developer Tools
- npm: `npm-check-updates` for dependency management

---

**Last Updated**: January 2026
**Focus**: Frontend Development Only
**Version**: 2.0
**Maintained By**: Frontend Team
