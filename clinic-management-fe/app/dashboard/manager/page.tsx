'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useAuthStore } from '@/store/auth.store';
import { managementService, type ManagerRevenueDay } from '@/services/management.service';
import { AppointmentReviewItem } from '@/services/review.service';
import { AlertTriangle, BarChart3, CalendarDays, Receipt, Star, Users2 } from 'lucide-react';

type DashboardMetrics = {
  totalStaff: number;
  doctorCount: number;
  nurseCount: number;
  activeStaff: number;
  todayAppointments: number;
  waitingAppointments: number;
  completedAppointments: number;
  monthRevenue: number;
  monthPaidInvoices: number;
  averageRating: number;
  pendingReviews: number;
};

const EMPTY_METRICS: DashboardMetrics = {
  totalStaff: 0,
  doctorCount: 0,
  nurseCount: 0,
  activeStaff: 0,
  todayAppointments: 0,
  waitingAppointments: 0,
  completedAppointments: 0,
  monthRevenue: 0,
  monthPaidInvoices: 0,
  averageRating: 0,
  pendingReviews: 0,
};

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0);

const renderStars = (rating: number) =>
  Array.from({ length: 5 }, (_, index) => (
    <Star
      key={`rating-${index}`}
      className={`h-3.5 w-3.5 ${index < rating ? 'fill-amber-400 text-amber-400' : 'text-neutral-300'}`}
    />
  ));

function buildLastSevenDays() {
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - (6 - index));
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    return {
      key,
      label: `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}`,
    };
  });
}

