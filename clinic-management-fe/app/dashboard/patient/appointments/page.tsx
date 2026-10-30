'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { appointmentService, AppointmentItem } from '@/services/appointment.service';
import { Loader2, Search, Filter, RefreshCw } from 'lucide-react';
import { APPOINTMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const AUTO_REFRESH_MS = 10000;

/* ─── Status helpers ─────────────────────────────────────── */
const STATUS_LABEL_VI: Record<string, string> = {
  REQUESTED: 'CHỜ XÁC NHẬN',
  CHECKED_IN: 'ĐÃ CHECK-IN',
  WAITING:   'SẮP TỚI',
  CONFIRMED: 'SẮP TỚI',
  IN_PROGRESS: 'ĐANG KHÁM',
  WAITING_RESULTS: 'CHỜ KẾT QUẢ CLS',
  COMPLETED: 'HOÀN THÀNH',
  CANCELLED: 'ĐÃ HỦY',
};

const STATUS_STYLE: Record<string, string> = {
  REQUESTED: 'border border-dashed border-purple-400 text-purple-700 bg-purple-50',
  CHECKED_IN: 'border border-amber-300 text-amber-700 bg-amber-50',
  WAITING:   'border border-dashed border-gray-600 text-gray-700',
  CONFIRMED: 'border border-dashed border-gray-600 text-gray-700',
  IN_PROGRESS: 'border border-blue-300 text-blue-700 bg-blue-50',
  WAITING_RESULTS: 'border border-amber-300 text-amber-700 bg-amber-50',
  COMPLETED: 'border border-gray-800 text-gray-800 bg-gray-100',
  CANCELLED: 'border border-red-300 text-red-500 bg-red-50',
};

const FILTER_OPTIONS = [
  { label: 'Tất cả',     value: '' },
  { label: 'Chờ xác nhận', value: 'REQUESTED' },
  { label: 'Sắp tới',   value: 'WAITING' },
  { label: 'Đang khám', value: 'IN_PROGRESS' },
  { label: 'Hoàn thành', value: 'COMPLETED' },
  { label: 'Đã hủy',    value: 'CANCELLED' },
];

function formatAppointmentDate(value?: string | null) {
  if (!value) return '—';
  const datePart = value.split('T')[0];
  const [year, month, day] = datePart.split('-');
  if (!year || !month || !day) return value;
  return `${Number(day)}/${Number(month)}/${year}`;
}

function isCancellable(appt: AppointmentItem): boolean {
  if (!['REQUESTED', 'CONFIRMED', 'CHECKED_IN', 'WAITING'].includes(appt.status)) return false;
  return (appt.paymentStatus ?? '').toUpperCase() !== 'PAID';
}

/* ─── Component ──────────────────────────────────────────── */
export default function PatientAppointmentsPage() {
  const router = useRouter();

  const [allItems, setAllItems]         = useState<AppointmentItem[]>([]);
  const [loading, setLoading]           = useState(true);
  const [search, setSearch]             = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage]                 = useState(1);
  const [pageSize, setPageSize]         = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages]     = useState(1);
  const [syncing, setSyncing]           = useState(false);
  const [syncError, setSyncError]       = useState(false);

  // Cancel dialog state
  const [cancelTarget, setCancelTarget]   = useState<AppointmentItem | null>(null);
  const [cancelReason, setCancelReason]   = useState('');
  const [cancelling, setCancelling]       = useState(false);
  const [cancelError, setCancelError]     = useState('');
  const [cancelSuccess, setCancelSuccess] = useState(false);

  /* ─── Fetch ─── */
  const fetchAppointments = useCallback((quiet = false) => {
    if (!quiet) setLoading(true);
    else setSyncing(true);
    setSyncError(false);

    appointmentService
      .getMyAppointments(page, pageSize, statusFilter || undefined)
      .then((res) => {
        setAllItems(res.content ?? []);
        setTotalElements(res.totalElements ?? 0);
        setTotalPages(res.totalPages ?? 1);
      })
      .catch(() => {
        if (quiet) setSyncError(true);
      })
      .finally(() => {
        setLoading(false);
        setSyncing(false);
      });
  }, [page, pageSize, statusFilter]);

  useEffect(() => {
    fetchAppointments(false);
  }, [fetchAppointments, cancelSuccess]);

  useEffect(() => {
    const id = setInterval(() => fetchAppointments(true), AUTO_REFRESH_MS);
    return () => clearInterval(id);
  }, [fetchAppointments]);

  /* ─── Filter/pageSize handlers (reset page together) ─── */
  const handleStatusFilterChange = (val: string) => {
    setStatusFilter(val);
    setPage(1);
    setLoading(true);
  };

  const handlePageSizeChange = (val: number) => {
    setPageSize(val);
    setPage(1);
    setLoading(true);
  };

  /* ─── Client-side keyword search ─── */
  const visibleItems = search.trim()
    ? allItems.filter((a) => {
        const q = search.toLowerCase();
        return (
          (a.appointmentDate ?? '').includes(q) ||
          (a.doctorName ?? '').toLowerCase().includes(q) ||
          (a.appointmentTime ?? '').toLowerCase().includes(q)
        );
      })
    : allItems;

  /* ─── Cancel flow ─── */
  const openCancel = (appt: AppointmentItem) => {
    setCancelTarget(appt);
    setCancelReason('');
    setCancelError('');
  };

  const confirmCancel = () => {
    if (!cancelTarget) return;
    if (!cancelReason.trim()) {
      setCancelError('Vui lòng nhập lý do hủy lịch.');
      return;
    }
    setCancelling(true);
    appointmentService
      .cancelMyAppointment(cancelTarget.appointmentId, cancelReason.trim())
      .then(() => {
        setCancelTarget(null);
        setLoading(true);
        setCancelSuccess((v) => !v); // toggle to trigger re-fetch
      })
      .catch((err: unknown) => {
        const message = (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
          || (err as { message?: string })?.message
          || 'Hủy lịch thất bại. Vui lòng thử lại.';
        setCancelError(
          message
        );
      })
      .finally(() => setCancelling(false));
  };

  /* ─── Pagination helper ─── */
  const getPageNumbers = (): (number | '...')[] => {
    if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i + 1);
    const pages: (number | '...')[] = [1];
    if (page > 3) pages.push('...');
    for (let i = Math.max(2, page - 1); i <= Math.min(totalPages - 1, page + 1); i++) pages.push(i);
    if (page < totalPages - 2) pages.push('...');
    pages.push(totalPages);
    return pages;
  };

  return (
    <div className="min-h-full bg-gray-50 p-8 text-black">
      <h1 className="text-2xl font-bold text-[#1a2b3c] mb-6">Danh sách Lịch khám</h1>

      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3 mb-3">
        {/* Search */}
        <div className="relative flex-1 min-w-[220px] max-w-sm">
          <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Tìm theo ngày, tên bác sĩ..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-8 pr-3 py-2 text-sm border border-gray-300 rounded focus:outline-none focus:border-gray-500"
          />
        </div>

        {/* Status filter */}
        <div className="relative">
          <Filter size={13} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <select
            value={statusFilter}
            onChange={(e) => handleStatusFilterChange(e.target.value)}
            className="pl-7 pr-8 py-2 text-sm border border-gray-300 rounded appearance-none focus:outline-none focus:border-gray-500 bg-white"
          >
            {FILTER_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </div>

        <div className="flex-1" />

        {/* New appointment — patient self-service; goes to dedicated booking page */}
        <button
          onClick={() => router.push('/dashboard/patient/appointments/new')}
          className="px-4 py-2 text-sm font-bold bg-black text-white rounded hover:bg-gray-800 transition-colors whitespace-nowrap"
        >
          + ĐẶT LỊCH KHÁM MỚI
        </button>
      </div>

      <div className="flex justify-end mb-3 min-h-5">
        {syncing ? (
          <span className="flex items-center gap-1 text-xs text-slate-500">
            <RefreshCw size={12} className="animate-spin" /> Đang đồng bộ lịch khám...
          </span>
        ) : syncError ? (
          <span className="text-xs text-red-400">Không thể tự đồng bộ, vui lòng tải lại trang.</span>
        ) : (
          <span className="text-xs text-slate-500">Danh sách tự cập nhật mỗi 10s</span>
        )}
      </div>

      {/* Business rule note */}
      <p className="text-xs text-gray-500 mb-4 italic">
        * Lưu ý: Không thể hủy lịch đã thanh toán, đang khám hoặc đã hoàn thành.
      </p>

      {/* Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-4 py-3 text-left font-semibold text-gray-700 w-12">STT</th>
              <th className="px-4 py-3 text-left font-semibold text-gray-700 w-44">Ngày giờ</th>
              <th className="px-4 py-3 text-left font-semibold text-gray-700">Họ và tên</th>
              <th className="px-4 py-3 text-left font-semibold text-gray-700">Lý do khám</th>
              <th className="px-4 py-3 text-left font-semibold text-gray-700 w-32">Trạng thái</th>
              <th className="px-4 py-3 text-left font-semibold text-gray-700 w-40">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="py-16 text-center text-slate-600">
                  <div className="flex justify-center items-center gap-2">
                    <Loader2 size={18} className="animate-spin" /> Đang tải...
                  </div>
                </td>
              </tr>
            ) : visibleItems.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-16 text-center text-slate-600">
                  Không tìm thấy lịch hẹn nào.
                </td>
              </tr>
            ) : (
              visibleItems.map((appt, idx) => {
                const cancellable = isCancellable(appt);
                return (
                  <tr key={appt.appointmentId} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-gray-500">{(page - 1) * pageSize + idx + 1}</td>
                    <td className="px-4 py-3">
                      <div className="font-medium">{appt.appointmentTime ?? '—'}</div>
                      <div className="text-xs text-slate-500">
                        {appt.appointmentDate
                          ? formatAppointmentDate(appt.appointmentDate)
                          : '—'}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="font-medium">{appt.patientName ?? '—'}</span>
                      {appt.bookedByPatientId && appt.bookedByPatientId !== appt.patientId ? (
                        <span className="ml-1.5 text-xs font-semibold text-blue-600 bg-blue-50 border border-blue-200 px-1.5 py-0.5 rounded">
                          Đặt hộ
                        </span>
                      ) : (
                        <span className="ml-1 text-xs text-slate-500">(Tôi)</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-gray-600 max-w-xs truncate">
                      {appt.notes
                        ? appt.notes.length > 60 ? appt.notes.slice(0, 60) + '…' : appt.notes
                        : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-block text-xs font-bold px-2 py-0.5 rounded ${STATUS_STYLE[appt.status] ?? 'border border-gray-300 text-gray-500'}`}>
                        {STATUS_LABEL_VI[appt.status] ?? formatVietnameseLabel(appt.status, APPOINTMENT_STATUS_LABELS).toLocaleUpperCase('vi-VN')}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-col gap-1.5">
                        <button
                          onClick={() => router.push(`/dashboard/patient/appointments/${appt.appointmentId}`)}
                          className="px-3 py-1 text-xs font-bold border-2 border-gray-800 text-gray-800 rounded hover:bg-gray-800 hover:text-white transition-colors"
                        >
                          TRUY CẬP
                        </button>
                        {cancellable && (
                          <button
                            onClick={() => openCancel(appt)}
                            className="px-3 py-1 text-xs font-bold border border-red-300 text-red-600 rounded hover:bg-red-50 transition-colors"
                          >
                            HỦY KHÁM
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination footer */}
      {!loading && totalElements > 0 && (
        <div className="flex flex-wrap items-center justify-between mt-4 gap-3 text-sm">
          <div className="flex items-center gap-2 text-gray-500">
            <span>Hiển thị</span>
            <input
              type="number"
              min={1}
              max={100}
              value={pageSize}
              onChange={(e) => {
                const v = parseInt(e.target.value);
                if (v > 0) handlePageSizeChange(v);
              }}
              className="w-16 px-2 py-1 border border-gray-300 rounded text-center text-sm focus:outline-none"
            />
            <span>/ {totalElements} kết quả</span>
          </div>

          <div className="flex items-center gap-1">
            <PagBtn label="«" onClick={() => setPage(1)}               disabled={page === 1} />
            <PagBtn label="‹" onClick={() => setPage((p) => p - 1)}   disabled={page === 1} />
            {getPageNumbers().map((p, i) =>
              p === '...' ? (
                <span key={`e${i}`} className="select-none px-2 text-slate-500">…</span>
              ) : (
                <PagBtn key={p} label={String(p)} onClick={() => setPage(p as number)}
                  disabled={false} active={page === p} />
              )
            )}
            <PagBtn label="›" onClick={() => setPage((p) => p + 1)}          disabled={page === totalPages} />
            <PagBtn label="»" onClick={() => setPage(totalPages)}              disabled={page === totalPages} />
          </div>
        </div>
      )}

      {/* Cancel confirmation dialog */}
      {cancelTarget && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
            <h2 className="text-base font-bold text-gray-900 mb-2">Xác nhận hủy khám</h2>
            <p className="text-sm text-gray-500 mb-4">
              Bạn có chắc muốn hủy lịch khám ngày{' '}
              <span className="font-semibold text-gray-800">
                {cancelTarget.appointmentDate
                  ? formatAppointmentDate(cancelTarget.appointmentDate)
                  : '—'}
              </span>
              ? Hành động này không thể hoàn tác.
            </p>
            <div className="mb-3">
              <label className="mb-1 block text-sm font-semibold text-gray-700">
                Lý do hủy <span className="text-red-500">*</span>
              </label>
              <textarea
                value={cancelReason}
                onChange={(event) => {
                  setCancelReason(event.target.value);
                  setCancelError('');
                }}
                maxLength={500}
                rows={3}
                disabled={cancelling}
                className="w-full resize-none rounded border border-gray-300 px-3 py-2 text-sm text-gray-800 focus:outline-none focus:border-gray-500 disabled:opacity-60"
                placeholder="Nhập lý do hủy lịch..."
              />
            </div>
            {cancelError && <p className="text-xs text-red-500 mb-3">{cancelError}</p>}
            <div className="flex justify-end gap-2">
              <button
                onClick={() => {
                  setCancelTarget(null);
                  setCancelReason('');
                }}
                disabled={cancelling}
                className="px-4 py-2 text-sm border border-gray-300 rounded hover:bg-gray-50 transition-colors"
              >
                Quay lại
              </button>
              <button
                onClick={confirmCancel}
                disabled={cancelling || !cancelReason.trim()}
                className="px-4 py-2 text-sm font-bold bg-red-600 text-white rounded hover:bg-red-700 transition-colors flex items-center gap-2 disabled:opacity-60"
              >
                {cancelling && <Loader2 size={14} className="animate-spin" />}
                Xác nhận hủy
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* ─── Pagination button ─── */
function PagBtn({
  label, onClick, disabled, active,
}: {
  label: string; onClick: () => void; disabled: boolean; active?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`min-w-[32px] h-8 px-2 text-sm rounded border transition-colors ${
        active
          ? 'bg-black text-white border-black font-bold'
          : disabled
          ? 'border-gray-200 text-gray-300 cursor-not-allowed'
          : 'border-gray-300 text-gray-600 hover:bg-gray-100'
      }`}
    >
      {label}
    </button>
  );
}
