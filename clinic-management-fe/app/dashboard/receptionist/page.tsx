'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { CalendarCheck, CalendarPlus, ClipboardList, Loader2, UserCheck } from 'lucide-react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { appointmentService, AppointmentItem } from '@/services/appointment.service';

type ReceptionistStats = {
  todayAppointments: number;
  requested: number;
  waiting: number;
  completed: number;
  cancelled: number;
};

const initialStats: ReceptionistStats = {
  todayAppointments: 0,
  requested: 0,
  waiting: 0,
  completed: 0,
  cancelled: 0,
};

const todayKey = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const isTodayAppointment = (appointment: AppointmentItem) => {
  const datePart = appointment.appointmentDate?.split('T')[0];
  return datePart === todayKey();
};

const formatNumber = (value: number) => new Intl.NumberFormat('vi-VN').format(value);

export default function ReceptionistDashboard() {
  const [loading, setLoading] = useState(true);
  const [appointments, setAppointments] = useState<AppointmentItem[]>([]);

  const fetchDashboard = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const response = await appointmentService.getAppointments(1, 500);
      setAppointments(response.content || []);
    } catch (error) {
      console.error('Failed to fetch receptionist dashboard:', error);
      setAppointments([]);
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboard();
    const intervalId = window.setInterval(() => fetchDashboard(true), 30000);
    return () => window.clearInterval(intervalId);
  }, [fetchDashboard]);

  const stats = useMemo(() => {
    const todayAppointments = appointments.filter(isTodayAppointment);
    return todayAppointments.reduce<ReceptionistStats>(
      (acc, appointment) => {
        acc.todayAppointments += 1;
        if (appointment.status === 'REQUESTED' || appointment.status === 'CONFIRMED') acc.requested += 1;
        if (appointment.status === 'WAITING' || appointment.status === 'IN_PROGRESS') acc.waiting += 1;
        if (appointment.status === 'COMPLETED') acc.completed += 1;
        if (appointment.status === 'CANCELLED') acc.cancelled += 1;
        return acc;
      },
      { ...initialStats }
    );
  }, [appointments]);

  const cards = [
    {
      label: 'Lịch hẹn hôm nay',
      value: stats.todayAppointments,
      icon: CalendarCheck,
      color: 'text-blue-700',
      bg: 'bg-blue-50',
    },
    {
      label: 'Chờ xác nhận',
      value: stats.requested,
      icon: ClipboardList,
      color: 'text-amber-700',
      bg: 'bg-amber-50',
    },
    {
      label: 'Đang chờ khám',
      value: stats.waiting,
      icon: UserCheck,
      color: 'text-emerald-700',
      bg: 'bg-emerald-50',
    },
    {
      label: 'Hoàn thành',
      value: stats.completed,
      icon: CalendarCheck,
      color: 'text-indigo-700',
      bg: 'bg-indigo-50',
    },
  ];

  return (
    <div className="min-h-full">
      <DashboardHeader title="Dashboard - Lễ tân" subtitle="Đón tiếp, tạo lịch, check-in và theo dõi lịch hẹn hôm nay" />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          {cards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.label} className="rounded-lg border bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm font-medium text-slate-600">{card.label}</p>
                    <p className={`mt-2 text-3xl font-bold ${card.color}`}>
                      {loading ? <Loader2 className="h-7 w-7 animate-spin text-slate-400" /> : formatNumber(card.value)}
                    </p>
                  </div>
                  <div className={`rounded-md p-3 ${card.bg} ${card.color}`}>
                    <Icon className="h-6 w-6" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="mt-8 rounded-lg border bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-1 border-b pb-4">
            <h2 className="text-lg font-semibold text-slate-950">Chức năng nhanh</h2>
            <p className="text-sm text-slate-500">
              Số liệu tự làm mới mỗi 30 giây theo danh sách lịch hẹn đang có trong hệ thống.
            </p>
          </div>

          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <Link href="/dashboard/receptionist/create-appointment" className="rounded-md border p-4 hover:bg-gray-50">
              <CalendarPlus className="mb-3 h-5 w-5 text-blue-700" />
              <p className="font-semibold text-slate-950">Đăng ký khám mới</p>
              <p className="mt-1 text-sm text-slate-600">Tạo lịch khám mới hoặc tái khám cho bệnh nhân.</p>
            </Link>
            <Link href="/dashboard/receptionist/patients" className="rounded-md border p-4 hover:bg-gray-50">
              <UserCheck className="mb-3 h-5 w-5 text-emerald-700" />
              <p className="font-semibold text-slate-950">Danh sách bệnh nhân</p>
              <p className="mt-1 text-sm text-slate-600">Xem lịch hôm nay, xác nhận và check-in.</p>
            </Link>
          </div>

          {stats.cancelled > 0 && (
            <p className="mt-5 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              Hôm nay có {formatNumber(stats.cancelled)} lịch đã hủy.
            </p>
          )}
        </div>
      </main>
    </div>
  );
}
