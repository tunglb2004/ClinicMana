'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, usePathname, useRouter } from 'next/navigation';
import {
  Activity,
  AlertTriangle,
  ArrowLeft,
  FileText,
  FlaskConical,
  Loader2,
  Pill,
  Stethoscope,
  UserRound,
} from 'lucide-react';
import { getNormalizedResultFileUrls, getResultFileName, resolveResultFileUrl } from '@/lib/result-file';
import { examinationService } from '@/services/examination.service';
import { serviceOrderService, ServiceOrderItem } from '@/services/service-order.service';
import { ExaminationResultResponse } from '@/types/examination.types';
import { SERVICE_ORDER_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

type SymptomsRecord = Record<string, unknown>;

const RECORD_STATUS_LABELS: Record<string, string> = {
  COMPLETED: 'Hoàn thành',
  FINISHED: 'Hoàn thành',
  EXAMINING: 'Đang khám',
  WAITING: 'Chờ khám',
  WAITING_RESULTS: 'Chờ kết quả cận lâm sàng',
  CANCELLED: 'Đã hủy',
};

const SYMPTOM_FIELDS: Array<{ label: string; keys: string[] }> = [
  { label: 'Mạch', keys: ['mach', 'pulse'] },
  { label: 'Huyết áp', keys: ['huyetAp', 'bloodPressure'] },
  { label: 'Nhiệt độ', keys: ['nhietDo', 'temperature'] },
  { label: 'Nhịp tim', keys: ['nhipTim', 'heartRate'] },
  { label: 'Nhịp thở', keys: ['nhipTho', 'respiratoryRate'] },
  { label: 'Cân nặng', keys: ['canNang', 'weight'] },
  { label: 'Chiều cao', keys: ['chieuCao', 'height'] },
  { label: 'Tình trạng chung', keys: ['tinhTrang', 'generalCondition'] },
  { label: 'Mô tả khám', keys: ['moTa', 'description', 'physicalExaminationNotes'] },
];

function parseSymptoms(symptoms?: string | null): SymptomsRecord | null {
  if (!symptoms) return null;
  try {
    const parsed = JSON.parse(symptoms);
    return parsed && typeof parsed === 'object' ? (parsed as SymptomsRecord) : { raw: symptoms };
  } catch {
    return { raw: symptoms };
  }
}

function formatDate(value?: string | null) {
  if (!value) return 'Chưa cập nhật';
  return new Date(value).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function formatDateTime(value?: string | null) {
  if (!value) return 'Chưa cập nhật';
  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatGender(value?: string | null) {
  if (value === 'MALE') return 'Nam';
  if (value === 'FEMALE') return 'Nữ';
  if (value === 'OTHER') return 'Khác';
  return value || 'Chưa cập nhật';
}

function getParamId(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

export default function ClinicalDoctorRecordDetailPage() {
  const router = useRouter();
  const params = useParams();
  const pathname = usePathname();
  const recordId = getParamId(params.id);
  const listPath = useMemo(() => {
    const currentPath = pathname || '/dashboard/doctor/records';
    const segments = currentPath.split('/').filter(Boolean);
    if (segments.length <= 1) return '/dashboard/doctor/records';
    return `/${segments.slice(0, -1).join('/')}`;
  }, [pathname]);

  const [loading, setLoading] = useState(true);
  const [record, setRecord] = useState<ExaminationResultResponse | null>(null);
  const [serviceOrders, setServiceOrders] = useState<ServiceOrderItem[]>([]);

  const fetchRecord = useCallback(async () => {
    if (!recordId) {
      setRecord(null);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const [detail, orders] = await Promise.all([
        examinationService.getRecordDetailForStaff(recordId),
        serviceOrderService.getByRecord(recordId),
      ]);
      setRecord(detail);
      setServiceOrders(orders);
    } catch (error) {
      console.error('Failed to fetch record detail:', error);
      setRecord(null);
      setServiceOrders([]);
    } finally {
      setLoading(false);
    }
  }, [recordId]);

  useEffect(() => {
    fetchRecord();
  }, [fetchRecord]);

  const symptomSummary = useMemo(() => {
    const symptoms = parseSymptoms(record?.symptoms);
    if (!symptoms) {
      return { known: [] as Array<{ label: string; value: string }>, remaining: [] as Array<[string, unknown]> };
    }

    const consumedKeys = new Set<string>();
    const known = SYMPTOM_FIELDS.flatMap((field) => {
      const matchedKey = field.keys.find((key) => symptoms[key] !== undefined && symptoms[key] !== null && symptoms[key] !== '');
      if (!matchedKey) return [];
      consumedKeys.add(matchedKey);
      return [{ label: field.label, value: String(symptoms[matchedKey]) }];
    });
    const remaining = Object.entries(symptoms).filter(
      ([key, value]) => !consumedKeys.has(key) && value !== undefined && value !== null && value !== ''
    );

    return { known, remaining };
  }, [record?.symptoms]);

  const openFile = (fileUrl: string) => {
    const resolved = resolveResultFileUrl(fileUrl);
    if (resolved) {
      window.open(resolved, '_blank', 'noopener,noreferrer');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 p-6 text-slate-900">
        <div className="mx-auto flex max-w-6xl items-center justify-center gap-2 py-32 text-slate-600">
          <Loader2 className="h-6 w-6 animate-spin" />
          <span>Đang tải hồ sơ khám...</span>
        </div>
      </div>
    );
  }

  if (!record) {
    return (
      <div className="min-h-screen bg-gray-50 p-6 text-slate-900">
        <div className="mx-auto max-w-6xl">
          <div className="rounded-lg border border-red-200 bg-red-50 p-10 text-center">
            <AlertTriangle className="mx-auto mb-4 h-10 w-10 text-red-500" />
            <h1 className="text-xl font-bold text-red-800">Không tìm thấy hồ sơ khám</h1>
            <p className="mt-2 text-sm text-slate-600">Hồ sơ không tồn tại hoặc tài khoản hiện tại không có quyền xem.</p>
            <button
              type="button"
              onClick={() => router.push(listPath)}
              className="mt-6 inline-flex items-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800"
            >
              <ArrowLeft className="h-4 w-4" />
              Quay lại danh sách
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 text-slate-900">
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-950">Chi tiết hồ sơ khám</h1>
            <p className="mt-1 font-mono text-sm text-slate-500">{record.recordId}</p>
          </div>
          <button
            type="button"
            onClick={() => router.push(listPath)}
            className="inline-flex items-center gap-2 rounded-md border bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          >
            <ArrowLeft className="h-4 w-4" />
            Quay lại
          </button>
        </div>

        <section className="rounded-lg border bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center gap-2">
            <UserRound className="h-5 w-5 text-blue-700" />
            <h2 className="text-lg font-semibold">Thông tin hành chính</h2>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <div className="rounded-md border bg-slate-50 p-4">
              <p className="text-xs uppercase text-slate-500">Bệnh nhân</p>
              <p className="mt-1 font-semibold">{record.patient.fullName}</p>
            </div>
            <div className="rounded-md border bg-slate-50 p-4">
              <p className="text-xs uppercase text-slate-500">Ngày sinh</p>
              <p className="mt-1 font-semibold">{formatDate(record.patient.dob)}</p>
            </div>
            <div className="rounded-md border bg-slate-50 p-4">
              <p className="text-xs uppercase text-slate-500">Giới tính</p>
              <p className="mt-1 font-semibold">{formatGender(record.patient.gender)}</p>
            </div>
            <div className="rounded-md border bg-slate-50 p-4">
              <p className="text-xs uppercase text-slate-500">Thời gian khám</p>
              <p className="mt-1 font-semibold">{formatDateTime(record.createdAt)}</p>
            </div>
            <div className="rounded-md border bg-slate-50 p-4 md:col-span-2">
              <p className="text-xs uppercase text-slate-500">Bác sĩ</p>
              <p className="mt-1 font-semibold">{record.doctor.fullName || 'Chưa cập nhật'}</p>
              <p className="mt-1 text-xs text-slate-500">{record.doctor.department || 'Chưa cập nhật khoa/phòng'}</p>
            </div>
            <div className="rounded-md border bg-slate-50 p-4 md:col-span-2">
              <p className="text-xs uppercase text-slate-500">Trạng thái hồ sơ</p>
              <p className="mt-1 font-semibold">{formatVietnameseLabel(record.status, RECORD_STATUS_LABELS)}</p>
            </div>
          </div>
        </section>

        <section className="rounded-lg border bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center gap-2">
            <Activity className="h-5 w-5 text-blue-700" />
            <h2 className="text-lg font-semibold">Khám lâm sàng</h2>
          </div>
          {symptomSummary.known.length === 0 && symptomSummary.remaining.length === 0 ? (
            <p className="text-sm text-slate-500">Chưa có dữ liệu khám lâm sàng.</p>
          ) : (
            <div className="space-y-4">
              {symptomSummary.known.length > 0 && (
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {symptomSummary.known.map((item) => (
                    <div key={item.label} className="rounded-md border bg-slate-50 p-4">
                      <p className="text-xs uppercase text-slate-500">{item.label}</p>
                      <p className="mt-1 whitespace-pre-wrap font-medium">{item.value}</p>
                    </div>
                  ))}
                </div>
              )}
              {symptomSummary.remaining.length > 0 && (
                <div className="rounded-md border bg-slate-50 p-4">
                  <p className="text-xs uppercase text-slate-500">Thông tin khác</p>
                  <div className="mt-2 space-y-2 text-sm">
                    {symptomSummary.remaining.map(([key, value]) => (
                      <p key={key}>
                        <span className="font-semibold">{key}:</span> {String(value)}
                      </p>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </section>

        <section className="rounded-lg border bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center gap-2">
            <Stethoscope className="h-5 w-5 text-blue-700" />
            <h2 className="text-lg font-semibold">Kết luận bác sĩ</h2>
          </div>
          <div className="grid gap-4 lg:grid-cols-2">
            <div className="rounded-md border border-emerald-100 bg-emerald-50 p-4">
              <p className="text-xs uppercase text-emerald-700">Chẩn đoán</p>
              <p className="mt-2 whitespace-pre-wrap font-semibold">{record.diagnosis || 'Chưa có chẩn đoán'}</p>
            </div>
            <div className="rounded-md border border-blue-100 bg-blue-50 p-4">
              <p className="text-xs uppercase text-blue-700">Ghi chú / kết luận</p>
              <p className="mt-2 whitespace-pre-wrap text-sm">{record.doctorNotes || 'Chưa có ghi chú.'}</p>
            </div>
          </div>
        </section>

        <section className="rounded-lg border bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center gap-2">
            <FlaskConical className="h-5 w-5 text-blue-700" />
            <h2 className="text-lg font-semibold">Chỉ định và kết quả cận lâm sàng</h2>
          </div>
          {serviceOrders.length === 0 ? (
            <p className="text-sm text-slate-500">Không có chỉ định cận lâm sàng trong hồ sơ này.</p>
          ) : (
            <div className="space-y-4">
              {serviceOrders.map((order) => {
                const files = getNormalizedResultFileUrls(order);
                return (
                  <article key={order.orderId} className="rounded-lg border p-4">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <h3 className="font-semibold">{order.serviceName}</h3>
                        <p className="mt-1 font-mono text-xs text-slate-500">{order.orderId}</p>
                      </div>
                      <span className="inline-flex self-start rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700">
                        {formatVietnameseLabel(order.status, SERVICE_ORDER_STATUS_LABELS)}
                      </span>
                    </div>
                    <div className="mt-4 grid gap-4 lg:grid-cols-2">
                      <div className="rounded-md border bg-slate-50 p-4">
                        <p className="text-xs uppercase text-slate-500">Kết quả / ghi chú</p>
                        <p className="mt-2 whitespace-pre-wrap text-sm">{order.resultNote || 'Chưa có mô tả kết quả.'}</p>
                      </div>
                      <div className="rounded-md border bg-slate-50 p-4">
                        <p className="text-xs uppercase text-slate-500">Tệp kết quả</p>
                        {files.length === 0 ? (
                          <p className="mt-2 text-sm text-slate-500">Chưa có tệp kết quả.</p>
                        ) : (
                          <div className="mt-2 space-y-2">
                            {files.map((fileUrl, index) => (
                              <button
                                key={`${order.orderId}-${index}`}
                                type="button"
                                onClick={() => openFile(fileUrl)}
                                className="flex items-center gap-2 text-sm font-medium text-blue-700 hover:underline"
                              >
                                <FileText className="h-4 w-4" />
                                {getResultFileName(fileUrl, order.serviceName, index)}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                    {order.indicationNote && (
                      <div className="mt-4 rounded-md border border-amber-100 bg-amber-50 p-4 text-sm">
                        <p className="text-xs uppercase text-amber-700">Ghi chú chỉ định</p>
                        <p className="mt-2 whitespace-pre-wrap">{order.indicationNote}</p>
                      </div>
                    )}
                  </article>
                );
              })}
            </div>
          )}
        </section>

        {record.prescription && record.prescription.details.length > 0 && (
          <section className="rounded-lg border bg-white p-6 shadow-sm">
            <div className="mb-4 flex items-center gap-2">
              <Pill className="h-5 w-5 text-blue-700" />
              <h2 className="text-lg font-semibold">Đơn thuốc</h2>
            </div>
            {record.prescription.notes && (
              <div className="mb-4 rounded-md border bg-slate-50 p-4 text-sm whitespace-pre-wrap">
                {record.prescription.notes}
              </div>
            )}
            <div className="space-y-3">
              {record.prescription.details.map((item) => (
                <div key={item.id} className="rounded-md border p-4">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <p className="font-semibold">{item.medicineName}</p>
                      <p className="mt-1 text-sm text-slate-600">Liều dùng: {item.dosage}</p>
                    </div>
                    <p className="text-sm text-slate-700">Số lượng: {item.quantity}</p>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
