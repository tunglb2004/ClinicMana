'use client';

import { DashboardHeader } from '@/components/dashboard/DashboardHeader';
import { DashboardSidebar } from '@/components/dashboard/DashboardSidebar';

export default function DashboardShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="dashboard-gradient flex min-h-screen flex-col">
      <DashboardHeader />
      <div className="flex min-h-0 flex-1">
        <DashboardSidebar />
        <div className="min-h-0 min-w-0 flex-1 overflow-y-auto">
          {children}
        </div>
      </div>
    </div>
  );
}
