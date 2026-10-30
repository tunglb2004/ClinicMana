'use client';

import { useEffect, useState } from 'react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { appointmentService, AppointmentDetail } from '@/services/appointment.service';
import { useToast } from '@/hooks/use-toast';
import { ArrowLeft, Loader2, Save } from 'lucide-react';
import { useParams, useRouter } from 'next/navigation';
import { APPOINTMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

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

type ApiError = {
  response?: {
    data?: {
      message?: string;
      error?: string;
    };
  };
};

const parseSymptomsJson = (raw?: string | null): SymptomsData => {
  if (!raw) return {};
  try {
    return JSON.parse(raw) as SymptomsData;
  } catch {
    return { moTa: raw };
  }
};

const buildSymptomsJson = (data: SymptomsData) => JSON.stringify(data);

function getErrorMessage(error: unknown, fallback: string) {
  const apiError = error as ApiError;
  return apiError.response?.data?.message || apiError.response?.data?.error || fallback;
}

function calculateAge(dateOfBirth?: string | null) {
  if (!dateOfBirth) return null;
  const birthDate = new Date(dateOfBirth);
  if (Number.isNaN(birthDate.getTime())) return null;
  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age -= 1;
  }
  return age;
}

export default function NurseClinicalExaminationPage() {
  const params = useParams();
  const router = useRouter();
  const { toast } = useToast();
  const appointmentId = params.id as string;

  const [appointment, setAppointment] = useState<AppointmentDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [mach, setMach] = useState('');
  const [huyetAp, setHuyetAp] = useState('');
  const [nhietDo, setNhietDo] = useState('');
  const [nhipTim, setNhipTim] = useState('');
  const [canNang, setCanNang] = useState('');
  const [chieuCao, setChieuCao] = useState('');
  const [daiTien, setDaiTien] = useState('');
  const [tieuTien, setTieuTien] = useState('');
  const [moTa, setMoTa] = useState('');

  useEffect(() => {
    if (!appointmentId) return;

    let cancelled = false;
    setLoading(true);

    appointmentService.getNurseClinicalAppointmentDetail(appointmentId)
      .then((data) => {
        if (cancelled) return;

        setAppointment(data);
        const symptoms = parseSymptomsJson(data.medicalRecord?.symptoms);
        setMach(symptoms.mach ?? '');
        setHuyetAp(symptoms.huyetAp ?? '');
        setNhietDo(symptoms.nhietDo ?? '');
        setNhipTim(symptoms.nhipTim ?? '');
        setCanNang(symptoms.canNang ?? '');
        setChieuCao(symptoms.chieuCao ?? '');
        setDaiTien(symptoms.daiTien ?? '');
        setTieuTien(symptoms.tieuTien ?? '');
        setMoTa(symptoms.moTa ?? '');
      })
      .catch((error) => {
        if (cancelled) return;
        console.error('Error fetching nurse clinical appointment:', error);
        setAppointment(null);
        toast({
          description: getErrorMessage(error, 'Không thể tải thông tin bệnh nhân.'),
          variant: 'error',
        });
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [appointmentId, toast]);

  const handleSave = async () => {
    if (!appointmentId || saving) return;

    setSaving(true);
    try {
      await appointmentService.nurseSaveClinicalProgress(appointmentId, {
        symptoms: buildSymptomsJson({
          mach,
          huyetAp,
          nhietDo,
          nhipTim,
          canNang,
          chieuCao,
          daiTien,
          tieuTien,
          moTa,
        }),
      });

      toast({
        description: 'Đã lưu thông tin nhập hộ.',
        variant: 'success',
      });

      const refreshed = await appointmentService.getNurseClinicalAppointmentDetail(appointmentId);
      setAppointment(refreshed);
    } catch (error) {
      toast({
        description: getErrorMessage(error, 'Không thể lưu thông tin nhập hộ.'),
        variant: 'error',
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <Loader2 className="animate-spin text-slate-500" size={32} />
      </div>
    );
  }

  if (!appointment) {
    return (
      <div className="min-h-screen bg-gray-50">
        <DashboardHeader title="Nhập hộ lâm sàng" subtitle="Không tìm thấy thông tin bệnh nhân" />
        <main className="mx-auto max-w-4xl px-4 py-8">
          <button
            type="button"
            onClick={() => router.push('/dashboard/nurse/clinical-patients')}
            className="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium text-black hover:bg-white"
          >
            <ArrowLeft className="h-4 w-4" />
            Quay lại hàng đợi
          </button>
        </main>
      </div>
    );
  }

  const age = calculateAge(appointment.patientDateOfBirth);
  const vitalFields: Array<[string, string, (value: string) => void, string]> = [
    ['Mạch', mach, setMach, 'lần/phút'],
    ['Huyết áp', huyetAp, setHuyetAp, 'vd: 120/80'],
    ['Nhiệt độ', nhietDo, setNhietDo, '°C'],
    ['Nhịp tim', nhipTim, setNhipTim, 'lần/phút'],
    ['Cân nặng', canNang, setCanNang, 'kg'],
    ['Chiều cao', chieuCao, setChieuCao, 'cm'],
    ['Đại tiện', daiTien, setDaiTien, 'ghi nhận'],
    ['Tiểu tiện', tieuTien, setTieuTien, 'ghi nhận'],
  ];

  return (
    <div className="min-h-screen bg-gray-50 pb-24 text-black">
      <DashboardHeader
        title="Nhập hộ lâm sàng"
        subtitle="Đo sinh hiệu, nhập thông tin ban đầu và triệu chứng sơ bộ theo yêu cầu bác sĩ"
      />

      <main className="mx-auto max-w-7xl space-y-6 px-4 py-6 sm:px-6 lg:px-8">
        <div className="rounded-lg border bg-white p-6 shadow-sm">
          <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-lg font-bold uppercase">Thông tin bệnh nhân</h2>
              <p className="mt-1 text-sm text-slate-600">
                Mã lịch hẹn: <span className="font-mono">{appointment.appointmentId}</span>
              </p>
            </div>
            <span className="rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
              {formatVietnameseLabel(appointment.status, APPOINTMENT_STATUS_LABELS)}
            </span>
          </div>

          <div className="grid gap-4 text-sm md:grid-cols-4">
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">Họ tên</p>
              <p className="mt-1 font-medium">{appointment.patientName || '—'}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">Mã BN</p>
              <p className="mt-1 font-medium">{appointment.patientId || '—'}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">Tuổi</p>
              <p className="mt-1 font-medium">{age ?? '—'}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">SĐT</p>
              <p className="mt-1 font-medium">{appointment.patientPhone || '—'}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">BHYT</p>
              <p className="mt-1 font-medium">{appointment.patientHealthInsuranceNumber || 'Không có'}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">Bác sĩ</p>
              <p className="mt-1 font-medium">{appointment.doctorName || '—'}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">Phòng</p>
              <p className="mt-1 font-medium">{appointment.roomName || appointment.roomId || '—'}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">Số gọi</p>
              <p className="mt-1 font-medium">{appointment.queueNumber ?? '—'}</p>
            </div>
          </div>

          <div className="mt-4 grid gap-4 text-sm md:grid-cols-3">
            <div className="rounded-md border bg-gray-50 p-3">
              <p className="text-xs font-semibold uppercase text-slate-500">Lý do khám</p>
              <p className="mt-1 whitespace-pre-wrap">{appointment.notes || 'Không ghi nhận'}</p>
            </div>
            <div className="rounded-md border bg-gray-50 p-3">
              <p className="text-xs font-semibold uppercase text-slate-500">Dị ứng</p>
              <p className="mt-1 whitespace-pre-wrap">{appointment.patientAllergies || 'Không ghi nhận'}</p>
            </div>
            <div className="rounded-md border bg-gray-50 p-3">
              <p className="text-xs font-semibold uppercase text-slate-500">Bệnh nền</p>
              <p className="mt-1 whitespace-pre-wrap">{appointment.patientBackgroundDisease || 'Không ghi nhận'}</p>
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-lg font-bold">Đo sinh hiệu</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {vitalFields.map(([label, value, setter, placeholder]) => (
              <label key={label} className="flex flex-col gap-1">
                <span className="text-xs font-medium text-slate-600">{label}</span>
                <input
                  type="text"
                  value={value}
                  onChange={(event) => setter(event.target.value)}
                  placeholder={placeholder}
                  className="rounded-md border px-3 py-2 text-sm text-black focus:border-blue-500 focus:outline-none"
                />
              </label>
            ))}
          </div>
        </div>

        <div className="rounded-lg border bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-lg font-bold">Thông tin ban đầu / triệu chứng sơ bộ</h2>
          <textarea
            value={moTa}
            onChange={(event) => setMoTa(event.target.value)}
            rows={8}
            placeholder="Nhập triệu chứng sơ bộ, diễn biến ban đầu hoặc thông tin bác sĩ yêu cầu y tá ghi nhận..."
            className="w-full resize-y rounded-md border px-3 py-2 text-sm text-black focus:border-blue-500 focus:outline-none"
          />
        </div>
      </main>

      <div className="fixed bottom-0 left-0 right-0 z-20 border-t bg-white p-4 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
          <button
            type="button"
            onClick={() => router.push('/dashboard/nurse/clinical-patients')}
            className="inline-flex items-center gap-2 text-sm font-medium text-black hover:underline"
          >
            <ArrowLeft className="h-4 w-4" />
            Quay lại hàng đợi
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="inline-flex items-center gap-2 rounded-md bg-black px-6 py-2 text-sm font-semibold text-white hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            Lưu nhập hộ
          </button>
        </div>
      </div>
    </div>
  );
}
