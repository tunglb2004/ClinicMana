import { useState, useEffect } from 'react';

/**
 * Trả về giá trị được debounce - chỉ cập nhật sau khi value không đổi trong `delay` ms.
 * Dùng để giảm số lần gọi API khi user gõ tìm kiếm.
 */
export function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedValue(value);
    }, delay);
    return () => window.clearTimeout(timer);
  }, [value, delay]);

  return debouncedValue;
}
