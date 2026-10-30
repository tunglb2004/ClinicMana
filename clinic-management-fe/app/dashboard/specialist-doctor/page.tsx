'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { ClipboardList, FileCheck2, FlaskConical, Loader2, UploadCloud } from 'lucide-react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { serviceOrderService, ServiceOrderItem } from '@/services/service-order.service';

type SpecialistStats = {
  waiting: number;
  pendingResult: number;
  completedToday: number;
  totalQueue: number;
};

const initialStats: SpecialistStats = {
  waiting: 0,
  pendingResult: 0,
  completedToday: 0,
  totalQueue: 0,
};

const todayKey = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const isToday = (order: ServiceOrderItem) => Boolean(order.createdAt?.startsWith(todayKey()));

const formatNumber = (value: number) => new Intl.NumberFormat('vi-VN').format(value);

export default function SpecialistDoctorDashboard() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<SpecialistStats>(initialStats);

  const fetchDashboard = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const [queueResponse, completedResponse] = await Promise.all([
        serviceOrderService.getMyQueue(1, 100),
        serviceOrderService.getMyQueue(1, 100, 'COMPLETED'),
      ]);

      const queue = queueResponse.content || [];
      const completedToday = (completedResponse.content || []).filter(isToday).length;

      setStats({
        waiting: queue.filter((order) => order.status === 'PAID').length,
        pendingResult: queue.filter((order) => order.status === 'PENDING').length,
        completedToday,
        totalQueue: queue.length,
      });
    } catch (error) {
      console.error('Failed to fetch specialist dashboard:', error);
      setStats(initialStats);
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
        label: 'Chờ gọi vào phòng',
        value: stats.waiting,
        icon: ClipboardList,
        color: 'text-blue-700',
        bg: 'bg-blue-50',
      },
      {
        label: 'Chờ trả kết quả',
        value: stats.pendingResult,
        icon: UploadCloud,
        color: 'text-amber-700',
        bg: 'bg-amber-50',
      },
      {
        label: 'Hoàn thành hôm nay',
        value: stats.completedToday,
        icon: FileCheck2,
        color: 'text-emerald-700',
        bg: 'bg-emerald-50',
      },
      {
        label: 'Tổng chỉ định',
        value: stats.totalQueue,
        icon: FlaskConical,
        color: 'text-indigo-700',
        bg: 'bg-indigo-50',
      },
    ],
    [stats]
  );

  return (
    <div className="min-h-full">
      <DashboardHeader
        title="Dashboard - Bác sĩ cận lâm sàng"
        subtitle="Theo dõi hàng đợi chỉ định thuộc phòng đang làm"
      />

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
          <div className="grid gap-4 md:grid-cols-[1fr_auto] md:items-center">
            <div>
              <h2 className="text-lg font-semibold text-slate-950">Hàng đợi phòng</h2>
              <p className="mt-1 text-sm text-slate-500">
                Danh sách được cập nhật mỗi 30 giây theo API hàng đợi của bác sĩ/phòng cận lâm sàng.
              </p>
            </div>
            <Link
              href="/dashboard/specialist-doctor/patients"
              className="inline-flex items-center justify-center gap-2 rounded-md bg-blue-600 px-4 py-3 text-sm font-semibold text-white hover:bg-blue-700"
            >
              <FlaskConical className="h-4 w-4" />
              Mở hàng đợi cận lâm sàng
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
