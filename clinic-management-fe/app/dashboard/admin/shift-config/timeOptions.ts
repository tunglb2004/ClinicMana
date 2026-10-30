/** Các mốc giờ dùng cho select (bước 15 phút: 00:00 -> 23:45) */
const STEP_MINUTES = 15;
const HOURS = 24;

const options: string[] = [];
for (let h = 0; h < HOURS; h++) {
  for (let m = 0; m < 60; m += STEP_MINUTES) {
    options.push(
      `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
    );
  }
}

export const TIME_SELECT_OPTIONS = options;

/** Format "HH:mm" -> "HH:00 AM/PM" để hiển thị (tùy chọn) */
export function formatTimeLabel(value: string): string {
  const [hStr, mStr] = value.split(':');
  const h = parseInt(hStr ?? '0', 10);
  const m = parseInt(mStr ?? '0', 10);
  if (h === 0 && m === 0) return '12:00 AM';
  if (h === 12 && m === 0) return '12:00 PM';
  if (h < 12) return `${h}:${String(m).padStart(2, '0')} AM`;
  return `${h - 12}:${String(m).padStart(2, '0')} PM`;
}
