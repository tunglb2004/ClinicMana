'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import type { ComponentType } from 'react';
import {
  CalendarDays,
  ClipboardList,
  Clock,
  FileText,
  HandCoins,
  Heart,
  LayoutDashboard,
  MessageSquare,
  Network,
  Receipt,
  Settings,
  Stethoscope,
  UserCog,
  Users,
} from 'lucide-react';
import { useSidebar } from '@/contexts/SidebarContext';
import { getDashboardRoute, hasRole, ROLES } from '@/lib/roleRedirect';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/store/auth.store';

type MenuItem = {
  label: string;
  href: string;
  icon: ComponentType<{ className?: string }>;
};

const adminMenuItems: MenuItem[] = [
  { label: 'Dashboard', href: '/dashboard/admin', icon: LayoutDashboard },
  { label: 'Nhân viên', href: '/dashboard/admin/users', icon: UserCog },
  { label: 'Bệnh nhân', href: '/dashboard/admin/patients', icon: Users },
  { label: 'Khoa phòng', href: '/dashboard/admin/clinic-structure', icon: Network },
  { label: 'Dịch vụ', href: '/dashboard/admin/services', icon: Stethoscope },
  { label: 'Cấu hình ca', href: '/dashboard/admin/shift-config', icon: Clock },
];

const managerMenuItems: MenuItem[] = [
  { label: 'Tổng quan', href: '/dashboard/manager', icon: LayoutDashboard },
  { label: 'Nhân sự', href: '/dashboard/manager/staff', icon: UserCog },
  { label: 'Lịch làm việc', href: '/dashboard/manager/staff-schedules', icon: Clock },
  { label: 'Tài chính', href: '/dashboard/manager/finance', icon: Receipt },
  { label: 'Phản hồi', href: '/dashboard/manager/feedback', icon: MessageSquare },
  { label: 'Cài đặt', href: '/dashboard/manager/settings', icon: Settings },
];

const receptionistMenuItems: MenuItem[] = [
  { label: 'Dashboard', href: '/dashboard/receptionist', icon: LayoutDashboard },
  { label: 'Bệnh nhân', href: '/dashboard/receptionist/patients', icon: Users },
];

const cashierMenuItems: MenuItem[] = [
  { label: 'Dashboard', href: '/dashboard/cashier', icon: LayoutDashboard },
  { label: 'Hóa đơn', href: '/dashboard/cashier/invoices', icon: Receipt },
  { label: 'Bàn giao tiền', href: '/dashboard/cashier/handover', icon: HandCoins },
];

const doctorMenuItems: MenuItem[] = [
  { label: 'Dashboard', href: '/dashboard/doctor', icon: LayoutDashboard },
  { label: 'Hồ sơ khám', href: '/dashboard/doctor/records', icon: FileText },
  { label: 'Hàng đợi', href: '/dashboard/doctor/patients', icon: Users },
];

const specialistDoctorMenuItems: MenuItem[] = [
  { label: 'Dashboard', href: '/dashboard/specialist-doctor', icon: LayoutDashboard },
  { label: 'Hàng đợi CLS', href: '/dashboard/specialist-doctor/patients', icon: ClipboardList },
];

const nurseMenuItems: MenuItem[] = [
  { label: 'Dashboard', href: '/dashboard/nurse', icon: LayoutDashboard },
  { label: 'Lâm sàng', href: '/dashboard/nurse/clinical-patients', icon: ClipboardList },
  { label: 'Cận lâm sàng', href: '/dashboard/nurse/patients', icon: Users },
];

const patientMenuItems: MenuItem[] = [
  { label: 'Dashboard', href: '/dashboard/patient', icon: LayoutDashboard },
  { label: 'Lịch khám', href: '/dashboard/patient/appointments', icon: CalendarDays },
  { label: 'Hồ sơ sức khỏe', href: '/dashboard/patient/health-record', icon: Heart },
  { label: 'Thanh toán', href: '/dashboard/patient/payment-history', icon: Receipt },
  { label: 'Đánh giá', href: '/dashboard/patient/reviews', icon: MessageSquare },
];

