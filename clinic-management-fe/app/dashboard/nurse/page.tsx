'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { ClipboardList, FileUp, HeartPulse, Loader2, PhoneCall } from 'lucide-react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { appointmentService, AppointmentItem } from '@/services/appointment.service';
import { serviceOrderService, ServiceOrderItem } from '@/services/service-order.service';

type NurseStats = {
  clinicalWaiting: number;
  clinicalInProgress: number;
  paraclinicalWaiting: number;
  pendingResult: number;
  completedToday: number;
};

const initialStats: NurseStats = {
  clinicalWaiting: 0,
  clinicalInProgress: 0,
  paraclinicalWaiting: 0,
  pendingResult: 0,
  completedToday: 0,
};

const todayKey = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const isTodayAppointment = (appointment: AppointmentItem) => appointment.appointmentDate?.split('T')[0] === todayKey();

const isTodayOrder = (order: ServiceOrderItem) => Boolean(order.createdAt?.startsWith(todayKey()));

const formatNumber = (value: number) => new Intl.NumberFormat('vi-VN').format(value);

export default function NurseDashboard() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<NurseStats>(initialStats);

  const fetchDashboard = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    const [clinicalResult, clinicalDoneResult, paraResult, paraDoneResult] = await Promise.allSettled([
      appointmentService.getNurseClinicalPatients(1, 100),
      appointmentService.getNurseClinicalPatients(1, 100, 'COMPLETED'),
      serviceOrderService.getMyQueue(1, 100),
      serviceOrderService.getMyQueue(1, 100, 'COMPLETED'),
    ]);

    const clinical = clinicalResult.status === 'fulfilled' ? clinicalResult.value.content || [] : [];
    const clinicalDone =
      clinicalDoneResult.status === 'fulfilled' ? clinicalDoneResult.value.content || [] : [];
    const para = paraResult.status === 'fulfilled' ? paraResult.value.content || [] : [];
    const paraDone = paraDoneResult.status === 'fulfilled' ? paraDoneResult.value.content || [] : [];

    setStats({
      clinicalWaiting: clinical.filter((appointment) => appointment.status === 'WAITING').length,
      clinicalInProgress: clinical.filter((appointment) => appointment.status === 'IN_PROGRESS').length,
      paraclinicalWaiting: para.filter((order) => order.status === 'PAID').length,
      pendingResult: para.filter((order) => order.status === 'PENDING').length,
      completedToday:
        clinicalDone.filter(isTodayAppointment).length + paraDone.filter(isTodayOrder).length,
    });

    if (!silent) setLoading(false);
  }, []);

  useEffect(() => {
    const initialLoadId = window.setTimeout(() => {
      void fetchDashboard();
    }, 0);
    const intervalId = window.setInterval(() => {
      void fetchDashboard(true);
    }, 30000);
    return () => {
      window.clearTimeout(initialLoadId);
      window.clearInterval(intervalId);
    };
  }, [fetchDashboard]);

  const cards = useMemo(
    () => [
      {
        label: 'Lâm sàng chờ gọi',
        value: stats.clinicalWaiting,
        helper: `${formatNumber(stats.clinicalInProgress)} bệnh nhân đang trong phòng`,
        icon: PhoneCall,
        color: 'text-blue-700',
        bg: 'bg-blue-50',
      },
      {
        label: 'Cận lâm sàng chờ gọi',
        value: stats.paraclinicalWaiting,
        helper: 'Các chỉ định đã thanh toán',
        icon: ClipboardList,
        color: 'text-indigo-700',
        bg: 'bg-indigo-50',
      },
      {
        label: 'Chờ nhập kết quả',
        value: stats.pendingResult,
        helper: 'Tải file hoặc nhập kết quả hộ bác sĩ',
        icon: FileUp,
        color: 'text-amber-700',
        bg: 'bg-amber-50',
      },
      {
        label: 'Hoàn thành hôm nay',
        value: stats.completedToday,
        helper: 'Từ lâm sàng và cận lâm sàng',
        icon: HeartPulse,
        color: 'text-emerald-700',
        bg: 'bg-emerald-50',
      },
    ],
    [stats]
  );

  return (
    <div className="min-h-full">
      <DashboardHeader
        title="Dashboard - Y tá"
        subtitle="Hỗ trợ lâm sàng, gọi bệnh nhân và cập nhật kết quả cận lâm sàng theo phòng trực"
      />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          {cards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.label} className="rounded-lg border bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-medium text-slate-600">{card.label}</p>
                    <p className={`mt-2 text-3xl font-bold ${card.color}`}>
                      {loading ? <Loader2 className="h-7 w-7 animate-spin text-slate-400" /> : formatNumber(card.value)}
                    </p>
                    <p className="mt-2 text-sm text-slate-500">{card.helper}</p>
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
          <div className="border-b pb-4">
            <h2 className="text-lg font-semibold text-slate-950">Công việc nhanh</h2>
            <p className="mt-1 text-sm text-slate-500">
              Y tá có hai luồng riêng: nhập hộ lâm sàng và upload kết quả cận lâm sàng.
            </p>
          </div>

          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <Link href="/dashboard/nurse/clinical-patients" className="rounded-md border p-4 hover:bg-gray-50">
              <PhoneCall className="mb-3 h-5 w-5 text-blue-700" />
              <p className="font-semibold text-slate-950">Hỗ trợ lâm sàng</p>
              <p className="mt-1 text-sm text-slate-600">
                Gọi bệnh nhân, đo sinh hiệu và nhập triệu chứng sơ bộ.
              </p>
            </Link>
            <Link href="/dashboard/nurse/patients" className="rounded-md border p-4 hover:bg-gray-50">
              <FileUp className="mb-3 h-5 w-5 text-indigo-700" />
              <p className="font-semibold text-slate-950">Cận lâm sàng</p>
              <p className="mt-1 text-sm text-slate-600">
                Xem hàng đợi phòng và upload file kết quả hộ bác sĩ.
              </p>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
