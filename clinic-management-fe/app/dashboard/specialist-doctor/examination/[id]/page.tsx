'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { useParams, useSearchParams } from 'next/navigation';
import { appointmentService, AppointmentDetail } from '@/services/appointment.service';
import { serviceOrderService, ServiceOrderItem } from '@/services/service-order.service';
import { formatAppointmentDate } from '@/lib/date';
import { Loader2, Paperclip, X, CheckCircle2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { SERVICE_ORDER_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const isAppointmentId = (value?: string | null) => (value ?? '').toUpperCase().startsWith('APT-');
const isOrderId = (value?: string | null) => (value ?? '').toUpperCase().startsWith('ORD-');
const normalizeOrderId = (value?: string | null) => (value ?? '').trim().toUpperCase();

const getResultFileUrls = (order: ServiceOrderItem) =>
  order.resultFileUrls && order.resultFileUrls.length > 0
    ? order.resultFileUrls
    : order.resultFileUrl
      ? [order.resultFileUrl]
      : [];

// ── Trạng thái badge ───────────────────────────────────────────────────────────
const StatusBadge = ({ status }: { status: string }) => {
  const normalizedStatus = status?.trim().toUpperCase();
  const map: Record<string, { label: string; cls: string }> = {
    PENDING: { label: SERVICE_ORDER_STATUS_LABELS.PENDING, cls: 'bg-amber-100 text-amber-700 border border-amber-300' },
    COMPLETED: { label: SERVICE_ORDER_STATUS_LABELS.COMPLETED, cls: 'bg-green-100 text-green-700 border border-green-300' },
    PAID: { label: SERVICE_ORDER_STATUS_LABELS.PAID, cls: 'bg-blue-100 text-blue-700 border border-blue-300' },
    CANCELLED: { label: SERVICE_ORDER_STATUS_LABELS.CANCELLED, cls: 'bg-red-100 text-red-700 border border-red-300' },
  };
  const { label, cls } = map[normalizedStatus] ?? {
    label: formatVietnameseLabel(status, SERVICE_ORDER_STATUS_LABELS),
    cls: 'bg-gray-100 text-gray-600 border border-gray-300',
  };
  return <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${cls}`}>{label}</span>;
};

// ── Card cho từng phiếu chỉ định ──────────────────────────────────────────────
function ServiceOrderCard({
  order,
  appt,
  onUpdated,
}: {
  order: ServiceOrderItem;
  appt: AppointmentDetail;
  onUpdated: () => void;
}) {
  const [noteInput, setNoteInput] = useState(order.resultNote ?? '');
  const [submitting, setSubmitting] = useState(false);
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const fileRef = useRef<HTMLInputElement>(null);

  const age = appt.patientDateOfBirth
    ? new Date().getFullYear() - new Date(appt.patientDateOfBirth).getFullYear()
    : null;

  const examDate = appt.appointmentDate
    ? formatAppointmentDate(appt.appointmentDate)
    : '—';

  const createdDate = order.createdAt
    ? new Date(order.createdAt).toLocaleDateString('vi-VN')
    : '—';
  const createdTime = order.createdAt
    ? new Date(order.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
    : '—';
  const uploadedFiles = getResultFileUrls(order);

  const handleFilePick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    setPendingFiles(prev => [...prev, ...files]);
    e.target.value = '';
  };

  const removePendingFile = (idx: number) =>
    setPendingFiles(prev => prev.filter((_, i) => i !== idx));

  // Xác nhận & trả kết quả: upload files (nếu có) + lưu nhận xét cùng lúc
  const handleConfirm = async () => {
    setSubmitting(true);
    try {
      // Upload tất cả file pending
      for (const file of pendingFiles) {
        await serviceOrderService.uploadResult(order.orderId, file);
      }
      // Lưu nhận xét
      await serviceOrderService.updateResultNote(order.orderId, noteInput);
      setPendingFiles([]);
      toast.success('Đã xác nhận & trả kết quả thành công!');
      onUpdated();
    } catch {
      toast.error('Lỗi khi xác nhận kết quả. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="border border-blue-200 rounded-xl bg-white shadow-sm overflow-hidden mb-8">
      {/* ── Tiêu đề phiếu */}
      <div className="py-4 text-center border-b border-blue-100 bg-blue-50/40">
        <h2 className="text-base font-bold tracking-widest uppercase text-gray-800">
          Phiếu Khám Cận Lâm Sàng
        </h2>
      </div>

      {/* ── Section: Thông tin bệnh nhân */}
      <div className="px-6 py-4 border-b border-blue-100">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Thông tin bệnh nhân</h3>
        <div className="grid grid-cols-2 gap-x-8 gap-y-2 text-sm text-gray-800">
          <div>
            <span className="font-medium">Họ tên: </span>
            <span>{appt.patientName}</span>
          </div>
          <div>
            <span className="font-medium">Mã bệnh nhân: </span>
            <span>{appt.patientId}</span>
          </div>
          <div>
            <span className="font-medium">Tuổi: </span>
            <span>{age ?? 'N/A'}</span>
          </div>
          <div>
            <span className="font-medium">Giới tính: </span>
            <span>—</span>
          </div>
          <div>
            <span className="font-medium">Lần khám: </span>
            <span>{examDate}</span>
          </div>
          <div>
            <span className="font-medium">Bác sĩ chỉ định: </span>
            <span>{appt.doctorName ?? '—'}</span>
          </div>
        </div>
      </div>

      {/* ── Section: Thông tin cận lâm sàng */}
      <div className="px-6 py-4 border-b border-blue-100">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Thông tin cận lâm sàng</h3>
        <div className="flex flex-col gap-2 text-sm text-gray-800">
          <div className="flex items-center gap-2">
            <span className="font-medium w-36 shrink-0">Loại xét nghiệm:</span>
            <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-blue-500 text-white">
              {order.serviceName}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="font-medium w-36 shrink-0">Hình thức:</span>
            <span>{order.serviceName}</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="font-medium w-36 shrink-0">Trạng thái:</span>
            <StatusBadge status={order.status} />
          </div>
        </div>
      </div>

      {/* ── Section: File kết quả cận lâm sàng */}
      <div className="px-6 py-4 border-b border-blue-100">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">File kết quả cận lâm sàng</h3>

        {/* Dropzone */}
        <div
          onClick={() => fileRef.current?.click()}
          className="border-2 border-dashed border-gray-300 rounded-lg p-3 flex items-center justify-center cursor-pointer hover:border-blue-400 hover:bg-blue-50 transition-colors mb-3"
        >
          <span className="text-sm text-blue-500 flex items-center gap-2">
            <Paperclip size={14} />
            Chọn file kết quả (PDF, JPG, PNG)
          </span>
          <input
            ref={fileRef}
            type="file"
            className="hidden"
            accept="image/*,.pdf"
            multiple
            onChange={handleFilePick}
          />
        </div>

        {/* File đã upload */}
        {uploadedFiles.length > 0 && (
          <ul className="space-y-1 mb-2">
            {uploadedFiles.map((fileUrl, index) => (
              <li key={fileUrl} className="flex items-center gap-1.5 text-sm text-gray-700">
                <span className="text-slate-500">•</span>
                <a
                  href={fileUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 underline hover:text-blue-800 truncate"
                >
                  {uploadedFiles.length > 1
                    ? `File ${index + 1}: ${fileUrl.split('/').pop() ?? 'Xem file kết quả'}`
                    : fileUrl.split('/').pop() ?? 'Xem file kết quả'}
                </a>
              </li>
            ))}
          </ul>
        )}

        {/* Files chờ upload */}
        {pendingFiles.length > 0 && (
          <ul className="space-y-1 mt-1">
            {pendingFiles.map((f, i) => (
              <li key={i} className="flex items-center gap-2 text-sm text-gray-700">
                <span className="text-slate-500">•</span>
                <span className="truncate max-w-xs">{f.name}</span>
                <button
                  onClick={() => removePendingFile(i)}
                  className="ml-auto text-slate-500 hover:text-red-500"
                >
                  <X size={13} />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* ── Section: Nhận xét của bác sĩ cận lâm sàng */}
      <div className="px-6 py-4 border-b border-blue-100">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Nhận xét của bác sĩ cận lâm sàng</h3>
        <textarea
          className="w-full border border-gray-200 rounded-lg px-3 py-2.5 text-sm text-gray-800 resize-none focus:outline-none focus:border-blue-400 min-h-[100px] bg-gray-50"
          placeholder="Nhập nhận xét / kết luận xét nghiệm..."
          value={noteInput}
          onChange={e => setNoteInput(e.target.value)}
        />
      </div>

      {/* ── Footer */}
      <div className="px-6 py-3 bg-gray-50 flex items-center justify-between text-sm text-gray-700 flex-wrap gap-3">
        <div className="flex flex-col gap-0.5">
          <div>
            <span className="font-medium">Bác sĩ/KTV thực hiện: </span>
            <span>{appt.doctorName ?? '—'}</span>
          </div>
          <div>
            <span className="font-medium">Thời gian trả kết quả: </span>
            <span>{createdDate} - {createdTime}</span>
          </div>
        </div>
        <button
          onClick={handleConfirm}
          disabled={submitting}
          className="flex items-center gap-1.5 bg-teal-600 hover:bg-teal-700 text-white text-sm font-medium px-4 py-2 rounded-lg disabled:opacity-50 transition-colors"
        >
          {submitting
            ? <Loader2 size={14} className="animate-spin" />
            : <CheckCircle2 size={14} />}
          Xác nhận & trả kết quả
        </button>
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function SpecialtyExaminationPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const routeId = params.id as string;
  const requestedOrderId = searchParams.get('orderId') || (isOrderId(routeId) ? routeId : null);

  const [appt, setAppt] = useState<AppointmentDetail | null>(null);
  const [serviceOrders, setServiceOrders] = useState<ServiceOrderItem[]>([]);
  const [pageLoading, setPageLoading] = useState(true);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [resolvedAppointmentId, setResolvedAppointmentId] = useState<string | null>(isAppointmentId(routeId) ? routeId : null);

  // ── Resolve route context (appointmentId/orderId) + load appointment detail ──
  useEffect(() => {
    if (!routeId) return;

    let isMounted = true;

    const loadAppointmentDetail = async () => {
      setPageLoading(true);

      try {
        let nextAppointmentId = isAppointmentId(routeId) ? routeId : null;

        if (!nextAppointmentId && requestedOrderId) {
          const selectedOrder = await serviceOrderService.getById(requestedOrderId);
          nextAppointmentId = selectedOrder.appointmentId ?? null;
        }

        if (!isMounted) {
          return;
        }

        setResolvedAppointmentId(nextAppointmentId);

        if (!nextAppointmentId) {
          setAppt(null);
          return;
        }

        const data = await appointmentService.getAppointmentDetail(nextAppointmentId);
        if (!isMounted) {
          return;
        }

        setAppt(data);
      } catch {
        if (isMounted) {
          setResolvedAppointmentId(null);
          setAppt(null);
        }
      } finally {
        if (isMounted) {
          setPageLoading(false);
        }
      }
    };

    void loadAppointmentDetail();
    return () => {
      isMounted = false;
    };
  }, [routeId, requestedOrderId]);

  const recordId = appt?.medicalRecord?.recordId;

  // ── Load service orders ───────────────────────────────────────────────────
  const fetchOrders = useCallback(() => {
    const mergeSelectedOrder = (orders: ServiceOrderItem[], selectedOrder: ServiceOrderItem | null) => {
      if (!selectedOrder || selectedOrder.status === 'CANCELLED') {
        return orders;
      }

      const selectedOrderId = normalizeOrderId(selectedOrder.orderId);
      if (!selectedOrderId) {
        return orders;
      }

      if (orders.some(order => normalizeOrderId(order.orderId) === selectedOrderId)) {
        return orders;
      }

      return [selectedOrder, ...orders];
    };

    const loadOrders = async () => {
      setOrdersLoading(true);

      try {
        const selectedOrder = requestedOrderId
          ? await serviceOrderService.getById(requestedOrderId).catch(() => null)
          : null;

        if (!resolvedAppointmentId) {
          setServiceOrders(selectedOrder && selectedOrder.status !== 'CANCELLED' ? [selectedOrder] : []);
          return;
        }

        const orders = await serviceOrderService.getMyQueueByAppointment(resolvedAppointmentId);
        const activeOrders = orders.filter(order => order.status !== 'CANCELLED');
        setServiceOrders(mergeSelectedOrder(activeOrders, selectedOrder));
      } catch {
        setServiceOrders([]);
      } finally {
        setOrdersLoading(false);
      }
    };

    void loadOrders();
  }, [requestedOrderId, resolvedAppointmentId]);

  useEffect(() => {
    const loadOrders = async () => {
      await fetchOrders();
    };

    void loadOrders();
  }, [fetchOrders]);
  // ── Loading / Error states ────────────────────────────────────────────────
  if (pageLoading) return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <Loader2 className="animate-spin text-teal-500" size={36} />
    </div>
  );

  if (!appt) return (
    <div className="p-8 text-center text-red-500 font-medium">
      Không tìm thấy thông tin lịch hẹn!
    </div>
  );

  const age = appt.patientDateOfBirth
    ? new Date().getFullYear() - new Date(appt.patientDateOfBirth).getFullYear()
    : null;

  const examDate = appt.appointmentDate
    ? formatAppointmentDate(appt.appointmentDate)
    : '—';

  const activeServiceOrders = serviceOrders.filter(order => order.status !== 'CANCELLED');
  return (
    <div className="min-h-screen bg-gray-50 pb-28">
      <DashboardHeader title="Khám cận lâm sàng" subtitle="Phiếu khám cận lâm sàng của bệnh nhân" />

      <main className="mx-auto max-w-3xl px-4 py-8 sm:px-6 text-gray-800">

        <div className="border border-gray-200 rounded-xl bg-white shadow-sm overflow-hidden mb-6">
          <div className="px-6 py-4">
            <h3 className="text-base font-semibold text-teal-600 mb-4">Thông tin bệnh nhân</h3>
            <div className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
              <div>
                <span className="font-medium">Họ tên: </span>
                <span>{appt.patientName}</span>
              </div>
              <div>
                <span className="font-medium">Mã bệnh nhân: </span>
                <span>{appt.patientId}</span>
              </div>
              <div>
                <span className="font-medium">Tuổi: </span>
                <span>{age ?? 'N/A'}</span>
              </div>
              <div>
                <span className="font-medium">Giới tính: </span>
                <span>—</span>
              </div>
              <div>
                <span className="font-medium">Lần khám: </span>
                <span>{examDate}</span>
              </div>
              <div>
                <span className="font-medium">Bác sĩ chỉ định: </span>
                <span>{appt.doctorName ?? '—'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Cảnh báo chưa có hồ sơ */}
        {!recordId && (
          <div className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-xl p-4 mb-6 flex items-start gap-2">
            <span className="text-lg leading-none">⚠</span>
            <div>
              <p className="font-semibold">Chưa có Hồ sơ khám (Medical Record).</p>
              <p className="mt-0.5 text-amber-600">
                Bệnh nhân cần hoàn tất thanh toán phí khám ban đầu để hệ thống tạo hồ sơ.
              </p>
            </div>
          </div>
        )}

        {/* Danh sách phiếu cận lâm sàng */}
        {ordersLoading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="animate-spin text-teal-400" size={28} />
          </div>
        ) : activeServiceOrders.length === 0 ? (
          <div className="rounded-xl border border-dashed border-gray-300 p-10 text-center text-sm text-slate-600">
            Chưa có chỉ định cận lâm sàng nào cho hồ sơ này.
          </div>
        ) : (
          activeServiceOrders.map(order => (
            <ServiceOrderCard
              key={order.orderId}
              order={order}
              appt={appt}
              onUpdated={fetchOrders}
            />
          ))
        )}
      </main>
    </div>
  );
}
