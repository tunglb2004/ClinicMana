'use client';

import { useEffect } from 'react';
import { useToastStore } from '@/store/toast.store';
import { cn } from '@/lib/utils';

const TOAST_DURATION = 3000;

export function Toast() {
  const { message, variant, hide } = useToastStore();

  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(hide, TOAST_DURATION);
    return () => clearTimeout(timer);
  }, [message, hide]);

  if (!message) return null;

  return (
    <div className="fixed bottom-4 right-4 z-[9999] animate-in fade-in slide-in-from-bottom-2">
      <div
        className={cn(
          'rounded-lg border-2 border-black px-4 py-3 shadow-lg',
          variant === 'success' && 'bg-green-50 text-green-900 border-green-600',
          variant === 'error' && 'bg-red-50 text-red-900 border-red-600',
          variant === 'default' && 'bg-white text-gray-900'
        )}
      >
        <p className="text-sm font-medium">{message}</p>
      </div>
    </div>
  );
}
