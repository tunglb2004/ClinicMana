'use client';

import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { appointmentService, AvailableSlot } from '@/services/appointment.service';
import { catalogService, getUnavailableServiceAddMessage, ServiceItem } from '@/services/payment.service';
import { healthRecordService } from '@/services/health-record.service';
import { patientService, PatientResponse } from '@/services/patient.service';
import { FollowUpOption } from '@/types/health-record.types';
import { useToast } from '@/hooks/use-toast';
import { useAuthStore } from '@/store/auth.store';
import { ArrowLeft, Loader2, Search, UserPlus, X } from 'lucide-react';
import { ROLES } from '@/lib/roleRedirect';
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

interface ClinicalRoom {
  roomId: string;
  roomName: string;
  doctorId: string;
  doctorName: string;
}

interface NewPatientForm {
  fullName: string;
  email: string;
  phoneNumber: string;
  cccd: string;
  gender: '' | 'MALE' | 'FEMALE' | 'OTHER';
  dateOfBirth: string;
  healthInsuranceNumber: string;
  address: string;
}

const DEFAULT_NEW_PATIENT_FORM: NewPatientForm = {
  fullName: '',
  email: '',
  phoneNumber: '',
  cccd: '',
  gender: '',
  dateOfBirth: '',
  healthInsuranceNumber: '',
  address: '',
};

function getLocalDateInputValue() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatCurrency(price: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}

function isClinicalService(service: ServiceItem) {
  return service.department?.deptType === 'CLINICAL';
}

function isParaclinicalService(service: ServiceItem) {
  return service.department?.deptType === 'PARACLINICAL';
}

function getServiceTypeLabel(service: ServiceItem) {
  if (service.department?.deptType === 'PARACLINICAL') return 'Cận lâm sàng';
  return 'Lâm sàng';
}

