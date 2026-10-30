'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { Filter, Loader2, PhoneCall, Search } from 'lucide-react';
import { serviceOrderService, ServiceOrderItem } from '@/services/service-order.service';
import { SERVICE_ORDER_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  PAID: { label: SERVICE_ORDER_STATUS_LABELS.PAID, color: 'text-blue-700' },
  PENDING: { label: SERVICE_ORDER_STATUS_LABELS.PENDING, color: 'text-yellow-700' },
  COMPLETED: { label: SERVICE_ORDER_STATUS_LABELS.COMPLETED, color: 'text-green-700' },
  CANCELLED: { label: SERVICE_ORDER_STATUS_LABELS.CANCELLED, color: 'text-red-600' },
};

export default function SpecialistDoctorPatientListPage() {
  const [orders, setOrders] = useState<ServiceOrderItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('PAID');

  const fetchOrders = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
    }
    try {
      const data = await serviceOrderService.getMyQueue(page, 10, statusFilter || undefined);
      setOrders(data.content ?? []);
      setTotalPages(data.totalPages ?? 1);
    } catch (error) {
      console.error('Error fetching service orders:', error);
      setOrders([]);
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }, [page, statusFilter]);

  useEffect(() => {
    fetchOrders();
    const intervalId = window.setInterval(() => {
      fetchOrders(true);
    }, 30000);

    return () => window.clearInterval(intervalId);
  }, [fetchOrders]);

  const filtered = orders.filter((order) => {
    if (!keyword) return true;
    const q = keyword.toLowerCase();
    return (
      order.patientName?.toLowerCase().includes(q) ||
      order.patientId?.toLowerCase().includes(q) ||
      order.orderId?.toLowerCase().includes(q) ||
      order.serviceName?.toLowerCase().includes(q)
    );
  });

  const buildExaminationHref = (order: ServiceOrderItem) => {
    const routeId = order.appointmentId || order.orderId;
    if (!routeId) {
      return null;
    }

    const params = new URLSearchParams();
    if (order.orderId) {
      params.set('orderId', order.orderId);
    }

    return params.size > 0
      ? `/dashboard/specialist-doctor/examination/${routeId}?${params.toString()}`
      : `/dashboard/specialist-doctor/examination/${routeId}`;
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <DashboardHeader
        title="Hàng đợi cận lâm sàng"
        subtitle="Các chỉ định thuộc phòng của bạn hôm nay"
      />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
          <div className="flex flex-col gap-4 border-b p-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative w-full sm:w-2/3">
              <input
                type="text"
                placeholder="Tìm kiếm theo tên BN, mã BN, mã chỉ định, dịch vụ"
                className="w-full rounded-md border py-2 pl-10 pr-4 text-black focus:border-blue-500 focus:outline-none"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
              <Search className="absolute left-3 top-2.5 h-5 w-5 text-slate-500" />
            </div>

            <div className="relative w-full sm:w-64">
              <select
                className="w-full appearance-none rounded-md border bg-white py-2 pl-4 pr-10 text-black focus:border-blue-500 focus:outline-none"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(1);
                }}
              >
                <option value="PAID">Hàng đợi đang chờ</option>
                <option value="PENDING">Chờ trả kết quả</option>
                <option value="COMPLETED">Đã có kết quả</option>
              </select>
              <Filter className="pointer-events-none absolute right-3 top-2.5 h-5 w-5 text-slate-500" />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full whitespace-nowrap text-left text-sm">
              <thead className="border-b bg-gray-100/50">
                <tr>
                  <th className="px-6 py-3 font-semibold text-black">Vị trí</th>
                  <th className="px-6 py-3 font-semibold text-black">Mã chỉ định</th>
                  <th className="px-6 py-3 font-semibold text-black">Mã BN</th>
                  <th className="px-6 py-3 font-semibold text-black">Tên bệnh nhân</th>
                  <th className="px-6 py-3 font-semibold text-black">Dịch vụ</th>
                  <th className="px-6 py-3 font-semibold text-black">Thời gian</th>
                  <th className="px-6 py-3 font-semibold text-black">Trạng thái</th>
                  <th className="px-6 py-3 font-semibold text-black">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {loading ? (
                  <tr>
                    <td colSpan={8} className="py-10 text-center">
                      <Loader2 className="inline animate-spin text-slate-500" size={24} />
                    </td>
                  </tr>
                ) : filtered.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-slate-600">
                      Không có bệnh nhân nào trong hàng đợi
                    </td>
                  </tr>
                ) : (
                  filtered.map((order, index) => {
                    const status = STATUS_LABELS[order.status] ?? {
                      label: formatVietnameseLabel(order.status, SERVICE_ORDER_STATUS_LABELS),
                      color: 'text-gray-600',
                    };
                    const examinationHref = buildExaminationHref(order);

                    return (
                      <tr key={order.orderId} className="text-black hover:bg-gray-50">
                        <td className="px-6 py-4">{(page - 1) * 10 + index + 1}</td>
                        <td className="px-6 py-4 font-mono text-xs">{order.orderId}</td>
                        <td className="px-6 py-4">{order.patientId || '—'}</td>
                        <td className="px-6 py-4 font-semibold">{order.patientName || '—'}</td>
                        <td className="px-6 py-4">{order.serviceName}</td>
                        <td className="px-6 py-4">
                          {order.createdAt
                            ? new Date(order.createdAt).toLocaleString('vi-VN', {
                                hour: '2-digit',
                                minute: '2-digit',
                                day: '2-digit',
                                month: '2-digit',
                                year: 'numeric',
                              })
                            : '—'}
                        </td>
                        <td className={`px-6 py-4 font-medium ${status.color}`}>{status.label}</td>
                        <td className="px-6 py-4">
                          {examinationHref ? (
                            <Link
                              href={examinationHref}
                              className="inline-flex items-center gap-2 rounded border border-gray-300 px-3 py-1.5 text-sm font-medium hover:bg-gray-50"
                            >
                              <PhoneCall className="h-4 w-4" />
                              Gọi vào
                            </Link>
                          ) : (
                            <span className="text-xs text-slate-500">—</span>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          <div className="flex items-center justify-end border-t p-4 text-sm">
            <div className="flex space-x-1">
              <button
                onClick={() => setPage((currentPage) => Math.max(1, currentPage - 1))}
                disabled={page === 1}
                className="px-3 py-1 text-gray-500 hover:text-black disabled:opacity-50"
              >
                « Trước
              </button>
              <button className="rounded bg-black px-3 py-1 font-medium text-white">{page}</button>
              <button
                onClick={() => setPage((currentPage) => Math.min(totalPages, currentPage + 1))}
                disabled={page >= totalPages}
                className="px-3 py-1 text-gray-500 hover:text-black disabled:opacity-50"
              >
                Sau »
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
