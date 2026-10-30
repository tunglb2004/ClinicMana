# Password Management UI Components - Visual Guide

## 1. Reset Password Form - Step 1: Email Entry

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                   🔐 [Icon]                                │
│                                                             │
│            🎨 Đặt lại mật khẩu                            │
│         Nhập email để nhận mã OTP                          │
│                                                             │
│  ━━━━ Progress: ━━━ ━━━ ━━━                               │
│   (Step 1 completed, 2-3 pending)                          │
│                                                             │
│  ✘ Error message (if any)                                 │
│                                                             │
│  Email                                                     │
│  ┌─────────────────────────────────────────────┐          │
│  │ your@email.com                              │          │
│  └─────────────────────────────────────────────┘          │
│                                                             │
│         ┌───────────────────────────────────┐              │
│         │    Gửi mã OTP                     │              │
│         └───────────────────────────────────┘              │
│                                                             │
│                 Quay lại trang đăng nhập                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 2. Reset Password Form - Step 2: OTP Verification

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                   🔐 [Icon]                                │
│                                                             │
│            🎨 Đặt lại mật khẩu                            │
│       Nhập mã OTP từ email của bạn                        │
│                                                             │
│  ━━━━ ━━━━ Progress: ━━━━ ━━━                             │
│   (Steps 1-2 completed, 3 pending)                         │
│                                                             │
│  Mã OTP                                                    │
│  ┌─────────────────────────────────────────────┐          │
│  │        1 2 3 4 5 6                          │          │
│  └─────────────────────────────────────────────┘          │
│                                                             │
│  Hết hạn trong: 4:32                                      │
│  (Countdown timer showing expiration)                      │
│                                                             │
│      ┌───────────────────────────────────┐                │
│      │   Xác thực OTP                    │                │
│      └───────────────────────────────────┘                │
│                                                             │
│          [Quay lại]                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 3. Reset Password Form - Step 3: New Password

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                   🔐 [Icon]                                │
│                                                             │
│            🎨 Đặt lại mật khẩu                            │
│           Tạo mật khẩu mới                               │
│                                                             │
│  ━━━━ ━━━━ Progress: ━━━━                                │
│   (All steps completed)                                    │
│                                                             │
│  ✔ Mật khẩu đã được đặt lại thành công                   │
│  (Success message in green)                               │
│                                                             │
│  Mật khẩu mới                                             │
│  ┌─────────────────────────────────────────────┐          │
│  │ ••••••••••••••••                            │          │
│  └─────────────────────────────────────────────┘          │
│                                                             │
│  Xác nhận mật khẩu                                        │
│  ┌─────────────────────────────────────────────┐          │
│  │ ••••••••••••••••                            │          │
│  └─────────────────────────────────────────────┘          │
│                                                             │
│     ┌───────────────────────────────────┐                 │
│     │  Đặt lại mật khẩu                 │                 │
│     └───────────────────────────────────┘                 │
│                                                             │
│          [Quay lại]                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 4. Change Password Form

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                   🔐 [Icon]                                │
│                                                             │
│            🎨 Đổi mật khẩu                                │
│   Cập nhật mật khẩu để bảo vệ tài khoản của bạn         │
│                                                             │
│  ✘ Lỗi: Mật khẩu hiện tại không chính xác                │
│  (Error message in red, if any)                           │
│                                                             │
│  Mật khẩu hiện tại                                        │
│  ┌─────────────────────────────────┐ 👁               │
│  │ ••••••••••                      │                  │
│  └─────────────────────────────────┘                  │
│  (Click eye icon to show/hide password)                │
│                                                             │
│  Mật khẩu mới                                             │
│  ┌─────────────────────────────────┐ 👁               │
│  │ ••••••••••                      │                  │
│  └─────────────────────────────────┘                  │
│                                                             │
│  Xác nhận mật khẩu mới                                    │
│  ┌─────────────────────────────────┐ 👁               │
│  │ ••••••••••                      │                  │
│  └─────────────────────────────────┘                  │
│                                                             │
│    ┌───────────────────────────────────┐               │
│    │  Cập nhật mật khẩu                │               │
│    └───────────────────────────────────┘               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 5. Login Form - With Reset Link

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                   🔐 [Icon]                                │
│                                                             │
│            🎨 Đăng nhập                                   │
│     Truy cập vào hệ thống quản lý phòng khám            │
│                                                             │
│  Email hoặc Số điện thoại                                 │
│  ┌─────────────────────────────────────────────┐          │
│  │ Email hoặc số điện thoại                    │          │
│  └─────────────────────────────────────────────┘          │
│                                                             │
│  Mật khẩu              [Quên mật khẩu? >>]              │
│  ┌─────────────────────────────────────────────┐          │
│  │ ••••••••••••••••                            │          │
│  └─────────────────────────────────────────────┘          │
│                                                             │
│         ┌───────────────────────────────────┐              │
│         │    Đăng nhập                      │              │
│         └───────────────────────────────────┘              │
│                                                             │
│                ─────── hoặc ───────                        │
│                                                             │
│    ┌───────────────────────────────────────┐              │
│    │   Đăng ký tài khoản mới               │              │
│    └───────────────────────────────────────┘              │
│                                                             │
└─────────────────────────────────────────────────────────────┘

