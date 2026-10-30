'use client';

import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import type { AdminPatientRecord } from '@/services/patient.service';
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

export interface PatientFormValues {
  fullName: string;
  email: string;
  phoneNumber: string;
  cccd: string;
  gender: '' | 'MALE' | 'FEMALE' | 'OTHER';
  dateOfBirth: string;
  bloodType: string;
  healthInsuranceNumber: string;
  allergies: string;
  address: string;
  height: string;
  weight: string;
  backgroundDisease: string;
  emergencyContactName: string;
  emergencyContactPhone: string;
  emergencyContactRelationship: string;
}

interface PatientFormModalProps {
  isOpen: boolean;
  mode: 'create' | 'edit';
  initialData?: AdminPatientRecord | null;
  loading?: boolean;
  onClose: () => void;
  onSubmit: (values: PatientFormValues) => Promise<void> | void;
}

const DEFAULT_VALUES: PatientFormValues = {
  fullName: '',
  email: '',
  phoneNumber: '',
  cccd: '',
  gender: '',
  dateOfBirth: '',
  bloodType: '',
  healthInsuranceNumber: '',
  allergies: '',
  address: '',
  height: '',
  weight: '',
  backgroundDisease: '',
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelationship: '',
};

const BLOOD_TYPES = ['', 'A', 'B', 'AB', 'O', 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

type PatientFormErrors = Partial<Record<keyof PatientFormValues, string>>;

function buildFormValues(initialData?: AdminPatientRecord | null): PatientFormValues {
  if (!initialData) {
    return { ...DEFAULT_VALUES };
  }

  return {
    fullName: initialData.fullName ?? '',
    email: initialData.email ?? '',
    phoneNumber: initialData.phoneNumber ?? '',
    cccd: initialData.cccd ?? '',
    gender: (initialData.gender as PatientFormValues['gender']) ?? '',
    dateOfBirth: initialData.dateOfBirth ? String(initialData.dateOfBirth).slice(0, 10) : '',
    bloodType: initialData.bloodType ?? '',
    healthInsuranceNumber: initialData.healthInsuranceNumber ?? '',
    allergies: initialData.allergies ?? '',
    address: initialData.address ?? '',
    height: initialData.height != null ? String(initialData.height) : '',
    weight: initialData.weight != null ? String(initialData.weight) : '',
    backgroundDisease: initialData.backgroundDisease ?? '',
    emergencyContactName: initialData.emergencyContactName ?? '',
    emergencyContactPhone: initialData.emergencyContactPhone ?? '',
    emergencyContactRelationship: initialData.emergencyContactRelationship ?? '',
  };
}

export function PatientFormModal({
  isOpen,
  mode,
  initialData,
  loading = false,
  onClose,
  onSubmit,
}: PatientFormModalProps) {
  const [formValues, setFormValues] = useState<PatientFormValues>(DEFAULT_VALUES);
  const [formErrors, setFormErrors] = useState<PatientFormErrors>({});

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    setFormValues(buildFormValues(initialData));
    setFormErrors({});
  }, [initialData, isOpen]);

  if (!isOpen) {
    return null;
  }

  const handleChange = <K extends keyof PatientFormValues>(field: K, value: PatientFormValues[K]) => {
    let normalizedValue = value;
    if (['phoneNumber', 'cccd', 'emergencyContactPhone'].includes(field)) {
      normalizedValue = value.replace(/\D/g, '') as PatientFormValues[K];
    }
    setFormValues((prev) => ({ ...prev, [field]: normalizedValue }));
    setFormErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  const handleClose = () => {
    setFormValues(buildFormValues(initialData));
    setFormErrors({});
    onClose();
  };

  const validateForm = () => {
    const errors: PatientFormErrors = {};
    const fullName = formValues.fullName.trim();
    const email = formValues.email.trim();
    const phoneNumber = formValues.phoneNumber.trim();
    const cccd = formValues.cccd.trim();
    const emergencyContactName = formValues.emergencyContactName.trim();
    const emergencyContactPhone = formValues.emergencyContactPhone.trim();
    const emergencyContactRelationship = formValues.emergencyContactRelationship.trim();

    if (!fullName) errors.fullName = 'Vui lòng nhập họ tên.';
    else if (fullName.length > 100) errors.fullName = 'Họ tên không được vượt quá 100 ký tự.';
    else if (!FULL_NAME_REGEX.test(fullName)) errors.fullName = FULL_NAME_MESSAGE;

    if (!email) errors.email = 'Vui lòng nhập email.';
    else if (!EMAIL_REGEX.test(email)) errors.email = EMAIL_MESSAGE;

    if (!phoneNumber) errors.phoneNumber = 'Vui lòng nhập số điện thoại.';
    else if (!PHONE_REGEX.test(phoneNumber)) errors.phoneNumber = PHONE_MESSAGE;

    if (cccd && !CCCD_REGEX.test(cccd)) errors.cccd = CCCD_MESSAGE;
    if (emergencyContactName && !FULL_NAME_REGEX.test(emergencyContactName)) {
      errors.emergencyContactName = FULL_NAME_MESSAGE;
    }
    if (emergencyContactPhone && !PHONE_REGEX.test(emergencyContactPhone)) {
      errors.emergencyContactPhone = PHONE_MESSAGE;
    }
    if (emergencyContactRelationship.length > 50) {
      errors.emergencyContactRelationship = 'Mối quan hệ không được vượt quá 50 ký tự.';
    }

    if (formValues.height && Number(formValues.height) <= 0) {
      errors.height = 'Chiều cao phải lớn hơn 0.';
    }
    if (formValues.weight && Number(formValues.weight) <= 0) {
      errors.weight = 'Cân nặng phải lớn hơn 0.';
    }

    return errors;
  };

  const handleSubmit = async () => {
    const errors = validateForm();
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    await onSubmit({
      ...formValues,
      fullName: formValues.fullName.trim(),
      email: formValues.email.trim(),
      phoneNumber: formValues.phoneNumber.trim(),
      cccd: formValues.cccd.trim(),
    });
  };

  const errorMessages = Object.values(formErrors).filter(Boolean);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
      <div className="bg-white w-full max-w-4xl max-h-[90vh] overflow-hidden rounded-lg border border-slate-200 shadow-xl flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 bg-slate-50">
          <h2 className="text-lg font-bold text-slate-800">
            {mode === 'create' ? 'Thêm bệnh nhân mới' : 'Cập nhật bệnh nhân'}
          </h2>
          <button
            type="button"
            onClick={handleClose}
            className="inline-flex h-9 w-9 items-center justify-center rounded-full text-slate-500 hover:bg-slate-100 hover:text-slate-700"
            aria-label="Dong"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="p-6 overflow-y-auto space-y-6 text-slate-800">
          {errorMessages.length > 0 && (
            <div className="rounded border border-red-200 bg-red-50 p-3 text-sm text-red-700">
              {errorMessages[0]}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold mb-1">Họ và tên</label>
              <input
                type="text"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.fullName}
                onChange={(e) => handleChange('fullName', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">Email</label>
              <input
                type="email"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.email}
                onChange={(e) => handleChange('email', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">Số điện thoại</label>
              <input
                type="text"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.phoneNumber}
                onChange={(e) => handleChange('phoneNumber', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">CCCD</label>
              <input
                type="text"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.cccd}
                onChange={(e) => handleChange('cccd', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">Giới tính</label>
              <select
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm bg-white"
                value={formValues.gender}
                onChange={(e) => handleChange('gender', e.target.value as PatientFormValues['gender'])}
              >
                <option value="">Chọn giới tính</option>
                <option value="MALE">Nam</option>
                <option value="FEMALE">Nữ</option>
                <option value="OTHER">Khác</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">Ngày sinh</label>
              <input
                type="date"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.dateOfBirth}
                onChange={(e) => handleChange('dateOfBirth', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">Nhóm máu</label>
              <select
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm bg-white"
                value={formValues.bloodType}
                onChange={(e) => handleChange('bloodType', e.target.value)}
              >
                {BLOOD_TYPES.map((bloodType) => (
                  <option key={bloodType || 'empty'} value={bloodType}>
                    {bloodType || 'Chọn nhóm máu'}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">Số BHYT</label>
              <input
                type="text"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.healthInsuranceNumber}
                onChange={(e) => handleChange('healthInsuranceNumber', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">Chiều cao (cm)</label>
              <input
                type="number"
                step="0.01"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.height}
                onChange={(e) => handleChange('height', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">Cân nặng (kg)</label>
              <input
                type="number"
                step="0.01"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.weight}
                onChange={(e) => handleChange('weight', e.target.value)}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold mb-1">Địa chỉ</label>
            <input
              type="text"
              className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
              value={formValues.address}
              onChange={(e) => handleChange('address', e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-semibold mb-1">Dị ứng</label>
            <textarea
              className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
              rows={2}
              value={formValues.allergies}
              onChange={(e) => handleChange('allergies', e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-semibold mb-1">Tiền sử bệnh</label>
            <textarea
              className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
              rows={2}
              value={formValues.backgroundDisease}
              onChange={(e) => handleChange('backgroundDisease', e.target.value)}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-semibold mb-1">Người liên hệ khẩn cấp</label>
              <input
                type="text"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.emergencyContactName}
                onChange={(e) => handleChange('emergencyContactName', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">SĐT liên hệ khẩn cấp</label>
              <input
                type="text"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.emergencyContactPhone}
                onChange={(e) => handleChange('emergencyContactPhone', e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1">Mối quan hệ</label>
              <input
                type="text"
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                value={formValues.emergencyContactRelationship}
                onChange={(e) => handleChange('emergencyContactRelationship', e.target.value)}
              />
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 px-6 py-4 border-t border-slate-200 bg-slate-50">
          <button
            type="button"
            onClick={handleClose}
            className="px-4 py-2 text-sm font-semibold border border-slate-300 rounded bg-white hover:bg-slate-100"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={loading}
            className="px-4 py-2 text-sm font-semibold rounded bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? 'Đang lưu...' : mode === 'create' ? 'Tạo bệnh nhân' : 'Lưu thay đổi'}
          </button>
        </div>
      </div>
    </div>
  );
}
