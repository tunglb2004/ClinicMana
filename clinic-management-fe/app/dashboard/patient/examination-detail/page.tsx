'use client';

import { useCallback, useEffect, useState } from 'react';
import type { AppointmentDetail } from '@/services/appointment.service';
import { openExaminationResultPrintWindow, printExaminationResultSlip } from '@/lib/examination-result-print';
import { getNormalizedResultFileUrls, getResultFileName, resolveResultFileUrl } from '@/lib/result-file';
import { examinationService } from '@/services/examination.service';
import { ServiceOrderItem } from '@/services/service-order.service';
import { ExaminationResultResponse } from '@/types/examination.types';
import { Loader2, ArrowLeft, FileText, AlertTriangle, Pill, Activity, Stethoscope, FlaskConical, UserRound, Download } from 'lucide-react';
import { useRouter, useSearchParams } from 'next/navigation';
import toast from 'react-hot-toast';
import { SERVICE_ORDER_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

type SymptomsRecord = Record<string, unknown>;

const RECORD_STATUS_LABELS: Record<string, string> = {
  COMPLETED: 'Hoàn thành',
  FINISHED: 'Hoàn thành',
  IN_PROGRESS: 'Đang khám',
  WAITING_RESULTS: 'Chờ kết quả cận lâm sàng',
  DRAFT: 'Lưu tạm',
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
  { label: 'Tuần hoàn', keys: ['tuanHoan', 'circulatory'] },
  { label: 'Hô hấp', keys: ['hoHap', 'respiratory'] },
  { label: 'Tiêu hóa', keys: ['tieuHoa', 'digestive'] },
  { label: 'Thần kinh', keys: ['thanKinh', 'neurological'] },
  { label: 'Khác', keys: ['khac', 'other'] },
  { label: 'Mô tả khám', keys: ['moTa', 'description', 'physicalExaminationNotes'] },
];

const parseSymptoms = (symptoms?: string | null): SymptomsRecord | null => {
  if (!symptoms) {
    return null;
  }

  try {
    const parsed = JSON.parse(symptoms);
    return typeof parsed === 'object' && parsed !== null ? (parsed as SymptomsRecord) : { raw: symptoms };
  } catch {
    return { raw: symptoms };
  }
};

const formatDateTime = (value?: string | null) => {
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
};

const formatDate = (value?: string | null) => {
  if (!value) {
    return 'Chưa cập nhật';
  }

  return new Date(value).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

const formatGender = (value?: string | null) => {
  switch (value) {
    case 'MALE':
      return 'Nam';
    case 'FEMALE':
      return 'Nữ';
    case 'OTHER':
      return 'Khác';
    default:
      return value || 'Chưa cập nhật';
  }
};

const getResultFiles = (order: ServiceOrderItem) => {
  return getNormalizedResultFileUrls(order);
};

const getFileName = (fileUrl: string, fallbackName: string, index: number) => {
  return getResultFileName(fileUrl, fallbackName, index);
};

const buildPrintAppointment = (data: ExaminationResultResponse): AppointmentDetail => ({
  appointmentId: data.appointmentId || '',
  appointmentDate: data.appointmentDate || data.createdAt?.split('T')[0] || '',
  appointmentTime: data.appointmentTime || '',
  status: data.status,
  queueNumber: 0,
  notes: '',
  cancelReason: '',
  createdAt: data.createdAt,
  patientId: data.patientId || '',
  patientName: data.patient.fullName,
  patientPhone: data.patientPhone || '',
  patientDateOfBirth: data.patient.dob || '',
  patientBloodType: '',
  patientHealthInsuranceNumber: data.patientHealthInsuranceNumber || '',
  patientAllergies: '',
  patientBackgroundDisease: '',
  doctorId: 0,
  doctorCode: '',
  doctorName: data.doctor.fullName || '',
  departmentName: data.doctor.department || '',
  roomId: data.roomId || '',
  roomName: data.roomName || '',
  specialization: '',
  degree: '',
  services: [],
  medicalRecord: {
    recordId: data.recordId,
    symptoms: data.symptoms || '',
    diagnosis: data.diagnosis || '',
    doctorNotes: data.doctorNotes || '',
    recordStatus: data.status,
    recordCreatedAt: data.createdAt,
    visitType: data.visitType,
    parentRecordId: data.parentRecordId,
    rootRecordId: data.rootRecordId,
  },
  visitType: data.visitType,
});

export default function ExaminationDetailPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const recordId = searchParams.get('id');

  const [loading, setLoading] = useState(true);
  const [examinationData, setExaminationData] = useState<ExaminationResultResponse | null>(null);
  const [serviceResults, setServiceResults] = useState<ServiceOrderItem[]>([]);
  const [exportingRecordPdf, setExportingRecordPdf] = useState(false);

  const fetchExaminationData = useCallback(async () => {
    if (!recordId) {
      toast.error('Không tìm thấy hồ sơ khám.');
      router.push('/dashboard/patient/health-record');
      return;
    }

    setLoading(true);
    try {
      const [detail, results] = await Promise.all([
        examinationService.getExaminationResult(recordId),
        examinationService.getRecordServiceResults(recordId),
      ]);
      setExaminationData(detail);
      setServiceResults(results);
    } catch (error) {
      console.error('Failed to fetch examination detail:', error);
      toast.error('Không thể tải chi tiết kết quả khám.');
      router.push('/dashboard/patient/health-record');
    } finally {
      setLoading(false);
    }
  }, [recordId, router]);

  useEffect(() => {
    fetchExaminationData();
  }, [fetchExaminationData]);

  const handleDownloadMedicalRecordPdf = async () => {
    if (!examinationData) {
      toast.error('Không tìm thấy hồ sơ khám.');
      return;
    }

    setExportingRecordPdf(true);
    try {
      const printWindow = openExaminationResultPrintWindow();
      const printed = printExaminationResultSlip(
        {
          appointment: buildPrintAppointment(examinationData),
          symptoms: examinationData.symptoms,
          diagnosis: examinationData.diagnosis,
          doctorNotes: examinationData.doctorNotes,
          serviceOrders: serviceResults,
          prescription: examinationData.prescription,
        },
        printWindow,
        { autoPrint: false },
      );

      if (!printed) {
        toast.error('Trình duyệt đã chặn cửa sổ tải PDF. Vui lòng cho phép pop-up và thử lại.');
      }
    } catch (error) {
      console.error('Failed to open medical record PDF:', error);
      toast.error('Không thể mở Medical Record PDF.');
    } finally {
      setExportingRecordPdf(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-full bg-gray-50 p-6 text-black">
        <div className="max-w-6xl mx-auto">
          <div className="flex items-center justify-center gap-2 py-32 text-slate-600">
            <Loader2 className="animate-spin" size={22} />
            <span>Đang tải chi tiết kết quả khám...</span>
          </div>
        </div>
      </div>
    );
  }

  if (!examinationData) {
    return (
      <div className="min-h-full bg-gray-50 p-6 text-black">
        <div className="max-w-6xl mx-auto">
          <div className="bg-red-50 border border-red-200 rounded-xl p-12 text-center">
            <AlertTriangle className="w-12 h-12 text-red-500 mx-auto mb-4" />
            <h2 className="text-xl font-bold text-red-800 mb-2">Không tìm thấy kết quả khám</h2>
            <p className="text-gray-600 mb-6">Hồ sơ này không tồn tại hoặc bạn không có quyền xem.</p>
            <button
              onClick={() => router.push('/dashboard/patient/health-record')}
              className="inline-flex items-center gap-2 px-5 py-3 bg-[#1a2b3c] text-white rounded-lg hover:bg-[#24384d] transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
              Quay lại hồ sơ sức khỏe
            </button>
          </div>
        </div>
      </div>
    );
  }

  const symptoms = parseSymptoms(examinationData.symptoms);
  const consumedKeys = new Set<string>();
  const symptomItems = SYMPTOM_FIELDS.flatMap((field) => {
    if (!symptoms) {
      return [];
    }

    const matchedKey = field.keys.find((key) => symptoms[key] !== undefined && symptoms[key] !== null && symptoms[key] !== '');
    if (!matchedKey) {
      return [];
    }

    consumedKeys.add(matchedKey);
    return [{ label: field.label, value: String(symptoms[matchedKey]) }];
  });

  const remainingSymptoms = symptoms
    ? Object.entries(symptoms).filter(([key, value]) => !consumedKeys.has(key) && value !== undefined && value !== null && value !== '')
    : [];
  const canDownloadMedicalRecord = ['COMPLETED', 'FINISHED'].includes((examinationData.status ?? '').toUpperCase());

  return (
    <div className="min-h-full bg-gray-50 p-6 text-black">
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-[#1a2b3c]">Chi tiết kết quả khám</h1>
            <p className="text-sm text-gray-500 mt-1">Hồ sơ {examinationData.recordId}</p>
          </div>

          <div className="flex flex-wrap gap-2">
            {canDownloadMedicalRecord && (
              <button
                type="button"
                onClick={handleDownloadMedicalRecordPdf}
                disabled={exportingRecordPdf}
                className="inline-flex items-center gap-2 px-4 py-2.5 bg-[#1a2b3c] text-white rounded-lg hover:bg-[#24384d] disabled:opacity-60 transition-colors"
              >
                {exportingRecordPdf ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                Tải Medical Record PDF
              </button>
            )}

            <button
              onClick={() => router.push('/dashboard/patient/health-record')}
              className="inline-flex items-center gap-2 px-4 py-2.5 bg-white text-gray-700 rounded-lg hover:bg-gray-100 border border-gray-300 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Quay lại
            </button>
          </div>
        </div>

        <section className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
          <div className="flex items-center gap-2 mb-4">
            <UserRound className="w-5 h-5 text-[#1a2b3c]" />
            <h2 className="text-lg font-semibold text-[#1a2b3c]">Thông tin khám</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 text-sm">
            <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
              <p className="text-gray-500 mb-1">Bệnh nhân</p>
              <p className="font-semibold text-gray-900">{examinationData.patient.fullName}</p>
            </div>
            <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
              <p className="text-gray-500 mb-1">Ngày sinh</p>
              <p className="font-semibold text-gray-900">{formatDate(examinationData.patient.dob)}</p>
            </div>
            <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
              <p className="text-gray-500 mb-1">Giới tính</p>
              <p className="font-semibold text-gray-900">{formatGender(examinationData.patient.gender)}</p>
            </div>
            <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
              <p className="text-gray-500 mb-1">Bác sĩ</p>
              <p className="font-semibold text-gray-900">{examinationData.doctor.fullName || 'Chưa cập nhật'}</p>
              <p className="text-xs text-gray-500 mt-1">{examinationData.doctor.department || 'Chưa cập nhật chuyên khoa'}</p>
            </div>
            <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
              <p className="text-gray-500 mb-1">Thời gian khám</p>
              <p className="font-semibold text-gray-900">{formatDateTime(examinationData.createdAt)}</p>
            </div>
            <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
              <p className="text-gray-500 mb-1">Trạng thái hồ sơ</p>
              <p className="font-semibold text-gray-900">{formatVietnameseLabel(examinationData.status, RECORD_STATUS_LABELS)}</p>
            </div>
          </div>
        </section>

        <section className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
          <div className="flex items-center gap-2 mb-4">
            <Activity className="w-5 h-5 text-[#1a2b3c]" />
            <h2 className="text-lg font-semibold text-[#1a2b3c]">Kết quả khám lâm sàng</h2>
          </div>

          {symptomItems.length === 0 && remainingSymptoms.length === 0 ? (
            <p className="text-sm text-gray-500">Chưa có dữ liệu khám lâm sàng.</p>
          ) : (
            <div className="space-y-4">
              {symptomItems.length > 0 && (
                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                  {symptomItems.map((item) => (
                    <div key={item.label} className="rounded-lg bg-gray-50 p-4 border border-gray-100">
                      <p className="text-xs uppercase tracking-wide text-gray-500 mb-1">{item.label}</p>
                      <p className="font-medium text-gray-900 whitespace-pre-wrap">{item.value}</p>
                    </div>
                  ))}
                </div>
              )}

              {remainingSymptoms.length > 0 && (
                <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
                  <p className="text-xs uppercase tracking-wide text-gray-500 mb-3">Thông tin khác</p>
                  <div className="space-y-2 text-sm text-gray-700">
                    {remainingSymptoms.map(([key, value]) => (
                      <div key={key}>
                        <span className="font-medium text-gray-900">{key}:</span> {String(value)}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </section>

        <section className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
          <div className="flex items-center gap-2 mb-4">
            <Stethoscope className="w-5 h-5 text-[#1a2b3c]" />
            <h2 className="text-lg font-semibold text-[#1a2b3c]">Kết luận bác sĩ lâm sàng</h2>
          </div>

          <div className="space-y-4">
            <div className="rounded-lg bg-emerald-50 p-4 border border-emerald-100">
              <p className="text-xs uppercase tracking-wide text-emerald-700 mb-1">Chẩn đoán</p>
              <p className="text-base font-semibold text-gray-900 whitespace-pre-wrap">
                {examinationData.diagnosis || 'Chưa có chẩn đoán'}
              </p>
            </div>

            <div className="rounded-lg bg-blue-50 p-4 border border-blue-100">
              <p className="text-xs uppercase tracking-wide text-blue-700 mb-1">Nhận xét / kết luận</p>
              <p className="text-sm text-gray-800 whitespace-pre-wrap">
                {examinationData.doctorNotes || 'Chưa có nhận xét từ bác sĩ.'}
              </p>
            </div>
          </div>
        </section>

        <section className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
          <div className="flex items-center gap-2 mb-4">
            <FlaskConical className="w-5 h-5 text-[#1a2b3c]" />
            <h2 className="text-lg font-semibold text-[#1a2b3c]">Kết quả khám cận lâm sàng</h2>
          </div>

          {serviceResults.length === 0 ? (
            <p className="text-sm text-gray-500">Không có chỉ định cận lâm sàng cho hồ sơ này.</p>
          ) : (
            <div className="space-y-4">
              {serviceResults.map((order) => {
                const files = getResultFiles(order);

                return (
                  <div key={order.orderId} className="rounded-xl border border-gray-200 p-4">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <h3 className="font-semibold text-gray-900">{order.serviceName}</h3>
                        <p className="text-xs text-gray-500 mt-1">Mã chỉ định: {order.orderId}</p>
                      </div>
                      <span className="inline-flex items-center self-start px-2.5 py-1 rounded-full bg-gray-100 text-xs font-medium text-gray-700">
                        {formatVietnameseLabel(order.status, SERVICE_ORDER_STATUS_LABELS)}
                      </span>
                    </div>

                    <div className="mt-4 grid grid-cols-1 lg:grid-cols-2 gap-4 text-sm">
                      <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
                        <p className="text-xs uppercase tracking-wide text-gray-500 mb-2">Kết quả / ghi chú</p>
                        <p className="text-gray-800 whitespace-pre-wrap">
                          {order.resultNote || 'Chưa có mô tả kết quả.'}
                        </p>
                      </div>

                      <div className="rounded-lg bg-gray-50 p-4 border border-gray-100">
                        <p className="text-xs uppercase tracking-wide text-gray-500 mb-2">Tệp kết quả</p>
                        {files.length === 0 ? (
                          <p className="text-gray-500">Chưa có tệp kết quả.</p>
                        ) : (
                          <div className="space-y-2">
                            {files.map((fileUrl, index) => (
                              <a
                                key={`${order.orderId}-${index}`}
                                href={resolveResultFileUrl(fileUrl)}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-flex items-center gap-2 text-[#8B5CF6] hover:text-[#7C3AED] hover:underline"
                              >
                                <FileText className="w-4 h-4" />
                                {getFileName(fileUrl, order.serviceName, index)}
                              </a>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>

                    {order.indicationNote && (
                      <div className="mt-4 rounded-lg bg-amber-50 p-4 border border-amber-100 text-sm text-gray-800">
                        <p className="text-xs uppercase tracking-wide text-amber-700 mb-1">Ghi chú chỉ định</p>
                        <p className="whitespace-pre-wrap">{order.indicationNote}</p>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {examinationData.prescription && examinationData.prescription.details.length > 0 && (
          <section className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
            <div className="flex items-center gap-2 mb-4">
              <Pill className="w-5 h-5 text-[#1a2b3c]" />
              <h2 className="text-lg font-semibold text-[#1a2b3c]">Đơn thuốc</h2>
            </div>

            {examinationData.prescription.notes && (
              <div className="rounded-lg bg-gray-50 p-4 border border-gray-100 mb-4 text-sm text-gray-800 whitespace-pre-wrap">
                {examinationData.prescription.notes}
              </div>
            )}

            <div className="space-y-3">
              {examinationData.prescription.details.map((item) => (
                <div key={item.id} className="rounded-lg border border-gray-200 p-4">
                  <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                    <div>
                      <p className="font-semibold text-gray-900">{item.medicineName}</p>
                      <p className="text-sm text-gray-600 mt-1">Liều dùng: {item.dosage}</p>
                    </div>
                    <div className="text-sm text-gray-700 space-y-1">
                      <p>Số lượng: {item.quantity}{item.unit ? ` ${item.unit}` : ''}</p>
                      {item.frequency && <p>Tần suất: {item.frequency}</p>}
                      {item.duration && <p>Thời gian dùng: {item.duration}</p>}
                    </div>
                  </div>
                  {item.note && <p className="mt-3 text-sm text-gray-700 whitespace-pre-wrap">{item.note}</p>}
                </div>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