function isActivePath(pathname: string, href: string) {
  if (pathname === href) return true;
  if (href.split('/').length <= 3) return false;
  return pathname.startsWith(`${href}/`);
}

export function DashboardSidebar() {
  const pathname = usePathname();
  const { isSidebarOpen } = useSidebar();
  const user = useAuthStore((state) => state.user);
  const roles = user?.roles ?? [];

  const isAdmin = hasRole(roles, ROLES.SYSTEM_ADMIN);
  const isClinicManager = hasRole(roles, ROLES.CLINIC_MANAGER);
  const isReceptionist = hasRole(roles, ROLES.RECEPTIONIST);
  const isCashier = hasRole(roles, ROLES.CASHIER);
  const isClinicalDoctor = hasRole(roles, ROLES.CLINICAL_DOCTOR);
  const isSpecialistDoctor = hasRole(roles, ROLES.SPECIALIST_DOCTOR);
  const isDoctor = hasRole(roles, ROLES.DOCTOR);
  const isNurse = hasRole(roles, ROLES.NURSE);
  const isPatient = hasRole(roles, ROLES.PATIENT);

  let menuItems: MenuItem[] = [
    { label: 'Dashboard', href: getDashboardRoute(roles), icon: LayoutDashboard },
  ];

  if (isAdmin) menuItems = adminMenuItems;
  else if (isClinicManager) menuItems = managerMenuItems;
  else if (isReceptionist) menuItems = receptionistMenuItems;
  else if (isCashier) menuItems = cashierMenuItems;
  else if (isSpecialistDoctor) menuItems = specialistDoctorMenuItems;
  else if (isDoctor || isClinicalDoctor) menuItems = doctorMenuItems;
  else if (isNurse) menuItems = nurseMenuItems;
  else if (isPatient) menuItems = patientMenuItems;

  return (
    <aside
      className={cn(
        'flex shrink-0 flex-col overflow-hidden border-r border-slate-200/80 bg-white/95 pt-4 shadow-[10px_0_35px_-32px_rgb(15_23_42/0.45)] backdrop-blur transition-all duration-300',
        isSidebarOpen ? 'w-72' : 'w-20'
      )}
      style={{ minHeight: 'calc(100vh - 4rem)' }}
    >
      <div className="px-4 pb-3">
        {isSidebarOpen ? (
          <div className="rounded-xl border border-blue-100 bg-gradient-to-br from-blue-50 to-teal-50 px-4 py-3">
            <p className="text-xs font-bold uppercase tracking-wide text-blue-700">ClinicPro</p>
            <p className="mt-1 text-xs text-slate-600">Bảng điều khiển</p>
          </div>
        ) : (
          <div className="mx-auto h-2 w-8 rounded-full bg-blue-500" />
        )}
      </div>

      <nav className="flex-1 space-y-1.5 px-3">
        {menuItems.map((item) => {
          const active = isActivePath(pathname, item.href);
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                'group flex items-center rounded-xl py-3 text-sm font-semibold transition-all duration-200',
                isSidebarOpen ? 'justify-start gap-3 px-4' : 'justify-center px-2',
                active
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20'
                  : 'text-slate-600 hover:bg-blue-50 hover:text-blue-700'
              )}
              title={!isSidebarOpen ? item.label : ''}
            >
              <Icon className={cn('h-5 w-5 shrink-0', active ? 'text-white' : 'text-slate-500 group-hover:text-blue-700')} />
              <span
                className={cn(
                  'whitespace-nowrap transition-all duration-200',
                  isSidebarOpen ? 'w-auto opacity-100' : 'hidden w-0 overflow-hidden opacity-0'
                )}
              >
                {item.label}
              </span>
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}

