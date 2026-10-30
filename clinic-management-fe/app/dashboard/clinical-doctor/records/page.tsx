'use client';

import { useCallback, useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { Calendar, Eye, FileText, Loader2, Search, X } from 'lucide-react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { formatAppointmentDate } from '@/lib/date';
import { APPOINTMENT_STATUS_LABELS, VISIT_TYPE_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';
import { healthRecordService } from '@/services/health-record.service';
import { DoctorMedicalRecordItem } from '@/types/health-record.types';

const STATUS_BADGE: Record<string, string> = {
  COMPLETED: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  FINISHED: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  CANCELLED: 'border-red-200 bg-red-50 text-red-700',
};

const PAGE_SIZE = 10;

export default function ClinicalDoctorRecordsPage() {
  const router = useRouter();
  const pathname = usePathname();
  const basePath = (pathname || '/dashboard/doctor/records').replace(/\/$/, '');
  const [records, setRecords] = useState<DoctorMedicalRecordItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [fromDateInput, setFromDateInput] = useState('');
  const [toDateInput, setToDateInput] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const fetchRecords = useCallback(async () => {
    setLoading(true);
    try {
      const data = await healthRecordService.getDoctorExaminedRecords(page, PAGE_SIZE, keyword, fromDate, toDate);
      setRecords(data.content ?? []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error('Failed to fetch doctor medical records:', error);
      setRecords([]);
      setTotalPages(1);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  }, [page, keyword, fromDate, toDate]);

  useEffect(() => {
    fetchRecords();
  }, [fetchRecords]);

  const handleSearch = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(0);
    setKeyword(searchInput.trim());
    setFromDate(fromDateInput);
    setToDate(toDateInput);
  };

  const handleClearFilters = () => {
    setSearchInput('');
    setKeyword('');
    setFromDateInput('');
    setToDateInput('');
    setFromDate('');
    setToDate('');
    setPage(0);
  };

  const openRecordDetail = (recordId: string) => {
    router.push(`${basePath}/${recordId}`);
  };

  const handleRecordKeyDown = (event: React.KeyboardEvent<HTMLTableRowElement>, recordId: string) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      openRecordDetail(recordId);
    }
  };

  const firstItem = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const lastItem = Math.min((page + 1) * PAGE_SIZE, totalElements);
  const hasFilterValue = Boolean(keyword || searchInput || fromDate || toDate || fromDateInput || toDateInput);

  return (
    <div className="min-h-screen bg-gray-50">
      <DashboardHeader
        title="Hồ sơ khám"
        subtitle="Xem lại các hồ sơ bệnh nhân bạn đã khám và hoàn tất"
      />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <section className="rounded-lg border bg-white shadow-sm">
          <div className="flex flex-col gap-4 border-b p-4 xl:flex-row xl:items-center xl:justify-between">
            <div>
              <h2 className="text-lg font-semibold text-slate-950">Danh sách hồ sơ đã khám</h2>
              <p className="mt-1 text-sm text-slate-500">
                Hiển thị các hồ sơ ở trạng thái hoàn thành hoặc đã kết thúc.
              </p>
            </div>

            <form
              onSubmit={handleSearch}
              className="grid w-full gap-2 sm:grid-cols-2 lg:w-auto lg:grid-cols-[24rem_10rem_10rem_auto_auto]"
            >
              <div className="relative sm:col-span-2 lg:col-span-1 lg:self-end">
                <Search className="pointer-events-none absolute left-3 top-2.5 h-5 w-5 text-slate-400" />
                <input
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  placeholder="Tìm theo tên, mã BN, mã hồ sơ, chẩn đoán"
                  className="w-full rounded-md border py-2 pl-10 pr-4 text-sm text-slate-950 focus:border-blue-500 focus:outline-none"
                />
              </div>
              <label className="flex flex-col gap-1 text-xs font-medium text-slate-600">
                T&#7915; ng&#224;y
                <input
                  type="date"
                  value={fromDateInput}
                  max={toDateInput || undefined}
                  onChange={(event) => setFromDateInput(event.target.value)}
                  className="h-10 rounded-md border px-3 text-sm text-slate-950 focus:border-blue-500 focus:outline-none"
                />
              </label>
              <label className="flex flex-col gap-1 text-xs font-medium text-slate-600">
                &#272;&#7871;n ng&#224;y
                <input
                  type="date"
                  value={toDateInput}
                  min={fromDateInput || undefined}
                  onChange={(event) => setToDateInput(event.target.value)}
                  className="h-10 rounded-md border px-3 text-sm text-slate-950 focus:border-blue-500 focus:outline-none"
                />
              </label>
              <button
                type="submit"
                className="inline-flex h-10 items-center justify-center gap-2 self-end rounded-md bg-blue-600 px-4 text-sm font-semibold text-white hover:bg-blue-700"
              >
                <Calendar className="h-4 w-4" />
                Tìm
              </button>
              {hasFilterValue && (
                <button
                  type="button"
                  onClick={handleClearFilters}
                  disabled={loading}
                  className="inline-flex h-10 items-center justify-center gap-2 self-end rounded-md border border-slate-300 px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <X className="h-4 w-4" />
                  X&oacute;a
                </button>
              )}
            </form>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full whitespace-nowrap text-left text-sm">
              <thead className="border-b bg-gray-100/60 text-slate-700">
                <tr>
                  <th className="px-6 py-3 font-semibold">STT</th>
                  <th className="px-6 py-3 font-semibold">Mã hồ sơ</th>
                  <th className="px-6 py-3 font-semibold">Bệnh nhân</th>
                  <th className="px-6 py-3 font-semibold">Ngày khám</th>
                  <th className="px-6 py-3 font-semibold">Dịch vụ</th>
                  <th className="px-6 py-3 font-semibold">Chẩn đoán</th>
                  <th className="px-6 py-3 font-semibold">Trạng thái</th>
                  <th className="px-6 py-3 font-semibold">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {loading ? (
                  <tr>
                    <td colSpan={8} className="py-12 text-center text-slate-500">
                      <Loader2 className="inline h-6 w-6 animate-spin" />
                    </td>
                  </tr>
                ) : records.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-12 text-center">
                      <FileText className="mx-auto mb-3 h-10 w-10 text-slate-300" />
                      <p className="font-medium text-slate-700">Chưa có hồ sơ phù hợp</p>
                      <p className="mt-1 text-sm text-slate-500">Thử đổi từ khóa tìm kiếm hoặc kiểm tra lại sau.</p>
                    </td>
                  </tr>
                ) : (
                  records.map((record, index) => {
                    const statusClass = STATUS_BADGE[record.status] ?? 'border-slate-200 bg-slate-50 text-slate-700';
                    return (
                      <tr
                        key={record.recordId}
                        role="button"
                        tabIndex={0}
                        onClick={() => openRecordDetail(record.recordId)}
                        onKeyDown={(event) => handleRecordKeyDown(event, record.recordId)}
                        className="cursor-pointer text-slate-900 hover:bg-blue-50/70 focus:bg-blue-50 focus:outline-none"
                        title="Bấm để xem chi tiết hồ sơ"
                      >
                        <td className="px-6 py-4 text-slate-500">{page * PAGE_SIZE + index + 1}</td>
                        <td className="px-6 py-4 font-mono text-xs">{record.recordId}</td>
                        <td className="px-6 py-4">
                          <div className="font-semibold">{record.patientName || 'Chưa cập nhật'}</div>
                          <div className="mt-1 font-mono text-xs text-slate-500">{record.patientId || '--'}</div>
                        </td>
                        <td className="px-6 py-4">{formatAppointmentDate(record.examinationDate)}</td>
                        <td className="px-6 py-4">
                          <div>{record.clinicalServiceName || record.departmentName || '--'}</div>
                          <div className="mt-1 text-xs text-slate-500">
                            {formatVietnameseLabel(record.visitType, VISIT_TYPE_LABELS, 'Loại khám khác')}
                          </div>
                        </td>
                        <td className="max-w-xs truncate px-6 py-4">
                          {record.diagnosisSummary || 'Chưa có chẩn đoán'}
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${statusClass}`}>
                            {formatVietnameseLabel(record.status, APPOINTMENT_STATUS_LABELS)}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <button
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation();
                              openRecordDetail(record.recordId);
                            }}
                            className="inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium hover:bg-slate-50"
                          >
                            <Eye className="h-4 w-4" />
                            Xem chi tiết
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          <div className="flex flex-col gap-3 border-t p-4 text-sm sm:flex-row sm:items-center sm:justify-between">
            <span className="text-slate-500">
              Hiển thị <span className="font-semibold text-slate-900">{firstItem}-{lastItem}</span> /{' '}
              <span className="font-semibold text-slate-900">{totalElements}</span>
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                disabled={page === 0 || loading}
                className="rounded-md border px-3 py-1.5 text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Trước
              </button>
              <span className="rounded-md bg-slate-900 px-3 py-1.5 font-semibold text-white">
                {page + 1}
              </span>
              <button
                type="button"
                onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
                disabled={page >= totalPages - 1 || loading}
                className="rounded-md border px-3 py-1.5 text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Sau
              </button>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
