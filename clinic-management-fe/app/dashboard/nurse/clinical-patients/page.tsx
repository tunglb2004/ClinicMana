'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { Filter, Loader2, PauseCircle, RotateCcw, Search } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { appointmentService, AppointmentItem } from '@/services/appointment.service';
import { useToast } from '@/hooks/use-toast';
import { APPOINTMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  WAITING: { label: APPOINTMENT_STATUS_LABELS.WAITING, color: 'text-blue-600' },
  WAITING_RESULTS: { label: APPOINTMENT_STATUS_LABELS.WAITING_RESULTS, color: 'text-amber-700' },
  IN_PROGRESS: { label: APPOINTMENT_STATUS_LABELS.IN_PROGRESS, color: 'text-green-700' },
  POSTPONED: { label: 'Tạm hoãn', color: 'text-amber-700' },
  COMPLETED: { label: APPOINTMENT_STATUS_LABELS.COMPLETED, color: 'text-slate-600' },
  CONFIRMED: { label: APPOINTMENT_STATUS_LABELS.CONFIRMED, color: 'text-blue-700' },
  REQUESTED: { label: APPOINTMENT_STATUS_LABELS.REQUESTED, color: 'text-blue-700' },
  CANCELLED: { label: APPOINTMENT_STATUS_LABELS.CANCELLED, color: 'text-red-600' },
};

function isTemporarilyPostponed(appointment: AppointmentItem) {
  return appointment.status === 'POSTPONED';
}

type ApiError = {
  response?: {
    data?: {
      message?: string;
      error?: string;
    };
  };
};

function getErrorMessage(error: unknown, fallback: string) {
  const apiError = error as ApiError;
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}

function formatAppointmentDate(value?: string | null) {
  if (!value) return '—';
  const datePart = value.split('T')[0];
  const [year, month, day] = datePart.split('-');
  if (!year || !month || !day) return value;
  return `${Number(day)}/${Number(month)}/${year}`;
}

