import { useState, useEffect } from 'react';
import { appointmentService } from '@/services/appointment.service';
import { catalogService, ServiceItem } from '@/services/payment.service';
import { userService } from '@/services/user.service';
import { useToast } from '@/hooks/use-toast';
import { X, Loader2 } from 'lucide-react';

interface CreateRequestModalProps {
  onClose: () => void;
  onSuccess?: () => void;
}

interface DoctorOption {
  doctorId: string;
  fullName: string;
  departmentName?: string;
}

export function CreateRequestModal({ onClose, onSuccess }: CreateRequestModalProps) {
  const [loading, setLoading] = useState(false);
  const [servicesLoading, setServicesLoading] = useState(true);
  const [doctorsLoading, setDoctorsLoading] = useState(true);
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [doctors, setDoctors] = useState<DoctorOption[]>([]);
  const { toast } = useToast();

  const [patientId, setPatientId] = useState('');
  const [doctorId, setDoctorId] = useState('');
  const [notes, setNotes] = useState('');
  const [selectedServices, setSelectedServices] = useState<string[]>([]);

  // Load dịch vụ khám từ API
  useEffect(() => {
    catalogService.getServices()
      .then(data => setServices(data))
      .catch(() => setServices([]))
      .finally(() => setServicesLoading(false));
  }, []);

  // Load danh sách bác sĩ (ROLE_DOCTOR)
  useEffect(() => {
    userService.getUsers(0, 100, '', 'ALL')
      .then(res => {
        const doctorUsers = (res.users ?? []).filter(u =>
          u.roles?.some(r => r === 'ROLE_DOCTOR' || r === 'DOCTOR')
        );
        setDoctors(doctorUsers.map(u => ({
          // doctorId in appointment is staffId or doctorCode (BS-xxx)
          doctorId: u.staffId || String(u.userId),
          fullName: u.fullName,
        })));
      })
      .catch(() => setDoctors([]))
      .finally(() => setDoctorsLoading(false));
  }, []);

  const toggleService = (id: string) => {
    setSelectedServices(prev =>
      prev.includes(id) ? prev.filter(s => s !== id) : [...prev, id]
    );
  };

  const handleCreate = async () => {
    if (!patientId.trim()) {
      toast({ description: 'Vui lòng nhập mã bệnh nhân', variant: 'error' });
      return;
    }
    if (selectedServices.length === 0) {
      toast({ description: 'Vui lòng chọn ít nhất 1 hạng mục khám', variant: 'error' });
      return;
    }

    setLoading(true);
    try {
      const appointmentDate = new Date().toISOString().split('T')[0];
      const appointmentTime = new Date().toTimeString().substring(0, 5);

      await appointmentService.createAppointment({
        patientId: patientId.trim(),
        doctorId: doctorId || undefined,
        serviceIds: selectedServices,
        appointmentDate,
        appointmentTime,
        status: 'WAITING',
        notes,
      });

      toast({ description: 'Tạo lịch hẹn thành công!', variant: 'success' });
      onSuccess?.();
      onClose();
    } catch (error: unknown) {
      const data = (error as { response?: { data?: { message?: string } } })?.response?.data;
      toast({
        description: data?.message || 'Có lỗi xảy ra khi tạo lịch hẹn',
        variant: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 overflow-y-auto"
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-white w-full max-w-[780px] rounded-lg shadow-xl relative my-8 flex flex-col">
        <button onClick={onClose} className="absolute right-4 top-4 z-10 text-slate-500 hover:text-slate-950">
          <X size={24} />
        </button>

        <div className="overflow-y-auto p-8 max-h-[90vh]">
          <h2 className="text-2xl font-bold text-center text-[#1a2b3c] mb-8">Tạo Yêu cầu Khám bệnh</h2>

          {/* Patient ID input */}
          <div className="grid grid-cols-2 gap-6 mb-6">
            <div className="flex flex-col gap-2">
              <label className="text-sm font-bold text-gray-700">
                Mã bệnh nhân <span className="text-red-500">*</span>
                <span className="ml-1 text-xs font-normal text-slate-500">(VD: BN-XXXXXXXX)</span>
              </label>
              <input
                type="text"
                placeholder="BN-XXXXXXXX"
                className="border border-gray-200 rounded-md p-2.5 focus:outline-none focus:border-gray-400 font-mono"
                value={patientId}
                onChange={e => setPatientId(e.target.value)}
              />
            </div>

            {/* Doctor selection */}
            <div className="flex flex-col gap-2">
              <label className="text-sm font-bold text-gray-700">Bác sĩ phụ trách</label>
              {doctorsLoading ? (
                <div className="flex items-center gap-2 rounded-md border border-gray-200 p-2.5 text-sm text-slate-600">
                  <Loader2 size={14} className="animate-spin" /> Đang tải...
                </div>
              ) : (
                <select
                  className="border border-gray-200 rounded-md p-2.5 focus:outline-none focus:border-gray-400 bg-white"
                  value={doctorId}
                  onChange={e => setDoctorId(e.target.value)}
                >
                  <option value="">— Chưa chọn bác sĩ —</option>
                  {doctors.map(d => (
                    <option key={d.doctorId} value={d.doctorId}>
                      {d.fullName} {d.doctorId ? `(${d.doctorId})` : ''}
                    </option>
                  ))}
                </select>
              )}
            </div>
          </div>

          {/* Notes */}
          <div className="flex flex-col gap-2 mb-8">
            <label className="text-sm font-bold text-gray-700">Lý do khám / Ghi chú</label>
            <textarea
              className="border border-gray-200 rounded-md p-3 focus:outline-none focus:border-gray-400 h-24 resize-none"
              value={notes}
              onChange={e => setNotes(e.target.value)}
            />
          </div>

          {/* Service selection */}
          <div className="border border-slate-200 rounded-sm mb-6">
                    <div className="border-b border-slate-200 bg-slate-50 p-4 font-bold text-slate-950">
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
                  <div className="mb-6 rounded-lg border bg-gray-50 p-4">
                    <div className="mb-3 flex items-center justify-between gap-4">
                      <h3 className="text-sm font-semibold uppercase text-slate-950">Dịch vụ đã chọn</h3>
                    </div>
                    {services.filter(s => selectedServices.includes(s.serviceId)).length === 0 ? (
                      <div className="py-2 text-sm text-slate-600">Chưa có dịch vụ nào được chọn.</div>
                    ) : (
                      <table className="w-full text-left text-sm text-[#333]">
                        <thead className="border-b border-gray-200">
                          <tr>
                            <th className="py-2 px-2 font-medium text-slate-950 uppercase w-12">STT</th>
                            <th className="py-2 px-2 font-medium text-slate-950 uppercase">Tên hạng mục</th>
                            <th className="py-2 px-2 font-medium text-slate-950 uppercase text-center">Khoa</th>
                            <th className="py-2 px-2 font-medium text-slate-950 uppercase text-center">Giá tiền (VNĐ)</th>
                            <th className="py-2 px-2 font-medium text-slate-950 uppercase text-center w-24">Thao tác</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                          {services.filter(s => selectedServices.includes(s.serviceId)).map((srv, idx) => (
                            <tr key={srv.serviceId} className="hover:bg-gray-100">
                              <td className="py-3 px-2 text-slate-950">{idx + 1}</td>
                              <td className="py-3 px-2 font-medium text-slate-950">{srv.serviceName}</td>
                              <td className="py-3 px-2 text-center text-gray-700">{srv.department?.deptName || '---'}</td>
                              <td className="py-3 px-2 text-center text-gray-700">
                                {Number(srv.price).toLocaleString('vi-VN')}
                              </td>
                              <td className="py-3 px-2 text-center">
                                <button
                                  type="button"
                                  onClick={() => toggleService(srv.serviceId)}
                                  className="rounded border border-red-200 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
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

                  {services.filter(s => !selectedServices.includes(s.serviceId)).length === 0 ? (
                    <div className="py-4 text-sm text-slate-600">Tất cả dịch vụ khả dụng đã được chọn.</div>
                  ) : (
                    <table className="w-full text-left text-sm text-[#333] mb-4">
                      <thead className="border-b border-gray-200">
                        <tr>
                          <th className="py-2 px-2 font-medium text-slate-950 uppercase w-12">STT</th>
                          <th className="py-2 px-2 font-medium text-slate-950 uppercase">Tên hạng mục</th>
                          <th className="py-2 px-2 font-medium text-slate-950 uppercase text-center">Khoa</th>
                          <th className="py-2 px-2 font-medium text-slate-950 uppercase text-center">Giá tiền (VNĐ)</th>
                          <th className="py-2 px-2 font-medium text-slate-950 uppercase text-center w-24">Thao tác</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100">
                        {services.filter(s => !selectedServices.includes(s.serviceId)).map((srv, idx) => (
                          <tr key={srv.serviceId} className="hover:bg-gray-50">
                            <td className="py-3 px-2 text-slate-950">{idx + 1}</td>
                            <td className="py-3 px-2 font-medium text-slate-950">{srv.serviceName}</td>
                            <td className="py-3 px-2 text-center text-gray-700">{srv.department?.deptName || '---'}</td>
                            <td className="py-3 px-2 text-center text-gray-700">
                              {Number(srv.price).toLocaleString('vi-VN')}
                            </td>
                            <td className="py-3 px-2 text-center">
                              <button
                                type="button"
                                onClick={() => toggleService(srv.serviceId)}
                                className="rounded border border-slate-200 px-3 py-1 text-xs font-medium text-slate-950 hover:bg-gray-100"
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
            </div>
          </div>

          {/* Total summary */}
          {selectedServices.length > 0 && (
            <div className="mb-4 p-3 bg-gray-50 rounded border border-gray-200 text-sm font-semibold text-gray-700 flex justify-between">
              <span>Tổng chi phí dịch vụ:</span>
              <span className="text-[#1a2b3c] font-bold">
                {services
                  .filter(s => selectedServices.includes(s.serviceId))
                  .reduce((acc, s) => acc + Number(s.price), 0)
                  .toLocaleString('vi-VN')} VNĐ
              </span>
            </div>
          )}

          <button
            onClick={handleCreate}
            disabled={loading}
            className="w-full bg-[#334155] hover:bg-[#1e293b] text-white font-bold py-4 rounded transition-colors uppercase tracking-wide disabled:opacity-70 flex items-center justify-center gap-2"
          >
            {loading && <Loader2 className="animate-spin" size={18} />}
            {loading ? 'Đang xử lý...' : 'TẠO YÊU CẦU'}
          </button>
        </div>
      </div>
    </div>
  );
}

