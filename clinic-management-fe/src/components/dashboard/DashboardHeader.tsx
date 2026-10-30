'use client';

import Link from 'next/link';
import { Menu } from 'lucide-react';
import { UserProfileDropdown } from './UserProfileDropdown';
import { useSidebar } from '@/contexts/SidebarContext';
import { getDashboardRoute } from '@/lib/roleRedirect';
import { useAuthStore } from '@/store/auth.store';

export function DashboardHeader() {
  const { toggleSidebar } = useSidebar();
  const user = useAuthStore((state) => state.user);
  const dashboardHref = user?.roles?.length ? getDashboardRoute(user.roles) : '/dashboard';

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200/80 bg-white/90 shadow-sm shadow-slate-200/60 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-6">
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={toggleSidebar}
            className="inline-flex h-10 w-10 items-center justify-center rounded-xl text-slate-600 transition-colors hover:bg-blue-50 hover:text-blue-700"
            aria-label="Mở hoặc thu gọn menu"
          >
            <Menu className="h-5 w-5" />
          </button>

          <Link href={dashboardHref} className="group flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-blue-600 to-teal-600 text-lg font-bold text-white shadow-md shadow-blue-600/25 transition-transform duration-300 group-hover:scale-105">
              CP
            </div>
            <div className="hidden leading-tight sm:block">
              <span className="block text-lg font-bold text-slate-950">ClinicPro</span>
              <span className="block text-xs font-medium text-slate-500">Healthcare dashboard</span>
            </div>
          </Link>
        </div>

        <UserProfileDropdown />
      </div>
    </header>
  );
}

