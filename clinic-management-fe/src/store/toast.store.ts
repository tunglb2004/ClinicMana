import { create } from 'zustand';

type ToastVariant = 'success' | 'error' | 'default';

interface ToastState {
  message: string | null;
  variant: ToastVariant;
  show: (message: string, variant?: ToastVariant) => void;
  hide: () => void;
}

export const useToastStore = create<ToastState>((set) => ({
  message: null,
  variant: 'default',
  show: (message: string, variant: ToastVariant = 'default') => {
    set({ message, variant });
  },
  hide: () => set({ message: null }),
}));