export default function NurseClinicalPatientListPage() {
  const router = useRouter();
  const { toast } = useToast();
  const [appointments, setAppointments] = useState<AppointmentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const autoCallingNextRef = useRef(false);
  const [postponingId, setPostponingId] = useState<string | null>(null);
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
        ? `/dashboard/nurse/clinical-examination/${appointment.appointmentId}?${queryString}`
        : `/dashboard/nurse/clinical-examination/${appointment.appointmentId}`
    );
  };

  const fetchAppointments = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
    }
    try {
      const data = await appointmentService.getNurseClinicalPatients(page, 10, statusFilter || undefined);
      const content = data.content ?? [];
      const shouldAutoCallNext =
        !statusFilter &&
        !autoCallingNextRef.current &&
        content.some((appointment) => appointment.status === 'WAITING' && !isTemporarilyPostponed(appointment)) &&
        !content.some((appointment) => appointment.status === 'IN_PROGRESS');

      if (shouldAutoCallNext) {
        autoCallingNextRef.current = true;
        try {
          await appointmentService.callNextNurseClinicalPatient();
          const refreshed = await appointmentService.getNurseClinicalPatients(page, 10);
          setAppointments(refreshed.content ?? []);
          setTotalPages(refreshed.totalPages ?? 1);
          return;
        } catch (autoCallError) {
          console.error('Error auto-calling next nurse clinical patient:', autoCallError);
        } finally {
          autoCallingNextRef.current = false;
        }
      }

      setAppointments(content);
      setTotalPages(data.totalPages ?? 1);
    } catch (error) {
      console.error('Error fetching nurse clinical appointments:', error);
      setAppointments([]);
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }, [page, statusFilter]);

  useEffect(() => {
    fetchAppointments();
    const intervalId = window.setInterval(() => {
      fetchAppointments(true);
    }, 30000);

    return () => window.clearInterval(intervalId);
  }, [fetchAppointments]);

  const handlePostponePatient = async (appointment: AppointmentItem) => {
    if (postponingId) return;

    setPostponingId(appointment.appointmentId);
    try {
      const promotedPatient = await appointmentService.postponeNurseClinicalPatient(appointment.appointmentId);
      const calledNext = promotedPatient.status === 'IN_PROGRESS' && promotedPatient.appointmentId !== appointment.appointmentId;
      toast({
        description: calledNext
          ? `Đã tạm hoãn ${appointment.patientName || 'bệnh nhân'} và gọi ${promotedPatient.patientName || 'bệnh nhân tiếp theo'} vào khám.`
          : `Đã tạm hoãn ${appointment.patientName || 'bệnh nhân'}. Hiện chưa còn bệnh nhân khác để gọi.`,
        variant: 'success',
      });
      await fetchAppointments(true);
    } catch (error) {
      toast({
        description: getErrorMessage(error, 'Không thể tạm hoãn bệnh nhân này.'),
        variant: 'error',
      });
    } finally {
      setPostponingId(null);
    }
  };

  const refreshAppointmentsWithoutAutoCall = useCallback(async () => {
    const data = await appointmentService.getNurseClinicalPatients(page, 10, statusFilter || undefined);
    setAppointments(data.content ?? []);
    setTotalPages(data.totalPages ?? 1);
  }, [page, statusFilter]);

  const handleReturnToQueue = async (appointment: AppointmentItem) => {
    if (postponingId) return;

    setPostponingId(appointment.appointmentId);
    try {
      await appointmentService.returnPostponedNurseClinicalPatientToQueue(appointment.appointmentId);
      toast({
        description: `Đã đưa ${appointment.patientName || 'bệnh nhân'} quay lại hàng đợi.`,
        variant: 'success',
      });
      await refreshAppointmentsWithoutAutoCall();
    } catch (error) {
      toast({
        description: getErrorMessage(error, 'Không thể đưa bệnh nhân này quay lại hàng đợi.'),
        variant: 'error',
      });
    } finally {
      setPostponingId(null);
    }
  };

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
        title="Hàng đợi bệnh nhân"
        subtitle="Danh sách bệnh nhân trong phòng của bạn hôm nay"
      />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
          <div className="flex flex-col gap-4 border-b p-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="relative w-full lg:max-w-xl">
              <input
                type="text"
                placeholder="Tìm kiếm theo tên, mã BN, mã lịch hẹn, mã hồ sơ"
                className="w-full rounded-md border py-2 pl-10 pr-4 text-black focus:border-blue-500 focus:outline-none"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
              <Search className="absolute left-3 top-2.5 h-5 w-5 text-slate-500" />
            </div>

            <div className="flex w-full flex-col gap-3 sm:flex-row lg:w-auto">
              <div className="relative w-full sm:w-56">
                <select
                  className="w-full appearance-none rounded-md border bg-white py-2 pl-4 pr-10 text-black focus:border-blue-500 focus:outline-none"
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
                    setPage(1);
                  }}
                >
                  <option value="">Hàng đợi hiện tại</option>
                  <option value="WAITING">Chờ khám</option>
                  <option value="POSTPONED">Tạm hoãn</option>
                  <option value="WAITING_RESULTS">Lưu tạm - chờ cận lâm sàng</option>
                  <option value="IN_PROGRESS">Đang khám</option>
                  <option value="COMPLETED">Hoàn thành</option>
                </select>
                <Filter className="pointer-events-none absolute right-3 top-2.5 h-5 w-5 text-slate-500" />
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full whitespace-nowrap text-left text-sm">
              <thead className="border-b bg-gray-100/50">
                <tr>
                  <th className="px-6 py-3 font-semibold text-black">Vị trí</th>
                  <th className="px-6 py-3 font-semibold text-black">Mã lịch hẹn</th>
                  <th className="px-6 py-3 font-semibold text-black">Mã BN</th>
                  <th className="px-6 py-3 font-semibold text-black">Tên bệnh nhân</th>
                  <th className="px-6 py-3 font-semibold text-black">Bác sĩ</th>
                  <th className="px-6 py-3 font-semibold text-black">Ngày hẹn</th>
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
                  filtered.map((appointment, index) => {
                    const temporarilyPostponed = isTemporarilyPostponed(appointment);
                    const status = temporarilyPostponed
                      ? { label: 'Tạm hoãn', color: 'text-amber-700' }
                      : STATUS_LABELS[appointment.status] ?? {
                          label: formatVietnameseLabel(appointment.status, APPOINTMENT_STATUS_LABELS),
                          color: 'text-gray-600',
                        };
                    const canEnterExam = appointment.status === 'IN_PROGRESS';
                    const isPostponing = postponingId === appointment.appointmentId;

                    return (
                      <tr key={appointment.appointmentId} className="text-black hover:bg-gray-50">
                        <td className="px-6 py-4">{(page - 1) * 10 + index + 1}</td>
                        <td className="px-6 py-4 font-mono text-xs">{appointment.appointmentId}</td>
                        <td className="px-6 py-4">{appointment.patientId}</td>
                        <td className="px-6 py-4 font-semibold">{appointment.patientName}</td>
                        <td className="px-6 py-4">{appointment.doctorName || '—'}</td>
                        <td className="px-6 py-4">{formatAppointmentDate(appointment.appointmentDate)}</td>
                        <td className={`px-6 py-4 font-medium ${status.color}`}>{status.label}</td>
                        <td className="px-6 py-4">
                          <div className="flex flex-wrap gap-2">
                            {!temporarilyPostponed && (
                              <button
                                onClick={() => handleAccessExamination(appointment)}
                                disabled={!canEnterExam}
                                className="rounded border border-gray-300 px-3 py-1.5 text-sm font-medium hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                {canEnterExam ? 'Vào khám' : 'Chờ gọi'}
                              </button>
                            )}
                            {temporarilyPostponed && (
                              <button
                                type="button"
                                onClick={() => handleReturnToQueue(appointment)}
                                disabled={Boolean(postponingId)}
                                className="inline-flex items-center gap-1 rounded border border-blue-300 bg-blue-50 px-3 py-1.5 text-sm font-medium text-blue-800 hover:bg-blue-100 disabled:cursor-not-allowed disabled:opacity-60"
                              >
                                {isPostponing ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <RotateCcw className="h-4 w-4" />
                                )}
                                Quay lại hàng đợi
                              </button>
                            )}
                            {canEnterExam && (
                              <button
                                type="button"
                                onClick={() => handlePostponePatient(appointment)}
                                disabled={Boolean(postponingId)}
                                className="inline-flex items-center gap-1 rounded border border-amber-300 bg-amber-50 px-3 py-1.5 text-sm font-medium text-amber-800 hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-60"
                              >
                                {isPostponing ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <PauseCircle className="h-4 w-4" />
                                )}
                                Tạm hoãn
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
