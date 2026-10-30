'use client';

import { usePathname } from 'next/navigation';

export default function DashboardMain({ children }: { children: React.ReactNode }) {
    const pathname = usePathname();
    const isWidePage = pathname?.startsWith('/dashboard/admin') || pathname?.startsWith('/dashboard/manager');

    return (
        <main className={isWidePage ? "min-h-full w-full" : "min-h-full w-full max-w-7xl mx-auto px-6 py-8"}>
            {children}
        </main>
    );
}
