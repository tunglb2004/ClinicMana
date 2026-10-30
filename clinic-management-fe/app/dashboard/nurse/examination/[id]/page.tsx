'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { appointmentService, AppointmentDetail } from '@/services/appointment.service';
import { serviceOrderService, ServiceOrderItem } from '@/services/service-order.service';
import { Loader2, UploadCloud, FileText, Download } from 'lucide-react';
import { formatVietnameseLabel } from '@/lib/vietnamese-labels';

const getResultFileUrls = (order: ServiceOrderItem) =>
  order.resultFileUrls && order.resultFileUrls.length > 0
    ? order.resultFileUrls
    : order.resultFileUrl
      ? [order.resultFileUrl]
      : [];

const datePart = (value?: string | null) => {
  if (!value) return '';
  const [date] = value.split('T');
  return date || '';
};

const timePart = (value?: string | null) => {
  if (!value) return '';
  const [, time] = value.split('T');
  return time ? time.slice(0, 8) : '';
};

const buildAppointmentFromOrder = (routeId: string, order: ServiceOrderItem): AppointmentDetail => ({
  appointmentId: order.appointmentId || routeId,
  appointmentDate: datePart(order.createdAt),
  appointmentTime: timePart(order.createdAt),
  status: order.status || '',
  queueNumber: 0,
  notes: order.indicationNote || '',
  cancelReason: '',
  createdAt: order.createdAt || '',
  patientId: order.patientId || '',
  patientName: order.patientName || '',
  patientPhone: '',
  patientDateOfBirth: '',
  patientBloodType: '',
  patientHealthInsuranceNumber: '',
  patientAllergies: '',
  patientBackgroundDisease: '',
  doctorId: 0,
  doctorCode: '',
  doctorName: order.technicianId || '',
  departmentName: '',
  roomId: order.assignedRoomId || undefined,
  roomName: order.assignedRoomName || undefined,
  specialization: '',
  degree: '',
  services: [{
    serviceId: order.serviceId,
    serviceName: order.serviceName,
    price: order.price || 0,
    departmentType: 'PARACLINICAL',
    assignedRoomId: order.assignedRoomId || undefined,
    assignedRoomName: order.assignedRoomName || undefined,
  }],
  medicalRecord: order.recordId ? {
    recordId: order.recordId,
    symptoms: '',
    diagnosis: '',
    doctorNotes: '',
    recordStatus: '',
    recordCreatedAt: order.createdAt || '',
  } : null,
});

const orderBelongsToRoute = (routeId: string, order: ServiceOrderItem) =>
  order.orderId === routeId || !order.appointmentId || order.appointmentId === routeId;

const uniqueOrders = (orders: ServiceOrderItem[]) =>
  Array.from(new Map(orders.map(order => [order.orderId, order])).values());

// ── Status badge ──────────────────────────────────────────────────────────────
const OrderStatusBadge = ({ status }: { status: string }) => {
  const normalizedStatus = status?.trim().toUpperCase();
  const styles: Record<string, string> = {
    PAID: 'border-blue-400 text-blue-700 bg-blue-50',
    PENDING:   'border-yellow-400 text-yellow-700 bg-yellow-50',
    COMPLETED: 'border-green-500 text-green-700 bg-green-50',
    CANCELLED: 'border-red-300 text-red-600 bg-red-50',
  };
  const labels: Record<string, string> = {
    PAID: 'CHỜ THỰC HIỆN',
    PENDING:   'CHỜ TRẢ KẾT QUẢ',
    COMPLETED: 'ĐÃ CÓ KẾT QUẢ',
    CANCELLED: 'ĐÃ HỦY',
  };
  return (
    <span className={`border text-xs font-semibold px-2 py-1 rounded whitespace-nowrap ${styles[normalizedStatus] ?? 'border-gray-300 text-gray-600 bg-gray-50'}`}>
      {labels[normalizedStatus] ?? formatVietnameseLabel(status, labels).toLocaleUpperCase('vi-VN')}
    </span>
  );
};

