'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { X } from 'lucide-react';
import {
  CCCD_MESSAGE,
  CCCD_REGEX,
  EMAIL_MESSAGE,
  EMAIL_REGEX,
  FULL_NAME_MESSAGE,
  FULL_NAME_REGEX,
  PHONE_MESSAGE,
  PHONE_REGEX,
} from '@/lib/validation-rules';

type BookFor = 'self' | 'relative';

interface RelativeInfo {
  fullName: string;
  phone: string;
  dateOfBirth: string;
  relationship: string;
  email: string;
  cccd: string;
}

interface FieldErrors {
  fullName?: string;
  phone?: string;
  dateOfBirth?: string;
  relationship?: string;
  email?: string;
  cccd?: string;
  symptoms?: string;
}

/* ─── Stepper ─────────────────────────────────────────────── */
function Stepper({ step }: { step: 1 | 2 }) {
  return (
    <div className="flex items-center gap-0 mb-7">
      <div className="flex flex-col items-center flex-1">
        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold border-2 ${
          step >= 1 ? 'bg-gray-900 border-gray-900 text-white' : 'border-gray-300 text-slate-500'
        }`}>1</div>
        <span className={`text-xs mt-1 font-semibold ${step >= 1 ? 'text-gray-900' : 'text-slate-600'}`}>
          Thông tin
        </span>
      </div>
      <div className={`flex-1 h-px mb-4 ${step >= 2 ? 'bg-gray-900' : 'bg-gray-300'}`} />
      <div className="flex flex-col items-center flex-1">
        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold border-2 ${
          step >= 2 ? 'bg-gray-900 border-gray-900 text-white' : 'border-gray-300 text-slate-500'
        }`}>2</div>
        <span className={`text-xs mt-1 font-semibold ${step >= 2 ? 'text-gray-900' : 'text-slate-600'}`}>
          Chuyên khoa &amp; Giờ
        </span>
      </div>
    </div>
  );
}

/* ─── Text field helper ───────────────────────────────────── */
function Field({
  label, required, children, error,
}: {
  label: string; required?: boolean; children: React.ReactNode; error?: string;
}) {
  return (
    <div>
      <label className="block text-sm text-gray-700 mb-1.5">
        {label}{required && <span className="text-red-500 ml-0.5">*</span>}
      </label>
      {children}
      {error && <p className="text-xs text-red-500 mt-1">{error}</p>}
    </div>
  );
}

const inputCls = (err?: string) =>
  `w-full border rounded px-3 py-2 text-sm text-gray-800 placeholder-gray-400 focus:outline-none ${
    err ? 'border-red-400 focus:border-red-500' : 'border-gray-300 focus:border-gray-500'
  }`;

