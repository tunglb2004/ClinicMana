'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { Banknote, FileClock, HandCoins, Loader2, ReceiptText, WalletCards } from 'lucide-react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { financialService } from '@/services/financial.service';

type CashierStats = {
  pendingInitial: number;
  pendingSupplement: number;
  paidToday: number;
  revenueToday: number;
};

const initialStats: CashierStats = {
  pendingInitial: 0,
  pendingSupplement: 0,
  paidToday: 0,
  revenueToday: 0,
};

const formatNumber = (value: number) => new Intl.NumberFormat('vi-VN').format(value);

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);

export default function CashierDashboard() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<CashierStats>(initialStats);

  useEffect(() => {
    const fetchDashboard = async () => {
      setLoading(true);
      try {
        const dashboard = await financialService.getCashierDashboard();
        setStats({
          pendingInitial: Number(dashboard.pendingInitial || 0),
          pendingSupplement: Number(dashboard.pendingSupplement || 0),
          paidToday: Number(dashboard.paidToday || 0),
          revenueToday: Number(dashboard.revenueToday || 0),
        });
      } catch (error) {
        console.error('Failed to fetch cashier dashboard:', error);
        setStats(initialStats);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
  }, []);

  const cards = useMemo(
    () => [
      {
        label: 'Hồ sơ chờ thanh toán',
        value: formatNumber(stats.pendingInitial),
        helper: 'Dịch vụ ban đầu chưa thu',
        icon: FileClock,
        color: 'text-amber-700',
        bg: 'bg-amber-50',
      },
      {
        label: 'Chỉ định bổ sung',
        value: formatNumber(stats.pendingSupplement),
        helper: 'Cận lâm sàng chờ thu phí',
        icon: WalletCards,
        color: 'text-blue-700',
        bg: 'bg-blue-50',
      },
      {
        label: 'Hóa đơn hôm nay',
        value: formatNumber(stats.paidToday),
        helper: `Doanh thu hôm nay ${formatCurrency(stats.revenueToday)}`,
        icon: ReceiptText,
        color: 'text-emerald-700',
        bg: 'bg-emerald-50',
      },
      {
        label: 'Doanh thu hôm nay',
        value: formatCurrency(stats.revenueToday),
        helper: 'Từ hóa đơn đã thanh toán của bạn',
        icon: Banknote,
        color: 'text-indigo-700',
        bg: 'bg-indigo-50',
      },
    ],
    [stats]
  );

  return (
    <div className="min-h-full">
      <DashboardHeader title="Dashboard - Thu ngân" subtitle="Thanh toán viện phí, hóa đơn và chỉ định bổ sung" />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          {cards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.label} className="rounded-lg border bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-medium text-slate-600">{card.label}</p>
                    <p className={`mt-2 text-2xl font-bold ${card.color}`}>
                      {loading ? <Loader2 className="h-7 w-7 animate-spin text-slate-400" /> : card.value}
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
              Mở thẳng danh sách cần xử lý thay vì chỉ xem số liệu tổng quan.
            </p>
          </div>

          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <Link href="/dashboard/cashier/invoices" className="rounded-md border p-4 hover:bg-gray-50">
              <ReceiptText className="mb-3 h-5 w-5 text-emerald-700" />
              <p className="font-semibold text-slate-950">Lịch sử hóa đơn</p>
              <p className="mt-1 text-sm text-slate-600">Tra cứu, in lại phiếu thu và hóa đơn điện tử.</p>
            </Link>
            <Link href="/dashboard/cashier/handover" className="rounded-md border p-4 hover:bg-gray-50">
              <HandCoins className="mb-3 h-5 w-5 text-blue-700" />
              <p className="font-semibold text-slate-950">Bàn giao tiền mặt</p>
              <p className="mt-1 text-sm text-slate-600">Tổng kết doanh thu ngày và xác nhận bàn giao giữa các ca.</p>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