👆 Click "Quên mật khẩu?" to go to /reset-password
```

---

## UI Component Specifications

### Colors Used
- **Primary**: Blue-600 to Indigo-600 (gradients)
- **Hover**: Blue-700 to Indigo-700
- **Error**: Red-50 background, Red-700 text
- **Success**: Green-50 background, Green-700 text
- **Background**: Blue-50 to Indigo-50 gradient
- **Text**: Gray-700 to Gray-900

### Spacing
- Card max-width: 448px (md breakpoint)
- Padding: 24px
- Gap between fields: 20px
- Button height: 44-48px
- Border radius: 8px

### Typography
- Title: 30px bold (text-3xl)
- Description: 16px (text-base)
- Labels: 14px semibold (text-sm)
- Form text: 16px

### Interactive Elements
- Buttons: Full width
- Inputs: Full width with 10px padding
- Eye toggle: Absolute positioned right side
- Loading state: Spinner animation + text change

### Responsive Behavior
- Mobile: 16px padding on viewport
- Max content width: 448px
- Stacked layout
- Touch-friendly 44px minimum tap targets

---

## Form Validation Visual Feedback

### Before Submission
```
Email Field:
✓ Valid email format - Green border
✗ Invalid format - Red border with error

Password Fields:
✓ 8+ characters - Green indicator
✗ < 8 characters - Red error message

Password Confirmation:
✓ Matches new password - Green
✗ Doesn't match - Red error message
```

### After Submission
```
Loading State:
- Button text: "Đang gửi..." or "Đang xử lý..."
- Button disabled: true
- Spinner animation: Rotating circle

Success State:
✔ Green message: "Mật khẩu đã được [đặt lại/thay đổi] thành công"
- Auto-dismiss after 5 seconds
- Auto-redirect after 2 seconds

Error State:
✘ Red message: Specific error from server
- Does not auto-dismiss
- User can correct and retry
```

---

## Accessibility Features

- ✓ Proper `<label>` associations with form fields
- ✓ Required field indicators
- ✓ Clear error messages
- ✓ High contrast colors (WCAG AA compliant)
- ✓ Keyboard navigation support
- ✓ Focus indicators on buttons
- ✓ Screen reader friendly

---

## Mobile View Example

```
┌──────────────────┐
│                  │
│    🔐 [Icon]     │
│                  │
│  Đặt lại         │
│   mật khẩu       │
│                  │
│  Nhập email      │
│  để nhận OTP     │
│                  │
│ ━ Progress ━ ━   │
│                  │
│ Email            │
│ ┌──────────────┐ │
│ │ email@...    │ │
│ └──────────────┘ │
│                  │
│  ┌────────────┐  │
│  │ Gửi OTP    │  │
│  └────────────┘  │
│                  │
│  Quay lại        │
│                  │
└──────────────────┘

(Full width, 16px padding, mobile-optimized)
```

---

## Animation Effects

### Button Hover
- Smooth gradient color transition (200ms)
- Shadow increase on hover
- Scale effect on mobile (optional)

### Progress Bar
- Smooth fill animation (300ms)
- Incremental updates as steps complete
- Green highlight when active

### Loading Spinner
- 24px circle
- 4px border width
- Rotating animation
- Color: White (on blue background)

### Message Alerts
- Fade in animation (200ms)
- Auto fade out after 5 seconds (200ms)
- Slide in from top (optional)

---

## Form States

### Initial State
- All fields empty
- Submit button enabled but slightly transparent
- No error/success messages

### Filled State
- At least one field has input
- Submit button fully opaque and hover-enabled
- Real-time validation feedback

### Loading State
- All fields disabled
- Submit button disabled with spinner
- "Đang xử lý..." text

### Success State
- Green success message
- Fields remain visible (context)
- Button disabled
- Auto-redirect timer

### Error State
- Red error message
- Field highlights in red (optional)
- Submit button re-enabled
- Focus on error field

---

## Keyboard Navigation

```
Tab: Next field
Shift+Tab: Previous field
Enter: Submit form (when focused on button)
Escape: Clear field (optional)
```

---

## Theme Variables

```typescript
// Colors
primary: 'blue-600'
primaryHover: 'blue-700'
secondary: 'indigo-600'
secondaryHover: 'indigo-700'
error: 'red-700'
success: 'green-700'
background: 'blue-50'

// Spacing
cardWidth: '448px'
padding: '24px'
gap: '20px'
borderRadius: '8px'

// Typography
titleSize: '30px'
descSize: '16px'
labelSize: '14px'
```

---

## Browser Support

- ✓ Chrome/Edge (latest 2 versions)
- ✓ Firefox (latest 2 versions)
- ✓ Safari (latest 2 versions)
- ✓ Mobile browsers (iOS Safari, Chrome Mobile)
- ✓ Fallback for older browsers

---

**UI Design**: Custom Tailwind CSS
**Component Library**: Shadcn/UI + Custom
**Responsive**: Mobile-First Approach
**Accessibility**: WCAG 2.1 AA Compliant

Perfect for clinic management system! 🏥
