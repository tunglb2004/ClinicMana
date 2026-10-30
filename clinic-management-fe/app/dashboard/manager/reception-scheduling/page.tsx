'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { CalendarDays, Search } from 'lucide-react';
import { staffScheduleService } from '@/services/staff-schedule.service';
import { userService } from '@/services/user.service';
import { StaffScheduleResponse } from '@/types/staff-schedule.types';
import { User } from '@/types/user.types';

type DoctorRow = {
  userId: number;
  code: string;
  fullName: string;
  specialty: string;
  phone: string;
  status: string;
};

const hasAnyRole = (user: User, roleNames: string[]) => user.roles.some((role) => roleNames.includes(role));

const normalizePhone = (value?: string) => value || '--';

const normalizeSpecialty = (roles: string[]) => {
  if (roles.includes('ROLE_SPECIALISTDOCTOR')) return 'Chuyên khoa';
  if (roles.includes('ROLE_CLINICALDOCTOR')) return 'Lâm sàng';
  return 'Tổng hợp';
};

const todayKey = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const describeSchedule = (schedules: StaffScheduleResponse[]) => {
  if (schedules.length === 0) return 'Chưa xếp lịch hôm nay';

  const labels = schedules.map((schedule) => {
    const shift = schedule.shiftName || `${schedule.startTime}-${schedule.endTime}`;
    return schedule.roomName ? `${shift} - ${schedule.roomName}` : shift;
  });

  return `Đã có lịch: ${labels.join('; ')}`;
};

export default function ManagerReceptionSchedulingPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [rows, setRows] = useState<DoctorRow[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  useEffect(() => {
    const fetchDoctors = async () => {
      setLoading(true);
      try {
        const [usersResponse, schedulesResponse] = await Promise.all([
          userService.getUsers(0, 500, '', 'ALL'),
          staffScheduleService.getStaffSchedules({ date: todayKey(), page: 0, size: 1000 }),
        ]);

        const schedulesByUserId = new Map<number, StaffScheduleResponse[]>();
        (schedulesResponse.schedules || []).forEach((schedule) => {
          const current = schedulesByUserId.get(schedule.userId) || [];
          schedulesByUserId.set(schedule.userId, [...current, schedule]);
        });

        const staff = (usersResponse.users || []).filter((item) =>
          hasAnyRole(item, ['ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_SPECIALISTDOCTOR'])
        );

        const mapped: DoctorRow[] = staff.map((item, index) => ({
          userId: item.userId,
          code: item.staffId || item.doctorId || `DR${String(index + 1).padStart(3, '0')}`,
          fullName: item.fullName || '--',
          specialty: normalizeSpecialty(item.roles),
          phone: normalizePhone(item.phoneNumber),
          status: describeSchedule(schedulesByUserId.get(item.userId) || []),
        }));

        setRows(mapped);
      } catch (error) {
        console.error('Failed to fetch doctors for scheduling:', error);
        setRows([]);
      } finally {
        setLoading(false);
      }
    };

    fetchDoctors();
  }, []);

  const filteredRows = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return rows;

    return rows.filter((row) =>
      [row.code, row.fullName, row.specialty, row.phone].some((value) =>
        value.toLowerCase().includes(keyword)
      )
    );
  }, [rows, search]);

  const toggleRow = (userId: number) => {
    setSelectedIds((prev) =>
      prev.includes(userId) ? prev.filter((id) => id !== userId) : [...prev, userId]
    );
  };

  const toggleAll = () => {
    if (filteredRows.length === 0) return;
    const allIds = filteredRows.map((row) => row.userId);
    const isAllSelected = allIds.every((id) => selectedIds.includes(id));
    setSelectedIds(
      isAllSelected
        ? selectedIds.filter((id) => !allIds.includes(id))
        : [...new Set([...selectedIds, ...allIds])]
    );
  };

  const handleProceed = () => {
    if (selectedIds.length === 0) return;
    router.push(`/dashboard/manager/staff-schedules?doctorIds=${selectedIds.join(',')}`);
  };

  const allFilteredSelected =
    filteredRows.length > 0 && filteredRows.every((row) => selectedIds.includes(row.userId));

  return (
    <div className="min-h-[calc(100vh-4rem)] p-6 text-slate-950">
      <div className="dashboard-card overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-slate-200 bg-gradient-to-r from-blue-50 to-teal-50 px-5 py-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-950">Danh sách bác sĩ</h1>
            <p className="mt-1 text-sm text-slate-600">
              Chọn bác sĩ để xếp ca, trạng thái lấy theo lịch làm việc hôm nay.
            </p>
          </div>
          <div className="flex items-center gap-2 rounded-full bg-white px-3 py-2 text-xs font-semibold text-blue-700 shadow-sm">
            <CalendarDays className="h-4 w-4" />
            {todayKey()}
          </div>
        </div>

        <div className="border-b border-slate-200 p-4">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-5 w-5 text-slate-400" />
            <input
              className="h-10 w-full rounded-lg border border-slate-300 bg-white py-2 pl-10 pr-3 text-sm text-slate-950 focus:border-blue-500 focus:outline-none"
              placeholder="Nhập mã NV, số điện thoại hoặc tên bác sĩ..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
        </div>

        <div className="p-4">
          <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
            <table className="w-full min-w-[900px] text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th className="w-10 px-3 py-3 text-center">
                    <input
                      type="checkbox"
                      checked={allFilteredSelected}
                      onChange={toggleAll}
                      className="h-4 w-4 rounded border-slate-300"
                    />
                  </th>
                  <th className="px-3 py-3 text-left">Mã NV</th>
                  <th className="px-3 py-3 text-left">Họ tên bác sĩ</th>
                  <th className="px-3 py-3 text-left">Chuyên khoa</th>
                  <th className="px-3 py-3 text-left">Số điện thoại</th>
                  <th className="px-3 py-3 text-left">Trạng thái lịch</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading ? (
                  <tr>
                    <td colSpan={6} className="px-3 py-8 text-center text-sm text-slate-500">
                      Đang tải danh sách bác sĩ...
                    </td>
                  </tr>
                ) : filteredRows.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-3 py-8 text-center text-sm text-slate-500">
                      Không có dữ liệu phù hợp.
                    </td>
                  </tr>
                ) : (
                  filteredRows.map((row) => {
                    const checked = selectedIds.includes(row.userId);
                    const scheduled = row.status.startsWith('Đã có lịch');

                    return (
                      <tr key={row.userId} className="hover:bg-blue-50/40">
                        <td className="px-3 py-3 text-center">
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={() => toggleRow(row.userId)}
                            className="h-4 w-4 rounded border-slate-300"
                          />
                        </td>
                        <td className="px-3 py-3 font-semibold text-slate-800">{row.code}</td>
                        <td className="px-3 py-3 font-semibold text-slate-950">{row.fullName}</td>
                        <td className="px-3 py-3 text-slate-700">{row.specialty}</td>
                        <td className="px-3 py-3 text-slate-700">{row.phone}</td>
                        <td className="px-3 py-3">
                          <span className={scheduled ? 'status-badge status-success' : 'status-badge status-warning'}>
                            {row.status}
                          </span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="flex flex-col gap-3 border-t border-slate-200 bg-white px-5 py-4 text-sm font-semibold sm:flex-row sm:items-center sm:justify-between">
          <span className="text-slate-600">Đã chọn: {selectedIds.length} bác sĩ</span>
          <button
            onClick={handleProceed}
            disabled={selectedIds.length === 0}
            className="rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Tiến hành xếp ca
          </button>
        </div>
      </div>
    </div>
  );
}
