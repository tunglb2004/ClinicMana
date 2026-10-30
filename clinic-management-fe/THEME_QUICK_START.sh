#!/bin/bash
# ClinicPro - Theme System Demo
# Hướng dẫn nhanh cách sử dụng hệ thống theme

cat << 'EOF'

╔════════════════════════════════════════════════════════════════════╗
║                    🎨 THEME SYSTEM GUIDE 🎨                      ║
║         Hệ thống màu sắc linh hoạt cho ClinicPro                  ║
╚════════════════════════════════════════════════════════════════════╝

📁 CẤU TRÚC FILE
───────────────────────────────────────────────────────────────────
src/config/theme.ts          ← Định nghĩa tất cả màu sắc chính
app/globals.css              ← CSS variables toàn ứng dụng
src/lib/theme.ts             ← Utility functions cho theme
src/components/ui/button-themed.tsx    ← Button với theme
src/components/ui/card-themed.tsx      ← Card với theme

🎨 CÁCH THAY ĐỔI MÀU SẮC
───────────────────────────────────────────────────────────────────

1. MỞ FILE: src/config/theme.ts

2. TÌM PHẦN: colors > primary (hoặc color bạn muốn thay)

3. THAY ĐỔI GIÁ TRỊ HEX:
   Ví dụ từ BLUE sang PURPLE:

   TRƯỚC:
   primary: {
     light: '#3B82F6',
     main: '#2563EB',
     dark: '#1D4ED8',
     foreground: '#FFFFFF',
   }

   SAU:
   primary: {
     light: '#A78BFA',    // Purple 300
     main: '#8B5CF6',     // Purple 500
     dark: '#7C3AED',     // Purple 600
     foreground: '#FFFFFF',
   }

4. CẬP NHẬT GLOBALS.CSS:
   Tìm :root { ... } block
   Cập nhật các biến --primary tương ứng

5. ✨ XONG! Toàn bộ UI tự động thay đổi

💡 CSS VARIABLES CÓ SẮN
───────────────────────────────────────────────────────────────────

PRIMARY:       --primary, --primary-light, --primary-dark, --primary-foreground
SECONDARY:     --secondary, --secondary-light, --secondary-dark, --secondary-foreground
ACCENT:        --accent, --accent-light, --accent-dark, --accent-foreground
DESTRUCTIVE:   --destructive, --destructive-light, --destructive-dark, --destructive-foreground
STATUS:        --success, --warning, --error, --info
NEUTRAL:       --background, --foreground, --muted, --border, --input
SIDEBAR:       --sidebar, --sidebar-foreground, --sidebar-primary, etc.

🚀 SỬ DỤNG TRONG COMPONENT
───────────────────────────────────────────────────────────────────

CÁCH 1 - CSS CLASS (Khuyến nghị):
  <button className="bg-[var(--primary)] text-white">
    Click me
  </button>

CÁCH 2 - INLINE STYLE:
  <div style={{ backgroundColor: 'var(--primary)' }}>
    Content
  </div>

CÁCH 3 - THEME UTILITIES:
  import { getThemeColor } from '@/lib/theme';
  const color = getThemeColor('primary', 'main');

🎯 VÍ DỤ THỰC TẾ
───────────────────────────────────────────────────────────────────

Nút bấm với theme:
  <button className="px-4 py-2 bg-[var(--primary)] text-white 
                     rounded-lg hover:bg-[var(--primary-dark)]
                     transition-colors duration-300">
    Action
  </button>

Card với theme:
  <div className="rounded-xl p-6 bg-white border border-[var(--border)]
                  shadow-md hover:shadow-lg">
    <h2 className="text-[var(--primary)] font-bold">Title</h2>
    <p className="text-[var(--foreground)]">Content</p>
  </div>

Gradient với theme:
  <div className="bg-gradient-to-r from-[var(--primary)] 
                  to-[var(--secondary)]">
    Gradient Background
  </div>

📚 TÀI LIỆU
───────────────────────────────────────────────────────────────────

Chi tiết đầy đủ: THEME_GUIDE.md
Tóm tắt thay đổi: UI_IMPROVEMENTS.md

✅ BEST PRACTICES
───────────────────────────────────────────────────────────────────

✅ SỬ DỤNG CSS VARIABLES:
   bg-[var(--primary)] ✓
   
❌ TRÁNH HARDCODE MÀU:
   bg-blue-600 ✗
   
✅ THAY TOÀN BỘ Ở MỘT CHỖ:
   src/config/theme.ts ✓

🔄 THAY ĐỔI COLOR SCHEME
───────────────────────────────────────────────────────────────────

BLUE → INDIGO:    Đổi hex colors từ blue sang indigo
BLUE → GREEN:     Đổi sang emerald/green shades
BLUE → PURPLE:    Đổi sang purple/violet shades
BLUE → RED:       Đổi sang red/rose shades

Chỉ cần thay 2 file:
1. src/config/theme.ts (giá trị hex)
2. app/globals.css (CSS variables)

Và tất cả component sẽ tự động cập nhật! 🎨

╔════════════════════════════════════════════════════════════════════╗
║  Hệ thống này cho phép bạn thay đổi toàn bộ color scheme          ║
║  chỉ bằng cách sửa 2 file. Rất tiện lợi cho prototyping! 🚀      ║
╚════════════════════════════════════════════════════════════════════╝

EOF
