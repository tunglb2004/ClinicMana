This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## 🎨 UI/UX Improvements

**MAJOR UPDATE:** Giao diện ứng dụng vừa được cải thiện toàn diện!

### ✨ Highlights
- 🎭 **Hệ thống màu sắc linh hoạt** - Thay đổi color scheme từ một chỗ
- 🎨 **Thiết kế hiện đại** - Home, Login, Register pages cải thiện
- 🧩 **Themed Components** - Button, Card với theme colors
- 📚 **Tài liệu đầy đủ** - Hướng dẫn chi tiết cách sử dụng

### 📖 Docs
- **[THEME_GUIDE.md](THEME_GUIDE.md)** ← Chi tiết cách sử dụng theme system
- **[UI_IMPROVEMENTS.md](UI_IMPROVEMENTS.md)** ← Tóm tắt thay đổi
- **[COMPLETION_SUMMARY.md](COMPLETION_SUMMARY.md)** ← Công việc hoàn thành
- **[VISUAL_PREVIEW.md](VISUAL_PREVIEW.md)** ← Visual demo
- **[VERIFICATION_CHECKLIST.md](VERIFICATION_CHECKLIST.md)** ← Verification list

### 🚀 Quick Start

**1. Thay đổi color scheme:**
```tsx
// src/config/theme.ts
primary: {
  light: '#A78BFA',    // Purple 300
  main: '#8B5CF6',     // Purple 500
  dark: '#7C3AED',     // Purple 600
}
```

**2. Update CSS variables:**
```css
/* app/globals.css */
:root {
  --primary: #8B5CF6;
  --primary-light: #A78BFA;
  --primary-dark: #7C3AED;
}
```

**3. Refresh browser** → Everything changes! ✨

### 💡 Sử dụng Theme Trong Component

```tsx
// CSS Variables (Khuyến nghị)
<button className="bg-[var(--primary)] hover:bg-[var(--primary-dark)]">
  Click me
</button>

// Gradient
<div className="bg-gradient-to-r from-[var(--primary)] to-[var(--secondary)]">
  Content
</div>

// Inline Style
<div style={{ color: 'var(--primary)' }}>
  Content
</div>
```

### 📱 Pages Updated
- ✅ Home page - Modern design with navbar, hero, features
- ✅ Login page - Professional styling with animations
- ✅ Register page - Emerald color scheme
- ✅ Theme system - Easy color scheme changes

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