export default function CreateAppointmentPage() {
  const router = useRouter();
  const { user } = useAuthStore();
  const { toast } = useToast();

  const [loading, setLoading] = useState(false);
  const [servicesLoading, setServicesLoading] = useState(true);
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [clinicalRooms, setClinicalRooms] = useState<ClinicalRoom[]>([]);
  const [roomsLoading, setRoomsLoading] = useState(false);

  // Date & slot state
  const [selectedDate, setSelectedDate] = useState('');
  const [slots, setSlots] = useState<AvailableSlot[]>([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null);

  // Patient search state
  const [patientId, setPatientId] = useState('');
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [patientSearch, setPatientSearch] = useState('');
  const [patientResults, setPatientResults] = useState<PatientResponse[]>([]);
  const [patientSearchLoading, setPatientSearchLoading] = useState(false);
  const [showPatientDropdown, setShowPatientDropdown] = useState(false);
  const patientSearchRef = useRef<HTMLDivElement>(null);
  const [patientMode, setPatientMode] = useState<'existing' | 'new'>('existing');
  const [newPatient, setNewPatient] = useState<NewPatientForm>(DEFAULT_NEW_PATIENT_FORM);

  const [selectedDoctorId, setSelectedDoctorId] = useState('');
  const [notes, setNotes] = useState('');
  const [selectedServices, setSelectedServices] = useState<string[]>([]);
  const [visitMode, setVisitMode] = useState<'new' | 'followUp'>('new');
  const [followUpOptions, setFollowUpOptions] = useState<FollowUpOption[]>([]);
  const [followUpLoading, setFollowUpLoading] = useState(false);
  const [selectedFollowUpRecordId, setSelectedFollowUpRecordId] = useState('');
  const [error, setError] = useState('');
  const [slotError, setSlotError] = useState('');

  // Redirect if not receptionist or cashier
  useEffect(() => {
    if (user && !user.roles.includes(ROLES.RECEPTIONIST) && !user.roles.includes(ROLES.CASHIER)) {
      router.push('/unauthorized');
    }
  }, [user, router]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (patientSearchRef.current && !patientSearchRef.current.contains(e.target as Node)) {
        setShowPatientDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Search patients from API with debounce
  const searchPatients = useCallback(async (keyword: string) => {
    if (!keyword.trim()) {
      setPatientResults([]);
      setShowPatientDropdown(false);
      return;
    }
    setPatientSearchLoading(true);
    setShowPatientDropdown(true);
    try {
      const res = await patientService.getPatients(1, 10, keyword);
      setPatientResults(res.content ?? []);
    } catch {
      setPatientResults([]);
    } finally {
      setPatientSearchLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => searchPatients(patientSearch), 300);
    return () => clearTimeout(timer);
  }, [patientSearch, searchPatients]);

  const handleSelectPatient = (patient: PatientResponse) => {
    setSelectedPatient(patient);
    setPatientId(patient.patientId);
    setPatientSearch('');
    setShowPatientDropdown(false);
  };

  const handleClearPatient = () => {
    setSelectedPatient(null);
    setPatientId('');
    setPatientSearch('');
    setVisitMode('new');
    setFollowUpOptions([]);
    setSelectedFollowUpRecordId('');
  };

  const handleChangePatientMode = (mode: 'existing' | 'new') => {
    setPatientMode(mode);
    setError('');

    if (mode === 'new') {
      handleClearPatient();
      setVisitMode('new');
      setFollowUpOptions([]);
      setSelectedFollowUpRecordId('');
      setShowPatientDropdown(false);
      return;
    }

    setNewPatient(DEFAULT_NEW_PATIENT_FORM);
  };

  const updateNewPatientField = <K extends keyof NewPatientForm>(field: K, value: NewPatientForm[K]) => {
    let normalizedValue = value;
    if (field === 'phoneNumber' || field === 'cccd') {
      normalizedValue = value.replace(/\D/g, '') as NewPatientForm[K];
    }
    setNewPatient(prev => ({ ...prev, [field]: normalizedValue }));
  };

  const validateNewPatient = () => {
    const fullName = newPatient.fullName.trim();
    const email = newPatient.email.trim();
    const phoneNumber = newPatient.phoneNumber.trim();
    const cccd = newPatient.cccd.trim();

    if (!fullName) return 'Vui lòng nhập họ tên bệnh nhân mới.';
    if (!FULL_NAME_REGEX.test(fullName)) return FULL_NAME_MESSAGE;
    if (!email) return 'Vui lòng nhập email bệnh nhân mới.';
    if (!EMAIL_REGEX.test(email)) return EMAIL_MESSAGE;
    if (!phoneNumber) return 'Vui lòng nhập số điện thoại bệnh nhân mới.';
    if (!PHONE_REGEX.test(phoneNumber)) return PHONE_MESSAGE;
    if (cccd && !CCCD_REGEX.test(cccd)) return CCCD_MESSAGE;
    return '';
  };

  const buildNewPatientPayload = () => ({
    fullName: newPatient.fullName.trim(),
    email: newPatient.email.trim(),
    phoneNumber: newPatient.phoneNumber.trim(),
    cccd: newPatient.cccd.trim() || undefined,
    gender: newPatient.gender || undefined,
    dateOfBirth: newPatient.dateOfBirth || undefined,
    healthInsuranceNumber: newPatient.healthInsuranceNumber.trim() || undefined,
    address: newPatient.address.trim() || undefined,
  });

  // Load services from API
  useEffect(() => {
    catalogService.getServices()
      .then(data => setServices(data))
      .catch(() => {
        toast({ description: 'Không thể tải danh sách dịch vụ', variant: 'error' });
        setServices([]);
      })
      .finally(() => setServicesLoading(false));
  }, [toast]);

  const selectedServiceItems = useMemo(
    () => services.filter(service => selectedServices.includes(service.serviceId)),
    [services, selectedServices]
  );

  const selectedFollowUpOption = useMemo(
    () => followUpOptions.find(option => option.recordId === selectedFollowUpRecordId) ?? null,
    [followUpOptions, selectedFollowUpRecordId]
  );

  const availableServiceItems = useMemo(
    () => services.filter(service => !selectedServices.includes(service.serviceId)),
    [services, selectedServices]
  );

  const selectedClinicalServiceIds = useMemo(
    () => selectedServiceItems.filter(isClinicalService).map(service => service.serviceId),
    [selectedServiceItems]
  );

  const selectedParaclinicalServiceIds = useMemo(
    () => selectedServiceItems.filter(isParaclinicalService).map(service => service.serviceId),
    [selectedServiceItems]
  );

  const hasClinicalSelection = selectedClinicalServiceIds.length > 0;

  const schedulingServiceIds = useMemo(
    () => selectedClinicalServiceIds.length > 0 ? selectedClinicalServiceIds : selectedParaclinicalServiceIds,
    [selectedClinicalServiceIds, selectedParaclinicalServiceIds]
  );

  // Load clinical rooms after the service, date, and slot are selected.
  useEffect(() => {
    if (!hasClinicalSelection || visitMode === 'followUp' || !selectedDate || !selectedSlot) {
      setClinicalRooms([]);
      setSelectedDoctorId('');
      return;
    }
    setRoomsLoading(true);
    appointmentService.getClinicalRooms(selectedSlot.slotId, selectedDate, selectedClinicalServiceIds)
      .then(res => setClinicalRooms(res ?? []))
      .catch(() => setClinicalRooms([]))
      .finally(() => setRoomsLoading(false));
  }, [hasClinicalSelection, selectedDate, selectedClinicalServiceIds, selectedSlot, visitMode]);

  // Load available slots when date or selected services change
  useEffect(() => {
    if (!selectedDate || schedulingServiceIds.length === 0) {
      setSlotError('');
      setSlots([]);
      setSelectedSlot(null);
      return;
    }
    setSlotsLoading(true);
    setSlotError('');
    setSelectedSlot(null);
    setSelectedDoctorId('');
    setClinicalRooms([]);
    appointmentService.getAvailableSlots(selectedDate, schedulingServiceIds)
      .then(setSlots)
      .catch((err: unknown) => {
        const e = err as { response?: { data?: { message?: string } }; message?: string };
        setSlots([]);
        setSlotError(e?.response?.data?.message ?? e?.message ?? 'Không thể tải ca khám cho lựa chọn hiện tại.');
      })
      .finally(() => setSlotsLoading(false));
  }, [selectedDate, schedulingServiceIds]);

  useEffect(() => {
    if (visitMode !== 'followUp' || !patientId) {
      setFollowUpOptions([]);
      setSelectedFollowUpRecordId('');
      return;
    }

    setFollowUpLoading(true);
    setError('');
    healthRecordService.getFollowUpOptions(patientId)
      .then(options => setFollowUpOptions(options ?? []))
      .catch(() => {
        setFollowUpOptions([]);
        setError('Không thể tải danh sách hồ sơ tái khám của bệnh nhân.');
      })
      .finally(() => setFollowUpLoading(false));
  }, [patientId, visitMode]);

  useEffect(() => {
    if (visitMode !== 'followUp') {
      return;
    }

    if (!selectedFollowUpOption) {
      setSelectedServices([]);
      setSelectedDoctorId('');
      return;
    }

    setSelectedServices([selectedFollowUpOption.clinicalServiceId]);
    setSelectedDoctorId('');
  }, [selectedFollowUpOption, visitMode]);

  const toggleService = (id: string) => {
    if (visitMode === 'followUp') {
      return;
    }
    setSelectedServices(prev => {
      const service = services.find(candidate => candidate.serviceId === id);
      const isRemoving = prev.includes(id);

      if (!isRemoving && service?.hasOperationalRoom === false) {
        setError(getUnavailableServiceAddMessage(service));
        return prev;
      }

      if (!isRemoving && service && isClinicalService(service) && selectedClinicalServiceIds.length > 0) {
        setError('Mỗi lịch hẹn chỉ được chọn 1 dịch vụ lâm sàng.');
        return prev;
      }

      setError('');

      const next = isRemoving ? prev.filter(serviceId => serviceId !== id) : [...prev, id];
      const nextSelectedServices = services.filter(service => next.includes(service.serviceId));
      const nextClinicalCount = nextSelectedServices.filter(isClinicalService).length;

      if (nextClinicalCount === 0) {
        setSelectedDoctorId('');
      }

      return next;
    });
  };

  function formatTime(t: string) {
    return t && t.length > 5 ? t.substring(0, 5) : t;
  }

  function formatSlotCapacity(slot: AvailableSlot) {
    return `${slot.currentCount}/${slot.maxPatients} BN`;
  }

  const today = getLocalDateInputValue();

  const handleCreate = async () => {
    setError('');
    if (patientMode === 'existing' && !patientId) {
      setError('Vui lòng chọn bệnh nhân.');
      return;
    }
    if (patientMode === 'new') {
      const newPatientError = validateNewPatient();
      if (newPatientError) {
        setError(newPatientError);
        return;
      }
    }
    if (visitMode === 'followUp' && !selectedFollowUpRecordId) {
      setError('Vui lòng chọn hồ sơ tái khám.');
      return;
    }
    if (selectedServices.length === 0) {
      setError('Vui lòng chọn ít nhất 1 dịch vụ.');
      return;
    }
    if (schedulingServiceIds.length === 0) {
      setError('Không thể xác định nhóm dịch vụ để xếp ca khám.');
      return;
    }
    if (!selectedDate) {
      setError('Vui lòng chọn ngày khám.');
      return;
    }
    if (!selectedSlot) {
      setError('Vui lòng chọn ca khám.');
      return;
    }

    if (!notes.trim()) {
      setError('Vui lòng nhập lý do khám.');
      return;
    }

    setLoading(true);
    try {
      await appointmentService.createAppointment({
        patientId: patientMode === 'existing' ? patientId : undefined,
        newPatient: patientMode === 'new' ? buildNewPatientPayload() : undefined,
        serviceIds: selectedServices,
        appointmentDate: selectedDate,
        appointmentTime: selectedSlot.startTime,
        status: 'WAITING',
        notes: notes.trim(),
        doctorId: patientMode === 'new' || visitMode !== 'followUp' ? selectedDoctorId || undefined : undefined,
        followUpSourceRecordId: patientMode === 'existing' && visitMode === 'followUp' ? selectedFollowUpRecordId : undefined,
      });

      toast({
        description: patientMode === 'new'
          ? 'Tạo lịch hẹn và tài khoản bệnh nhân thành công. Thông tin đăng nhập đã được gửi qua email.'
          : 'Tạo lịch hẹn thành công!',
        variant: 'success',
      });
      router.push('/dashboard/receptionist/patients?created=true');
    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Có lỗi xảy ra khi tạo lịch hẹn';
      setError(message);
      toast({
        description: message,
        variant: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    router.back();
  };

  const totalCost = selectedServiceItems.reduce((acc, service) => acc + Number(service.price), 0);

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      {/* Header */}
      <div className="mb-8 flex items-center gap-4">
        <button
          onClick={handleCancel}
          className="p-2 hover:bg-gray-200 rounded-lg transition-colors"
          title="Quay lại"
        >
          <ArrowLeft size={24} className="text-gray-700" />
        </button>
        <h1 className="text-3xl font-bold text-[#1a2b3c] border-l-4 border-black pl-3 uppercase">
          Tạo Yêu cầu Khám bệnh
        </h1>
      </div>

      {/* Main content */}
      <div className="bg-white rounded-lg shadow-lg p-8 max-w-4xl mx-auto">
        <div className="mb-8 space-y-6">
          <div className="rounded-lg border border-gray-200 p-4">
            <div className="mb-4 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => handleChangePatientMode('existing')}
                className={`rounded border px-4 py-2 text-sm font-semibold ${patientMode === 'existing' ? 'border-[#334155] bg-[#334155] text-white' : 'border-gray-300 text-gray-700 hover:bg-gray-50'}`}
              >
                Bệnh nhân đã có hồ sơ
              </button>
              <button
                type="button"
                onClick={() => handleChangePatientMode('new')}
                className={`inline-flex items-center gap-2 rounded border px-4 py-2 text-sm font-semibold ${patientMode === 'new' ? 'border-[#334155] bg-[#334155] text-white' : 'border-gray-300 text-gray-700 hover:bg-gray-50'}`}
              >
                <UserPlus size={16} />
                Bệnh nhân mới
              </button>
            </div>

            {patientMode === 'existing' ? (
              <div className="flex flex-col gap-2" ref={patientSearchRef}>
                <label className="text-sm font-bold text-gray-700">
                  Bệnh nhân <span className="text-red-500">*</span>
                </label>

                {selectedPatient ? (
                  <div className="border border-gray-300 rounded-md p-3 bg-gray-50 flex items-center justify-between">
                    <span className="text-sm text-gray-800">
                      <span className="font-semibold">{selectedPatient.fullName}</span>
                      {selectedPatient.phoneNumber ? ` - ${selectedPatient.phoneNumber}` : ''}
                      {` - `}
                      <span className="font-mono text-slate-600">{selectedPatient.patientId}</span>
                    </span>
                    <button
                      onClick={handleClearPatient}
                      className="ml-2 text-slate-500 hover:text-slate-800"
                      disabled={loading}
                      type="button"
                    >
                      <X size={16} />
                    </button>
                  </div>
                ) : (
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Search size={15} className="text-slate-500" />
                    </div>
                    <input
                      type="text"
                      placeholder="Tìm theo tên, mã BN, số điện thoại..."
                      className="w-full rounded-md border border-gray-200 py-3 pl-9 pr-3 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-gray-400"
                      value={patientSearch}
                      onChange={e => setPatientSearch(e.target.value)}
                      onFocus={() => patientSearch.trim() && setShowPatientDropdown(true)}
                      disabled={loading}
                    />
                    {showPatientDropdown && (
                      <div className="absolute z-30 top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-64 overflow-y-auto">
                        {patientSearchLoading ? (
                          <div className="flex items-center gap-2 px-4 py-3 text-sm text-slate-600">
                            <Loader2 size={14} className="animate-spin" /> Đang tìm kiếm...
                          </div>
                        ) : patientResults.length === 0 ? (
                          <div className="px-4 py-3 text-sm text-slate-600">Không tìm thấy bệnh nhân</div>
                        ) : (
                          patientResults.map(p => (
                            <button
                              key={p.patientId}
                              type="button"
                              className="w-full text-left px-4 py-3 hover:bg-blue-50 border-b border-gray-100 last:border-0 transition-colors"
                              onClick={() => handleSelectPatient(p)}
                            >
                              <span className="text-sm text-gray-800">
                                <span className="font-semibold">{p.fullName}</span>
                                {p.phoneNumber ? (
                                  <span className="text-slate-600"> - {p.phoneNumber}</span>
                                ) : null}
                                <span className="text-slate-500"> - </span>
                                <span className="font-mono text-slate-600">{p.patientId}</span>
                              </span>
                            </button>
                          ))
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-bold text-gray-700">Họ tên <span className="text-red-500">*</span></label>
                    <input
                      type="text"
                      value={newPatient.fullName}
                      onChange={e => updateNewPatientField('fullName', e.target.value)}
                      className="rounded-md border border-gray-200 p-3 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-gray-400"
                      disabled={loading}
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-bold text-gray-700">Email <span className="text-red-500">*</span></label>
                    <input
                      type="email"
                      value={newPatient.email}
                      onChange={e => updateNewPatientField('email', e.target.value)}
                      className="rounded-md border border-gray-200 p-3 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-gray-400"
                      disabled={loading}
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-bold text-gray-700">Số điện thoại <span className="text-red-500">*</span></label>
                    <input
                      type="text"
                      value={newPatient.phoneNumber}
                      onChange={e => updateNewPatientField('phoneNumber', e.target.value)}
                      className="rounded-md border border-gray-200 p-3 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-gray-400"
                      disabled={loading}
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-bold text-gray-700">CCCD</label>
                    <input
                      type="text"
                      value={newPatient.cccd}
                      onChange={e => updateNewPatientField('cccd', e.target.value)}
                      className="rounded-md border border-gray-200 p-3 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-gray-400"
                      disabled={loading}
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-bold text-gray-700">Giới tính</label>
                    <select
                      value={newPatient.gender}
                      onChange={e => updateNewPatientField('gender', e.target.value as NewPatientForm['gender'])}
                      className="rounded-md border border-gray-200 bg-white p-3 text-sm text-slate-900 focus:outline-none focus:border-gray-400"
                      disabled={loading}
                    >
                      <option value="">Chọn giới tính</option>
                      <option value="MALE">Nam</option>
                      <option value="FEMALE">Nữ</option>
                      <option value="OTHER">Khác</option>
                    </select>
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-bold text-gray-700">Ngày sinh</label>
                    <input
                      type="date"
                      value={newPatient.dateOfBirth}
                      onChange={e => updateNewPatientField('dateOfBirth', e.target.value)}
                      className="rounded-md border border-gray-200 p-3 text-sm text-slate-900 focus:outline-none focus:border-gray-400"
                      disabled={loading}
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-bold text-gray-700">Số BHYT</label>
                    <input
                      type="text"
                      value={newPatient.healthInsuranceNumber}
                      onChange={e => updateNewPatientField('healthInsuranceNumber', e.target.value)}
                      className="rounded-md border border-gray-200 p-3 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-gray-400"
                      disabled={loading}
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-bold text-gray-700">Địa chỉ</label>
                    <input
                      type="text"
                      value={newPatient.address}
                      onChange={e => updateNewPatientField('address', e.target.value)}
                      className="rounded-md border border-gray-200 p-3 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-gray-400"
                      disabled={loading}
                    />
                  </div>
                </div>
              </div>
            )}
          </div>

        </div>

        <div className="mb-8 rounded-lg border border-gray-200 p-4">
          <label className="mb-3 block text-sm font-bold text-gray-700">Loại lịch khám</label>
          <div className="mb-4 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => {
                setVisitMode('new');
                setSelectedFollowUpRecordId('');
                setFollowUpOptions([]);
                setSelectedServices([]);
              }}
              className={`rounded border px-4 py-2 text-sm font-semibold ${visitMode === 'new' ? 'border-[#334155] bg-[#334155] text-white' : 'border-gray-300 text-gray-700 hover:bg-gray-50'}`}
            >
              Khám mới
            </button>
            <button
              type="button"
              disabled={patientMode === 'new' || !patientId}
              onClick={() => setVisitMode('followUp')}
              className={`rounded border px-4 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50 ${visitMode === 'followUp' ? 'border-[#334155] bg-[#334155] text-white' : 'border-gray-300 text-gray-700 hover:bg-gray-50'}`}
            >
              Tái khám
            </button>
          </div>

          {visitMode === 'followUp' && (
            <div className="space-y-3">
              {!patientId ? (
                <div className="text-sm text-slate-600">Chọn bệnh nhân trước khi chọn hồ sơ tái khám.</div>
              ) : followUpLoading ? (
                <div className="flex items-center gap-2 text-sm text-slate-600">
                  <Loader2 size={14} className="animate-spin" /> Đang tải hồ sơ tái khám...
                </div>
              ) : followUpOptions.length === 0 ? (
                <div className="text-sm text-slate-600">Bệnh nhân chưa có hồ sơ lâm sàng đã hoàn tất để tái khám.</div>
              ) : (
                <select
                  value={selectedFollowUpRecordId}
                  onChange={(event) => setSelectedFollowUpRecordId(event.target.value)}
                  className="w-full rounded-md border border-gray-200 bg-white p-3 text-sm text-slate-900 focus:outline-none focus:border-gray-400"
                >
                  <option value="">-- Chọn hồ sơ tái khám --</option>
                  {followUpOptions.map(option => (
                    <option key={option.recordId} value={option.recordId}>
                      {option.recordId} - {option.clinicalServiceName} - {option.doctorName ?? 'N/A'}
                    </option>
                  ))}
                </select>
              )}
              {selectedFollowUpOption && (
                <div className="rounded border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
                  Dịch vụ được khóa: {selectedFollowUpOption.clinicalServiceName}
                  {selectedFollowUpOption.doctorName ? ` - ưu tiên BS. ${selectedFollowUpOption.doctorName}` : ''}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Service selection */}
        <div className="border-2 border-black rounded-sm mb-8">
          <div className="bg-[#f5f5f5] p-4 font-bold border-b border-black text-[#1a2b3c]">
            LỰA CHỌN KHÁM
          </div>
          <div className="p-4">
            {servicesLoading ? (
              <div className="py-6 flex justify-center">
                <Loader2 className="animate-spin text-slate-500" size={24} />
              </div>
            ) : services.length === 0 ? (
              <div className="py-6 text-center text-sm text-slate-600">Không có dịch vụ nào</div>
            ) : (
              <>
                <p className="mb-4 text-sm text-slate-600">
                  Mỗi lịch hẹn chỉ được chọn 1 dịch vụ lâm sàng. Sau khi chọn dịch vụ, chọn ngày và ca khám để lọc bác sĩ/phòng trực.
                  Dịch vụ cận lâm sàng luôn được hệ thống tự xếp.
                </p>
                <div className="mb-6 rounded-lg border bg-gray-50 p-4">
                  <div className="mb-3 flex items-center justify-between gap-4">
                    <h3 className="text-sm font-semibold uppercase text-black">Dịch vụ đã chọn</h3>
                  </div>
                  {selectedServiceItems.length === 0 ? (
                    <div className="py-2 text-sm text-slate-600">Chưa có dịch vụ nào được chọn.</div>
                  ) : (
                    <table className="w-full text-left text-sm text-[#333]">
                      <thead className="border-b border-gray-200">
                        <tr>
                          <th className="py-2 px-2 font-medium text-black uppercase w-12">STT</th>
                          <th className="py-2 px-2 font-medium text-black uppercase">Tên hạng mục</th>
                          <th className="py-2 px-2 font-medium text-black uppercase">Nhóm</th>
                          <th className="py-2 px-2 font-medium text-black uppercase text-center">Giá tiền (VNĐ)</th>
                          <th className="py-2 px-2 font-medium text-black uppercase text-center w-24">Thao tác</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100">
                        {selectedServiceItems.map((srv, idx) => (
                          <tr key={srv.serviceId} className="hover:bg-gray-100">
                            <td className="py-3 px-2 text-black">{idx + 1}</td>
                            <td className="py-3 px-2 font-medium text-black">{srv.serviceName}</td>
                            <td className="py-3 px-2 text-gray-700">{getServiceTypeLabel(srv)}</td>
                            <td className="py-3 px-2 text-center text-gray-700">
                              {formatCurrency(Number(srv.price))}
                            </td>
                            <td className="py-3 px-2 text-center">
                              <button
                                type="button"
                                disabled={loading || visitMode === 'followUp'}
                                onClick={() => toggleService(srv.serviceId)}
                                className="rounded border border-red-200 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
                              >
                                Xóa
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>

                {visitMode === 'followUp' ? (
                  <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
                    Dịch vụ tái khám được khóa theo hồ sơ cũ. Bác sĩ lâm sàng sẽ chỉ định bổ sung nếu cần.
                  </div>
                ) : availableServiceItems.length === 0 ? (
                  <div className="py-4 text-sm text-slate-600">Tất cả dịch vụ khả dụng đã được chọn.</div>
                ) : (
                  <table className="w-full text-left text-sm text-[#333] mb-4">
                    <thead className="border-b border-gray-200">
                      <tr>
                        <th className="py-2 px-2 font-medium text-black uppercase w-12">STT</th>
                        <th className="py-2 px-2 font-medium text-black uppercase">Tên hạng mục</th>
                        <th className="py-2 px-2 font-medium text-black uppercase">Nhóm</th>
                        <th className="py-2 px-2 font-medium text-black uppercase text-center">Giá tiền (VNĐ)</th>
                        <th className="py-2 px-2 font-medium text-black uppercase text-center w-24">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {availableServiceItems.map((srv, idx) => {
                        const disableAdd = isClinicalService(srv) && hasClinicalSelection;

                        return (
                        <tr key={srv.serviceId} className="hover:bg-gray-50">
                          <td className="py-3 px-2 text-black">{idx + 1}</td>
                          <td className="py-3 px-2 font-medium text-black">{srv.serviceName}</td>
                          <td className="py-3 px-2 text-gray-700">{getServiceTypeLabel(srv)}</td>
                          <td className="py-3 px-2 text-center text-gray-700">
                            {formatCurrency(Number(srv.price))}
                          </td>
                          <td className="py-3 px-2 text-center">
                            <button
                              type="button"
                              disabled={loading || disableAdd}
                              onClick={() => toggleService(srv.serviceId)}
                              className={`rounded px-3 py-1 text-xs font-medium disabled:opacity-50 ${
                                disableAdd
                                    ? 'cursor-not-allowed border border-gray-200 bg-gray-100 text-slate-500'
                                  : 'border border-black text-black hover:bg-gray-100'
                              }`}
                            >
                              {disableAdd ? 'Giới hạn' : 'Thêm'}
                            </button>
                          </td>
                        </tr>
                        );
                      })}
                    </tbody>
                  </table>
                )}
              </>
            )}
          </div>
        </div>

        {/* Total summary */}
        {selectedServices.length > 0 && (
          <div className="mb-8 p-4 bg-gray-50 rounded border border-gray-200 text-sm font-semibold text-gray-700 flex justify-between">
            <span>Tổng chi phí dịch vụ:</span>
            <span className="text-[#1a2b3c] font-bold">
              {totalCost.toLocaleString('vi-VN')} VNĐ
            </span>
          </div>
        )}

        <div className="mb-8 grid grid-cols-1 gap-6 md:grid-cols-2">
          <div className="flex flex-col gap-2">
            <label className="text-sm font-bold text-gray-700">
              Ngày khám <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              min={today}
              value={selectedDate}
              onChange={e => setSelectedDate(e.target.value)}
              className="rounded-md border border-gray-200 p-3 text-sm text-slate-900 focus:outline-none focus:border-gray-400"
              disabled={loading}
            />
          </div>
        </div>

        {/* Slot picker */}
        {selectedDate && (
          <div className="mb-8">
            <label className="text-sm font-bold text-gray-700 block mb-2">
              Ca khám <span className="text-red-500">*</span>
            </label>
            {selectedServices.length === 0 ? (
              <div className="text-sm text-slate-600">Chọn ít nhất 1 dịch vụ để xem ca còn trống.</div>
            ) : schedulingServiceIds.length === 0 ? (
              <div className="text-blue-700 text-sm">Không thể xác định nhóm dịch vụ để xếp ca khám.</div>
            ) : slotsLoading ? (
              <div className="flex items-center gap-2 text-sm text-slate-600">
                <Loader2 size={14} className="animate-spin" /> Đang tải ca khám...
              </div>
            ) : slotError ? (
              <div className="text-red-700 text-sm">{slotError}</div>
            ) : slots.length === 0 ? (
              <div className="text-sm text-slate-600">Không có ca khám nào.</div>
            ) : (
              <div className="flex flex-wrap gap-3">
                {slots.map(slot => {
                  const isSelected = selectedSlot?.slotId === slot.slotId;
                  return (
                    <button
                      key={slot.slotId}
                      type="button"
                      disabled={slot.isFull || loading}
                      onClick={() => { setSelectedSlot(slot); setSelectedDoctorId(''); }}
                      className={`px-4 py-2 rounded-lg border text-sm font-medium transition-colors ${
                        slot.isFull
                          ? 'cursor-not-allowed border-gray-200 bg-gray-100 text-slate-500'
                          : isSelected
                          ? 'border-[#334155] bg-[#334155] text-white'
                          : 'border-gray-300 bg-white text-gray-700 hover:border-gray-500'
                      }`}
                    >
                      <div>{slot.shiftName}</div>
                      <div className="text-xs mt-0.5">
                        {formatTime(slot.startTime)} – {formatTime(slot.endTime)}
                      </div>
                      <div className="text-xs mt-0.5 opacity-80">{formatSlotCapacity(slot)}</div>
                      {slot.isFull && <div className="text-xs text-red-500">(Đầy)</div>}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Doctor selection — available after a clinical service slot is selected */}
        {hasClinicalSelection && visitMode !== 'followUp' && selectedSlot && (
          <div className="mb-8">
            <label className="text-sm font-bold text-gray-700 block mb-2">
              Chọn bác sĩ lâm sàng (không bắt buộc)
              {selectedSlot && selectedDate && (
                <span className="ml-1 text-xs font-normal text-slate-600">— chỉ hiển thị phòng có bác sĩ trực trong ca đã chọn</span>
              )}
            </label>
            <p className="mb-2 text-sm text-slate-600">
              Nếu bỏ trống, hệ thống sẽ tự xếp bác sĩ và phòng lâm sàng phù hợp.
            </p>
            {selectedParaclinicalServiceIds.length > 0 && (
              <div className="mb-3 rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
                Dịch vụ cận lâm sàng vẫn được hệ thống tự xếp lịch riêng, không dùng lựa chọn bác sĩ này.
              </div>
            )}
            {roomsLoading ? (
              <div className="flex items-center gap-2 text-sm text-slate-600">
                <Loader2 size={14} className="animate-spin" /> Đang tải danh sách bác sĩ/phòng...
              </div>
            ) : clinicalRooms.length === 0 ? (
              <div className="text-sm text-slate-600">
                {selectedSlot && selectedDate
                  ? 'Không có bác sĩ/phòng lâm sàng phù hợp trực trong ca này.'
                  : 'Không có bác sĩ/phòng lâm sàng phù hợp.'}
              </div>
            ) : (
              <select
                className="w-full max-w-md rounded-md border border-gray-200 bg-white p-3 text-sm text-slate-900 focus:outline-none focus:border-gray-400"
                value={selectedDoctorId}
                onChange={e => setSelectedDoctorId(e.target.value)}
                disabled={loading}
              >
                <option value="">— Để hệ thống tự xếp bác sĩ/phòng —</option>
                {clinicalRooms.map(r => (
                  <option key={`${r.doctorId}-${r.roomId}`} value={r.doctorId}>
                    {r.roomName}{r.doctorName ? ` — BS. ${r.doctorName}` : ''}
                  </option>
                ))}
              </select>
            )}
          </div>
        )}

        {/* Notes */}
        <div className="flex flex-col gap-2 mb-8">
          <label className="text-sm font-bold text-gray-700">
            Lý do khám / Ghi chú <span className="text-red-500">*</span>
          </label>
          <textarea
            className="h-24 resize-none rounded-md border border-gray-200 p-3 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-gray-400"
            value={notes}
            onChange={e => setNotes(e.target.value)}
            disabled={loading}
          />
        </div>

        {error && (
          <div className="mb-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {/* Action buttons */}
        <div className="flex gap-3 justify-end">
          <button
            onClick={handleCancel}
            disabled={loading}
            className="px-6 py-3 border border-gray-300 text-gray-700 font-bold rounded transition-colors hover:bg-gray-50 disabled:opacity-70 uppercase text-sm tracking-wide"
          >
            Hủy
          </button>
          <button
            onClick={handleCreate}
            disabled={loading}
            className="px-6 py-3 bg-[#334155] hover:bg-[#1e293b] text-white font-bold rounded transition-colors disabled:opacity-70 flex items-center justify-center gap-2 uppercase text-sm tracking-wide"
          >
            {loading && <Loader2 className="animate-spin" size={18} />}
            {loading ? 'Đang xử lý...' : 'TẠO YÊU CẦU'}
          </button>
        </div>
      </div>
    </div>
  );
}