function getLocalDateString(offsetDays = 0) {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/* ─── Main Page ───────────────────────────────────────────── */
export default function NewAppointmentPage() {
  const router = useRouter();
  const today = getLocalDateString();
  const maxBirthDate = getLocalDateString(-1);

  const [bookFor, setBookFor] = useState<BookFor>('self');
  const [symptoms, setSymptoms] = useState('');

  const [relative, setRelative] = useState<RelativeInfo>({
    fullName: '', phone: '', dateOfBirth: '', relationship: '', email: '', cccd: '',
  });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const setRel = (key: keyof RelativeInfo, val: string) => {
    setRelative((prev) => ({ ...prev, [key]: val }));
    setFieldErrors((prev) => ({ ...prev, [key]: undefined }));
  };

  const setSymptomsValue = (value: string) => {
    setSymptoms(value);
    setFieldErrors((prev) => ({ ...prev, symptoms: undefined }));
  };

  const handleBookForChange = (val: BookFor) => {
    setBookFor(val);
    setFieldErrors({});
  };

  const validate = (): boolean => {
    const errs: FieldErrors = {};
    if (!symptoms.trim()) errs.symptoms = 'Vui lòng nhập lý do khám / triệu chứng.';
    if (bookFor === 'relative') {
      if (!relative.fullName.trim()) errs.fullName = 'Vui lòng nhập họ tên người thân.';
      else if (!FULL_NAME_REGEX.test(relative.fullName.trim())) errs.fullName = FULL_NAME_MESSAGE;
      if (!relative.phone.trim()) errs.phone = 'Vui lòng nhập số điện thoại.';
      else if (!PHONE_REGEX.test(relative.phone.trim())) errs.phone = PHONE_MESSAGE;
      if (!relative.dateOfBirth) errs.dateOfBirth = 'Vui lòng chọn ngày sinh.';
      else if (relative.dateOfBirth >= today) errs.dateOfBirth = 'Ngày sinh phải trước ngày hôm nay.';
      if (!relative.relationship.trim()) errs.relationship = 'Vui lòng nhập quan hệ với bạn.';
      if (relative.email.trim() && !EMAIL_REGEX.test(relative.email.trim())) errs.email = EMAIL_MESSAGE;
      if (relative.cccd.trim() && !CCCD_REGEX.test(relative.cccd.trim())) errs.cccd = CCCD_MESSAGE;
    }
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleNext = () => {
    if (!validate()) return;
    sessionStorage.setItem(
      'appt_draft',
      JSON.stringify({ bookFor, symptoms: symptoms.trim(), relative: bookFor === 'relative' ? relative : null })
    );
    router.push('/dashboard/patient/appointments/new/step2');
  };

  const handleCancel = () => {
    sessionStorage.removeItem('appt_draft');
    router.push('/dashboard/patient/appointments');
  };

  const headerTitle = bookFor === 'relative'
    ? 'Đặt Lịch Khám Mới – Cho Người Thân'
    : 'Đặt Lịch Khám Mới – Cho Bản Thân';

  return (
    <div className="min-h-full bg-gray-100 flex items-start justify-center pt-12 p-4">
      <div className="bg-white rounded-xl shadow-lg w-full max-w-2xl overflow-hidden">

        {/* Header */}
        <div className="flex items-center justify-between px-7 py-5 border-b border-gray-200">
          <h1 className="text-base font-bold text-gray-900 uppercase tracking-wide">
            {headerTitle}
          </h1>
          <button
            onClick={handleCancel}
            className="rounded p-1 text-slate-500 transition-colors hover:text-gray-700"
            aria-label="Đóng"
          >
            <X size={20} />
          </button>
        </div>

        {/* Body */}
        <div className="px-8 pt-7 pb-4">
          <Stepper step={1} />

          {/* Đặt lịch cho ai? */}
          <div className="mb-6 border border-gray-200 rounded-lg px-5 py-4">
            <p className="text-sm font-semibold text-gray-700 mb-3">
              Đặt lịch cho ai? <span className="text-red-500">*</span>
            </p>
            <div className="flex items-center gap-8">
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="radio" name="bookFor" value="self"
                  checked={bookFor === 'self'}
                  onChange={() => handleBookForChange('self')}
                  className="w-4 h-4 accent-gray-900"
                />
                <span className="text-sm text-gray-800">Bản thân</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="radio" name="bookFor" value="relative"
                  checked={bookFor === 'relative'}
                  onChange={() => handleBookForChange('relative')}
                  className="w-4 h-4 accent-gray-900"
                />
                <span className="text-sm text-gray-800">Người thân</span>
              </label>
            </div>
          </div>

          {/* Relative fields — shown only when bookFor === 'relative' */}
          {bookFor === 'relative' && (
            <div className="mb-6 space-y-4">
              {/* Row 1: Họ tên + SĐT */}
              <div className="grid grid-cols-2 gap-4">
                <Field label="Họ và tên người thân" required error={fieldErrors.fullName}>
                  <input
                    type="text"
                    value={relative.fullName}
                    onChange={(e) => setRel('fullName', e.target.value)}
                    placeholder="Nhập họ tên người thân..."
                    maxLength={255}
                    className={inputCls(fieldErrors.fullName)}
                  />
                </Field>
                <Field label="Số điện thoại" required error={fieldErrors.phone}>
                  <input
                    type="tel"
                    value={relative.phone}
                    onChange={(e) => setRel('phone', e.target.value.replace(/\D/g, '').slice(0, 11))}
                    placeholder="Nhập SĐT người thân..."
                    maxLength={11}
                    className={inputCls(fieldErrors.phone)}
                  />
                </Field>
              </div>

              {/* Row 2: Ngày sinh + Quan hệ */}
              <div className="grid grid-cols-2 gap-4">
                <Field label="Ngày tháng năm sinh" required error={fieldErrors.dateOfBirth}>
                  <input
                    type="date"
                    value={relative.dateOfBirth}
                    onChange={(e) => setRel('dateOfBirth', e.target.value)}
                    max={maxBirthDate}
                    className={inputCls(fieldErrors.dateOfBirth)}
                  />
                </Field>
                <Field label="Quan hệ với bạn" required error={fieldErrors.relationship}>
                  <input
                    type="text"
                    value={relative.relationship}
                    onChange={(e) => setRel('relationship', e.target.value)}
                    placeholder="VD: Vợ, Chồng, Con, Bố, Mẹ..."
                    maxLength={50}
                    className={inputCls(fieldErrors.relationship)}
                  />
                </Field>
              </div>

              {/* Row 3: Email + CCCD */}
              <div className="grid grid-cols-2 gap-4">
                <Field label="Email" error={fieldErrors.email}>
                  <input
                    type="email"
                    value={relative.email}
                    onChange={(e) => setRel('email', e.target.value)}
                    placeholder="Nhập email người thân..."
                    maxLength={30}
                    className={inputCls(fieldErrors.email)}
                  />
                </Field>
                <Field label="Căn cước công dân (CCCD)" error={fieldErrors.cccd}>
                  <input
                    type="text"
                    value={relative.cccd}
                    onChange={(e) => setRel('cccd', e.target.value.replace(/\D/g, '').slice(0, 12))}
                    placeholder="Nhập số CCCD..."
                    maxLength={12}
                    className={inputCls(fieldErrors.cccd)}
                  />
                </Field>
              </div>
            </div>
          )}

          {/* Lý do khám */}
          <div className="mb-2">
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Lý do khám / Triệu chứng <span className="text-red-500">*</span>
            </label>
            <textarea
              value={symptoms}
              onChange={(e) => setSymptomsValue(e.target.value)}
              placeholder="Mô tả chi tiết triệu chứng..."
              rows={5}
              maxLength={1000}
              className={`w-full border rounded-lg px-4 py-3 text-sm text-gray-800 placeholder-gray-400 resize-none focus:outline-none ${fieldErrors.symptoms ? 'border-red-400 focus:border-red-500' : 'border-gray-300 focus:border-gray-500'}`}
            />
            {fieldErrors.symptoms && <p className="mt-1 text-xs text-red-500">{fieldErrors.symptoms}</p>}
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-8 py-5 border-t border-gray-200 bg-gray-50">
          <button
            onClick={handleCancel}
            className="px-5 py-2.5 text-sm font-semibold border border-gray-300 text-gray-700 rounded hover:bg-gray-100 transition-colors"
          >
            HỦY BỎ
          </button>
          <button
            onClick={handleNext}
            className="px-6 py-2.5 text-sm font-bold bg-gray-900 text-white rounded hover:bg-gray-700 transition-colors"
          >
            TIẾP THEO (CHỌN CHUYÊN KHOA &amp; GIỜ) →
          </button>
        </div>
      </div>
    </div>
  );
}
