'use client';

import { useState, useEffect, useCallback } from 'react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { getNormalizedResultFileUrls, getResultFileName, triggerResultFileDownload } from '@/lib/result-file';
import { useParams, useSearchParams } from 'next/navigation';
import { appointmentService, AppointmentDetail } from '@/services/appointment.service';
import { prescriptionService, PrescriptionSavePayload } from '@/services/prescription.service';
import { serviceOrderService, ServiceOrderItem } from '@/services/service-order.service';
import { catalogService, getUnavailableServiceAddMessage, ServiceItem } from '@/services/payment.service';
import { openParaclinicalOrderPrintWindow, printParaclinicalOrderSlip } from '@/lib/paraclinical-order-print';
import { openExaminationResultPrintWindow, printExaminationResultSlip } from '@/lib/examination-result-print';
import { ExaminationResultResponse, PrescriptionMedicine } from '@/types/examination.types';
import { Loader2, FileText, Download, ImageIcon, Printer, Save } from 'lucide-react';
import { PAYMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const normalizeServiceId = (serviceId?: string | null) => (serviceId ?? '').trim().toUpperCase();
const normalizeDepartmentType = (departmentType?: string | null) => (departmentType ?? '').trim().toUpperCase();
const isParaclinicalDepartment = (departmentType?: string | null) => normalizeDepartmentType(departmentType) === 'PARACLINICAL';
const resolvePaymentState = (paymentStatus?: string | null, orderStatus?: string | null) => {
  const normalizedPaymentStatus = (paymentStatus ?? '').trim().toUpperCase();
  if (normalizedPaymentStatus) {
    return normalizedPaymentStatus;
  }

  return (orderStatus ?? '').trim().toUpperCase() === 'PAID' ? 'PAID' : 'UNPAID';
};

interface SelectedServiceTableRow {
  key: string;
  serviceId: string;
  serviceName: string;
  source: 'draft' | 'submitted';
  orderId?: string;
  paymentStatus?: string | null;
  orderStatus?: string;
  createdAt?: string;
}

interface ParaclinicalResultItem {
  key: string;
  source: 'service-order' | 'appointment-booked';
  serviceId: string;
  serviceName: string;
  status: string;
  orderId?: string;
  paymentStatus?: string | null;
  createdAt?: string;
  resultNote?: string | null;
  resultFileUrl?: string | null;
  resultFileUrls?: string[];
  assignedRoomId?: string | null;
  assignedRoomName?: string | null;
  assignedDoctorId?: string | null;
  assignedDoctorName?: string | null;
}

interface PrescriptionDraftRow {
  key: string;
  medicineName: string;
  dosage: string;
  quantity: string;
}

const createPrescriptionDraftRow = (seed?: Partial<Omit<PrescriptionDraftRow, 'key'>>): PrescriptionDraftRow => ({
  key: `prescription-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  medicineName: seed?.medicineName ?? '',
  dosage: seed?.dosage ?? '',
  quantity: seed?.quantity ?? '',
});

const printPrescriptionHtml = (html: string) => {
  if (typeof window === 'undefined') return false;

  const frame = document.createElement('iframe');
  frame.style.position = 'fixed';
  frame.style.right = '0';
  frame.style.bottom = '0';
  frame.style.width = '0';
  frame.style.height = '0';
  frame.style.border = '0';
  frame.setAttribute('aria-hidden', 'true');
  document.body.appendChild(frame);

  const frameWindow = frame.contentWindow;
  const frameDocument = frame.contentDocument ?? frameWindow?.document;
  if (!frameWindow || !frameDocument) {
    frame.remove();
    return false;
  }

  const cleanup = () => {
    window.setTimeout(() => frame.remove(), 500);
  };

  frameWindow.onafterprint = cleanup;
  frameDocument.open();
  frameDocument.write(html);
  frameDocument.close();
  window.setTimeout(() => {
    frameWindow.focus();
    frameWindow.print();
    cleanup();
  }, 250);
  return true;
};

// ── Symptoms JSON helpers ─────────────────────────────────────────────────────
interface SymptomsData {
  mach?: string;
  huyetAp?: string;
  nhietDo?: string;
  nhipTim?: string;
  canNang?: string;
  chieuCao?: string;
  daiTien?: string;
  tieuTien?: string;
  moTa?: string;
}

const parseSymptomsJson = (raw?: string | null): SymptomsData => {
  if (!raw) return {};
  try { return JSON.parse(raw) as SymptomsData; } catch { return { moTa: raw }; }
};

const buildSymptomsJson = (d: SymptomsData): string => JSON.stringify(d);

interface ClinicalInputField {
  label: string;
  value: string;
  setter: (value: string) => void;
  pattern: RegExp;
  errorMsg: string;
  placeholder: string;
  unit?: string;
}

// ── Status badge ──────────────────────────────────────────────────────────────
const OrderStatusBadge = ({ status }: { status: string }) => {
  const normalizedStatus = status?.trim().toUpperCase();
  const styles: Record<string, string> = {
    PENDING:   'border-yellow-400 text-yellow-700 bg-yellow-50',
    PAID: 'border-yellow-400 text-yellow-700 bg-yellow-50',
    COMPLETED: 'border-green-500 text-green-700 bg-green-50',
    CANCELLED: 'border-red-300 text-red-600 bg-red-50',
  };
  const labels: Record<string, string> = {
    PENDING: 'CHỜ TRẢ KẾT QUẢ',
    PAID: 'CHỜ KẾT QUẢ',
    COMPLETED: 'ĐÃ CÓ KẾT QUẢ',
    CANCELLED: 'ĐÃ HỦY',
  };
  return (
    <span className={`border text-xs font-semibold px-2 py-1 rounded whitespace-nowrap ${styles[normalizedStatus] ?? 'border-gray-300 text-gray-600 bg-gray-50'}`}>
      {labels[normalizedStatus] ?? 'TRẠNG THÁI KHÁC'}
    </span>
  );
};

const SelectionStateBadge = ({ row }: { row: SelectedServiceTableRow }) => {
  if (row.source === 'draft') {
    return (
      <span className="rounded-full border border-gray-300 bg-white px-2 py-1 text-xs font-semibold text-gray-700">
        MỚI CHỌN
      </span>
    );
  }

  const paymentStatus = resolvePaymentState(row.paymentStatus, row.orderStatus);
  const config: Record<string, string> = {
    PAID: 'border-green-300 bg-green-50 text-green-700',
    PENDING: 'border-amber-300 bg-amber-50 text-amber-700',
    UNPAID: 'border-amber-300 bg-amber-50 text-amber-700',
  };
  const label = paymentStatus === 'PAID'
    ? 'ĐÃ GỬI - ĐÃ THANH TOÁN'
    : paymentStatus === 'PENDING'
      ? 'ĐÃ GỬI - CHỜ XÁC NHẬN TT'
      : `ĐÃ GỬI - ${formatVietnameseLabel(paymentStatus, PAYMENT_STATUS_LABELS).toLocaleUpperCase('vi-VN')}`;

  return (
    <span className={`rounded-full border px-2 py-1 text-xs font-semibold ${config[paymentStatus] ?? 'border-gray-300 bg-gray-50 text-gray-700'}`}>
      {label}
    </span>
  );
};

// ── Helpers for file type detection ───────────────────────────────────────────
const isImageUrl = (url: string) => /\.(jpg|jpeg|png|gif|webp)(\?|$)/i.test(url);
const isPdfUrl = (url: string) => /\.pdf(\?|$)/i.test(url);
const getResultFileUrls = (order: { resultFileUrls?: string[]; resultFileUrl?: string | null }) =>
  getNormalizedResultFileUrls(order);
const hasResultContent = (order: { resultNote?: string | null; resultFileUrls?: string[]; resultFileUrl?: string | null }) =>
  Boolean(order.resultNote) || getResultFileUrls(order).length > 0;

// ── Service order row (read-only: view + download only) ───────────────────────
function ServiceOrderRow({ order }: { order: ParaclinicalResultItem }) {
  const [showPreview, setShowPreview] = useState(false);
  const [selectedPreviewUrl, setSelectedPreviewUrl] = useState<string | null>(null);

  const resultFileUrls = getResultFileUrls(order);
  const latestFileUrl = resultFileUrls[resultFileUrls.length - 1] ?? null;
  const fileUrl = selectedPreviewUrl && resultFileUrls.includes(selectedPreviewUrl)
    ? selectedPreviewUrl
    : latestFileUrl;
  const fileIsImage = fileUrl ? isImageUrl(fileUrl) : false;
  const fileIsPdf = fileUrl ? isPdfUrl(fileUrl) : false;

  const handleDownloadResultFile = (downloadUrl?: string | null) => {
    if (!downloadUrl) {
      alert('Chưa có file kết quả để tải.');
      return;
    }

    triggerResultFileDownload(downloadUrl, order.serviceName || `KetQua_${order.orderId ?? order.serviceId}`);
  };

  return (
    <div className="border rounded-lg p-4 bg-gray-50 flex flex-col gap-3">
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div className="flex flex-col gap-1">
          <span className="font-semibold text-sm text-black">
            Loại xét nghiệm: <span className="font-normal">{order.serviceName}</span>
          </span>
          <span className="text-xs text-gray-500">
            Ngày: {order.createdAt ? new Date(order.createdAt).toLocaleDateString('vi-VN') : '—'}
          </span>
        </div>
        <OrderStatusBadge status={order.status} />
      </div>

      {/* File result preview */}
      {resultFileUrls.length > 0 && (
        <div className="flex flex-col gap-2">
          <div className="flex flex-col gap-2">
            {resultFileUrls.map((resultUrl, index) => {
              const isSelected = resultUrl === fileUrl;
              const resultIsPdf = isPdfUrl(resultUrl);
              return (
                <div key={resultUrl} className="flex items-center gap-2 flex-wrap">
                  <button
                    onClick={() => {
                      setSelectedPreviewUrl(resultUrl);
                      setShowPreview(true);
                    }}
                    className={`flex items-center gap-1 text-xs font-medium ${isSelected ? 'text-blue-800' : 'text-blue-600 hover:text-blue-800'}`}
                  >
                    {isImageUrl(resultUrl) ? <ImageIcon size={12} /> : <FileText size={12} />}
                    {resultFileUrls.length > 1 ? `Xem file ${index + 1}` : 'Xem kết quả'}
                  </button>
                  <a href={resultUrl} target="_blank" rel="noopener noreferrer"
                    className="text-xs text-gray-500 underline hover:text-gray-700">
                    Mở tab mới
                  </a>
                  <button onClick={() => handleDownloadResultFile(resultUrl)}
                    className="flex items-center gap-1 border border-black px-3 py-1.5 rounded text-xs font-medium hover:bg-gray-100">
                    <Download size={12} />
                    {resultIsPdf ? 'Tải PDF' : 'Tải file'}
                  </button>
                  <span className="text-xs text-gray-500 truncate max-w-[260px]">
                    {getResultFileName(resultUrl, order.serviceName || `KetQua_${order.orderId ?? order.serviceId}`, index)}
                  </span>
                </div>
              );
            })}
          </div>
          {fileUrl && (
            <>
              <div className="flex items-center gap-2">
            <button onClick={() => setShowPreview(p => !p)}
              className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-800 font-medium">
              {fileIsImage ? <ImageIcon size={12} /> : <FileText size={12} />}
              {showPreview ? 'Ẩn xem trước' : 'Xem kết quả'}
            </button>
            <a href={fileUrl} target="_blank" rel="noopener noreferrer"
              className="text-xs text-gray-500 underline hover:text-gray-700">
              Mở tab mới
            </a>
          </div>
          {showPreview && (
            <div className="border rounded-lg overflow-hidden bg-white">
              {fileIsImage ? (
                <img src={fileUrl} alt="Kết quả cận lâm sàng" className="max-w-full max-h-[500px] object-contain mx-auto" />
              ) : fileIsPdf ? (
                <iframe src={fileUrl} className="w-full h-[500px]" title="Kết quả PDF" />
              ) : (
                <a href={fileUrl} target="_blank" rel="noopener noreferrer"
                  className="block p-4 text-sm text-blue-600 underline">
                  <FileText size={14} className="inline mr-1" /> Tải/xem file kết quả
                </a>
              )}
            </div>
          )}
            </>
          )}
        </div>
      )}

      {!hasResultContent(order) && (
        <div className="rounded-md border border-dashed border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {order.source === 'appointment-booked'
            ? 'Dịch vụ này đã được đặt sẵn từ lịch hẹn ban đầu và đang chờ bác sĩ cận lâm sàng trả kết quả.'
            : 'Dịch vụ này đã được gửi sang cận lâm sàng và đang chờ trả kết quả.'}
        </div>
      )}

      {order.resultNote && (
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-gray-600">Mô tả / Kết luận</label>
          <p className="border rounded-md px-3 py-2 text-sm bg-white text-black">{order.resultNote}</p>
        </div>
      )}

    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function ExaminationPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const appointmentId = params.id as string; // APT-xxx
  const initialRecordId = searchParams.get('recordId') || undefined;

  const [appt, setAppt] = useState<AppointmentDetail | null>(null);
  const [serviceOrders, setServiceOrders] = useState<ServiceOrderItem[]>([]);
  const [availableServices, setAvailableServices] = useState<ServiceItem[]>([]);
  const [selectedServiceIds, setSelectedServiceIds] = useState<string[]>([]);
  const [pageLoading, setPageLoading] = useState(true);
  const [ordersLoadedOnce, setOrdersLoadedOnce] = useState(false);
  const [cancelingOrderId, setCancelingOrderId] = useState<string | null>(null);
  const [indicationNote, setIndicationNote] = useState('');

  // Clinical form
  const [mach, setMach] = useState('');
  const [huyetAp, setHuyetAp] = useState('');
  const [nhietDo, setNhietDo] = useState('');
  const [nhipTim, setNhipTim] = useState('');
  const [canNang, setCanNang] = useState('');
  const [chieuCao, setChieuCao] = useState('');
  const [daiTien, setDaiTien] = useState('');
  const [tieuTien, setTieuTien] = useState('');
  const [moTa, setMoTa] = useState('');
  const [ghiChuChuyen, setGhiChuChuyen] = useState('');
  const [ketLuan, setKetLuan] = useState('');
  const [prescriptionId, setPrescriptionId] = useState<string | null>(null);
  const [prescriptionRows, setPrescriptionRows] = useState<PrescriptionDraftRow[]>([createPrescriptionDraftRow()]);
  const [prescriptionNotes, setPrescriptionNotes] = useState('');
  const [prescriptionLoading, setPrescriptionLoading] = useState(false);
  const [prescriptionSaving, setPrescriptionSaving] = useState(false);
  const [prescriptionPrinting, setPrescriptionPrinting] = useState(false);
  const [prescriptionFeedback, setPrescriptionFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [previousRecordDetail, setPreviousRecordDetail] = useState<ExaminationResultResponse | null>(null);
  const [previousRecordOrders, setPreviousRecordOrders] = useState<ServiceOrderItem[]>([]);
  const [previousRecordLoading, setPreviousRecordLoading] = useState(false);

  // Load appointment detail and use its medical record as the canonical source.
  useEffect(() => {
    if (!appointmentId) return;
    setPageLoading(true);
    appointmentService.getAppointmentDetail(appointmentId)
      .then(data => {
        setAppt(data);
        // Pre-fill from medical record if exists
        if (data.medicalRecord) {
          setKetLuan(data.medicalRecord.diagnosis ?? '');
          const s = parseSymptomsJson(data.medicalRecord.symptoms);
          setMach(s.mach ?? '');
          setHuyetAp(s.huyetAp ?? '');
          setNhietDo(s.nhietDo ?? '');
          setNhipTim(s.nhipTim ?? '');
          setCanNang(s.canNang ?? '');
          setChieuCao(s.chieuCao ?? '');
          setDaiTien(s.daiTien ?? '');
          setTieuTien(s.tieuTien ?? '');
          setMoTa(s.moTa ?? '');
        }
      })
      .catch(() => setAppt(null))
      .finally(() => setPageLoading(false));
  }, [appointmentId]);

  // Prefer the medical record attached to the current appointment over any stale query param.
  const recordId = appt?.medicalRecord?.recordId || initialRecordId;
  const previousRecordId = appt?.followUpSourceRecordId || appt?.followUpSourceRecord?.recordId || null;

  useEffect(() => {
    if (!previousRecordId) {
      setPreviousRecordDetail(null);
      setPreviousRecordOrders([]);
      return;
    }

    let cancelled = false;
    setPreviousRecordLoading(true);
    Promise.all([
      prescriptionService.getMedicalRecordDetail(previousRecordId),
      serviceOrderService.getByRecord(previousRecordId),
    ])
      .then(([detail, orders]) => {
        if (cancelled) return;
        setPreviousRecordDetail(detail);
        setPreviousRecordOrders(orders ?? []);
      })
      .catch(() => {
        if (cancelled) return;
        setPreviousRecordDetail(null);
        setPreviousRecordOrders([]);
      })
      .finally(() => {
        if (!cancelled) setPreviousRecordLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [previousRecordId]);

  useEffect(() => {
    if (!recordId) {
      setPrescriptionId(null);
      setPrescriptionNotes('');
      setPrescriptionRows([createPrescriptionDraftRow()]);
      return;
    }

    let cancelled = false;
    setPrescriptionLoading(true);
    setPrescriptionFeedback(null);

    prescriptionService.getMedicalRecordDetail(recordId)
      .then((recordDetail) => {
        if (cancelled) {
          return;
        }

        const existingPrescription = recordDetail.prescription;
        setPrescriptionId(existingPrescription?.prescriptionId ?? null);
        setPrescriptionNotes(existingPrescription?.notes ?? '');
        if (existingPrescription?.details?.length) {
          setPrescriptionRows(
            existingPrescription.details.map((detail) => createPrescriptionDraftRow({
              medicineName: detail.medicineName ?? '',
              dosage: detail.dosage ?? detail.note ?? '',
              quantity: detail.quantity ?? '',
            }))
          );
          return;
        }

        setPrescriptionRows([createPrescriptionDraftRow()]);
      })
      .catch(() => {
        if (cancelled) {
          return;
        }

        setPrescriptionId(null);
        setPrescriptionNotes('');
        setPrescriptionRows([createPrescriptionDraftRow()]);
      })
      .finally(() => {
        if (!cancelled) {
          setPrescriptionLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [recordId]);

  const fetchOrders = useCallback(() => {
    if (!appointmentId && !recordId) {
      setServiceOrders([]);
      setOrdersLoadedOnce(true);
      return;
    }
    const request = appointmentId
      ? serviceOrderService.getByAppointment(appointmentId)
      : serviceOrderService.getByRecord(recordId!);

    request
      .then(orders => {
        setServiceOrders(orders);
        const activeOrders = orders.filter(order => order.status !== 'CANCELLED');
        const nextHiddenServiceIds = new Set(
          activeOrders.map(order => normalizeServiceId(order.serviceId)).filter(Boolean)
        );
        setSelectedServiceIds(prev => prev.filter(serviceId => !nextHiddenServiceIds.has(normalizeServiceId(serviceId))));
      })
      .catch(() => setServiceOrders([]))
      .finally(() => {
        setOrdersLoadedOnce(true);
      });
  }, [appointmentId, recordId]);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  // Load catalog for specialty referral
  useEffect(() => {
    catalogService.getServices('PARACLINICAL')
      .then(setAvailableServices)
      .catch(() => setAvailableServices([]));
  }, []);

  const addSelectedService = (id: string) => {
    const service = availableServices.find(candidate => candidate.serviceId === id);
    if (service?.hasOperationalRoom === false) {
      alert(getUnavailableServiceAddMessage(service));
      return;
    }

    setSelectedServiceIds(prev => (prev.includes(id) ? prev : [...prev, id]));
  };

  const removeSelectedService = (id: string) =>
    setSelectedServiceIds(prev => prev.filter(serviceId => serviceId !== id));

  const handleCancelOrder = async (orderId: string) => {
    if (!window.confirm('Hủy chỉ định này? Dịch vụ chưa thanh toán sẽ bị gỡ khỏi danh sách chờ thu tiền.')) {
      return;
    }

    setCancelingOrderId(orderId);
    try {
      await serviceOrderService.cancel(orderId);
      fetchOrders();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      alert(err?.response?.data?.message || 'Không thể hủy chỉ định này.');
    } finally {
      setCancelingOrderId(null);
    }
  };

  const activeServiceOrders = serviceOrders.filter(order => order.status !== 'CANCELLED');
  const paraclinicalCatalogServiceIds = new Set(
    availableServices.map(service => normalizeServiceId(service.serviceId)).filter(Boolean)
  );
  const bookedParaclinicalServices = (appt?.services ?? []).filter(service =>
    isParaclinicalDepartment(service.departmentType)
      || paraclinicalCatalogServiceIds.has(normalizeServiceId(service.serviceId))
  );
  const bookedParaclinicalServiceIds = new Set(
    bookedParaclinicalServices.map(service => normalizeServiceId(service.serviceId)).filter(Boolean)
  );
  const activeParaclinicalOrders = activeServiceOrders.filter(order => {
    const serviceId = normalizeServiceId(order.serviceId);
    return serviceId.length > 0
      && (bookedParaclinicalServiceIds.has(serviceId) || paraclinicalCatalogServiceIds.has(serviceId));
  });
  const activeParaclinicalOrderServiceIds = new Set(
    activeParaclinicalOrders.map(order => normalizeServiceId(order.serviceId)).filter(Boolean)
  );
  const visibleParaclinicalResultOrders = activeParaclinicalOrders.filter(order =>
    hasResultContent(order) || resolvePaymentState(order.paymentStatus, order.status) === 'PAID'
  );
  const bookedResultsWithoutOrders: ParaclinicalResultItem[] = bookedParaclinicalServices
    .filter(service => !activeParaclinicalOrderServiceIds.has(normalizeServiceId(service.serviceId)))
    .map(service => ({
      key: `booked-${service.serviceId}`,
      source: 'appointment-booked',
      serviceId: service.serviceId,
      serviceName: service.serviceName,
      status: 'PENDING',
      paymentStatus: 'PAID',
      createdAt: appt?.createdAt,
      resultNote: null,
      resultFileUrl: null,
      resultFileUrls: [],
      assignedRoomId: service.assignedRoomId,
      assignedRoomName: service.assignedRoomName,
      assignedDoctorId: service.assignedDoctorId,
      assignedDoctorName: service.assignedDoctorName,
    }));
  const paraclinicalResultItems: ParaclinicalResultItem[] = [
    ...visibleParaclinicalResultOrders.map(order => ({
      key: `order-${order.orderId}`,
      source: 'service-order' as const,
      serviceId: order.serviceId,
      serviceName: order.serviceName,
      status: order.status,
      orderId: order.orderId,
      paymentStatus: order.paymentStatus,
      createdAt: order.createdAt,
      resultNote: order.resultNote,
      resultFileUrl: order.resultFileUrl,
      resultFileUrls: order.resultFileUrls,
      assignedRoomId: order.assignedRoomId,
      assignedRoomName: order.assignedRoomName,
    })),
    ...bookedResultsWithoutOrders,
  ].sort((left, right) => {
    const leftHasResult = hasResultContent(left);
    const rightHasResult = hasResultContent(right);
    if (leftHasResult !== rightHasResult) {
      return leftHasResult ? 1 : -1;
    }

    if (left.createdAt && right.createdAt) {
      return right.createdAt.localeCompare(left.createdAt);
    }

    if (left.createdAt) {
      return -1;
    }

    if (right.createdAt) {
      return 1;
    }

    return left.serviceName.localeCompare(right.serviceName, 'vi');
  });
  const waitingResultItems = paraclinicalResultItems.filter(item => !hasResultContent(item));
  const hasActiveParaclinicalWork = activeParaclinicalOrders.some(order => {
    const status = (order.status ?? '').toUpperCase();
    return status !== 'COMPLETED' && status !== 'CANCELLED';
  });
  const hiddenServiceIds = new Set([
    ...Array.from(bookedParaclinicalServiceIds),
    ...Array.from(activeParaclinicalOrderServiceIds),
  ]);
  const submittedPendingSelectionOrders = activeParaclinicalOrders.filter(order => {
    if (hasResultContent(order)) {
      return false;
    }

    return resolvePaymentState(order.paymentStatus, order.status) !== 'PAID';
  });
  const selectedServices = availableServices.filter(service => selectedServiceIds.includes(service.serviceId));
  const unselectedServices = availableServices.filter(
    service => !hiddenServiceIds.has(normalizeServiceId(service.serviceId))
  );
  const selectableServices = unselectedServices.filter(
    service => !selectedServiceIds.includes(service.serviceId)
  );
  const selectedServiceRows: SelectedServiceTableRow[] = [
    ...selectedServices.map(service => ({
      key: `draft-${service.serviceId}`,
      serviceId: service.serviceId,
      serviceName: service.serviceName,
      source: 'draft' as const,
    })),
    ...submittedPendingSelectionOrders.map(order => ({
      key: `submitted-${order.orderId}`,
      serviceId: order.serviceId,
      serviceName: order.serviceName,
      source: 'submitted' as const,
      orderId: order.orderId,
      paymentStatus: order.paymentStatus,
      orderStatus: order.status,
      createdAt: order.createdAt,
    })),
  ];
  const serviceSelectionLoading = Boolean(appointmentId || recordId) && !ordersLoadedOnce;

  const handleCreateOrders = async () => {
    if (!recordId) {
      alert('Chưa có hồ sơ khám. Bệnh nhân cần có Medical Record để chỉ định cận lâm sàng.');
      return;
    }
    if (selectedServiceIds.length === 0) return;

    const confirmed = window.confirm(
      `Xác nhận tạo chỉ định bổ sung cho ${selectedServiceIds.length} dịch vụ đã chọn? ` +
      'Thông tin thanh toán sẽ được chuyển sang quầy thu ngân, còn bác sĩ vẫn ở màn khám hiện tại.'
    );
    if (!confirmed) {
      return;
    }

    const printWindow = appt ? openParaclinicalOrderPrintWindow() : null;
    if (appt && !printWindow) {
      alert('Trình duyệt đang chặn cửa sổ in phiếu cận lâm sàng. Vui lòng cho phép pop-up và thử lại.');
      return;
    }

    try {
      const createdOrders = await serviceOrderService.createSupplement({
        recordId,
        serviceIds: selectedServiceIds,
        indicationNote,
      });
      setSelectedServiceIds([]);
      if (appt) {
        const printed = printParaclinicalOrderSlip(
          appt,
          createdOrders,
          createdOrders[0]?.indicationNote ?? indicationNote,
          printWindow,
        );
        if (!printed) {
          alert('Đã tạo chỉ định nhưng trình duyệt chặn cửa sổ in phiếu cận lâm sàng.');
        }
      }
      setIndicationNote('');
      fetchOrders();
      alert('Đã tạo chỉ định bổ sung thành công. Bệnh nhân có thể mang phiếu này đến quầy thu ngân để thanh toán bổ sung.');
    } catch (e: unknown) {
      printWindow?.close();
      console.error(e);
      const err = e as { response?: { data?: { message?: string } } };
      alert(err?.response?.data?.message || 'Không thể tạo chỉ định cận lâm sàng.');
    }
  };

  const handleReprintOrders = () => {
    if (!appt || submittedPendingSelectionOrders.length === 0) {
      alert('Không có phiếu chỉ định bổ sung nào để in lại.');
      return;
    }

    const printed = printParaclinicalOrderSlip(
      appt,
      submittedPendingSelectionOrders,
      submittedPendingSelectionOrders[0]?.indicationNote ?? indicationNote,
    );

    if (!printed) {
      alert('Trình duyệt chặn cửa sổ in phiếu cận lâm sàng. Vui lòng cho phép pop-up và thử lại.');
    }
  };

  const [completing, setCompleting] = useState(false);
  const [savingProgress, setSavingProgress] = useState(false);
  const [pausingForParaclinical, setPausingForParaclinical] = useState(false);
  const [saveFeedback, setSaveFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const buildExaminationPayload = () => ({
    symptoms: buildSymptomsJson({ mach, huyetAp, nhietDo, nhipTim, canNang, chieuCao, daiTien, tieuTien, moTa }),
    diagnosis: ketLuan,
    doctorNotes: ghiChuChuyen,
  });

  const addPrescriptionRow = () => {
    setPrescriptionFeedback(null);
    setPrescriptionRows((prev) => [...prev, createPrescriptionDraftRow()]);
  };

  const updatePrescriptionRow = (rowKey: string, field: keyof Omit<PrescriptionDraftRow, 'key'>, value: string) => {
    setPrescriptionFeedback(null);
    setPrescriptionRows((prev) => prev.map((row) => (
      row.key === rowKey
        ? { ...row, [field]: value }
        : row
    )));
  };

  const removePrescriptionRow = (rowKey: string) => {
    setPrescriptionFeedback(null);
    setPrescriptionRows((prev) => {
      if (prev.length === 1) {
        return prev.map((row) => (
          row.key === rowKey
            ? { ...row, medicineName: '', dosage: '', quantity: '' }
            : row
        ));
      }

      return prev.filter((row) => row.key !== rowKey);
    });
  };

  const buildPrescriptionPayload = (): PrescriptionSavePayload | null => {
    if (!recordId) {
      throw new Error('Chưa có hồ sơ khám để lưu đơn thuốc.');
    }

    const notes = prescriptionNotes.trim();
    const details = prescriptionRows
      .map((row) => ({
        medicineName: row.medicineName.trim(),
        dosage: row.dosage.trim(),
        quantity: row.quantity.trim(),
      }))
      .filter((row) => row.medicineName || row.dosage || row.quantity);

    if (details.length === 0) {
      return null;
    }

    for (const [index, row] of details.entries()) {
      if (!row.medicineName) {
        throw new Error(`Dòng thuốc ${index + 1} chưa nhập tên thuốc.`);
      }

      if (!row.quantity) {
        throw new Error(`Dòng thuốc ${index + 1} chưa nhập số lượng.`);
      }
    }

    return {
      medicalRecordId: recordId,
      notes: notes || undefined,
      details,
    };
  };

  const persistPrescription = async ({
    allowEmptySkip = false,
    silentSuccess = false,
  }: {
    allowEmptySkip?: boolean;
    silentSuccess?: boolean;
  } = {}): Promise<PrescriptionMedicine | true | false> => {
    let payload: PrescriptionSavePayload | null;

    try {
      payload = buildPrescriptionPayload();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Không thể chuẩn bị dữ liệu đơn thuốc.';
      setPrescriptionFeedback({ type: 'error', message });
      return false;
    }

    if (!payload) {
      if (allowEmptySkip) {
        return true;
      }

      setPrescriptionFeedback({
        type: 'error',
        message: 'Nhập ít nhất một dòng thuốc trước khi lưu đơn thuốc.',
      });
      return false;
    }

    setPrescriptionSaving(true);
    setPrescriptionFeedback(null);

    try {
      const savedPrescription = prescriptionId
        ? await prescriptionService.updatePrescription(prescriptionId, payload)
        : await prescriptionService.issuePrescription(payload);

      setPrescriptionId(savedPrescription.prescriptionId);
      setPrescriptionNotes(savedPrescription.notes ?? '');
      setPrescriptionRows(
        savedPrescription.details.length > 0
          ? savedPrescription.details.map((detail) => createPrescriptionDraftRow({
              medicineName: detail.medicineName ?? '',
              dosage: detail.dosage ?? detail.note ?? '',
              quantity: detail.quantity ?? '',
            }))
          : [createPrescriptionDraftRow()]
      );

      if (!silentSuccess) {
        setPrescriptionFeedback({
          type: 'success',
          message: 'Đã lưu đơn thuốc thành công.',
        });
      }

      return savedPrescription;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      setPrescriptionFeedback({
        type: 'error',
        message: err?.response?.data?.message || 'Không thể lưu đơn thuốc.',
      });
      return false;
    } finally {
      setPrescriptionSaving(false);
    }
  };

  const handlePrintPrescription = async () => {
    if (prescriptionPrinting || prescriptionSaving) return;

    setPrescriptionPrinting(true);
    try {
      await appointmentService.saveExaminationProgress(appointmentId, buildExaminationPayload());

      const savedPrescription = await persistPrescription({ silentSuccess: true });
      if (!savedPrescription || savedPrescription === true) {
        return;
      }

      const html = await prescriptionService.exportPrescriptionDocument(savedPrescription.prescriptionId);
      const printed = printPrescriptionHtml(html);
      if (!printed) {
        setPrescriptionFeedback({
          type: 'error',
          message: 'Không thể mở cửa sổ in đơn thuốc.',
        });
        return;
      }

      setPrescriptionFeedback({
        type: 'success',
        message: 'Đã lưu đơn thuốc và mở hộp thoại in cho bệnh nhân.',
      });
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      setPrescriptionFeedback({
        type: 'error',
        message: err?.response?.data?.message || (error instanceof Error ? error.message : 'Không thể in đơn thuốc.'),
      });
    } finally {
      setPrescriptionPrinting(false);
    }
  };

  const handleSaveProgress = async () => {
    if (!appointmentId) return;
    setSavingProgress(true);
    setSaveFeedback(null);
    try {
      await appointmentService.saveExaminationProgress(appointmentId, buildExaminationPayload());
      setSaveFeedback({
        type: 'success',
        message: `Đã lưu thông tin khám lúc ${new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`,
      });
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      setSaveFeedback({
        type: 'error',
        message: err?.response?.data?.message || 'Không thể lưu thông tin khám.',
      });
    } finally {
      setSavingProgress(false);
    }
  };

  const handlePauseForParaclinical = async () => {
    if (!appointmentId) return;
    if (!hasActiveParaclinicalWork) {
      alert('Chưa có chỉ định cận lâm sàng đang chờ xử lý.');
      return;
    }

    const confirmed = window.confirm(
      'Xác nhận lưu tạm hồ sơ và chuyển bệnh nhân sang chờ kết quả cận lâm sàng? Bác sĩ sẽ có thể gọi bệnh nhân tiếp theo.'
    );
    if (!confirmed) return;

    setPausingForParaclinical(true);
    setSaveFeedback(null);
    try {
      const savedAppointment = await appointmentService.pauseForParaclinicalResults(appointmentId, buildExaminationPayload());
      setAppt(current => current ? { ...current, status: savedAppointment.status } : current);
      alert('Đã lưu tạm hồ sơ và chuyển bệnh nhân sang trạng thái chờ kết quả cận lâm sàng.');
      window.location.href = '/dashboard/doctor/patients';
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      alert(err?.response?.data?.message || 'Không thể lưu tạm chờ kết quả cận lâm sàng.');
    } finally {
      setPausingForParaclinical(false);
    }
  };

  const handleComplete = async () => {
    if (!appointmentId || !appt) return;
    if (hasActiveParaclinicalWork) {
      alert('Không thể hoàn tất khám khi còn dịch vụ cận lâm sàng chưa có kết quả.');
      return;
    }

    const confirmed = window.confirm('Xác nhận hoàn thành khám? Sau khi hoàn thành, hồ sơ sẽ được chốt và không thể thêm chỉ định cận lâm sàng mới.');
    if (!confirmed) return;

    const resultPrintWindow = openExaminationResultPrintWindow();
    const closeResultPrintWindow = () => {
      if (resultPrintWindow && !resultPrintWindow.closed) {
        resultPrintWindow.close();
      }
    };
    const examinationPayload = buildExaminationPayload();

    setCompleting(true);
    try {
      await appointmentService.saveExaminationProgress(appointmentId, examinationPayload);

      const prescriptionSaved = await persistPrescription({ allowEmptySkip: true, silentSuccess: true });
      if (!prescriptionSaved) {
        closeResultPrintWindow();
        return;
      }

      await appointmentService.completeExamination(appointmentId, examinationPayload);
      const printed = printExaminationResultSlip(
        {
          appointment: {
            ...appt,
            status: 'COMPLETED',
            medicalRecord: appt.medicalRecord
              ? {
                  ...appt.medicalRecord,
                  symptoms: examinationPayload.symptoms,
                  diagnosis: examinationPayload.diagnosis,
                  doctorNotes: examinationPayload.doctorNotes,
                  recordStatus: 'COMPLETED',
                }
              : appt.medicalRecord,
          },
          symptoms: examinationPayload.symptoms,
          diagnosis: examinationPayload.diagnosis,
          doctorNotes: examinationPayload.doctorNotes,
          serviceOrders,
          prescription: prescriptionSaved === true ? null : prescriptionSaved,
        },
        resultPrintWindow,
      );
      alert(
        printed
          ? 'Hoàn thành khám thành công. Đã mở phiếu kết quả khám để in cho bệnh nhân.'
          : 'Hoàn thành khám thành công, nhưng trình duyệt đã chặn cửa sổ in phiếu kết quả khám. Vui lòng cho phép pop-up và thử in lại từ hồ sơ.'
      );
      window.location.href = '/dashboard/doctor/patients';
    } catch (e: unknown) {
      closeResultPrintWindow();
      const err = e as { response?: { data?: { message?: string } } };
      alert(err?.response?.data?.message || 'Có lỗi xảy ra khi hoàn thành khám');
    } finally {
      setCompleting(false);
    }
  };

  if (pageLoading) return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <Loader2 className="animate-spin text-slate-500" size={32} />
    </div>
  );

  if (!appt) return (
    <div className="p-8 text-center text-red-500">Không tìm thấy thông tin lịch hẹn!</div>
  );

  // Calculate age from dateOfBirth
  const age = appt.patientDateOfBirth
    ? new Date().getFullYear() - new Date(appt.patientDateOfBirth).getFullYear()
    : null;

  const clinicalFields: ClinicalInputField[] = [
    { label: 'Mạch', value: mach, setter: setMach, pattern: /^\d{1,3}$/, errorMsg: 'Nhập số (VD: 72)', placeholder: '72', unit: 'lần/phút' },
    { label: 'Huyết áp', value: huyetAp, setter: setHuyetAp, pattern: /^\d{2,3}\/\d{2,3}$/, errorMsg: 'Định dạng: 120/80', placeholder: '120/80', unit: 'mmHg' },
    { label: 'Nhiệt độ', value: nhietDo, setter: setNhietDo, pattern: /^\d{2}(\.\d{1})?$/, errorMsg: 'Nhập số (VD: 36.5)', placeholder: '36.5', unit: '°C' },
    { label: 'Nhịp tim', value: nhipTim, setter: setNhipTim, pattern: /^\d{1,3}$/, errorMsg: 'Nhập số (VD: 80)', placeholder: '80', unit: 'lần/phút' },
    { label: 'Cân nặng', value: canNang, setter: setCanNang, pattern: /^\d{1,3}(\.\d{1,2})?$/, errorMsg: 'Nhập số (VD: 65.5)', placeholder: '65.5', unit: 'kg' },
    { label: 'Chiều cao', value: chieuCao, setter: setChieuCao, pattern: /^\d{2,3}(\.\d{1})?$/, errorMsg: 'Nhập số (VD: 170)', placeholder: '170', unit: 'cm' },
    { label: 'Đại tiện', value: daiTien, setter: setDaiTien, pattern: /^.{1,50}$/, errorMsg: 'Tối đa 50 ký tự', placeholder: 'Bình thường' },
    { label: 'Tiểu tiện', value: tieuTien, setter: setTieuTien, pattern: /^.{1,50}$/, errorMsg: 'Tối đa 50 ký tự', placeholder: 'Bình thường' },
  ];

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      <DashboardHeader title="Khám bệnh" subtitle="Chi tiết khám bệnh của bệnh nhân" />

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6 text-black">

        {/* ── THÔNG TIN BỆNH NHÂN ─────────────────────────────── */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4 uppercase">Thông tin bệnh nhân</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mb-4">
            <div><span className="font-medium">Mã phiếu:</span> {appt.appointmentId}</div>
            <div><span className="font-medium">Họ tên:</span> {appt.patientName}</div>
            <div><span className="font-medium">Giới tính:</span> Nam</div>
            <div><span className="font-medium">Tuổi:</span> {age ?? 'N/A'}</div>
            <div className="col-span-2"><span className="font-medium">SĐT:</span> {appt.patientPhone || 'N/A'}</div>
          </div>
          <div className="text-sm space-y-2">
            <div>
              <span className="font-medium">Lý do khám:</span> {appt.notes || appt.patientBackgroundDisease || 'Không ghi nhận'}
              <br />
              <span className="font-medium">Dị ứng:</span> {appt.patientAllergies || 'Không ghi nhận'}
            </div>
            <div className="flex items-center gap-2">
              <span className="font-medium">Trạng thái:</span>
              <span className="border text-xs font-medium px-2 py-1 rounded border-black text-black">ĐANG KHÁM</span>
            </div>
          </div>
        </div>

        {/* ── KHÁM LÂM SÀNG ───────────────────────────────────── */}
        {previousRecordId && (
          <div className="bg-white border rounded-lg p-6 shadow-sm">
            <div className="mb-4 flex items-center justify-between gap-3">
              <h2 className="text-lg font-bold">Ho so kham truoc</h2>
              <span className="rounded border border-blue-200 bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">
                {previousRecordId}
              </span>
            </div>
            {previousRecordLoading ? (
              <div className="flex items-center gap-2 text-sm text-slate-600">
                <Loader2 size={14} className="animate-spin" /> Dang tai ho so cu...
              </div>
            ) : previousRecordDetail ? (
              <div className="space-y-4 text-sm">
                <div className="grid gap-3 md:grid-cols-2">
                  <div><span className="font-medium">Bac si truoc:</span> {previousRecordDetail.doctor?.fullName || 'N/A'}</div>
                  <div><span className="font-medium">Ngay kham:</span> {previousRecordDetail.createdAt ? new Date(previousRecordDetail.createdAt).toLocaleDateString('vi-VN') : 'N/A'}</div>
                  <div className="md:col-span-2"><span className="font-medium">Chan doan:</span> {previousRecordDetail.diagnosis || 'Chua co chan doan'}</div>
                  <div className="md:col-span-2"><span className="font-medium">Ghi chu:</span> {previousRecordDetail.doctorNotes || 'Khong co ghi chu'}</div>
                </div>
                {previousRecordDetail.prescription?.details?.length ? (
                  <div className="rounded border border-gray-200 p-3">
                    <div className="mb-2 font-medium">Don thuoc truoc</div>
                    <div className="grid gap-2 md:grid-cols-2">
                      {previousRecordDetail.prescription.details.map((detail) => (
                        <div key={`${detail.id}-${detail.medicineName}`} className="rounded bg-gray-50 px-3 py-2">
                          <div className="font-medium">{detail.medicineName}</div>
                          <div className="text-xs text-slate-600">{detail.dosage || 'N/A'} - SL: {detail.quantity || 'N/A'}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : null}
                {previousRecordOrders.length > 0 && (
                  <div className="rounded border border-gray-200 p-3">
                    <div className="mb-2 font-medium">Ket qua can lam sang cu</div>
                    <div className="space-y-2">
                      {previousRecordOrders.map((order) => {
                        const files = getNormalizedResultFileUrls(order);
                        return (
                          <div key={order.orderId} className="rounded bg-gray-50 px-3 py-2">
                            <div className="font-medium">{order.serviceName}</div>
                            {order.resultNote && <div className="text-xs text-slate-600">{order.resultNote}</div>}
                            {files.length > 0 && (
                              <div className="mt-1 flex flex-wrap gap-2">
                                {files.map((url, index) => (
                                  <a key={url} href={url} target="_blank" rel="noopener noreferrer" className="text-xs font-medium text-blue-600 hover:underline">
                                    File {index + 1}
                                  </a>
                                ))}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="text-sm text-slate-600">Khong the tai ho so kham truoc.</div>
            )}
          </div>
        )}

        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h2 className="text-lg font-bold">Khám lâm sàng</h2>
            <button onClick={handleSaveProgress} disabled={savingProgress || completing || pausingForParaclinical}
              className="flex items-center justify-center gap-2 border rounded px-4 py-2 text-sm font-medium hover:bg-gray-50 bg-white text-black disabled:opacity-50 sm:min-w-[220px]">
              {savingProgress ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
              {savingProgress ? 'ĐANG LƯU...' : 'LƯU THÔNG TIN KHÁM'}
            </button>
          </div>
          <h3 className="font-medium text-sm mb-3">Triệu chứng lâm sàng</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
            {clinicalFields.map((field) => {
              const hasError = field.value !== '' && !field.pattern.test(field.value);
              return (
                <div key={field.label} className="flex flex-col gap-1">
                  <label className="text-xs font-medium text-gray-600">{field.label}</label>
                  <div className="relative">
                    <input type="text" placeholder={field.placeholder}
                    className={`w-full border rounded-md px-3 py-2 text-sm focus:outline-none ${field.unit ? 'pr-20' : ''} ${
                      hasError ? 'border-red-400 focus:border-red-500' : 'focus:border-gray-400'
                    }`}
                    value={field.value} onChange={e => field.setter(e.target.value)} />
                    {field.unit && (
                      <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs font-medium text-gray-500">
                        {field.unit}
                      </span>
                    )}
                  </div>
                  {hasError && <span className="text-xs text-red-500">{field.errorMsg}</span>}
                </div>
              );
            })}
          </div>
          <textarea placeholder="Mô tả chung..."
            className="w-full border rounded-md px-3 py-2 text-sm min-h-[100px] focus:outline-none focus:border-gray-400 resize-none"
            value={moTa} onChange={e => setMoTa(e.target.value)} />
          <div className={`mt-3 text-sm font-medium ${saveFeedback?.type === 'error' ? 'text-red-600' : 'text-green-600'}`}>
            {saveFeedback?.message ?? ''}
          </div>
        </div>

        {/* ── KẾT QUẢ CẬN LÂM SÀNG ───────────────────────────── */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4">Kết quả cận lâm sàng</h2>
          {paraclinicalResultItems.length === 0 ? (
            <div className="py-4 text-sm text-slate-600">Chưa có dịch vụ cận lâm sàng nào cho lịch hẹn này.</div>
          ) : (
            <div className="flex flex-col gap-4">
              {waitingResultItems.length > 0 && (
                <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                  Có {waitingResultItems.length} dịch vụ cận lâm sàng đang chờ bác sĩ cận lâm sàng trả kết quả.
                </div>
              )}
              {paraclinicalResultItems.map(order => (
                <ServiceOrderRow key={order.key} order={order} />
              ))}
            </div>
          )}
        </div>

        {/* ── CHỈ ĐỊNH CẬN LÂM SÀNG ────────────────────────────── */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4 uppercase">Chỉ định cận lâm sàng</h2>
          {serviceSelectionLoading ? (
            <div className="flex justify-center py-6"><Loader2 className="animate-spin text-slate-500" size={24} /></div>
          ) : (
            <>
              <div className="mb-6 rounded-lg border bg-gray-50 p-4">
                <div className="mb-3 flex items-center justify-between gap-4">
                  <h3 className="text-sm font-semibold uppercase text-black">Dịch vụ đã chọn</h3>
                  <div className="flex items-center gap-2">
                    {submittedPendingSelectionOrders.length > 0 && (
                      <button
                        type="button"
                        onClick={handleReprintOrders}
                        className="rounded border border-black bg-white px-4 py-2 text-sm font-medium text-black hover:bg-gray-100"
                      >
                        IN LẠI PHIẾU ({submittedPendingSelectionOrders.length})
                      </button>
                    )}
                    <button onClick={handleCreateOrders} disabled={selectedServiceIds.length === 0 || serviceSelectionLoading}
                      className="bg-black text-white px-4 py-2 rounded text-sm font-medium hover:bg-gray-800 disabled:opacity-50">
                      XÁC NHẬN CHỈ ĐỊNH ({selectedServiceIds.length})
                    </button>
                  </div>
                </div>
                {selectedServiceRows.length === 0 ? (
                  <div className="py-2 text-sm text-slate-600">Chưa có dịch vụ nào đang được chọn hoặc đã gửi chờ kết quả.</div>
                ) : (
                  <table className="w-full text-left text-sm">
                    <thead className="border-b">
                      <tr>
                        <th className="py-2 font-medium text-black uppercase w-12">STT</th>
                        <th className="py-2 font-medium text-black uppercase">Tên dịch vụ</th>
                        <th className="py-2 font-medium text-black uppercase text-center w-52">Trạng thái gửi</th>
                        <th className="py-2 font-medium text-black uppercase text-center w-24">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {selectedServiceRows.map((row, idx) => (
                        <tr key={row.key}>
                          <td className="py-3 text-black">{idx + 1}</td>
                          <td className="py-3">
                            <div className="font-medium text-black">{row.serviceName}</div>
                            {row.source === 'submitted' && (
                              <div className="mt-1 text-xs text-gray-500">
                                {row.orderId} • {row.createdAt ? new Date(row.createdAt).toLocaleDateString('vi-VN') : '—'}
                              </div>
                            )}
                          </td>
                          <td className="py-3 text-center">
                            <SelectionStateBadge row={row} />
                          </td>
                          <td className="py-3 text-center">
                            {row.source === 'draft' ? (
                              <button
                                type="button"
                                onClick={() => removeSelectedService(row.serviceId)}
                                className="rounded border border-red-200 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
                              >
                                Xóa
                              </button>
                            ) : (
                              (row.paymentStatus ?? 'UNPAID').toUpperCase() === 'PAID' ? (
                                <span className="rounded border border-green-200 px-3 py-1 text-xs font-medium text-green-700 bg-green-50">
                                  Chờ kết quả
                                </span>
                              ) : (
                                <button
                                  type="button"
                                  onClick={() => handleCancelOrder(row.orderId!)}
                                  disabled={cancelingOrderId === row.orderId}
                                  className="rounded border border-amber-200 px-3 py-1 text-xs font-medium text-amber-700 hover:bg-amber-50 disabled:opacity-50"
                                >
                                  {cancelingOrderId === row.orderId ? 'Đang hủy...' : 'Hủy'}
                                </button>
                              )
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}

                <div className="mt-4">
                  <label className="mb-2 block text-sm font-medium text-gray-700">Ghi chú thêm cho chỉ định</label>
                  <textarea
                    value={indicationNote}
                    onChange={e => setIndicationNote(e.target.value)}
                    placeholder="Ví dụ: Ưu tiên siêu âm ổ bụng, cần đối chiếu với kết quả máu cùng ngày..."
                    rows={3}
                    className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-black focus:outline-none focus:border-gray-500 resize-none"
                  />
                </div>
              </div>

              {unselectedServices.length === 0 ? (
                <div className="py-4 text-sm text-slate-600">Tất cả dịch vụ cận lâm sàng đã được chỉ định cho hồ sơ khám này.</div>
              ) : selectableServices.length === 0 ? (
                <div className="py-4 text-sm text-slate-600">Tất cả dịch vụ khả dụng đã được thêm vào bảng chọn phía trên.</div>
              ) : (
                <table className="w-full text-left text-sm mb-4">
                  <thead className="border-b">
                    <tr>
                      <th className="py-2 font-medium text-black uppercase w-12">STT</th>
                      <th className="py-2 font-medium text-black uppercase">Tên dịch vụ cận lâm sàng</th>
                      <th className="py-2 font-medium text-black uppercase text-center w-24">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {selectableServices.map((srv, idx) => (
                      <tr key={srv.serviceId}>
                        <td className="py-3 text-black">{idx + 1}</td>
                        <td className="py-3 font-medium text-black">{srv.serviceName}</td>
                        <td className="py-3 text-center">
                          <button
                            type="button"
                            onClick={() => addSelectedService(srv.serviceId)}
                            className="rounded border border-black px-3 py-1 text-xs font-medium text-black hover:bg-gray-100"
                          >
                            Thêm
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </>
          )}
          <textarea placeholder="Ghi chú chỉ định thêm..."
            className="w-full border rounded-md px-3 py-2 text-sm mb-4 min-h-[80px] focus:outline-none focus:border-gray-400 resize-none"
            value={ghiChuChuyen} onChange={e => setGhiChuChuyen(e.target.value)} />
        </div>

        {/* ── ĐƠN THUỐC ───────────────────────────────────────── */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h2 className="text-lg font-bold">Đơn thuốc</h2>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={addPrescriptionRow}
                disabled={prescriptionLoading || prescriptionSaving || prescriptionPrinting}
                className="border border-black px-4 py-2 rounded text-sm font-medium hover:bg-gray-50 disabled:opacity-50"
              >
                + THÊM DÒNG
              </button>
              <button
                type="button"
                onClick={() => { void handlePrintPrescription(); }}
                disabled={prescriptionLoading || prescriptionSaving || prescriptionPrinting || completing || !recordId}
                className="flex items-center justify-center gap-2 bg-black text-white px-4 py-2 rounded text-sm font-medium hover:bg-gray-800 disabled:opacity-50"
              >
                {prescriptionPrinting || prescriptionSaving ? <Loader2 size={16} className="animate-spin" /> : <Printer size={16} />}
                {prescriptionPrinting || prescriptionSaving ? 'ĐANG LƯU & IN...' : 'LƯU & IN ĐƠN THUỐC'}
              </button>
            </div>
          </div>
          {prescriptionLoading ? (
            <div className="flex justify-center py-6"><Loader2 className="animate-spin text-slate-500" size={24} /></div>
          ) : (
            <div className="space-y-4">
              {prescriptionRows.map((row, index) => (
                <div
                  key={row.key}
                  className="grid grid-cols-1 gap-3 rounded-lg border border-gray-200 bg-gray-50 p-4 md:grid-cols-[minmax(0,1.7fr)_minmax(0,1.2fr)_160px_auto] md:items-end"
                >
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-medium text-gray-600">Thuốc {index + 1}</label>
                    <input
                      type="text"
                      placeholder="Tên thuốc..."
                      className="border rounded-md px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
                      value={row.medicineName}
                      onChange={(event) => updatePrescriptionRow(row.key, 'medicineName', event.target.value)}
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-medium text-gray-600">Cách dùng / ghi chú</label>
                    <input
                      type="text"
                      placeholder="VD: Uống sau ăn sáng và tối"
                      className="border rounded-md px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
                      value={row.dosage}
                      onChange={(event) => updatePrescriptionRow(row.key, 'dosage', event.target.value)}
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-medium text-gray-600">Số lượng</label>
                    <input
                      type="text"
                      placeholder="VD: 10 viên"
                      className="border rounded-md px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
                      value={row.quantity}
                      onChange={(event) => updatePrescriptionRow(row.key, 'quantity', event.target.value)}
                    />
                  </div>
                  <div className="flex justify-end md:pb-0.5">
                    <button
                      type="button"
                      onClick={() => removePrescriptionRow(row.key)}
                      className="rounded border border-red-200 px-3 py-2 text-xs font-medium text-red-600 hover:bg-red-50"
                    >
                      {prescriptionRows.length === 1 ? 'Làm trống' : 'Xóa'}
                    </button>
                  </div>
                </div>
              ))}

              <div>
                <label className="mb-2 block text-sm font-medium text-gray-700">Ghi chú chung cho đơn thuốc</label>
                <textarea
                  value={prescriptionNotes}
                  onChange={(event) => {
                    setPrescriptionFeedback(null);
                    setPrescriptionNotes(event.target.value);
                  }}
                  placeholder="Ví dụ: Tái khám sau 5 ngày hoặc ngưng thuốc nếu có dấu hiệu dị ứng..."
                  rows={3}
                  className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-black focus:outline-none focus:border-gray-500 resize-none"
                />
              </div>

              <div className={`text-sm font-medium ${prescriptionFeedback?.type === 'error' ? 'text-red-600' : 'text-green-600'}`}>
                {prescriptionFeedback?.message ?? ''}
              </div>
            </div>
          )}
        </div>

        {/* ── KẾT LUẬN & DẶN DÒ ─────────────────────────────── */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4">Kết luận &amp; dặn dò</h2>
          <textarea placeholder="Chẩn đoán cuối cùng và lời dặn..."
            className="w-full border rounded-md px-3 py-2 text-sm min-h-[120px] focus:outline-none focus:border-gray-400 resize-none"
            value={ketLuan} onChange={e => setKetLuan(e.target.value)} />
        </div>
      </main>

      {/* ── Floating Action Bar ─────────────────────────────── */}
      <div className="fixed bottom-0 left-0 right-0 border-t bg-white p-4 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)] z-20">
        <div className="mx-auto max-w-7xl flex justify-end gap-3 px-4 sm:px-6 lg:px-8">
            {hasActiveParaclinicalWork && (
              <button onClick={handlePauseForParaclinical} disabled={pausingForParaclinical || completing || savingProgress}
              className="border border-amber-300 bg-amber-50 text-amber-800 rounded px-6 py-2 text-sm font-medium hover:bg-amber-100 disabled:opacity-50">
              {pausingForParaclinical ? 'Đang lưu tạm...' : 'LƯU TẠM - CHỜ CẬN LÂM SÀNG'}
              </button>
            )}
            <button onClick={handleComplete} disabled={completing || savingProgress || pausingForParaclinical}
            className="bg-gray-600 text-white rounded px-6 py-2 text-sm font-medium hover:bg-gray-700 disabled:opacity-50">
            {completing ? 'Đang xử lý...' : 'HOÀN THÀNH KHÁM'}
            </button>
        </div>
      </div>
    </div>
  );
}
