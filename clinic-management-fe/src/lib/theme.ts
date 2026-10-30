/**
 * THEME UTILITIES - Hỗ trợ sử dụng theme colors trong component
 */

import { themeConfig } from '@/config/theme';

/**
 * Lấy CSS class từ theme config
 * @example
 * getThemeClass('primary', 'main') // return '#2563EB'
 */
export function getThemeColor(category: keyof typeof themeConfig.colors, level?: string): string {
  const colorGroup = themeConfig.colors[category];
  if (!colorGroup) return '';
  
  if (typeof colorGroup === 'string') {
    return colorGroup;
  }
  
  return colorGroup[level as keyof typeof colorGroup] || '';
}

/**
 * Tailwind classes for theme colors
 * @example
 * <button className={getThemeTailwindClasses('primary')}>Login</button>
 */
export const themeTailwind = {
  // Primary color classes
  primaryBg: 'bg-[var(--primary)]',
  primaryText: 'text-[var(--primary)]',
  primaryBorder: 'border-[var(--primary)]',
  primaryHover: 'hover:bg-[var(--primary-light)]',
  primaryDark: 'bg-[var(--primary-dark)]',

  // Secondary
  secondaryBg: 'bg-[var(--secondary)]',
  secondaryText: 'text-[var(--secondary)]',
  secondaryBorder: 'border-[var(--secondary)]',
  
  // Accent
  accentBg: 'bg-[var(--accent)]',
  accentText: 'text-[var(--accent)]',
  accentBorder: 'border-[var(--accent)]',

  // Destructive
  destructiveBg: 'bg-[var(--destructive)]',
  destructiveText: 'text-[var(--destructive)]',
  destructiveBorder: 'border-[var(--destructive)]',

  // Status colors
  successText: 'text-[var(--success)]',
  warningText: 'text-[var(--warning)]',
  errorText: 'text-[var(--error)]',
  infoText: 'text-[var(--info)]',

  // Background & Text
  bg: 'bg-[var(--background)]',
  text: 'text-[var(--foreground)]',
  mutedText: 'text-[var(--muted)]',
  border: 'border-[var(--border)]',
};

/**
 * Inline style object for theme colors
 */
export const themeStyles = {
  getStyle: (colorKey: string) => ({
    color: `var(--${colorKey})`,
  }),
  getPrimaryStyle: () => ({
    backgroundColor: 'var(--primary)',
    color: 'var(--primary-foreground)',
  }),
  getSecondaryStyle: () => ({
    backgroundColor: 'var(--secondary)',
    color: 'var(--secondary-foreground)',
  }),
};
