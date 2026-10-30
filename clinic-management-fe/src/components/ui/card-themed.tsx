/**
 * THEMED CARD COMPONENT
 * Card component với theme colors
 */

import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const cardVariants = cva(
  'rounded-xl border transition-all duration-300',
  {
    variants: {
      variant: {
        default: 'bg-white border-slate-200 shadow-[0_18px_40px_-32px_rgb(15_23_42/0.45)] hover:shadow-[0_22px_48px_-34px_rgb(15_23_42/0.5)]',
        primary: 'bg-gradient-to-br from-white via-blue-50 to-blue-100 border-blue-100 shadow-sm',
        secondary: 'bg-gradient-to-br from-white via-teal-50 to-teal-100 border-teal-100 shadow-sm',
        accent: 'bg-gradient-to-br from-white via-violet-50 to-violet-100 border-violet-100 shadow-sm',
      },
      size: {
        sm: 'p-3',
        md: 'p-6',
        lg: 'p-8',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  }
);

interface CardProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof cardVariants> {}

const ThemedCard = React.forwardRef<HTMLDivElement, CardProps>(
  ({ className, variant, size, ...props }, ref) => (
    <div
      className={cn(cardVariants({ variant, size, className }))}
      ref={ref}
      {...props}
    />
  )
);

ThemedCard.displayName = 'ThemedCard';

export { ThemedCard, cardVariants };
export type { CardProps };
