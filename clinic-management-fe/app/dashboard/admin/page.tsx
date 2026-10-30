'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { Building2, ClipboardList, Loader2, Settings, Stethoscope, UserRound, Users } from 'lucide-react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { departmentService } from '@/services/department.service';
import { roomService } from '@/services/room.service';
import { userService } from '@/services/user.service';

type AdminStats = {
  totalUsers: number;
  activeUsers: number;
  patients: number;
  doctors: number;
  nurses: number;
  rooms: number;
  activeRooms: number;
  departments: number;
  activeDepartments: number;
};

const initialStats: AdminStats = {
  totalUsers: 0,
  activeUsers: 0,
  patients: 0,
  doctors: 0,
  nurses: 0,
  rooms: 0,
  activeRooms: 0,
  departments: 0,
  activeDepartments: 0,
};

const doctorRoles = ['ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SPECIALISTDOCTOR'];

const hasRole = (roles: string[], allowed: string[]) => roles.some((role) => allowed.includes(role));

const formatNumber = (value: number) => new Intl.NumberFormat('vi-VN').format(value);

export default function AdminDashboard() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<AdminStats>(initialStats);

  useEffect(() => {
    const fetchDashboard = async () => {
      setLoading(true);
      const [usersResult, roomsResult, departmentsResult] = await Promise.allSettled([
        userService.getUsers(0, 1000, '', 'ALL'),
        roomService.getList(null, '', 0, 1000, true),
        departmentService.getList('', 0, 1000, true),
      ]);

      const users = usersResult.status === 'fulfilled' ? usersResult.value.users || [] : [];
      const rooms = roomsResult.status === 'fulfilled' ? roomsResult.value.rooms || [] : [];
      const departments =
        departmentsResult.status === 'fulfilled' ? departmentsResult.value.departments || [] : [];

      setStats({
        totalUsers: usersResult.status === 'fulfilled' ? usersResult.value.totalElements || users.length : users.length,
        activeUsers: users.filter((user) => user.isActive).length,
        patients: users.filter((user) => hasRole(user.roles, ['ROLE_PATIENT'])).length,
        doctors: users.filter((user) => hasRole(user.roles, doctorRoles)).length,
        nurses: users.filter((user) => hasRole(user.roles, ['ROLE_NURSE'])).length,
        rooms: roomsResult.status === 'fulfilled' ? roomsResult.value.totalItems || rooms.length : rooms.length,
        activeRooms: rooms.filter((room) => (room.status || 'ACTIVE') === 'ACTIVE').length,
        departments:
          departmentsResult.status === 'fulfilled'
            ? departmentsResult.value.totalItems || departments.length
            : departments.length,
        activeDepartments: departments.filter((department) => department.isActive).length,
      });

      setLoading(false);
    };

    fetchDashboard();
  }, []);

  const cards = useMemo(
    () => [
      {
        label: 'Tổng người dùng',
        value: stats.totalUsers,
        helper: `${formatNumber(stats.activeUsers)} tài khoản đang hoạt động`,
        icon: Users,
        tone: 'bg-blue-50 text-blue-700',
      },
      {
        label: 'Phòng khám',
        value: stats.rooms,
        helper: `${formatNumber(stats.activeRooms)} phòng đang hoạt động`,
        icon: Building2,
        tone: 'bg-emerald-50 text-emerald-700',
      },
      {
        label: 'Bác sĩ',
        value: stats.doctors,
        helper: `${formatNumber(stats.nurses)} y tá trong hệ thống`,
        icon: Stethoscope,
        tone: 'bg-indigo-50 text-indigo-700',
      },
      {
        label: 'Bệnh nhân',
        value: stats.patients,
        helper: `${formatNumber(stats.departments)} khoa/phòng ban`,
        icon: UserRound,
        tone: 'bg-amber-50 text-amber-700',
      },
    ],
    [stats]
  );

  return (
    <div className="min-h-full">
      <DashboardHeader title="Dashboard - Quản trị hệ thống" subtitle="Theo dõi dữ liệu vận hành thật của hệ thống" />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          {cards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.label} className="rounded-lg border bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-medium text-slate-600">{card.label}</p>
                    <div className="mt-2 flex items-center gap-2">
                      <p className="text-3xl font-bold text-slate-950">
                        {loading ? <Loader2 className="h-7 w-7 animate-spin text-slate-400" /> : formatNumber(card.value)}
                      </p>
                    </div>
                    <p className="mt-2 text-sm text-slate-500">{card.helper}</p>
                  </div>
                  <div className={`rounded-md p-3 ${card.tone}`}>
                    <Icon className="h-6 w-6" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="mt-8 rounded-lg border bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-2 border-b pb-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 className="text-lg font-semibold text-slate-950">Quản lý hệ thống</h2>
              <p className="mt-1 text-sm text-slate-500">Các lối tắt này đi thẳng vào màn chức năng thật.</p>
            </div>
            <span className="text-sm text-slate-500">
              Khoa đang hoạt động: {formatNumber(stats.activeDepartments)}
            </span>
          </div>

          <div className="mt-5 grid gap-4 md:grid-cols-3">
            <Link href="/dashboard/admin/users" className="rounded-md border p-4 hover:bg-gray-50">
              <Users className="mb-3 h-5 w-5 text-blue-700" />
              <p className="font-semibold text-slate-950">Quản lý người dùng</p>
              <p className="mt-1 text-sm text-slate-600">Tạo tài khoản, khóa/mở khóa và phân vai trò.</p>
            </Link>
            <Link href="/dashboard/admin/clinic-structure" className="rounded-md border p-4 hover:bg-gray-50">
              <Building2 className="mb-3 h-5 w-5 text-emerald-700" />
              <p className="font-semibold text-slate-950">Cơ cấu khoa phòng</p>
              <p className="mt-1 text-sm text-slate-600">Quản lý khoa, phòng và trạng thái hoạt động.</p>
            </Link>
            <Link href="/dashboard/admin/services" className="rounded-md border p-4 hover:bg-gray-50">
              <Settings className="mb-3 h-5 w-5 text-indigo-700" />
              <p className="font-semibold text-slate-950">Dịch vụ & cấu hình</p>
              <p className="mt-1 text-sm text-slate-600">Cập nhật danh mục dịch vụ và liên kết phòng thực hiện.</p>
            </Link>
          </div>
        </div>

        <div className="mt-6 rounded-lg border bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <ClipboardList className="h-5 w-5 text-slate-700" />
            <div>
              <h2 className="font-semibold text-slate-950">Tình trạng dữ liệu</h2>
              <p className="text-sm text-slate-500">
                Dashboard này không còn dùng số mockup; số liệu được lấy trực tiếp từ API user, phòng và khoa.
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