export default function ManagerDashboard() {
  const user = useAuthStore((state) => state.user);
  const [loading, setLoading] = useState(true);
  const [metrics, setMetrics] = useState<DashboardMetrics>(EMPTY_METRICS);
  const [recentReviews, setRecentReviews] = useState<AppointmentReviewItem[]>([]);
  const [revenueByDay, setRevenueByDay] = useState<ManagerRevenueDay[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const fetchDashboard = async () => {
      setLoading(true);
      setError(null);
      try {
        const summary = await managementService.getDashboardSummary();

        if (cancelled) return;

        setMetrics({ ...EMPTY_METRICS, ...summary.metrics });
        setRevenueByDay(summary.revenueByDay || []);
        setRecentReviews(summary.recentReviews || []);
      } catch (fetchError) {
        console.error('Không thể tải dashboard manager:', fetchError);
        if (!cancelled) {
          setError('Không thể tải đủ dữ liệu dashboard. Vui lòng thử lại sau.');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchDashboard();

    return () => {
      cancelled = true;
    };
  }, []);

  const revenueBars = useMemo(() => {
    const days = revenueByDay.length > 0
      ? revenueByDay
      : buildLastSevenDays().map((day) => ({ date: day.key, label: day.label, value: 0 }));
    const max = Math.max(...days.map((day) => Number(day.value || 0)), 1);
    return days.map((day) => ({
      key: day.date,
      label: day.label,
      value: Number(day.value || 0),
      height: Math.max(8, Math.round((Number(day.value || 0) / max) * 100)),
    }));
  }, [revenueByDay]);

  const summary = [
    {
      label: 'Doanh thu tháng này',
      value: formatCurrency(metrics.monthRevenue),
      sub: `${metrics.monthPaidInvoices} hóa đơn đã thanh toán`,
      icon: Receipt,
    },
    {
      label: 'Lượt khám hôm nay',
      value: metrics.todayAppointments.toString(),
      sub: `Đã xong: ${metrics.completedAppointments} | Đang xử lý: ${metrics.waitingAppointments}`,
      icon: CalendarDays,
    },
    {
      label: 'Đánh giá trung bình',
      value: metrics.averageRating ? metrics.averageRating.toFixed(1) : '0.0',
      sub: `${metrics.pendingReviews} phản hồi chưa trả lời`,
      icon: Star,
    },
    {
      label: 'Nhân sự hoạt động',
      value: `${metrics.activeStaff}/${metrics.totalStaff}`,
      sub: `Bác sĩ: ${metrics.doctorCount} | Y tá: ${metrics.nurseCount}`,
      icon: Users2,
    },
  ];

  const alerts = [
    metrics.pendingReviews > 0 ? `${metrics.pendingReviews} đánh giá chưa được phản hồi` : null,
    metrics.waitingAppointments > 0 ? `${metrics.waitingAppointments} lịch hẹn hôm nay đang chờ xử lý` : null,
    metrics.totalStaff > metrics.activeStaff ? `${metrics.totalStaff - metrics.activeStaff} tài khoản nhân sự đang khóa` : null,
  ].filter(Boolean) as string[];

  return (
    <div className="min-h-[calc(100vh-4rem)] p-6 text-slate-950">
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="flex flex-col justify-between gap-3 md:flex-row md:items-end">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Tổng quan quản lý</h1>
            <p className="mt-1 text-sm text-slate-600">
              Dữ liệu vận hành thật từ lịch hẹn, hóa đơn, nhân sự và đánh giá.
            </p>
          </div>
          <div className="rounded-md border bg-white px-3 py-2 text-sm text-slate-700">
            Giám đốc: <span className="font-semibold">{user?.fullName || '—'}</span>
          </div>
        </div>

        {error && (
          <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-4">
          {summary.map((item) => {
            const Icon = item.icon;
            return (
              <div key={item.label} className="rounded-lg border bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{item.label}</p>
                    <p className="mt-2 text-3xl font-bold text-slate-900">{loading ? '...' : item.value}</p>
                    <p className="mt-1 text-xs text-slate-600">{item.sub}</p>
                  </div>
                  <div className="rounded-md bg-slate-100 p-2">
                    <Icon className="h-5 w-5 text-slate-700" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-[2fr_1fr]">
          <div className="rounded-lg border bg-white shadow-sm">
            <div className="flex items-center justify-between border-b px-4 py-3">
              <span className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                <BarChart3 className="h-4 w-4" />
                Doanh thu 7 ngày gần nhất
              </span>
              <Link href="/dashboard/manager/finance" className="text-xs font-semibold text-blue-600 hover:underline">
                Xem tài chính
              </Link>
            </div>
            <div className="h-72 p-4">
              <div className="flex h-full items-end justify-between gap-3 border-b border-l px-4 py-3">
                {revenueBars.map((bar) => (
                  <div key={bar.key} className="flex h-full flex-1 flex-col items-center justify-end gap-2">
                    <div className="w-full max-w-10 rounded-t bg-blue-600" style={{ height: `${bar.height}%` }} />
                    <span className="text-[11px] font-medium text-slate-600">{bar.label}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="rounded-lg border bg-white shadow-sm">
            <div className="flex items-center gap-2 border-b px-4 py-3 text-sm font-semibold text-slate-900">
              <AlertTriangle className="h-4 w-4" />
              Việc cần chú ý
            </div>
            <div className="space-y-3 p-4">
              {loading ? (
                <p className="text-sm text-slate-500">Đang tải...</p>
              ) : alerts.length === 0 ? (
                <p className="text-sm text-slate-600">Không có cảnh báo vận hành nổi bật.</p>
              ) : (
                alerts.map((alert) => (
                  <div key={alert} className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
                    {alert}
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-white shadow-sm">
          <div className="flex items-center justify-between border-b px-4 py-3">
            <span className="flex items-center gap-2 text-sm font-semibold text-slate-900">
              <Users2 className="h-4 w-4" />
              Phản hồi khách hàng mới nhất
            </span>
            <Link href="/dashboard/manager/feedback" className="text-xs font-semibold text-blue-600 hover:underline">
              Xem tất cả
            </Link>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-4 py-3">Khách hàng</th>
                  <th className="px-4 py-3">Dịch vụ</th>
                  <th className="px-4 py-3">Đánh giá</th>
                  <th className="px-4 py-3">Nội dung</th>
                  <th className="px-4 py-3">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {recentReviews.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-6 text-center text-slate-500">
                      Chưa có đánh giá.
                    </td>
                  </tr>
                ) : (
                  recentReviews.map((item) => (
                    <tr key={item.reviewId} className="align-top">
                      <td className="px-4 py-3 font-medium">{item.patientName || '—'}</td>
                      <td className="px-4 py-3">{item.serviceName || item.doctorName || '—'}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-0.5">{renderStars(item.rating || 0)}</div>
                      </td>
                      <td className="max-w-[360px] px-4 py-3 text-slate-700">{item.comment || 'Không có nội dung'}</td>
                      <td className="px-4 py-3">
                        <span className={`rounded-full px-2 py-1 text-xs font-semibold ${item.replyContent ? 'bg-green-50 text-green-700' : 'bg-amber-50 text-amber-700'}`}>
                          {item.replyContent ? 'Đã trả lời' : 'Chưa trả lời'}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
