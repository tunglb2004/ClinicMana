# Authentication Module - Hướng dẫn sử dụng

## 📦 Cài đặt dependencies

```bash
npm install zustand axios
npm install -D @types/node
```

Cài đặt Shadcn/UI components (tùy chọn - để UI đẹp hơn):
```bash
npx shadcn-ui@latest init
npx shadcn-ui@latest add button input label card select
```

## 🔧 Cấu hình môi trường

Tạo file `.env.local`:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## 📁 Cấu trúc đã tạo

```
src/
├── types/
│   └── auth.types.ts          # TypeScript types từ OpenAPI
├── services/
│   └── auth.service.ts        # API calls sử dụng axiosClient
├── store/
│   └── auth.store.ts          # Zustand global state
├── lib/
│   └── axios.ts               # Axios instance với interceptors
├── components/
│   ├── feature/
│   │   └── auth/
│   │       ├── LoginForm.tsx      # Form đăng nhập
│   │       └── RegisterForm.tsx   # Form đăng ký
│   └── layout/
│       └── ProtectedRoute.tsx     # HOC bảo vệ routes
└── middleware.ts              # Next.js middleware
```

## 🚀 Cách sử dụng

### 1. Tạo trang Login

Tạo `app/login/page.tsx`:
```tsx
import LoginForm from '@/components/feature/auth/LoginForm';

export default function LoginPage() {
  return <LoginForm />;
}
```

### 2. Tạo trang Register

Tạo `app/register/page.tsx`:
```tsx
import RegisterForm from '@/components/feature/auth/RegisterForm';

export default function RegisterPage() {
  return <RegisterForm />;
}
```

### 3. Bảo vệ route với ProtectedRoute

Tạo `app/dashboard/layout.tsx`:
```tsx
import ProtectedRoute from '@/components/layout/ProtectedRoute';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ProtectedRoute>
      {children}
    </ProtectedRoute>
  );
}
```

Hoặc bảo vệ theo role:
```tsx
<ProtectedRoute requiredRoles={['ROLE_SYSTEM_ADMIN', 'ROLE_CLINICAL_DOCTOR']}>
  {children}
</ProtectedRoute>
```

### 4. Sử dụng Auth Store trong component

```tsx
'use client';

import { useAuthStore } from '@/store/auth.store';

export default function ProfileComponent() {
  const { user, logout } = useAuthStore();

  return (
    <div>
      <p>Xin chào, {user?.fullName}</p>
      <button onClick={logout}>Đăng xuất</button>
    </div>
  );
}
```

## ✅ Features đã implement

- ✅ Login/Register với validation
- ✅ Auto refresh token khi hết hạn
- ✅ Global state management (Zustand)
- ✅ Protected routes với role-based access
- ✅ Auto redirect khi chưa đăng nhập
- ✅ TypeScript types từ OpenAPI spec
- ✅ Axios interceptors (auto attach token)
- ✅ Error handling

## 🔐 Security

- Tokens được lưu trong localStorage (Zustand persist)
- Auto refresh token trước khi hết hạn
- BCrypt password hashing (backend)
- JWT authentication
- Role-based access control

## 📝 Lưu ý

1. **Shadcn/UI**: Components hiện dùng Tailwind thuần, có thể thay bằng Shadcn components để UI đẹp hơn
2. **Backend URL**: Nhớ cập nhật `NEXT_PUBLIC_API_URL` trong `.env.local`
3. **Error Messages**: Backend cần trả về format `{ message: string }` để hiển thị lỗi đúng
4. **Logout**: Khi logout, refreshToken sẽ được gửi lên backend để invalidate

## 🔄 Flow hoạt động

1. User login → Nhận `accessToken` + `refreshToken`
2. Tokens được lưu vào Zustand store (persist localStorage)
3. Mọi API call tự động gắn `Authorization: Bearer <token>`
4. Khi token hết hạn (401) → Auto refresh token
5. Nếu refresh thất bại → Logout & redirect login
