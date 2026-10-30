'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { Activity, ClipboardList, FileText, Loader2, PhoneCall, Stethoscope } from 'lucide-react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { appointmentService, AppointmentItem } from '@/services/appointment.service';

type DoctorStats = {
  waiting: number;
  inProgress: number;
  completedToday: number;
  totalQueue: number;
};

const initialStats: DoctorStats = {
  waiting: 0,
  inProgress: 0,
  completedToday: 0,
  totalQueue: 0,
};

const todayKey = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const isToday = (appointment: AppointmentItem) => appointment.appointmentDate?.split('T')[0] === todayKey();

const formatNumber = (value: number) => new Intl.NumberFormat('vi-VN').format(value);

export default function ClinicalDoctorDashboard() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DoctorStats>(initialStats);
  const [currentPatient, setCurrentPatient] = useState<AppointmentItem | null>(null);

  const fetchDashboard = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const [queueResponse, completedResponse] = await Promise.all([
        appointmentService.getMyPatients(1, 100),
        appointmentService.getMyPatients(1, 100, 'COMPLETED'),
      ]);

      const queue = queueResponse.content || [];
      const completedToday = (completedResponse.content || []).filter(isToday).length;
      const inProgressPatient = queue.find((appointment) => appointment.status === 'IN_PROGRESS') || null;

      setStats({
        waiting: queue.filter((appointment) => appointment.status === 'WAITING').length,
        inProgress: inProgressPatient ? 1 : 0,
        completedToday,
        totalQueue: queue.length,
      });
      setCurrentPatient(inProgressPatient);
    } catch (error) {
      console.error('Failed to fetch clinical doctor dashboard:', error);
      setStats(initialStats);
      setCurrentPatient(null);
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboard();
    const intervalId = window.setInterval(() => fetchDashboard(true), 30000);
    return () => window.clearInterval(intervalId);
  }, [fetchDashboard]);

  const cards = useMemo(
    () => [
      {
        label: 'Đang chờ gọi',
        value: stats.waiting,
        icon: ClipboardList,
        color: 'text-blue-700',
        bg: 'bg-blue-50',
      },
      {
        label: 'Đang khám',
        value: stats.inProgress,
        icon: Activity,
        color: 'text-emerald-700',
        bg: 'bg-emerald-50',
      },
      {
        label: 'Hoàn thành hôm nay',
        value: stats.completedToday,
        icon: Stethoscope,
        color: 'text-indigo-700',
        bg: 'bg-indigo-50',
      },
      {
        label: 'Tổng hàng đợi',
        value: stats.totalQueue,
        icon: PhoneCall,
        color: 'text-amber-700',
        bg: 'bg-amber-50',
      },
    ],
    [stats]
  );

  return (
    <div className="min-h-full">
      <DashboardHeader title="Dashboard - Bác sĩ lâm sàng" subtitle="Theo dõi hàng đợi phòng khám và ca đang khám" />

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

        <div className="mt-8 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <div className="rounded-lg border bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-950">Ca đang xử lý</h2>
            {currentPatient ? (
              <div className="mt-4 rounded-md border bg-emerald-50 p-4">
                <p className="text-sm font-medium text-emerald-800">Đang khám</p>
                <p className="mt-1 text-xl font-semibold text-slate-950">{currentPatient.patientName}</p>
                <p className="mt-1 text-sm text-slate-600">
                  {currentPatient.patientId} · Số gọi {currentPatient.queueNumber || '--'}
                </p>
              </div>
            ) : (
              <p className="mt-4 rounded-md border bg-slate-50 p-4 text-sm text-slate-600">
                Chưa có bệnh nhân nào ở trạng thái đang khám.
              </p>
            )}
          </div>

          <div className="rounded-lg border bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-950">Thao tác nhanh</h2>
            <div className="mt-4 grid gap-3">
              <Link
                href="/dashboard/clinical-doctor/patients"
                className="inline-flex items-center justify-center gap-2 rounded-md bg-blue-600 px-4 py-3 text-sm font-semibold text-white hover:bg-blue-700"
              >
                <PhoneCall className="h-4 w-4" />
                Mở hàng đợi bệnh nhân
              </Link>
              <Link
                href="/dashboard/doctor/records"
                className="inline-flex items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-4 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                <FileText className="h-4 w-4" />
                Hồ sơ khám
              </Link>
              <p className="text-sm text-slate-500">
                Hàng đợi tự cập nhật mỗi 30 giây và dùng đúng thứ tự ưu tiên của phòng.
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
