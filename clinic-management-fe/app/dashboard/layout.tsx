import ProtectedRoute from '@/components/layout/ProtectedRoute';
import DashboardMain from '@/components/layout/DashboardMain';
import DashboardShell from '@/components/layout/DashboardShell';
import { Toast } from '@/components/ui/toast';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Dashboard - Clinic Management',
  description: 'Trang quản lý hệ thống',
};

import { SidebarProvider } from '@/contexts/SidebarContext';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ProtectedRoute>
      <SidebarProvider>
        <DashboardShell>
          <DashboardMain>{children}</DashboardMain>
        </DashboardShell>
      </SidebarProvider>
      <Toast />
    </ProtectedRoute>
  );
}