// ── Service order row — Y tá upload file + ghi kết luận ──────────────────────
function NurseOrderRow({ order, onUpdated }: { order: ServiceOrderItem; onUpdated: () => void }) {
  const [noteInput, setNoteInput] = useState(order.resultNote ?? '');
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const resultFileUrls = getResultFileUrls(order);

  const handleSaveNote = async () => {
    setSaving(true);
    try { await serviceOrderService.updateResultNote(order.orderId, noteInput); onUpdated(); }
    catch (e) { console.error(e); }
    finally { setSaving(false); }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try { await serviceOrderService.uploadResult(order.orderId, file); onUpdated(); }
    catch (e) { console.error(e); }
    finally { setUploading(false); e.target.value = ''; }
  };

  const handleExport = async () => {
    try {
      const blob = await serviceOrderService.exportPdf(order.orderId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `Result_${order.orderId}.pdf`; a.click();
      URL.revokeObjectURL(url);
    } catch (e) { console.error(e); }
  };

  return (
    <div className="border rounded-lg p-4 bg-gray-50 flex flex-col gap-3">
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div className="flex flex-col gap-1">
          <span className="font-semibold text-sm text-black">
            Loại xét nghiệm: <span className="font-normal">{order.serviceName}</span>
          </span>
          <span className="text-xs text-gray-500">Mã chỉ định: {order.orderId}</span>
          {resultFileUrls.length > 0 && (
            <div className="flex flex-col gap-1">
              {resultFileUrls.map((fileUrl, index) => (
                <a key={fileUrl} href={fileUrl} target="_blank" rel="noopener noreferrer"
                  className="text-xs text-blue-600 underline flex items-center gap-1 w-fit">
                  <FileText size={12} /> {resultFileUrls.length > 1 ? `Xem file kết quả ${index + 1}` : 'Xem file kết quả'}
                </a>
              ))}
            </div>
          )}
        </div>
        <OrderStatusBadge status={order.status} />
      </div>

      <div className="flex flex-col gap-1">
        <label className="text-xs font-medium text-gray-600">Mô tả / Kết luận kỹ thuật</label>
        <textarea
          className="border rounded-md px-3 py-2 text-sm resize-none focus:outline-none focus:border-gray-400 bg-white text-black"
          rows={2}
          value={noteInput}
          onChange={e => setNoteInput(e.target.value)}
          placeholder="Nhập kết luận xét nghiệm..."
        />
      </div>

      <div className="flex items-center gap-2 flex-wrap">
        <button onClick={handleSaveNote} disabled={saving}
          className="flex items-center gap-1 border border-black px-3 py-1.5 rounded text-xs font-medium hover:bg-gray-100 disabled:opacity-50 text-black">
          {saving && <Loader2 size={12} className="animate-spin" />} Lưu kết luận
        </button>
        <button onClick={() => fileRef.current?.click()} disabled={uploading}
          className="flex items-center gap-1 border border-black px-3 py-1.5 rounded text-xs font-medium hover:bg-gray-100 disabled:opacity-50 text-black">
          {uploading ? <Loader2 size={12} className="animate-spin" /> : <UploadCloud size={12} />} Tải file kết quả
        </button>
        <input ref={fileRef} type="file" className="hidden" accept="image/*,.pdf" onChange={handleFileChange} />
        <button onClick={handleExport}
          className="flex items-center gap-1 border border-black px-3 py-1.5 rounded text-xs font-medium hover:bg-gray-100 text-black">
          <Download size={12} /> Xuất PDF
        </button>
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function NurseExaminationPage() {
  const params = useParams();
  const router = useRouter();
  const searchParams = useSearchParams();
  const appointmentId = params.id as string;
  const focusedOrderId = searchParams.get('orderId');

  const [appt, setAppt] = useState<AppointmentDetail | null>(null);
  const [serviceOrders, setServiceOrders] = useState<ServiceOrderItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [ordersLoading, setOrdersLoading] = useState(false);

  // Load appointment detail (1 call → patient info + recordId)
  useEffect(() => {
    if (!appointmentId) return;
    let active = true;

    const loadAppointment = async () => {
      try {
        const data = await appointmentService.getAppointmentDetail(appointmentId);
        if (active) setAppt(data);
      } catch (err) {
        console.error('Error fetching appointment:', err);
        if (!focusedOrderId) {
          if (active) setAppt(null);
          return;
        }

        try {
          const order = await serviceOrderService.getById(focusedOrderId);
          if (!orderBelongsToRoute(appointmentId, order)) {
            throw new Error('Service order does not belong to route appointment');
          }
          if (active) setAppt(buildAppointmentFromOrder(appointmentId, order));
        } catch (orderErr) {
          console.error('Error fetching service order fallback:', orderErr);
          if (active) setAppt(null);
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    void loadAppointment();

    return () => {
      active = false;
    };
  }, [appointmentId, focusedOrderId]);

  // Load service orders khi có recordId
  const recordId = appt?.medicalRecord?.recordId;

  const fetchOrders = useCallback(() => {
    if (!recordId) { setServiceOrders([]); return; }
    setOrdersLoading(true);
    serviceOrderService.getMyQueueByAppointment(appointmentId)
      .then(async (orders) => {
        if (!focusedOrderId || orders.some(order => order.orderId === focusedOrderId)) {
          setServiceOrders(orders);
          return;
        }

        try {
          const focusedOrder = await serviceOrderService.getById(focusedOrderId);
          const sameRecord = focusedOrder.recordId === recordId;
          const sameAppointment = !focusedOrder.appointmentId || focusedOrder.appointmentId === appointmentId;
          setServiceOrders(sameRecord && sameAppointment
            ? uniqueOrders([focusedOrder, ...orders])
            : orders);
        } catch {
          setServiceOrders(orders);
        }
      })
      .catch(async () => {
        if (!focusedOrderId) {
          setServiceOrders([]);
          return;
        }

        try {
          const focusedOrder = await serviceOrderService.getById(focusedOrderId);
          setServiceOrders(focusedOrder.recordId === recordId ? [focusedOrder] : []);
        } catch {
          setServiceOrders([]);
        }
      })
      .finally(() => setOrdersLoading(false));
  }, [appointmentId, focusedOrderId, recordId]);

  useEffect(() => {
    let active = true;

    const loadOrders = async () => {
      if (!recordId) {
        if (active) setServiceOrders([]);
        return;
      }
      await fetchOrders();
    };

    void loadOrders();

    return () => {
      active = false;
    };
  }, [recordId, fetchOrders]);

  if (loading) return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <Loader2 className="animate-spin text-slate-500" size={32} />
    </div>
  );

  if (!appt) return (
    <div className="p-8 text-center text-red-500">Không tìm thấy thông tin lịch hẹn!</div>
  );

  const age = appt.patientDateOfBirth
    ? new Date().getFullYear() - new Date(appt.patientDateOfBirth).getFullYear()
    : null;
  const activeServiceOrders = serviceOrders.filter(order => order.status !== 'CANCELLED');

  return (
    <div className="min-h-screen bg-gray-50 pb-24 text-black">
      <DashboardHeader
        title="Tiếp nhận & Cập nhật kết quả"
        subtitle="Hỗ trợ cập nhật kết quả cận lâm sàng"
      />

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">

        {/* ── THÔNG TIN BỆNH NHÂN ─────────────────────────────── */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4 uppercase">Thông tin bệnh nhân</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mb-4">
            <div><span className="font-medium">Mã lịch hẹn:</span> {appt.appointmentId}</div>
            <div><span className="font-medium">Họ tên:</span> {appt.patientName}</div>
            <div><span className="font-medium">Giới tính:</span> Nam</div>
            <div><span className="font-medium">Tuổi:</span> {age ?? 'N/A'}</div>
            <div><span className="font-medium">SĐT:</span> {appt.patientPhone || 'N/A'}</div>
            <div><span className="font-medium">BHYT:</span> {appt.patientHealthInsuranceNumber || 'Không có'}</div>
          </div>
          <div className="text-sm space-y-2">
            <div><span className="font-medium">Lý do khám:</span> {appt.notes || appt.patientBackgroundDisease || 'Không ghi nhận'}</div>
            <div><span className="font-medium">Dị ứng:</span> {appt.patientAllergies || 'Không ghi nhận'}</div>
            <div className="flex items-center gap-2">
              <span className="font-medium">Trạng thái:</span>
              <span className="border text-xs font-medium px-2 py-1 rounded border-black text-black">ĐANG KHÁM</span>
            </div>
          </div>
        </div>

        {/* ── KẾT QUẢ CẬN LÂM SÀNG ───────────────────────────── */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4 uppercase">Cập nhật kết quả chỉ định cận lâm sàng</h2>

          {!recordId && (
            <div className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded p-3 mb-4">
              ⚠ Bệnh nhân này chưa được bác sĩ khởi tạo hồ sơ khám (Medical Record). Hãy đợi bác sĩ tạo chỉ định trước.
            </div>
          )}

          {ordersLoading ? (
            <div className="flex justify-center py-6"><Loader2 className="animate-spin text-slate-500" size={24} /></div>
          ) : activeServiceOrders.length === 0 ? (
            <div className="py-4 text-sm italic text-slate-600">
              {recordId ? 'Chưa có chỉ định cận lâm sàng nào từ bác sĩ.' : 'Chưa có hồ sơ khám.'}
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              {activeServiceOrders.map(order => (
                <NurseOrderRow key={order.orderId} order={order} onUpdated={fetchOrders} />
              ))}
            </div>
          )}
        </div>

      </main>

      {/* ── Floating Action Bar ──────────────────────────────── */}
      <div className="fixed bottom-0 left-0 right-0 border-t bg-white p-4 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)] z-20">
        <div className="mx-auto max-w-7xl flex justify-between items-center px-4 sm:px-6 lg:px-8">
          <button
            onClick={() => router.push('/dashboard/nurse/patients')}
            className="text-black font-medium hover:underline text-sm"
          >
            ← Quay lại danh sách
          </button>
          <div className="flex gap-3">
            <button className="border border-black rounded px-6 py-2 text-sm font-medium hover:bg-gray-50 bg-white text-black">
              LƯU TẠM HỘ
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
