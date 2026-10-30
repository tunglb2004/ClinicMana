'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import {
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Clock3,
  FileText,
  Loader2,
  MapPin,
  MessageSquare,
  Star,
  UserRound,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { appointmentService, AppointmentDetail, ServiceInfo } from '@/services/appointment.service';
import { AppointmentReviewItem, reviewService } from '@/services/review.service';
import { APPOINTMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

type ReviewDraft = {
  rating: number;
  comment: string;
  submitting: boolean;
};

type ReviewDraftMap = Record<string, ReviewDraft>;

const STATUS_LABELS: Record<string, string> = {
  REQUESTED: 'Chờ xác nhận',
  WAITING: 'Sắp tới',
  CONFIRMED: 'Sắp tới',
  IN_PROGRESS: 'Đang khám',
  WAITING_RESULTS: 'Chờ kết quả cận lâm sàng',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
};

function buildReviewKey(serviceId?: string | null, doctorId?: string | null) {
  return `${serviceId ?? 'none'}::${doctorId ?? 'none'}`;
}

function createDefaultDraft(): ReviewDraft {
  return {
    rating: 5,
    comment: '',
    submitting: false,
  };
}

function formatDate(value?: string | null) {
  if (!value) {
    return 'Chưa cập nhật';
  }

  return new Date(value).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return 'Chưa cập nhật';
  }

  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function renderStars(rating: number) {
  return Array.from({ length: 5 }, (_, index) => {
    const starValue = index + 1;
    return (
      <Star
        key={starValue}
        className={`h-4 w-4 ${starValue <= rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'}`}
      />
    );
  });
}

export default function PatientAppointmentDetailPage() {
  const params = useParams<{ id: string | string[] }>();
  const router = useRouter();
  const appointmentId = Array.isArray(params.id) ? params.id[0] : params.id;

  const [loading, setLoading] = useState(true);
  const [appointment, setAppointment] = useState<AppointmentDetail | null>(null);
  const [reviews, setReviews] = useState<AppointmentReviewItem[]>([]);
  const [drafts, setDrafts] = useState<ReviewDraftMap>({});
  const [submittedReviewNotice, setSubmittedReviewNotice] = useState<{
    doctorName: string;
    serviceName: string;
  } | null>(null);

  const loadData = useCallback(async () => {
    if (!appointmentId) {
      toast.error('Không tìm thấy lịch hẹn.');
      router.push('/dashboard/patient/appointments');
      return;
    }

    setLoading(true);
    try {
      const [detailResponse, reviewResponse] = await Promise.all([
        appointmentService.getMyAppointmentDetail(appointmentId),
        reviewService.getMyAppointmentReviews(appointmentId),
      ]);

      setAppointment(detailResponse);
      setReviews(reviewResponse);
    } catch (error) {
      console.error('Failed to fetch patient appointment detail:', error);
      toast.error('Không thể tải chi tiết lịch khám.');
      router.push('/dashboard/patient/appointments');
    } finally {
      setLoading(false);
    }
  }, [appointmentId, router]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const reviewMap = useMemo(() => {
    return new Map(reviews.map((review) => [buildReviewKey(review.serviceId, review.doctorId), review]));
  }, [reviews]);

  const isCompleted = appointment?.status === 'COMPLETED';

  const getDraft = (service: ServiceInfo): ReviewDraft => {
    return drafts[buildReviewKey(service.serviceId, service.assignedDoctorId)] ?? createDefaultDraft();
  };

  const updateDraft = (service: ServiceInfo, updater: (current: ReviewDraft) => ReviewDraft) => {
    const reviewKey = buildReviewKey(service.serviceId, service.assignedDoctorId);
    setDrafts((current) => ({
      ...current,
      [reviewKey]: updater(current[reviewKey] ?? createDefaultDraft()),
    }));
  };

  const submitReview = async (service: ServiceInfo) => {
    if (!appointmentId || !service.assignedDoctorId) {
      toast.error('Không xác định được bác sĩ phụ trách để đánh giá.');
      return;
    }

    const draft = getDraft(service);
    updateDraft(service, (current) => ({ ...current, submitting: true }));

    try {
      await reviewService.createAppointmentReview({
        appointmentId,
        doctorId: service.assignedDoctorId,
        serviceId: service.serviceId,
        rating: draft.rating,
        comment: draft.comment.trim(),
      });

      const updatedReviews = await reviewService.getMyAppointmentReviews(appointmentId);
      setReviews(updatedReviews);
      updateDraft(service, () => createDefaultDraft());
      setSubmittedReviewNotice({
        doctorName: service.assignedDoctorName || 'bác sĩ',
        serviceName: service.serviceName,
      });
      toast.success(`Đã gửi đánh giá thành công. Bạn có thể xem lại tại trang Đánh giá của tôi.`);
    } catch (error) {
      console.error('Failed to create appointment review:', error);
      toast.error('Không thể gửi đánh giá. Vui lòng thử lại.');
      updateDraft(service, (current) => ({ ...current, submitting: false }));
    }
  };

  if (loading) {
    return (
      <div className="min-h-full bg-gray-50 p-6 text-black">
        <div className="mx-auto flex max-w-5xl items-center justify-center gap-2 py-32 text-slate-600">
          <Loader2 className="h-5 w-5 animate-spin" />
          <span>Đang tải chi tiết lịch khám...</span>
        </div>
      </div>
    );
  }

  if (!appointment) {
    return null;
  }

  return (
    <div className="min-h-full bg-gray-50 p-6 text-black">
      <div className="max-w-5xl mx-auto space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-[#1a2b3c]">Chi tiết ca khám</h1>
            <p className="text-sm text-gray-500 mt-1">Mã lịch hẹn: {appointment.appointmentId}</p>
          </div>

          <div className="flex flex-wrap gap-2">
            {appointment.medicalRecord?.recordId && (
              <button
                onClick={() => router.push(`/dashboard/patient/examination-detail?id=${appointment.medicalRecord?.recordId}`)}
                className="inline-flex items-center gap-2 rounded-lg border border-[#1a2b3c] px-4 py-2 text-sm font-semibold text-[#1a2b3c] hover:bg-[#1a2b3c] hover:text-white transition-colors"
              >
                <FileText className="h-4 w-4" />
                Xem kết quả khám
              </button>
            )}
            <button
              onClick={() => router.push('/dashboard/patient/appointments')}
              className="inline-flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-100 transition-colors"
            >
              <ArrowLeft className="h-4 w-4" />
              Quay lại lịch khám
            </button>
          </div>
        </div>

        {submittedReviewNotice && (
          <section className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 shadow-sm">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="text-sm font-semibold text-emerald-800">Đánh giá đã được gửi thành công</p>
                <p className="mt-1 text-sm text-emerald-700">
                  Bạn đã gửi đánh giá cho {submittedReviewNotice.doctorName} ở dịch vụ {submittedReviewNotice.serviceName}. Hệ thống đã lưu đánh giá này vào lịch sử của bạn.
                </p>
              </div>
              <button
                type="button"
                onClick={() => router.push('/dashboard/patient/reviews')}
                className="inline-flex items-center justify-center rounded-lg bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800 transition-colors"
              >
                Xem đánh giá của tôi
              </button>
            </div>
          </section>
        )}

        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-600">
              <CalendarDays className="h-4 w-4" />
              Ngày khám
            </div>
            <p className="mt-2 text-base font-bold text-gray-900">{formatDate(appointment.appointmentDate)}</p>
          </div>
          <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-600">
              <Clock3 className="h-4 w-4" />
              Giờ khám
            </div>
            <p className="mt-2 text-base font-bold text-gray-900">{appointment.appointmentTime || 'Chưa cập nhật'}</p>
          </div>
          <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-600">
              <UserRound className="h-4 w-4" />
              Bác sĩ lâm sàng
            </div>
            <p className="mt-2 text-base font-bold text-gray-900">{appointment.doctorName || 'Chưa phân công'}</p>
            <p className="mt-1 text-sm text-gray-500">{appointment.departmentName || 'Chưa cập nhật chuyên khoa'}</p>
          </div>
          <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-600">
              <MapPin className="h-4 w-4" />
              Phòng khám
            </div>
            <p className="mt-2 text-base font-bold text-gray-900">{appointment.roomName || 'Chưa cập nhật'}</p>
            <p className="mt-1 text-sm text-gray-500">
              {STATUS_LABELS[appointment.status] || formatVietnameseLabel(appointment.status, APPOINTMENT_STATUS_LABELS)}
            </p>
          </div>
        </section>

        <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-2 mb-4 text-[#1a2b3c]">
            <ClipboardList className="h-5 w-5" />
            <h2 className="text-lg font-semibold">Tóm tắt ca khám</h2>
          </div>

          <div className="grid gap-4 md:grid-cols-2 text-sm">
            <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
              <p className="text-xs uppercase tracking-wide text-gray-500 mb-1">Bệnh nhân</p>
              <p className="font-semibold text-gray-900">{appointment.patientName}</p>
              <p className="text-gray-500 mt-1">{appointment.patientPhone || 'Chưa cập nhật số điện thoại'}</p>
            </div>
            <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
              <p className="text-xs uppercase tracking-wide text-gray-500 mb-1">Ngày tạo lịch</p>
              <p className="font-semibold text-gray-900">{formatDateTime(appointment.createdAt)}</p>
            </div>
          </div>

          {appointment.notes && (
            <div className="mt-4 rounded-lg bg-blue-50 p-4 border border-blue-100 text-sm text-gray-800">
              <p className="text-xs uppercase tracking-wide text-blue-700 mb-1">Ghi chú đặt lịch</p>
              <p className="whitespace-pre-wrap">{appointment.notes}</p>
            </div>
          )}
        </section>

        <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between mb-4">
            <div className="flex items-center gap-2 text-[#1a2b3c]">
              <MessageSquare className="h-5 w-5" />
              <h2 className="text-lg font-semibold">Đánh giá bác sĩ theo dịch vụ trong ca khám</h2>
            </div>
            <span className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold ${
              isCompleted ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-amber-50 text-amber-700 border border-amber-200'
            }`}>
              <CheckCircle2 className="h-3.5 w-3.5" />
              {isCompleted ? 'Ca khám đã hoàn thành, có thể gửi đánh giá' : 'Chỉ gửi đánh giá sau khi ca khám hoàn thành'}
            </span>
          </div>

          {appointment.services?.length ? (
            <div className="space-y-4">
              {appointment.services.map((service) => {
                const reviewKey = buildReviewKey(service.serviceId, service.assignedDoctorId);
                const existingReview = reviewMap.get(reviewKey);
                const draft = getDraft(service);

                return (
                  <div key={service.serviceId} className="rounded-xl border border-gray-200 p-4">
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <h3 className="text-base font-semibold text-gray-900">{service.serviceName}</h3>
                        <p className="text-sm text-gray-500 mt-1">
                          Bác sĩ phụ trách: <span className="font-medium text-gray-800">{service.assignedDoctorName || 'Chưa xác định'}</span>
                        </p>
                        <p className="text-sm text-gray-500 mt-1">
                          Phòng thực hiện: <span className="font-medium text-gray-800">{service.assignedRoomName || 'Chưa cập nhật'}</span>
                        </p>
                      </div>
                      <div className="rounded-lg bg-gray-50 px-3 py-2 text-sm text-gray-600 border border-gray-200">
                        Giá dịch vụ: <span className="font-semibold text-gray-900">{Number(service.price || 0).toLocaleString('vi-VN')}đ</span>
                      </div>
                    </div>

                    {existingReview ? (
                      <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-4">
                        <p className="text-sm font-semibold text-emerald-800 mb-2">Bạn đã gửi đánh giá cho dịch vụ này</p>
                        <div className="flex items-center gap-1 mb-2">{renderStars(existingReview.rating)}</div>
                        <p className="text-sm text-gray-800 whitespace-pre-wrap">{existingReview.comment || 'Không có nhận xét bổ sung.'}</p>
                        <p className="mt-2 text-xs text-gray-500">Gửi lúc: {formatDateTime(existingReview.createdAt)}</p>
                      </div>
                    ) : !isCompleted ? (
                      <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                        Ca khám này chưa hoàn thành nên chưa thể gửi đánh giá cho dịch vụ này.
                      </div>
                    ) : !service.assignedDoctorId ? (
                      <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                        Hệ thống chưa xác định được bác sĩ phụ trách dịch vụ này nên chưa thể mở form đánh giá.
                      </div>
                    ) : (
                      <div className="mt-4 rounded-lg border border-gray-200 bg-gray-50 p-4 space-y-4">
                        <div>
                          <p className="text-sm font-medium text-gray-700 mb-2">Số sao đánh giá</p>
                          <div className="flex items-center gap-2">
                            {Array.from({ length: 5 }, (_, index) => {
                              const starValue = index + 1;
                              return (
                                <button
                                  key={starValue}
                                  type="button"
                                  onClick={() => updateDraft(service, (current) => ({ ...current, rating: starValue }))}
                                  className="transition-transform hover:scale-110"
                                >
                                  <Star
                                    className={`h-6 w-6 ${starValue <= draft.rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'}`}
                                  />
                                </button>
                              );
                            })}
                            <span className="text-sm font-semibold text-gray-700">{draft.rating}/5</span>
                          </div>
                        </div>

                        <div>
                          <label className="mb-2 block text-sm font-medium text-gray-700">Nhận xét cho bác sĩ ở dịch vụ này</label>
                          <textarea
                            value={draft.comment}
                            onChange={(event) => updateDraft(service, (current) => ({ ...current, comment: event.target.value }))}
                            rows={4}
                            placeholder={`Chia sẻ trải nghiệm của bạn với ${service.assignedDoctorName || 'bác sĩ'} ở dịch vụ ${service.serviceName}...`}
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-[#1a2b3c] focus:outline-none"
                          />
                        </div>

                        <div className="flex justify-end">
                          <button
                            type="button"
                            onClick={() => submitReview(service)}
                            disabled={draft.submitting}
                            className="inline-flex items-center gap-2 rounded-lg bg-[#1a2b3c] px-4 py-2 text-sm font-semibold text-white hover:bg-[#24384d] disabled:opacity-60 transition-colors"
                          >
                            {draft.submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                            Gửi đánh giá dịch vụ này
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-gray-300 p-8 text-center text-sm text-gray-500">
              Lịch khám này chưa có danh sách dịch vụ để hiển thị và đánh giá.
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
