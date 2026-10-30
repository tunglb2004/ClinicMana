'use client';

import { useCallback, useEffect, useState } from 'react';
import { CalendarClock, CheckCircle2, Clock3, Loader2, MapPin, RefreshCw, Stethoscope } from 'lucide-react';
import { appointmentService, AppointmentItem } from '@/services/appointment.service';
import { useAuthStore } from '@/store/auth.store';
import { APPOINTMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const AUTO_REFRESH_MS = 10000;
const ACTIVE_APPOINTMENT_STATUSES = ['WAITING', 'CONFIRMED', 'IN_PROGRESS', 'WAITING_RESULTS'] as const;

const OVERALL_STATUS_LABEL_VI: Record<string, string> = {
  WAITING: 'ĐANG CHỜ',
  CONFIRMED: 'ĐÃ XÁC NHẬN',
  IN_PROGRESS: 'ĐANG KHÁM',
  WAITING_RESULTS: 'CHỜ KẾT QUẢ CLS',
  COMPLETED: 'ĐÃ HOÀN THÀNH',
  CANCELLED: 'ĐÃ HỦY',
};

const OVERALL_STATUS_DESC: Record<string, string> = {
  WAITING: 'Lễ tân đã xếp phòng khám. Vui lòng theo dõi bảng gọi số và chờ được gọi tên.',
  CONFIRMED: 'Lịch hẹn đã được xác nhận. Vui lòng có mặt đúng giờ và theo dõi lịch trình bên dưới.',
  IN_PROGRESS: 'Bác sĩ đang khám cho bạn. Màn hình này sẽ tự cập nhật tiến trình trong lúc khám.',
  WAITING_RESULTS: 'Bác sĩ đã lưu tạm hồ sơ. Vui lòng thực hiện cận lâm sàng và chờ quay lại phòng khám.',
  COMPLETED: 'Buổi khám đã hoàn tất. Bạn có thể xem kết quả trong hồ sơ sức khỏe.',
  CANCELLED: 'Lịch hẹn đã bị hủy. Vui lòng liên hệ lễ tân để đặt lại lịch khám.',
};

const STATUS_BADGE_CLASS: Record<string, string> = {
  WAITING: 'status-badge status-info',
  CONFIRMED: 'status-badge status-brand',
  IN_PROGRESS: 'status-badge status-warning',
  WAITING_RESULTS: 'status-badge status-warning',
  COMPLETED: 'status-badge status-success',
  CANCELLED: 'status-badge status-danger',
};

type StepStatus = 'done' | 'current' | 'pending';

function getLocalDateKey(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getAppointmentDateKey(value?: string | null) {
  return value?.split('T')[0] ?? '';
}

function compareAppointmentsForToday(left: AppointmentItem, right: AppointmentItem) {
  const leftIsActive = ACTIVE_APPOINTMENT_STATUSES.includes(
    left.status as (typeof ACTIVE_APPOINTMENT_STATUSES)[number]
  );
  const rightIsActive = ACTIVE_APPOINTMENT_STATUSES.includes(
    right.status as (typeof ACTIVE_APPOINTMENT_STATUSES)[number]
  );

  if (leftIsActive !== rightIsActive) return leftIsActive ? -1 : 1;
  return (left.appointmentTime ?? '').localeCompare(right.appointmentTime ?? '');
}

function formatAppointmentDate(value?: string | null) {
  if (!value) return '—';
  const [year, month, day] = getAppointmentDateKey(value).split('-');
  if (!year || !month || !day) return value;
  return `${Number(day)}/${Number(month)}/${year}`;
}

function getServiceStatuses(appt: AppointmentItem): StepStatus[] {
  const count = appt.services?.length ?? 0;
  if (!count) return [];
  if (appt.status === 'COMPLETED') return Array(count).fill('done');
  if (appt.status === 'CANCELLED') return Array(count).fill('pending');
  return appt.services.map((_, index) => (index === 0 ? 'current' : 'pending'));
}

function getStepClasses(status: StepStatus) {
  if (status === 'done') {
    return {
      item: 'border-emerald-200 bg-emerald-50/80',
      icon: 'bg-emerald-100 text-emerald-700',
      badge: 'status-badge status-success',
      label: 'HOÀN THÀNH',
    };
  }

  if (status === 'current') {
    return {
      item: 'border-blue-300 bg-blue-50/90 shadow-sm shadow-blue-600/10',
      icon: 'bg-blue-600 text-white',
      badge: 'status-badge status-info',
      label: 'ĐANG THỰC HIỆN',
    };
  }

  return {
    item: 'border-slate-200 bg-white',
    icon: 'bg-slate-100 text-slate-500',
    badge: 'status-badge bg-slate-100 text-slate-600',
    label: 'CHƯA DIỄN RA',
  };
}

export default function PatientDashboard() {
  const { user } = useAuthStore();
  const [todayAppt, setTodayAppt] = useState<AppointmentItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [syncError, setSyncError] = useState(false);

  const fetchAppointment = useCallback((quiet = false) => {
    if (!quiet) setLoading(true);
    else setSyncing(true);
    setSyncError(false);

    appointmentService
      .getMyAppointments(1, 20)
      .then((res) => {
        const today = getLocalDateKey();
        const todaysAppointments = (res.content ?? [])
          .filter((appointment) => getAppointmentDateKey(appointment.appointmentDate) === today)
          .sort(compareAppointmentsForToday);

        setTodayAppt(todaysAppointments[0] ?? null);
      })
      .catch(() => {
        if (quiet) setSyncError(true);
      })
      .finally(() => {
        setLoading(false);
        setSyncing(false);
      });
  }, []);

  useEffect(() => {
    const initialLoadId = window.setTimeout(() => {
      fetchAppointment(false);
    }, 0);
    return () => window.clearTimeout(initialLoadId);
  }, [fetchAppointment]);

  useEffect(() => {
    const id = window.setInterval(() => fetchAppointment(true), AUTO_REFRESH_MS);
    return () => window.clearInterval(id);
  }, [fetchAppointment]);

  const statuses = todayAppt ? getServiceStatuses(todayAppt) : [];

  return (
    <div className="min-h-full p-6 text-slate-950 lg:p-8">
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="dashboard-card-soft overflow-hidden p-6">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-wide text-blue-700">Xin chào</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
                {user?.fullName ?? 'Bệnh nhân'}
              </h1>
              <p className="mt-2 max-w-2xl text-sm text-slate-600">
                Theo dõi lịch khám hôm nay, phòng được phân và trạng thái hàng đợi theo thời gian thực.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-3 sm:min-w-[320px]">
              <div className="rounded-xl border border-blue-100 bg-white/80 p-4">
                <CalendarClock className="mb-2 h-5 w-5 text-blue-700" />
                <p className="text-xs font-semibold uppercase text-slate-500">Ngày khám</p>
                <p className="mt-1 font-semibold text-slate-950">
                  {todayAppt ? formatAppointmentDate(todayAppt.appointmentDate) : getLocalDateKey()}
                </p>
              </div>
              <div className="rounded-xl border border-teal-100 bg-white/80 p-4">
                <Clock3 className="mb-2 h-5 w-5 text-teal-700" />
                <p className="text-xs font-semibold uppercase text-slate-500">Giờ hẹn</p>
                <p className="mt-1 font-semibold text-slate-950">{todayAppt?.appointmentTime || '—'}</p>
              </div>
            </div>
          </div>
        </section>

        {loading ? (
          <div className="dashboard-card flex items-center justify-center gap-2 py-24 text-slate-600">
            <Loader2 className="h-5 w-5 animate-spin" />
            <span>Đang tải lịch trình...</span>
          </div>
        ) : !todayAppt ? (
          <div className="dashboard-card p-12 text-center">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50 text-blue-700">
              <CalendarClock className="h-7 w-7" />
            </div>
            <p className="text-base font-semibold text-slate-950">Bạn không có lịch hẹn nào hôm nay.</p>
            <p className="mt-1 text-sm text-slate-500">Vui lòng liên hệ lễ tân để đặt lịch khám.</p>
          </div>
        ) : (
          <section className="dashboard-card p-6">
            <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <h2 className="text-lg font-bold text-slate-950">Lịch trình khám hôm nay</h2>
                <div className="mt-3 flex flex-wrap items-center gap-2">
                  <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">Trạng thái</span>
                  <span className={STATUS_BADGE_CLASS[todayAppt.status] || 'status-badge status-brand'}>
                    {OVERALL_STATUS_LABEL_VI[todayAppt.status] ?? formatVietnameseLabel(todayAppt.status, APPOINTMENT_STATUS_LABELS).toLocaleUpperCase('vi-VN')}
                  </span>
                </div>
                <p className="mt-2 max-w-2xl text-sm leading-relaxed text-slate-600">
                  {OVERALL_STATUS_DESC[todayAppt.status] ?? ''}
                </p>
              </div>

              <div className="flex flex-col gap-1 lg:items-end">
                {syncing ? (
                  <span className="flex items-center gap-1 text-xs font-medium text-slate-500">
                    <RefreshCw className="h-3 w-3 animate-spin" /> Đang cập nhật...
                  </span>
                ) : syncError ? (
                  <span className="text-xs font-medium text-red-600">
                    Không thể tải dữ liệu trực tiếp, vui lòng tải lại trang.
                  </span>
                ) : (
                  <span className="text-xs font-medium text-slate-500">Tự động cập nhật mỗi 10s</span>
                )}
              </div>
            </div>

            {todayAppt.services && todayAppt.services.length > 0 ? (
              <div className="mt-5 space-y-3">
                {todayAppt.services.map((service, index) => {
                  const style = getStepClasses(statuses[index]);

                  return (
                    <div key={service.serviceId} className={`rounded-xl border p-4 transition-all ${style.item}`}>
                      <div className="flex items-start gap-4">
                        <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${style.icon}`}>
                          {statuses[index] === 'done' ? <CheckCircle2 className="h-5 w-5" /> : <Stethoscope className="h-5 w-5" />}
                        </div>

                        <div className="min-w-0 flex-1">
                          <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                            <div>
                              <p className="text-sm font-semibold text-slate-950">
                                {index + 1}. {service.serviceName}
                              </p>
                              {todayAppt.departmentName && index === 0 && (
                                <p className="mt-1 flex items-center gap-1 text-xs text-slate-500">
                                  <MapPin className="h-3.5 w-3.5" />
                                  {todayAppt.departmentName}
                                </p>
                              )}
                              {todayAppt.doctorName && index === 0 && (
                                <p className="mt-0.5 text-xs text-slate-500">BS. {todayAppt.doctorName}</p>
                              )}
                              {statuses[index] === 'current' && todayAppt.queueNumber != null && (
                                <p className="mt-2 text-xs font-semibold text-blue-700">
                                  Số đang chờ: {todayAppt.queueNumber}
                                </p>
                              )}
                            </div>
                            <span className={style.badge}>{style.label}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="mt-4 text-sm text-slate-600">Không có dịch vụ nào trong lịch hẹn này.</p>
            )}

            <div className="mt-6 flex flex-wrap gap-x-6 gap-y-2 border-t border-slate-100 pt-5 text-xs text-slate-500">
              <span>
                Mã lịch hẹn: <span className="font-mono font-semibold text-slate-700">{todayAppt.appointmentId}</span>
              </span>
              <span>
                Ngày: <span className="font-semibold text-slate-700">{formatAppointmentDate(todayAppt.appointmentDate)}</span>
              </span>
              {todayAppt.appointmentTime && (
                <span>
                  Giờ: <span className="font-semibold text-slate-700">{todayAppt.appointmentTime}</span>
                </span>
              )}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
