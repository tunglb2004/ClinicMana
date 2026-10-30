# Hướng dẫn Sử dụng Theme System

## Tổng quan
Hệ thống theme được thiết kế để dễ dàng quản lý màu sắc, khoảng cách, và các biến styling khác toàn ứng dụng.

## Cấu trúc File

```
src/
├── config/
│   ├── theme.ts          # File cấu hình chính (định nghĩa tất cả màu sắc)
│   └── index.ts          # Export các config
├── lib/
│   └── theme.ts          # Utility functions cho theme
```

## 1. Thay đổi Màu Sắc Chính

Tất cả các màu sắc được quản lý tập trung trong file [src/config/theme.ts](src/config/theme.ts).

### Ví dụ: Thay đổi màu Primary

```typescript
// src/config/theme.ts
export const themeConfig = {
  colors: {
    primary: {
      light: '#3B82F6',    // Màu sáng
      main: '#2563EB',     // Màu chính
      dark: '#1D4ED8',     // Màu tối
      foreground: '#FFFFFF', // Màu text
    },
    // ...
  },
};
```

Sau khi thay đổi, tất cả component sử dụng `--primary` CSS variable sẽ tự động cập nhật.

## 2. Sử dụng Màu Sắc trong Component

### Cách 1: CSS Variables (Khuyến nghị)

```tsx
// Trong Tailwind CSS class
<button className="bg-[var(--primary)] text-white hover:bg-[var(--primary-dark)]">
  Login
</button>

// Hoặc inline style
<div style={{ backgroundColor: 'var(--primary)' }}>
  Content
</div>
```

### Cách 2: Utility Functions

```tsx
import { getThemeColor, themeTailwind, themeStyles } from '@/lib/theme';

// Lấy giá trị màu sắc
const primaryColor = getThemeColor('primary', 'main'); // '#2563EB'

// Sử dụng Tailwind classes
<button className={themeTailwind.primaryBg}>
  Click me
</button>

// Sử dụng inline style
<div style={themeStyles.getPrimaryStyle()}>
  Content
</div>
```

## 3. CSS Variables Có Sẵn

```css
/* Primary */
--primary: #2563EB
--primary-light: #3B82F6
--primary-dark: #1D4ED8
--primary-foreground: #FFFFFF

/* Secondary */
--secondary: #059669
--secondary-light: #10B981
--secondary-dark: #047857
--secondary-foreground: #FFFFFF

/* Accent */
--accent: #D97706
--accent-light: #F59E0B
--accent-dark: #B45309
--accent-foreground: #FFFFFF

/* Destructive/Error */
--destructive: #DC2626
--destructive-light: #EF4444
--destructive-dark: #991B1B
--destructive-foreground: #FFFFFF

/* Status Colors */
--success: #10B981
--warning: #F59E0B
--error: #EF4444
--info: #3B82F6

/* Neutral */
--background: #FFFFFF
--foreground: #1F2937
--muted: #6B7280
--muted-foreground: #4B5563
--border: #E5E7EB
--input: #E5E7EB
--ring: #3B82F6
```

## 4. Ví dụ Thực Tế

### Thay đổi Color Scheme

Nếu bạn muốn đổi sang color scheme khác (ví dụ: Purple thay vì Blue):

```typescript
// src/config/theme.ts
export const themeConfig = {
  colors: {
    primary: {
      light: '#A78BFA',    // Purple 300
      main: '#8B5CF6',     // Purple 500
      dark: '#7C3AED',     // Purple 600
      foreground: '#FFFFFF',
    },
    // ...
  },
};
```

Tất cả UI sẽ tự động thay đổi sang màu Purple!

### Tạo Button với Theme Color

```tsx
import { Button } from '@/components/ui/button-themed';

export default function MyComponent() {
  return (
    <div className="space-y-4">
      {/* Primary Button */}
      <Button variant="primary" size="md">
        Primary Action
      </Button>

      {/* Secondary Button */}
      <Button variant="secondary" size="md">
        Secondary Action
      </Button>

      {/* Outline Button */}
      <Button variant="outline" size="md">
        Outline Action
      </Button>

      {/* Destructive Button */}
      <Button variant="destructive" size="lg">
        Delete
      </Button>
    </div>
  );
}
```

### Styling Component với Theme

```tsx
export default function Card() {
  return (
    <div className="rounded-lg p-6 bg-white border border-[var(--border)] shadow-md">
      <h2 className="text-[var(--primary)] font-bold mb-4">
        Tiêu đề
      </h2>
      <p className="text-[var(--foreground)] mb-4">
        Nội dung
      </p>
      <button className="px-4 py-2 bg-[var(--primary)] text-[var(--primary-foreground)] rounded-lg hover:bg-[var(--primary-dark)]">
        Action
      </button>
    </div>
  );
}
```

## 5. Globals CSS Variables

File [app/globals.css](app/globals.css) định nghĩa tất cả CSS variables trong phần `:root` block.

```css
:root {
  --primary: #2563EB;
  --primary-light: #3B82F6;
  --primary-dark: #1D4ED8;
  /* ... */
}
```

Những biến này có thể được sử dụng ở bất kỳ đâu trong ứng dụng.

## 6. Best Practices

### ✅ Nên làm

- **Sử dụng theme config** cho tất cả màu sắc chính
- **CSS Variables** cho styling: `bg-[var(--primary)]`
- **Tailwind classes** kết hợp theme: `hover:bg-[var(--primary-dark)]`
- **Consistent naming**: Sử dụng tên có ý nghĩa (primary, secondary, etc.)

### ❌ Không nên

- **Hardcode màu sắc**: `bg-blue-600` ❌ (thay vào đó dùng `bg-[var(--primary)]`)
- **Inline color values**: `<div style={{ color: '#2563EB' }}>` ❌
- **Tạo color riêng**: Tất cả màu đều nên trong theme config

## 7. Mở rộng Theme

Muốn thêm màu sắc hoặc biến mới? Thêm vào `src/config/theme.ts`:

```typescript
export const themeConfig = {
  colors: {
    // ... existing colors
    myCustomColor: {
      light: '#D4FC7D',
      main: '#BFFC7F',
      dark: '#A3E635',
      foreground: '#1F2937',
    },
  },
  // Thêm CSS variable trong globals.css:
  // --my-custom-color: #BFFC7F;
};
```

Sau đó dùng: `bg-[var(--my-custom-color)]`

## Tóm tắt

1. **Thay đổi màu** → Edit [src/config/theme.ts](src/config/theme.ts)
2. **Sử dụng trong component** → `className="bg-[var(--primary)]"`
3. **CSS variables tự động update** → Không cần compile lại
4. **Consistency** → Toàn bộ ứng dụng cùng color scheme

Happy theming! 🎨
