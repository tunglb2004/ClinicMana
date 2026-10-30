'use client';

import { useEffect, useMemo, useState } from 'react';
import { reviewService, AppointmentReviewItem } from '@/services/review.service';
import { Loader2, Star } from 'lucide-react';

const STATUS_ALL = 'ALL';
const STATUS_PENDING = 'PENDING';
const STATUS_REPLIED = 'REPLIED';

const renderStars = (rating: number) =>
  Array.from({ length: 5 }, (_, index) => (
    <Star
      key={`star-${index}`}
      className={`h-4 w-4 ${index < rating ? 'fill-amber-400 text-amber-400' : 'text-slate-300'}`}
    />
  ));

const formatDate = (value?: string | null) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

const getErrorMessage = (error: unknown, fallback: string) => {
  const data = (error as { response?: { data?: { message?: string; error?: string } } })?.response?.data;
  return data?.message || data?.error || fallback;
};

export default function ManagerFeedbackPage() {
  const [rows, setRows] = useState<AppointmentReviewItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [statusFilter, setStatusFilter] = useState(STATUS_ALL);
  const [ratingFilter, setRatingFilter] = useState(STATUS_ALL);
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<AppointmentReviewItem | null>(null);
  const [reply, setReply] = useState('');
  const [error, setError] = useState<string | null>(null);

  const fetchReviews = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await reviewService.getAllReviews(1, 500);
      setRows(response.content ?? []);
    } catch (fetchError) {
      console.error('Không thể tải phản hồi:', fetchError);
      setRows([]);
      setError('Không thể tải danh sách phản hồi.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviews();
  }, []);

  const filteredRows = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    return rows.filter((item) => {
      const byStatus =
        statusFilter === STATUS_ALL
          ? true
          : statusFilter === STATUS_REPLIED
            ? Boolean(item.replyContent)
            : !item.replyContent;
      const byRating = ratingFilter === STATUS_ALL ? true : String(item.rating) === ratingFilter;
      const byKeyword =
        !q ||
        [item.patientId, item.patientName, item.doctorName, item.serviceName, item.appointmentId]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(q));

      return byStatus && byRating && byKeyword;
    });
  }, [keyword, ratingFilter, rows, statusFilter]);

  const openDetail = (row: AppointmentReviewItem) => {
    setSelected(row);
    setReply(row.replyContent ?? '');
  };

  const handleReply = async () => {
    if (!selected || !reply.trim()) return;

    setSubmitting(true);
    try {
      const updated = await reviewService.replyReview(selected.reviewId, { replyContent: reply.trim() });
      setRows((prev) => prev.map((item) => (item.reviewId === updated.reviewId ? updated : item)));
      setSelected(updated);
    } catch (submitError) {
      alert(getErrorMessage(submitError, 'Không thể gửi phản hồi.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="min-h-[calc(100vh-4rem)] p-6 text-slate-950">
        <div className="mx-auto max-w-7xl space-y-5">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Phản hồi khách hàng</h1>
            <p className="mt-1 text-sm text-slate-600">Danh sách đánh giá thật từ bệnh nhân và thao tác trả lời.</p>
          </div>

          {error && <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

          <div className="rounded-lg border bg-white p-4 shadow-sm">
            <div className="grid grid-cols-1 gap-3 md:grid-cols-[1fr_1fr_2fr]">
              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Trạng thái</span>
                <select className="h-10 w-full rounded-md border px-3" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                  <option value={STATUS_ALL}>Tất cả</option>
                  <option value={STATUS_PENDING}>Chưa trả lời</option>
                  <option value={STATUS_REPLIED}>Đã trả lời</option>
                </select>
              </label>

              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Đánh giá</span>
                <select className="h-10 w-full rounded-md border px-3" value={ratingFilter} onChange={(event) => setRatingFilter(event.target.value)}>
                  <option value={STATUS_ALL}>Tất cả</option>
                  <option value="5">5 sao</option>
                  <option value="4">4 sao</option>
                  <option value="3">3 sao</option>
                  <option value="2">2 sao</option>
                  <option value="1">1 sao</option>
                </select>
              </label>

              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Tìm kiếm</span>
                <input
                  className="h-10 w-full rounded-md border px-3"
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="Tên, mã BN, bác sĩ, dịch vụ, mã lịch hẹn..."
                />
              </label>
            </div>
          </div>

          <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1080px] text-sm">
                <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-4 py-3">STT</th>
                    <th className="px-4 py-3">Mã BN</th>
                    <th className="px-4 py-3">Khách hàng</th>
                    <th className="px-4 py-3">Đánh giá</th>
                    <th className="px-4 py-3">Dịch vụ / bác sĩ</th>
                    <th className="px-4 py-3">Nội dung</th>
                    <th className="px-4 py-3">Ngày gửi</th>
                    <th className="px-4 py-3">Trạng thái</th>
                    <th className="px-4 py-3 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {loading ? (
                    <tr>
                      <td colSpan={9} className="px-4 py-10 text-center text-slate-500">
                        <Loader2 className="inline h-5 w-5 animate-spin" />
                      </td>
                    </tr>
                  ) : filteredRows.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="px-4 py-8 text-center text-slate-500">
                        Không có phản hồi phù hợp.
                      </td>
                    </tr>
                  ) : (
                    filteredRows.map((row, index) => (
                      <tr key={row.reviewId} className="align-top hover:bg-slate-50">
                        <td className="px-4 py-3">{index + 1}</td>
                        <td className="px-4 py-3 font-mono text-xs">{row.patientId || '—'}</td>
                        <td className="px-4 py-3 font-medium">{row.patientName || '—'}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-0.5">{renderStars(row.rating || 0)}</div>
                        </td>
                        <td className="px-4 py-3">
                          <p>{row.serviceName || '—'}</p>
                          <p className="text-xs text-slate-500">{row.doctorName || row.departmentName || '—'}</p>
                        </td>
                        <td className="max-w-[320px] px-4 py-3 text-slate-700">{row.comment || 'Không có nội dung'}</td>
                        <td className="px-4 py-3">{formatDate(row.createdAt)}</td>
                        <td className="px-4 py-3">
                          <span className={`rounded-full px-2 py-1 text-xs font-semibold ${row.replyContent ? 'bg-green-50 text-green-700' : 'bg-amber-50 text-amber-700'}`}>
                            {row.replyContent ? 'Đã trả lời' : 'Chưa trả lời'}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => openDetail(row)}
                            className="rounded-md border px-3 py-1.5 text-xs font-semibold hover:bg-slate-50"
                          >
                            {row.replyContent ? 'Xem chi tiết' : 'Trả lời'}
                          </button>
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

      {selected && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4">
          <div className="w-full max-w-2xl rounded-lg border bg-white text-slate-900 shadow-xl">
            <div className="flex items-center justify-between border-b px-5 py-4">
              <h2 className="text-lg font-bold text-slate-900">Chi tiết phản hồi</h2>
              <button
                onClick={() => setSelected(null)}
                className="rounded-md border border-slate-300 bg-white px-2 py-1 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                Đóng
              </button>
            </div>

            <div className="space-y-4 p-5 text-sm text-slate-900">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-base font-semibold text-slate-950">{selected.patientName || '—'} ({selected.patientId || '—'})</p>
                  <p className="text-slate-600">Lịch hẹn: {selected.appointmentId || '—'}</p>
                  <p className="text-slate-600">Dịch vụ: {selected.serviceName || '—'}</p>
                </div>
                <div className="flex items-center gap-0.5">{renderStars(selected.rating || 0)}</div>
              </div>

              <div className="rounded-md border bg-slate-50 p-3">
                <p className="mb-1 text-xs font-semibold uppercase text-slate-500">Nội dung khách viết</p>
                <p className="whitespace-pre-wrap text-slate-900">{selected.comment || 'Không có nội dung'}</p>
              </div>

              <label className="block">
                <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Trả lời của phòng khám</span>
                <textarea
                  rows={5}
                  className="w-full resize-y rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none disabled:bg-slate-100 disabled:text-slate-700"
                  placeholder="Nhập nội dung phản hồi khách hàng..."
                  value={reply}
                  onChange={(event) => setReply(event.target.value)}
                  disabled={Boolean(selected.replyContent)}
                />
              </label>
            </div>

            <div className="flex justify-end gap-2 border-t px-5 py-4">
              <button
                onClick={() => setSelected(null)}
                className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                Đóng
              </button>
              {!selected.replyContent && (
                <button
                  onClick={handleReply}
                  disabled={submitting || !reply.trim()}
                  className="rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {submitting ? 'Đang gửi...' : 'Gửi phản hồi'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
