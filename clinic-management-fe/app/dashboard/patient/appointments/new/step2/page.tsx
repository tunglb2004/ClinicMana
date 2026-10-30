'use client';

import { useEffect, useState, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { catalogService, getUnavailableServiceAddMessage, ServiceItem } from '@/services/payment.service';
import { appointmentService, AvailableSlot, BookAppointmentPayload } from '@/services/appointment.service';

interface DraftRelative {
  patientId?: string;
  fullName: string;
  phone?: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  relationship?: string;
  email?: string;
  cccd?: string;
}

interface DraftData {
  bookFor: 'self' | 'relative';
  symptoms: string;
  relative?: DraftRelative;
  followUpSourceRecordId?: string;
  followUpSourceRecord?: {
    recordId: string;
    examinationDate?: string;
    diagnosisSummary?: string;
    doctorName?: string;
    clinicalServiceId?: string | null;
    clinicalServiceName?: string | null;
  };
}

function formatTime(t: string) {
  if (!t) return t;
  // "HH:mm:ss" → "HH:mm"
  return t.length > 5 ? t.substring(0, 5) : t;
}

function formatCurrency(price: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}

function isClinicalService(service: ServiceItem) {
  return service.department?.deptType === 'CLINICAL';
}

function getServiceTypeLabel(service: ServiceItem) {
  if (service.department?.deptType === 'PARACLINICAL') return 'Cận lâm sàng';
  return 'Lâm sàng';
}

function getTodayString() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export default function Step2Page() {
  const router = useRouter();
  const [draft, setDraft] = useState<DraftData | null>(null);
  const isFollowUp = Boolean(draft?.followUpSourceRecordId);

  // Services
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [selectedServiceIds, setSelectedServiceIds] = useState<string[]>([]);
  const [servicesLoading, setServicesLoading] = useState(true);

  // Date & slots
  const [selectedDate, setSelectedDate] = useState('');
  const [slots, setSlots] = useState<AvailableSlot[]>([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null);

  // Submission
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [servicesError, setServicesError] = useState('');
  const [slotError, setSlotError] = useState('');

  const selectedServices = useMemo(
    () => services.filter(s => selectedServiceIds.includes(s.serviceId)),
    [services, selectedServiceIds]
  );

  const selectedServicesTotal = useMemo(
    () => selectedServices.reduce((sum, service) => sum + (Number(service.price) || 0), 0),
    [selectedServices]
  );

  const availableServices = useMemo(
    () => services.filter(s => !selectedServiceIds.includes(s.serviceId)),
    [services, selectedServiceIds]
  );

  const selectedClinicalServiceIds = useMemo(
    () => selectedServices.filter(isClinicalService).map(service => service.serviceId),
    [selectedServices]
  );

  const selectedParaclinicalServiceIds = useMemo(
    () => selectedServices
      .filter(service => service.department?.deptType === 'PARACLINICAL')
      .map(service => service.serviceId),
    [selectedServices]
  );

  const hasClinicalSelection = selectedClinicalServiceIds.length > 0;

  const schedulingServiceIds = useMemo(
    () => selectedClinicalServiceIds.length > 0 ? selectedClinicalServiceIds : selectedParaclinicalServiceIds,
    [selectedClinicalServiceIds, selectedParaclinicalServiceIds]
  );

  // Load draft from sessionStorage
  useEffect(() => {
    try {
      const raw = sessionStorage.getItem('appt_draft');
      if (raw) {
        setDraft(JSON.parse(raw));
      } else {
        // No draft — redirect back to step 1
        router.replace('/dashboard/patient/appointments/new');
      }
    } catch {
      router.replace('/dashboard/patient/appointments/new');
    }
  }, [router]);

  useEffect(() => {
    const followUpServiceId = draft?.followUpSourceRecord?.clinicalServiceId;
    if (draft?.followUpSourceRecordId && followUpServiceId) {
      setSelectedServiceIds([followUpServiceId]);
    }
  }, [draft?.followUpSourceRecordId, draft?.followUpSourceRecord?.clinicalServiceId]);

  // Load services
  useEffect(() => {
    setServicesLoading(true);
    setServicesError('');
    catalogService.getServices()
      .then(setServices)
      .catch(() => {
        setServices([]);
        setServicesError('Không thể tải danh sách dịch vụ khám.');
      })
      .finally(() => setServicesLoading(false));
  }, []);

  // Load available slots when date changes
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
    appointmentService.getAvailableSlots(selectedDate, schedulingServiceIds)
      .then(setSlots)
      .catch((err: unknown) => {
        const e = err as { response?: { data?: { message?: string } }; message?: string };
        setSlots([]);
        setSlotError(e?.response?.data?.message ?? e?.message ?? 'Không thể tải ca khám cho lựa chọn hiện tại.');
      })
      .finally(() => setSlotsLoading(false));
  }, [selectedDate, schedulingServiceIds]);

  function toggleService(id: string) {
    if (isFollowUp) {
      return;
    }
    setSelectedServiceIds(prev => {
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
      return isRemoving ? prev.filter(x => x !== id) : [...prev, id];
    });
  }

  function handleCancel() {
    sessionStorage.removeItem('appt_draft');
    router.push('/dashboard/patient/appointments');
  }

  function handleBack() {
    if (isFollowUp) {
      router.push('/dashboard/patient/health-record');
      return;
    }
    router.push('/dashboard/patient/appointments/new');
  }

  async function handleSubmit() {
    setError('');
    if (selectedServiceIds.length === 0) {
      setError('Vui lòng chọn ít nhất một dịch vụ.');
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
      setError('Vui lòng chọn giờ khám.');
      return;
    }
    const notes = draft?.symptoms?.trim()
      || (isFollowUp && draft?.followUpSourceRecordId
        ? `Tái khám theo hồ sơ ${draft.followUpSourceRecordId}`
        : '');

    if (!notes) {
      setError('Vui lòng nhập lý do khám.');
      router.push('/dashboard/patient/appointments/new');
      return;
    }
    setSubmitting(true);
    try {
      const payload: BookAppointmentPayload = {
        serviceIds: selectedServiceIds,
        appointmentDate: selectedDate,
        appointmentTime: selectedSlot.startTime,
        notes,
        followUpSourceRecordId: draft?.followUpSourceRecordId,
        relative: draft?.bookFor === 'relative' && draft.relative ? {
          patientId: draft.relative.patientId,
          fullName: draft.relative.fullName,
          phoneNumber: draft.relative.phone || draft.relative.phoneNumber || undefined,
          dateOfBirth: draft.relative.dateOfBirth || undefined,
          relationship: draft.relative.relationship || undefined,
          email: draft.relative.email?.trim() || undefined,
          cccd: draft.relative.cccd?.trim() || undefined,
        } : undefined,
      };
      await appointmentService.bookMyAppointment(payload);
      sessionStorage.removeItem('appt_draft');
      router.push('/dashboard/patient/appointments?created=true');
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      const msg = e?.response?.data?.message ?? e?.message ?? 'Đặt lịch thất bại.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  if (!draft) return null;

  const today = getTodayString();

  return (
    <div className="p-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800">{isFollowUp ? 'Đặt lịch tái khám' : 'Đặt lịch khám'}</h1>
        <button
          onClick={handleCancel}
          className="text-gray-500 hover:text-gray-700 text-2xl leading-none"
          title="Hủy bỏ"
        >
          ×
        </button>
      </div>

      {/* Stepper */}
      <div className="flex items-center mb-8">
        <div className="flex items-center">
          <div className="w-8 h-8 rounded-full bg-green-500 text-white flex items-center justify-center text-sm font-semibold">
            ✓
          </div>
          <span className="ml-2 text-sm font-medium text-green-600">Thông tin bệnh nhân</span>
        </div>
        <div className="flex-1 h-px bg-gray-300 mx-4" />
        <div className="flex items-center">
          <div className="w-8 h-8 rounded-full bg-blue-600 text-white flex items-center justify-center text-sm font-semibold">
            2
          </div>
          <span className="ml-2 text-sm font-semibold text-blue-600">Chọn dịch vụ &amp; thời gian</span>
        </div>
      </div>

      {/* Summary of step 1 */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6 text-sm text-blue-800">
        <span className="font-medium">
          {draft.bookFor === 'self' ? 'Đặt khám cho: Bản thân' : `Đặt khám cho: ${draft.relative?.fullName ?? 'Người thân'}`}
        </span>
        {draft.symptoms && (
          <span className="ml-4 text-blue-600">Triệu chứng: {draft.symptoms}</span>
        )}
        {isFollowUp && draft.followUpSourceRecord && (
          <div className="mt-2 text-blue-700">
            Tái khám hồ sơ {draft.followUpSourceRecord.recordId}
            {draft.followUpSourceRecord.clinicalServiceName ? ` - ${draft.followUpSourceRecord.clinicalServiceName}` : ''}
            {draft.followUpSourceRecord.doctorName ? ` - BS. ${draft.followUpSourceRecord.doctorName}` : ''}
          </div>
        )}
      </div>

      {/* Services table */}
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-3">Dịch vụ khám</h2>
        <p className="mb-3 text-sm text-slate-600">
          Mỗi lịch hẹn chỉ được chọn 1 dịch vụ lâm sàng. Hệ thống sẽ tự xếp bác sĩ và phòng phù hợp theo dịch vụ, ngày và giờ khám.
        </p>
        {servicesLoading ? (
          <div className="text-gray-500 text-sm py-4">Đang tải dịch vụ...</div>
        ) : servicesError ? (
          <div className="text-sm text-red-700 py-4 px-4 border border-red-200 bg-red-50 rounded-lg">{servicesError}</div>
        ) : services.length === 0 ? (
          <div className="border rounded-lg py-6 text-center text-sm text-slate-600">Không có dịch vụ nào</div>
        ) : (
          <>
            <div className="mb-6 border rounded-lg overflow-hidden bg-white shadow-sm">
              <div className="bg-blue-50/50 px-4 py-3 border-b border-gray-200">
                <h3 className="text-sm font-semibold text-gray-800">Dịch vụ đã chọn</h3>
              </div>
              {selectedServices.length === 0 ? (
                <div className="px-4 py-4 text-sm text-slate-600">Chưa có dịch vụ nào được chọn.</div>
              ) : (
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 border-b">
                    <tr>
                      <th className="px-4 py-3 text-left font-medium text-gray-600 w-12">STT</th>
                      <th className="px-4 py-3 text-left font-medium text-gray-600">Tên dịch vụ</th>
                      <th className="px-4 py-3 text-left font-medium text-gray-600">Nhóm</th>
                      <th className="px-4 py-3 text-left font-medium text-gray-600">Giá tiền</th>
                      <th className="px-4 py-3 text-center font-medium text-gray-600 w-24">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedServices.map((svc, idx) => (
                      <tr key={svc.serviceId} className="border-b last:border-b-0 hover:bg-gray-50">
                        <td className="px-4 py-3 text-gray-500">{idx + 1}</td>
                        <td className="px-4 py-3 font-medium text-gray-800">{svc.serviceName}</td>
                        <td className="px-4 py-3 text-gray-600">{getServiceTypeLabel(svc)}</td>
                        <td className="px-4 py-3 text-gray-700">{formatCurrency(svc.price)}</td>
                        <td className="px-4 py-3 text-center">
                          <button
                            type="button"
                            disabled={isFollowUp}
                            onClick={() => toggleService(svc.serviceId)}
                            className="rounded border border-red-200 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors disabled:cursor-not-allowed disabled:border-gray-200 disabled:text-slate-400"
                          >
                            Xóa
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {selectedServices.length > 0 && (
                <div className="flex items-center justify-end gap-3 border-t bg-blue-50/60 px-4 py-3 text-sm">
                  <span className="font-medium text-slate-700">Tổng tiền khám</span>
                  <span className="text-base font-bold text-blue-700">{formatCurrency(selectedServicesTotal)}</span>
                </div>
              )}
            </div>

            {isFollowUp ? (
              <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
                Dịch vụ tái khám được khóa theo hồ sơ cũ. Bác sĩ sẽ chỉ định bổ sung nếu cần.
              </div>
            ) : availableServices.length === 0 ? (
              <div className="py-4 text-sm text-slate-600">Tất cả dịch vụ khả dụng đã được chọn.</div>
            ) : (
              <div className="border rounded-lg overflow-hidden bg-white shadow-sm mb-4">
                <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
                  <h3 className="text-sm font-semibold text-gray-800">Dịch vụ khả dụng</h3>
                </div>
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 border-b">
                    <tr>
                      <th className="px-4 py-3 text-left font-medium text-gray-600 w-12">STT</th>
                      <th className="px-4 py-3 text-left font-medium text-gray-600">Tên dịch vụ</th>
                      <th className="px-4 py-3 text-left font-medium text-gray-600">Nhóm</th>
                      <th className="px-4 py-3 text-left font-medium text-gray-600">Giá tiền</th>
                      <th className="px-4 py-3 text-center font-medium text-gray-600 w-24">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {availableServices.map((svc, idx) => (
                      <tr key={svc.serviceId} className="border-b last:border-b-0 hover:bg-gray-50">
                        <td className="px-4 py-3 text-gray-500">{idx + 1}</td>
                        <td className="px-4 py-3 font-medium text-gray-800">{svc.serviceName}</td>
                        <td className="px-4 py-3 text-gray-600">{getServiceTypeLabel(svc)}</td>
                        <td className="px-4 py-3 text-gray-700">{formatCurrency(svc.price)}</td>
                        <td className="px-4 py-3 text-center">
                          {(() => {
                            const disableAdd = isClinicalService(svc) && hasClinicalSelection;
                            return (
                          <button
                            type="button"
                            disabled={disableAdd}
                            onClick={() => toggleService(svc.serviceId)}
                            className={`rounded px-3 py-1 text-xs font-medium transition-colors ${
                              disableAdd
                                ? 'cursor-not-allowed border border-gray-200 bg-gray-100 text-slate-500'
                                : 'border border-blue-600 text-blue-600 hover:bg-blue-50'
                            }`}
                          >
                            {disableAdd ? 'Giới hạn' : 'Thêm'}
                          </button>
                            );
                          })()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>

      {/* Date picker */}
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-3">Ngày khám</h2>
        <input
          type="date"
          min={today}
          value={selectedDate}
          onChange={e => setSelectedDate(e.target.value)}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {/* Time slots */}
      {selectedDate && (
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-gray-700 mb-3">Giờ khám</h2>
          {selectedServiceIds.length === 0 ? (
            <div className="text-gray-500 text-sm py-2">Vui lòng chọn ít nhất một dịch vụ để xem ca khám.</div>
          ) : schedulingServiceIds.length === 0 ? (
            <div className="py-2 text-sm text-blue-700">Không thể xác định nhóm dịch vụ để xếp ca khám.</div>
          ) : slotsLoading ? (
            <div className="py-2 text-sm text-slate-600">Đang tải ca khám...</div>
          ) : slotError ? (
            <div className="text-red-700 text-sm py-2">{slotError}</div>
          ) : slots.length === 0 ? (
            <div className="py-2 text-sm text-slate-600">Không có ca khám nào.</div>
          ) : (
            <div className="flex flex-wrap gap-3">
              {slots.map(slot => {
                const isSelected =
                  selectedSlot?.slotId === slot.slotId && selectedSlot?.startTime === slot.startTime;
                return (
                  <button
                    key={`${slot.slotId}-${slot.startTime}`}
                    disabled={slot.isFull}
                    onClick={() => { if (!slot.isFull) setSelectedSlot(slot); }}
                    className={`px-4 py-2 rounded-lg border text-sm font-medium transition-colors ${
                      slot.isFull
                        ? 'cursor-not-allowed border-gray-200 bg-gray-100 text-slate-500'
                        : isSelected
                        ? 'border-blue-600 bg-blue-600 text-white'
                        : 'border-gray-300 bg-white text-gray-700 hover:border-blue-400 hover:text-blue-600'
                    }`}
                  >
                    <div>{slot.shiftName}</div>
                    <div className="text-xs mt-0.5">
                      {formatTime(slot.startTime)} - {formatTime(slot.endTime)}
                    </div>
                    {slot.isFull && <div className="text-xs text-red-400">(Quá giờ)</div>}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center justify-between pt-4">
        <button
          onClick={handleBack}
          className="flex items-center gap-2 px-5 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
        >
          {isFollowUp ? '← Quay lại hồ sơ' : '← Quay lại'}
        </button>
        <button
          onClick={handleSubmit}
          disabled={submitting}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
        >
          {submitting ? 'Đang xử lý...' : 'Xác nhận đặt lịch'}
        </button>
      </div>
    </div>
  );
}
