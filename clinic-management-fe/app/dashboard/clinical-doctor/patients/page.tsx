'use client';

import { useState, useEffect } from 'react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { Search, Filter, Loader2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { appointmentService, AppointmentItem } from '@/services/appointment.service';
import { formatAppointmentDate } from '@/lib/date';
import { APPOINTMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  REQUESTED: { label: APPOINTMENT_STATUS_LABELS.REQUESTED, color: 'text-purple-700' },
  CONFIRMED: { label: APPOINTMENT_STATUS_LABELS.CONFIRMED, color: 'text-blue-700' },
  WAITING: { label: APPOINTMENT_STATUS_LABELS.WAITING, color: 'text-yellow-700' },
  WAITING_RESULTS: { label: APPOINTMENT_STATUS_LABELS.WAITING_RESULTS, color: 'text-amber-700' },
  IN_PROGRESS: { label: APPOINTMENT_STATUS_LABELS.IN_PROGRESS, color: 'text-blue-700' },
  COMPLETED: { label: APPOINTMENT_STATUS_LABELS.COMPLETED, color: 'text-green-700' },
  CANCELLED: { label: APPOINTMENT_STATUS_LABELS.CANCELLED, color: 'text-red-600' },
};

export default function ClinicalDoctorPatientListPage() {
  const router = useRouter();
  const [appointments, setAppointments] = useState<AppointmentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const handleAccessExamination = (appointment: AppointmentItem) => {
    const params = new URLSearchParams();
    if (appointment.recordId) {
      params.set('recordId', appointment.recordId);
    }
    const queryString = params.toString();
    router.push(
      queryString
        ? `/dashboard/clinical-doctor/examination/${appointment.appointmentId}?${queryString}`
        : `/dashboard/clinical-doctor/examination/${appointment.appointmentId}`
    );
  };

  useEffect(() => {
    const fetchAppointments = async () => {
      setLoading(true);
      try {
        const data = await appointmentService.getMyPatients(page, 10, statusFilter || undefined);
        setAppointments(data.content ?? []);
        setTotalPages(data.totalPages ?? 1);
      } catch (error) {
        console.error('Error fetching appointments:', error);
        setAppointments([]);
      } finally {
        setLoading(false);
      }
    };
    fetchAppointments();
  }, [page, statusFilter]);

  const filtered = appointments.filter((appointment) => {
    if (!keyword) return true;
    const q = keyword.toLowerCase();
    return (
      appointment.patientName?.toLowerCase().includes(q) ||
      appointment.patientId?.toLowerCase().includes(q) ||
      appointment.appointmentId?.toLowerCase().includes(q) ||
      appointment.recordId?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="min-h-screen bg-gray-50">
      <DashboardHeader
        title="Danh sách bệnh nhân"
        subtitle="Quản lý danh sách bệnh nhân khám hôm nay"
      />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
          <div className="flex flex-col gap-4 border-b p-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative w-full sm:w-2/3">
              <input
                type="text"
                placeholder="Tìm kiếm theo tên, mã BN, mã lịch hẹn, mã hồ sơ"
                className="w-full rounded-md border py-2 pl-10 pr-4 text-black focus:border-blue-500 focus:outline-none"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
              <Search className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
            </div>
            <div className="flex w-full justify-end sm:w-1/3">
              <div className="relative w-full sm:w-auto">
                <select
                  className="w-full appearance-none rounded-md border bg-white py-2 pl-4 pr-10 text-black focus:border-blue-500 focus:outline-none"
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
                    setPage(1);
                  }}
                >
                  <option value="">Tất cả trạng thái</option>
                  <option value="WAITING">Chờ khám</option>
                  <option value="WAITING_RESULTS">Lưu tạm - chờ cận lâm sàng</option>
                  <option value="IN_PROGRESS">Đang khám</option>
                  <option value="CONFIRMED">Đã xác nhận</option>
                  <option value="COMPLETED">Hoàn thành</option>
                  <option value="CANCELLED">Đã hủy</option>
                </select>
                <Filter className="pointer-events-none absolute right-3 top-2.5 h-5 w-5 text-gray-400" />
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full whitespace-nowrap text-left text-sm">
              <thead className="border-b bg-gray-100/50">
                <tr>
                  <th className="px-6 py-3 font-semibold text-black">STT</th>
                  <th className="px-6 py-3 font-semibold text-black">Mã lịch hẹn</th>
                  <th className="px-6 py-3 font-semibold text-black">Mã BN</th>
                  <th className="px-6 py-3 font-semibold text-black">Tên bệnh nhân</th>
                  <th className="px-6 py-3 font-semibold text-black">Ngày hẹn</th>
                  <th className="px-6 py-3 font-semibold text-black">Trạng thái</th>
                  <th className="px-6 py-3 font-semibold text-black">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {loading ? (
                  <tr>
                    <td colSpan={7} className="py-10 text-center">
                      <Loader2 className="inline animate-spin text-gray-400" size={24} />
                    </td>
                  </tr>
                ) : filtered.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-gray-500">
                      Không tìm thấy lịch hẹn nào
                    </td>
                  </tr>
                ) : (
                  filtered.map((appointment, index) => {
                    const status = STATUS_LABELS[appointment.status] ?? {
                      label: formatVietnameseLabel(appointment.status, APPOINTMENT_STATUS_LABELS),
                      color: 'text-gray-600',
                    };
                    return (
                      <tr key={appointment.appointmentId} className="text-black hover:bg-gray-50">
                        <td className="px-6 py-4">{(page - 1) * 10 + index + 1}</td>
                        <td className="px-6 py-4 font-mono text-xs">{appointment.appointmentId}</td>
                        <td className="px-6 py-4">{appointment.patientId}</td>
                        <td className="px-6 py-4 font-semibold">{appointment.patientName}</td>
                        <td className="px-6 py-4">
                          {appointment.appointmentDate
                            ? formatAppointmentDate(appointment.appointmentDate)
                            : '—'}
                        </td>
                        <td className={`px-6 py-4 font-medium ${status.color}`}>
                          {status.label}
                        </td>
                        <td className="px-6 py-4">
                          <button
                            onClick={() => handleAccessExamination(appointment)}
                            className="rounded border border-gray-300 px-3 py-1.5 text-sm font-medium hover:bg-gray-50"
                          >
                            Truy cập
                          </button>
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
