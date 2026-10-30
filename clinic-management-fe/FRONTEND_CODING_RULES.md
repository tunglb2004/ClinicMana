# 🚀 Front-end Coding Rules - Clinic Management System

Tài liệu này quy định các tiêu chuẩn lập trình cho Front-end của dự án Clinic Management để đảm bảo tính nhất quán, dễ đọc, và khả năng tái sử dụng cao giữa các thành viên trong đội ngũ.

## 1. Cấu trúc Thư mục (The "Standard Order")

Mọi tệp tin phải được đặt đúng nơi quy định để dễ dàng bảo trì và tìm kiếm.

| Thư mục | Trách nhiệm | Loại nội dung |
| :--- | :--- | :--- |
| `app/` | Routing & Layouts | Định nghĩa route (`page.tsx`), layout (`layout.tsx`) của Next.js. |
| `src/components/ui/` | Primitive UI | Các component dùng chung (Button, Input, Card...) từ Shadcn/UI. **Không chứa logic nghiệp vụ.** |
| `src/components/feature/` | Feature UI | Các component phức tạp gắn với tính năng cụ thể (vd: `auth/RegisterForm.tsx`, `patient/PatientTable.tsx`). |
| `src/components/layout/` | Global Layout | Các thành phần giao diện chung như Navbar, Sidebar, ProtectedRoute. |
| `src/services/` | API Communication | **Nơi duy nhất** thực hiện gọi API bằng `axiosClient`. |
| `src/store/` | Global State | Quản lý trạng thái toàn cục bằng Zustand (vd: `auth.store.ts`). |
| `src/hooks/` | Custom Hooks | Chứa các logic React có thể tái sử dụng (vd: `useAuth.ts`). |
| `src/types/` | Data Contracts | Định nghĩa TypeScript Interface/Type tương ứng với DTO từ Backend. |
| `src/lib/` | Infrastructure | Cấu hình thư viện (`axios.ts`), tiện ích (`utils.ts`), và validation schemas (`validations/`). |

---

## 2. Quy tắc Đặt tên (Naming Conventions)

Việc tuân thủ quy tắc đặt tên giúp mã nguồn trở nên chuyên nghiệp và đồng bộ.

*   **Components**: Sử dụng `PascalCase.tsx` (Vd: `RegisterForm.tsx`).
*   **Services**: Sử dụng `camelCase.service.ts` (Vd: `auth.service.ts`).
*   **Stores**: Sử dụng `camelCase.store.ts` (Vd: `auth.store.ts`).
*   **Types**: Sử dụng `camelCase.types.ts` (Vd: `auth.types.ts`).
*   **Hooks**: Sử dụng `useCamelCase.ts` (Vd: `useAuth.ts`).
*   **Schemas**: Đặt trong `src/lib/validations/` kèm hậu tố `.schema.ts`.
*   **Thư mục con**: Sử dụng `kebab-case` (Vd: `medical-records/`).

---

## 3. Kiến trúc Component

### A. Quy tắc "Service First"
*   **KHÔNG** gọi `axios` trực tiếp trong component.
*   **PHẢI** định nghĩa lời gọi API trong thư mục `services/` sau đó component mới gọi service đó.

### B. Server vs. Client Components
*   Ưu tiên sử dụng **Server Components** theo mặc định của Next.js để tối ưu hiệu năng.
*   Chỉ thêm `'use client'` khi component sử dụng: `useState`, `useEffect`, `useForm`, hoặc các sự kiện người dùng như `onClick`.

### C. Luồng Dữ liệu (Hierarchy)
`UI Component (View)` ⮕ `Custom Hook (Logic - nếu cần)` ⮕ `Service (API)` ⮕ `Axios Client`

---

## 4. Xử lý Form & Validation

Chúng ta sử dụng bộ ba: **React Hook Form + Zod + Shadcn/UI Form.**

*   **Validation**: Luôn định nghĩa schema bằng Zod để đảm bảo tính đúng đắn của dữ liệu ngay tại client.
*   **Type Safety**: Sử dụng `z.infer` để tạo type từ schema, tránh định nghĩa type thủ công cho form.

```typescript
// Ví dụ mẫu
const registerSchema = z.object({ email: z.string().email() });
type RegisterInput = z.infer<typeof registerSchema>;
```

---

## 5. Xử lý API & Trạng thái Người dùng

*   **API Client**: Luôn sử dụng `axiosClient` từ `@/lib/axios` để tận dụng các Interceptors (tự động gắn Token, xử lý lỗi 401).
*   **Feedback**: 
    *   Luôn có trạng thái `loading` khi thực hiện các tác vụ bất đồng bộ.
    *   Sử dụng `react-hot-toast` để hiển thị thông báo thành công/thất bại dựa trên message từ Backend.
    *   Mẫu catch error: `error.response?.data?.message || "Đã có lỗi xảy ra"`.

---

## 6. CSS & Styling

*   Sử dụng **Tailwind CSS** cho toàn bộ giao diện.
*   Sử dụng hàm `cn()` trong `src/lib/utils.ts` để kết hợp class có điều kiện.
*   Không sử dụng inline styles trừ trường hợp bắt buộc liên quan đến tính toán động.

---

> [!IMPORTANT]
> **Mục tiêu**: Một thành viên mới khi nhìn vào module `Auth` phải có thể hiểu ngay cách xây dựng module `Patient` hay `Doctor` vì chúng tuân theo cùng một "trật tự" và "cấu trúc" logic.
