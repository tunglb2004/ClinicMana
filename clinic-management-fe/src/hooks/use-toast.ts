'use client';

import { useCallback } from 'react';
import { useToastStore } from '@/store/toast.store';

interface ToastOptions {
  description: string;
  variant?: 'success' | 'error' | 'destructive' | 'default';
}

export function useToast() {
  const show = useToastStore((s) => s.show);

  const toast = useCallback((options: ToastOptions) => {
    const variant =
      options.variant === 'destructive' ? 'error' : (options.variant ?? 'default');
    show(options.description, variant);
  }, [show]);

  return { toast };
}
