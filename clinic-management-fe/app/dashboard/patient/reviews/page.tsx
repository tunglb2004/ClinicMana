'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppointmentReviewItem, reviewService } from '@/services/review.service';
import {
  ArrowRight,
  CalendarDays,
  ClipboardList,
  Loader2,
  MessageSquare,
  Reply,
  Star,
  Stethoscope,
} from 'lucide-react';
import toast from 'react-hot-toast';

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

function renderStars(rating: number) {
  return (
    <div className="flex items-center gap-1">
      {Array.from({ length: 5 }, (_, index) => {
        const starValue = index + 1;
        return (
          <Star
            key={starValue}
            className={`h-4 w-4 ${starValue <= rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'}`}
          />
        );
      })}
      <span className="ml-2 text-sm font-semibold text-gray-700">{rating}/5</span>
    </div>
  );
}

export default function ReviewsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [reviews, setReviews] = useState<AppointmentReviewItem[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const fetchReviews = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const response = await reviewService.getMyReviews(page, pageSize);
      setReviews(response.content);
      setTotalCount(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Failed to fetch patient reviews:', error);
      toast.error('Không thể tải danh sách đánh giá. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, [pageSize]);

  useEffect(() => {
    fetchReviews(currentPage);
  }, [fetchReviews, currentPage]);

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
    setCurrentPage(1);
  };

  return (
    <div className="min-h-full bg-gray-50 p-6 text-black">
      <div className="max-w-6xl mx-auto space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-[#1a2b3c]">Đánh giá của tôi</h1>
          <p className="mt-2 text-sm text-gray-500">
            Những đánh giá bạn đã gửi cho bác sĩ theo từng dịch vụ trong các ca khám đã hoàn thành.
          </p>
        </div>

        <section className="rounded-xl border border-blue-200 bg-blue-50 p-4 shadow-sm">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-sm font-semibold text-blue-800">Đánh giá được gửi từ màn chi tiết ca khám</p>
              <p className="mt-1 text-sm text-blue-700">
                Sau khi gửi thành công, đánh giá sẽ xuất hiện tại đây để bạn xem lại nội dung đã gửi và phản hồi từ bác sĩ hoặc phòng khám.
              </p>
            </div>
            <button
              type="button"
              onClick={() => router.push('/dashboard/patient/appointments')}
              className="inline-flex items-center justify-center gap-2 rounded-lg bg-blue-700 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-800"
            >
              Xem lịch khám của tôi
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        </section>

        <section className="grid gap-4 md:grid-cols-3">
          <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-600">
              <MessageSquare className="h-4 w-4" />
              Tổng đánh giá
            </div>
            <p className="mt-2 text-2xl font-bold text-gray-900">{totalCount}</p>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-600">
              <ClipboardList className="h-4 w-4" />
              Trang hiện tại
            </div>
            <p className="mt-2 text-2xl font-bold text-gray-900">{totalCount === 0 ? 0 : currentPage}</p>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-600">
              <CalendarDays className="h-4 w-4" />
              Mỗi trang
            </div>
            <div className="mt-2">
              <select
                value={pageSize}
                onChange={(event) => handlePageSizeChange(Number(event.target.value))}
                className="rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-800 focus:border-[#1a2b3c] focus:outline-none"
              >
                {[5, 10, 20].map((size) => (
                  <option key={size} value={size}>
                    {size} đánh giá
                  </option>
                ))}
              </select>
            </div>
          </div>
        </section>

        {loading ? (
          <div className="flex items-center justify-center gap-2 py-32 text-slate-600">
            <Loader2 className="h-5 w-5 animate-spin" />
            <span>Đang tải đánh giá...</span>
          </div>
        ) : reviews.length === 0 ? (
          <div className="rounded-xl border border-gray-200 bg-white p-12 text-center shadow-sm">
            <MessageSquare className="mx-auto mb-3 h-12 w-12 text-slate-400" />
            <p className="text-base text-slate-600">Bạn chưa gửi đánh giá nào</p>
            <p className="mb-4 mt-1 text-sm text-slate-500">
              Hãy vào chi tiết một ca khám đã hoàn thành để đánh giá bác sĩ theo từng dịch vụ đã sử dụng.
            </p>
            <button
              type="button"
              onClick={() => router.push('/dashboard/patient/appointments')}
              className="inline-flex items-center gap-2 rounded-lg bg-[#1a2b3c] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#24384d]"
            >
              Đi tới lịch khám của tôi
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        ) : (
          <>
            <div className="space-y-4">
              {reviews.map((review) => (
                <article
                  key={review.reviewId}
                  className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-md"
                >
                  <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
                    <div className="flex items-start gap-4">
                      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#1a2b3c] text-white shadow-sm">
                        <Stethoscope className="h-7 w-7" />
                      </div>

                      <div className="space-y-2">
                        <div>
                          <p className="text-base font-semibold text-gray-900">
                            {review.doctorName || 'Bác sĩ chưa xác định'}
                          </p>
                          <p className="text-sm text-gray-500">
                            {review.departmentName || 'Chưa cập nhật chuyên khoa'}
                          </p>
                        </div>

                        <div className="flex flex-wrap gap-2 text-xs">
                          {review.serviceName && (
                            <span className="rounded-full border border-blue-200 bg-blue-50 px-3 py-1 font-medium text-blue-700">
                              Dịch vụ: {review.serviceName}
                            </span>
                          )}
                          {review.appointmentId && (
                            <span className="rounded-full border border-gray-200 bg-gray-50 px-3 py-1 font-medium text-gray-600">
                              Ca khám: {review.appointmentId}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-col items-start gap-3 lg:items-end">
                      {renderStars(review.rating)}
                      <p className="text-sm text-gray-500">Gửi ngày {formatDate(review.createdAt)}</p>
                    </div>
                  </div>

                  <div className="mt-4 rounded-lg border border-gray-100 bg-gray-50 p-4">
                    <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">Nhận xét của bạn</p>
                    <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-gray-800">
                      {review.comment?.trim() || 'Bạn đã gửi đánh giá sao, nhưng chưa thêm nhận xét chi tiết.'}
                    </p>
                  </div>

                  {review.replyContent && (
                    <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-4">
                      <div className="flex items-center gap-2 text-sm font-semibold text-emerald-800">
                        <Reply className="h-4 w-4" />
                        Phản hồi từ {review.replyByName || 'phòng khám'}
                      </div>
                      <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-emerald-900">
                        {review.replyContent}
                      </p>
                    </div>
                  )}

                  {review.appointmentId && (
                    <div className="mt-4">
                      <button
                        type="button"
                        onClick={() => router.push(`/dashboard/patient/appointments/${review.appointmentId}`)}
                        className="inline-flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-100"
                      >
                        Xem lại ca khám
                        <ArrowRight className="h-4 w-4" />
                      </button>
                    </div>
                  )}
                </article>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex flex-wrap items-center justify-center gap-2 pt-2">
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage <= 1}
                  className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Trước
                </button>

                {Array.from({ length: totalPages }, (_, index) => (
                  <button
                    key={index + 1}
                    onClick={() => handlePageChange(index + 1)}
                    className={`rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
                      currentPage === index + 1
                        ? 'bg-[#1a2b3c] text-white'
                        : 'border border-gray-300 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    {index + 1}
                  </button>
                ))}

                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage >= totalPages}
                  className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Sau
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}