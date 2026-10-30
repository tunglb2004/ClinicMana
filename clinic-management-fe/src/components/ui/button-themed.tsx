/**
 * THEMED BUTTON COMPONENT
 * Sử dụng theme colors từ config
 */

import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center whitespace-nowrap rounded-lg font-medium transition-all duration-250 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        primary:
          'bg-blue-600 text-white shadow-sm shadow-blue-600/20 hover:bg-blue-700 active:scale-95',
        secondary:
          'bg-teal-600 text-white shadow-sm shadow-teal-600/20 hover:bg-teal-700 active:scale-95',
        accent:
          'bg-violet-600 text-white shadow-sm shadow-violet-600/20 hover:bg-violet-700 active:scale-95',
        destructive:
          'bg-red-600 text-white shadow-sm shadow-red-600/20 hover:bg-red-700 active:scale-95',
        outline:
          'border border-slate-300 bg-white text-slate-700 hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 active:scale-95',
        ghost:
          'text-slate-700 hover:bg-blue-50 hover:text-blue-700 active:scale-95',
      },
      size: {
        sm: 'h-9 px-3 text-sm',
        md: 'h-10 px-4 text-base',
        lg: 'h-12 px-6 text-lg',
        xl: 'h-14 px-8 text-lg',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  }
);

interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => (
    <button
      className={cn(buttonVariants({ variant, size, className }))}
      ref={ref}
      {...props}
    />
  )
);

Button.displayName = 'Button';

export { Button, buttonVariants };
export type { ButtonProps };
