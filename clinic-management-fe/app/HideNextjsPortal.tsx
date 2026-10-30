'use client';

import { useEffect } from 'react';

/**
 * Ẩn phần tử nextjs-portal 0x0 do Next.js dev inject (tránh lỗi Cursor/DevTools).
 * Chỉ ẩn khi portal rỗng hoặc chỉ dùng cho dev-tools-scale.
 */
export function HideNextjsPortal() {
  useEffect(() => {
    const hide = () => {
      if (typeof document === 'undefined') return;
      const portals = document.getElementsByTagName('nextjs-portal');
      for (let i = 0; i < portals.length; i++) {
        const el = portals[i] as HTMLElement;
        const style = el.getAttribute('style') || '';
        const hasContent = el.childElementCount > 0 || (el.shadowRoot?.childElementCount ?? 0) > 0;
        const isDevTools = style.includes('nextjs-dev-tools-scale');
        if (hasContent && !isDevTools) {
          el.style.removeProperty('display');
        } else if (!hasContent || isDevTools) {
          el.style.setProperty('display', 'none', 'important');
        }
      }
    };
    hide();
    const observer = new MutationObserver(hide);
    observer.observe(document.body, { childList: true, subtree: true });
    return () => observer.disconnect();
  }, []);
  return null;
}
