/**
 * Central visual tokens for the clinic dashboard.
 * Keep product colors here, then consume them through CSS variables/classes.
 */

export const themeConfig = {
  colors: {
    primary: {
      light: '#60A5FA',
      main: '#2563EB',
      dark: '#1D4ED8',
      soft: '#DBEAFE',
      foreground: '#FFFFFF',
    },
    secondary: {
      light: '#5EEAD4',
      main: '#0F766E',
      dark: '#115E59',
      soft: '#CCFBF1',
      foreground: '#FFFFFF',
    },
    accent: {
      light: '#A78BFA',
      main: '#7C3AED',
      dark: '#5B21B6',
      soft: '#EDE9FE',
      foreground: '#FFFFFF',
    },
    success: {
      main: '#16A34A',
      soft: '#DCFCE7',
      foreground: '#14532D',
    },
    warning: {
      main: '#D97706',
      soft: '#FEF3C7',
      foreground: '#78350F',
    },
    danger: {
      main: '#DC2626',
      soft: '#FEE2E2',
      foreground: '#7F1D1D',
    },
    info: {
      main: '#0284C7',
      soft: '#E0F2FE',
      foreground: '#075985',
    },
    neutral: {
      page: '#F6F8FB',
      surface: '#FFFFFF',
      surfaceTint: '#F8FAFC',
      border: '#DDE5F0',
      text: '#0F172A',
      muted: '#64748B',
    },
  },

  radius: {
    sm: '0.375rem',
    md: '0.5rem',
    lg: '0.75rem',
    xl: '1rem',
    full: '9999px',
  },

  shadow: {
    sm: '0 1px 2px 0 rgb(15 23 42 / 0.05)',
    md: '0 10px 24px -18px rgb(15 23 42 / 0.35)',
    lg: '0 20px 45px -28px rgb(15 23 42 / 0.35)',
  },

  transition: {
    fast: '150ms cubic-bezier(0.4, 0, 0.2, 1)',
    base: '250ms cubic-bezier(0.4, 0, 0.2, 1)',
  },
};

export type ThemeConfig = typeof themeConfig;
