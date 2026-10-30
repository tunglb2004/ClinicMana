'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { departmentService, DepartmentResponse } from '@/services/department.service';
import { roomService, RoomResponse } from '@/services/room.service';
import { userService, SyncExistingStaffProfilesResponse } from '@/services/user.service';
import { User } from '@/types/user.types';
import { Loader2, RefreshCw } from 'lucide-react';

const hasAnyRole = (user: User, roles: string[]) => user.roles.some((role) => roles.includes(role));

const staffRoles = [
  'ROLE_DOCTOR',
  'ROLE_CLINICALDOCTOR',
  'ROLE_SPECIALISTDOCTOR',
  'ROLE_NURSE',
  'ROLE_RECEPTIONIST',
  'ROLE_CASHIER',
  'ROLE_CLINIC_MANAGER',
];

const getErrorMessage = (error: unknown, fallback: string) => {
  const data = (error as { response?: { data?: { message?: string; error?: string } } })?.response?.data;
  return data?.message || data?.error || fallback;
};

export default function ManagerSettingsPage() {
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [rooms, setRooms] = useState<RoomResponse[]>([]);
  const [syncResult, setSyncResult] = useState<SyncExistingStaffProfilesResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [usersResult, departmentsResult, roomsResult] = await Promise.all([
          userService.getUsers(0, 1000, '', 'ALL'),
          departmentService.getList('', 0, 100, true),
          roomService.getList(null, '', 0, 200, true),
        ]);
        if (cancelled) return;
        setUsers(usersResult.users ?? []);
        setDepartments(departmentsResult.departments ?? []);
        setRooms(roomsResult.rooms ?? []);
      } catch (fetchError) {
        console.error('Không thể tải cấu hình hệ thống:', fetchError);
        if (!cancelled) {
          setError('Không thể tải cấu hình hệ thống.');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchData();

    return () => {
      cancelled = true;
    };
  }, []);

  const stats = useMemo(() => {
    const staff = users.filter((item) => hasAnyRole(item, staffRoles));
    return {
      staffCount: staff.length,
      lockedStaff: staff.filter((item) => !item.isActive).length,
      activeDepartments: departments.filter((item) => item.isActive).length,
      inactiveDepartments: departments.filter((item) => !item.isActive).length,
      activeRooms: rooms.filter((item) => !item.status || item.status === 'ACTIVE').length,
      inactiveRooms: rooms.filter((item) => item.status && item.status !== 'ACTIVE').length,
    };
  }, [departments, rooms, users]);

  const handleSyncProfiles = async () => {
    setSyncing(true);
    setSyncResult(null);
    try {
      const result = await userService.syncExistingStaffProfiles();
      setSyncResult(result);
    } catch (syncError) {
      alert(getErrorMessage(syncError, 'Không thể đồng bộ hồ sơ nhân sự.'));
    } finally {
      setSyncing(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] p-6 text-slate-950">
      <div className="mx-auto max-w-7xl space-y-5">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Cài đặt vận hành</h1>
          <p className="mt-1 text-sm text-slate-600">
            Các thao tác cấu hình thật đang có trong hệ thống: nhân sự, khoa, phòng và đồng bộ hồ sơ.
          </p>
        </div>

        {error && <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div className="rounded-lg border bg-white p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase text-slate-500">Nhân sự</p>
            <p className="mt-2 text-3xl font-bold">{loading ? '...' : stats.staffCount}</p>
            <p className="mt-1 text-xs text-slate-600">{stats.lockedStaff} tài khoản đang khóa</p>
          </div>
          <div className="rounded-lg border bg-white p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase text-slate-500">Khoa/phòng ban</p>
            <p className="mt-2 text-3xl font-bold">{loading ? '...' : stats.activeDepartments}</p>
            <p className="mt-1 text-xs text-slate-600">{stats.inactiveDepartments} khoa tạm ngưng</p>
          </div>
          <div className="rounded-lg border bg-white p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase text-slate-500">Phòng khám</p>
            <p className="mt-2 text-3xl font-bold">{loading ? '...' : stats.activeRooms}</p>
            <p className="mt-1 text-xs text-slate-600">{stats.inactiveRooms} phòng không hoạt động</p>
          </div>
        </div>

        <div className="grid gap-4 lg:grid-cols-[1.2fr_1fr]">
          <div className="rounded-lg border bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold">Cấu hình nhanh</h2>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <Link href="/dashboard/manager/staff" className="rounded-md border p-4 hover:bg-slate-50">
                <p className="font-semibold">Quản lý nhân sự</p>
                <p className="mt-1 text-sm text-slate-600">Thêm, sửa, khóa hoặc mở khóa tài khoản nhân viên.</p>
              </Link>
              <Link href="/dashboard/manager/staff-schedules" className="rounded-md border p-4 hover:bg-slate-50">
                <p className="font-semibold">Lịch làm việc</p>
                <p className="mt-1 text-sm text-slate-600">Xếp ca bác sĩ, y tá theo phòng và ngày trực.</p>
              </Link>
              <Link href="/dashboard/admin/clinic-structure" className="rounded-md border p-4 hover:bg-slate-50">
                <p className="font-semibold">Cấu trúc phòng khám</p>
                <p className="mt-1 text-sm text-slate-600">Quản lý khoa, phòng và trạng thái hoạt động.</p>
              </Link>
              <Link href="/dashboard/admin/services" className="rounded-md border p-4 hover:bg-slate-50">
                <p className="font-semibold">Dịch vụ</p>
                <p className="mt-1 text-sm text-slate-600">Quản lý danh mục dịch vụ và giá áp dụng.</p>
              </Link>
            </div>
          </div>

          <div className="rounded-lg border bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold">Bảo trì dữ liệu nhân sự</h2>
            <p className="mt-2 text-sm text-slate-600">
              Đồng bộ hồ sơ bác sĩ/y tá cho các tài khoản cũ sau khi chỉnh role hoặc import dữ liệu.
            </p>
            <button
              onClick={handleSyncProfiles}
              disabled={syncing}
              className="mt-4 inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {syncing ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
              Đồng bộ hồ sơ nhân sự
            </button>

            {syncResult && (
              <div className="mt-4 rounded-md border bg-slate-50 p-3 text-sm text-slate-700">
                <p>Đã quét: {syncResult.totalUsersScanned} tài khoản</p>
                <p>Cập nhật: {syncResult.usersUpdated} tài khoản</p>
                <p>Tạo hồ sơ bác sĩ: {syncResult.doctorProfilesCreated}</p>
                <p>Tạo hồ sơ y tá: {syncResult.nurseProfilesCreated}</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
