'use client';

import { useEffect, useState, useCallback } from 'react';
import { useAuthStore } from '@/store/auth.store';
import { useRouter } from 'next/navigation';
import { ROLES } from '@/lib/roleRedirect';
import { Search, Loader2, X, Trash2, Shuffle } from 'lucide-react';
import Link from 'next/link';
import { appointmentService, AppointmentDetail, AppointmentItem, RoomTransferOption } from '@/services/appointment.service';
import { catalogService, getUnavailableServiceAddMessage, ServiceItem } from '@/services/payment.service';
import { serviceOrderService, ServiceOrderItem } from '@/services/service-order.service';
import { APPOINTMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  CHECKED_IN: { label: 'Đã check-in - chờ thanh toán', color: 'bg-amber-50 text-amber-700 border-amber-200' },
  REQUESTED: { label: 'Chờ xác nhận', color: 'bg-blue-50 text-blue-700 border-blue-200' },
  WAITING:   { label: 'Chờ khám',    color: 'bg-blue-100 text-blue-800 border-blue-200' },
  WAITING_RESULTS: { label: 'Lưu tạm - chờ cận lâm sàng', color: 'bg-amber-50 text-amber-700 border-amber-200' },
  IN_PROGRESS: { label: 'Đang khám', color: 'bg-green-50 text-green-700 border-green-200' },
  CONFIRMED: { label: 'Đã xác nhận', color: 'bg-blue-50 text-blue-700 border-blue-200' },
  COMPLETED: { label: 'Hoàn thành',  color: 'bg-blue-50 text-blue-600 border-blue-200' },
  CANCELLED: { label: 'Đã hủy',      color: 'bg-red-50 text-red-700 border-red-200' },
};

const EDITABLE_APPOINTMENT_STATUSES = new Set(['REQUESTED', 'CHECKED_IN', 'WAITING', 'CONFIRMED']);

type EditableServiceChoice = {
  serviceId: string;
  serviceName: string;
  price: number;
  departmentType?: string;
  hasOperationalRoom?: boolean;
};

function formatAppointmentDate(value?: string | null) {
  if (!value) return '—';
  const datePart = value.split('T')[0];
  const [year, month, day] = datePart.split('-');
  if (!year || !month || !day) return value;
  return `${Number(day)}/${Number(month)}/${year}`;
}

function normalizeDepartmentType(value?: string | null) {
  return (value ?? '').trim().toUpperCase();
}

function formatCurrency(price?: number | null) {
  if (typeof price !== 'number' || Number.isNaN(price)) return '—';
  return `${price.toLocaleString('vi-VN')} đ`;
}

function AppointmentDetailModal({
  appointmentId,
  onClose,
  onCheckIn,
  onUpdateServices,
  onTransferRoom,
  onCancelAppointment,
  checkingInId,
}: {
  appointmentId: string;
  onClose: () => void;
  onCheckIn: (appointmentId: string) => Promise<void>;
  onUpdateServices: (appointmentId: string, serviceIds: string[]) => Promise<void>;
  onTransferRoom: (appointmentId: string, roomId: string, reason?: string) => Promise<void>;
  onCancelAppointment: (appointmentId: string, reason: string) => Promise<void>;
  checkingInId: string | null;
}) {
  const [detail, setDetail] = useState<AppointmentDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [serviceOrders, setServiceOrders] = useState<ServiceOrderItem[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(true);
  const [cancelingId, setCancelingId] = useState<string | null>(null);
  const [catalogServices, setCatalogServices] = useState<ServiceItem[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [editingServices, setEditingServices] = useState(false);
  const [selectedServiceIds, setSelectedServiceIds] = useState<string[]>([]);
  const [serviceEditorError, setServiceEditorError] = useState<string | null>(null);
  const [savingServices, setSavingServices] = useState(false);
  const [cancelingAppointment, setCancelingAppointment] = useState(false);
  const [transferOptions, setTransferOptions] = useState<RoomTransferOption[]>([]);
  const [transferLoading, setTransferLoading] = useState(false);
  const [transferOpen, setTransferOpen] = useState(false);
  const [selectedTransferRoomId, setSelectedTransferRoomId] = useState('');
  const [transferReason, setTransferReason] = useState('');
  const [transferError, setTransferError] = useState<string | null>(null);
  const [transferringRoom, setTransferringRoom] = useState(false);

  const fetchOrders = useCallback(() => {
    setOrdersLoading(true);
    serviceOrderService.getByAppointment(appointmentId)
      .then(orders => setServiceOrders(orders.filter(o => o.status !== 'CANCELLED')))
      .catch(() => setServiceOrders([]))
      .finally(() => setOrdersLoading(false));
  }, [appointmentId]);

  const fetchDetail = useCallback(() => {
    setLoading(true);
    appointmentService.getAppointmentDetail(appointmentId)
      .then(data => setDetail(data))
      .catch(() => setDetail(null))
      .finally(() => setLoading(false));
  }, [appointmentId]);

  const loadCatalogServices = useCallback(async () => {
    setCatalogLoading(true);
    try {
      const services = await catalogService.getServices();
      setCatalogServices(services);
      return services;
    } catch {
      setCatalogServices([]);
      throw new Error('Không thể tải danh sách dịch vụ.');
    } finally {
      setCatalogLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDetail();
    fetchOrders();
  }, [fetchDetail, fetchOrders]);

  useEffect(() => {
    setSelectedServiceIds(detail?.services?.map(service => service.serviceId) ?? []);
    setEditingServices(false);
    setServiceEditorError(null);
  }, [detail]);

  const handleCancelOrder = async (orderId: string) => {
    if (!window.confirm('Bạn có chắc muốn hủy dịch vụ này?')) return;
    setCancelingId(orderId);
    try {
      await serviceOrderService.cancel(orderId);
      fetchOrders();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      alert(err?.response?.data?.message || 'Không thể hủy dịch vụ này.');
    } finally {
      setCancelingId(null);
    }
  };

  const canCheckIn = detail?.status === 'REQUESTED' || detail?.status === 'CONFIRMED';
  const isFollowUpAppointment = Boolean(detail)
    && ((detail?.visitType ?? '').toUpperCase() === 'FOLLOW_UP' || Boolean(detail?.followUpSourceRecordId));
  const canEditServices = Boolean(detail)
    && !isFollowUpAppointment
    && EDITABLE_APPOINTMENT_STATUSES.has(detail?.status ?? '')
    && serviceOrders.length === 0;
  const hasPaidServiceOrder = serviceOrders.some(order => {
    const paymentStatus = (order.paymentStatus ?? '').toUpperCase();
    const orderStatus = (order.status ?? '').toUpperCase();
    return paymentStatus === 'PAID' || orderStatus === 'PAID';
  });
  const canCancelAppointment = Boolean(detail)
    && EDITABLE_APPOINTMENT_STATUSES.has(detail?.status ?? '')
    && !hasPaidServiceOrder;
  const canTransferRoom = Boolean(detail)
    && !['CANCELLED', 'COMPLETED', 'IN_PROGRESS'].includes(detail?.status ?? '');

  const getServiceChoice = (serviceId: string): EditableServiceChoice | null => {
    const catalogMatch = catalogServices.find(service => service.serviceId === serviceId);
    if (catalogMatch) {
      return {
        serviceId: catalogMatch.serviceId,
        serviceName: catalogMatch.serviceName,
        price: Number(catalogMatch.price),
        departmentType: catalogMatch.department?.deptType,
        hasOperationalRoom: catalogMatch.hasOperationalRoom,
      };
    }

    const detailMatch = detail?.services.find(service => service.serviceId === serviceId);
    if (detailMatch) {
      return {
        serviceId: detailMatch.serviceId,
        serviceName: detailMatch.serviceName,
        price: Number(detailMatch.price),
        departmentType: detailMatch.departmentType,
        hasOperationalRoom: true,
      };
    }

    return null;
  };

  const displayedServices: EditableServiceChoice[] = editingServices
    ? selectedServiceIds
      .map(getServiceChoice)
      .filter((service): service is EditableServiceChoice => service !== null)
    : (detail?.services ?? []).map(service => ({
      serviceId: service.serviceId,
      serviceName: service.serviceName,
      price: Number(service.price),
      departmentType: service.departmentType,
      hasOperationalRoom: true,
    }));

  const availableCatalogServices = catalogServices.filter(service => !selectedServiceIds.includes(service.serviceId));

  const handleModalCheckIn = async () => {
    await onCheckIn(appointmentId);
    fetchDetail();
  };

  const handleStartEditingServices = async () => {
    if (isFollowUpAppointment) {
      setServiceEditorError('Lịch tái khám không được thêm/xóa dịch vụ tại lễ tân.');
      return;
    }

    setServiceEditorError(null);
    setEditingServices(true);
    if (catalogServices.length === 0) {
      try {
        await loadCatalogServices();
      } catch (error) {
        setServiceEditorError(error instanceof Error ? error.message : 'Không thể tải danh sách dịch vụ.');
      }
    }
  };

  const handleToggleService = (service: ServiceItem) => {
    if (isFollowUpAppointment) {
      setServiceEditorError('Lịch tái khám không được thêm/xóa dịch vụ tại lễ tân.');
      return;
    }

    const isRemoving = selectedServiceIds.includes(service.serviceId);

    if (!isRemoving && service.hasOperationalRoom === false) {
      setServiceEditorError(getUnavailableServiceAddMessage(service));
      return;
    }

    if (!isRemoving && normalizeDepartmentType(service.department?.deptType) === 'CLINICAL') {
      const hasAnotherClinicalService = selectedServiceIds.some(selectedId => {
        const selectedService = getServiceChoice(selectedId);
        return normalizeDepartmentType(selectedService?.departmentType) === 'CLINICAL';
      });

      if (hasAnotherClinicalService) {
        setServiceEditorError('Mỗi lịch hẹn chỉ được chọn 1 dịch vụ lâm sàng.');
        return;
      }
    }

    setServiceEditorError(null);
    setSelectedServiceIds(current => (
      isRemoving
        ? current.filter(serviceId => serviceId !== service.serviceId)
        : [...current, service.serviceId]
    ));
  };

  const handleDiscardServiceChanges = () => {
    setSelectedServiceIds(detail?.services?.map(service => service.serviceId) ?? []);
    setEditingServices(false);
    setServiceEditorError(null);
  };

  const handleSaveServiceChanges = async () => {
    if (isFollowUpAppointment) {
      setServiceEditorError('Lịch tái khám không được thêm/xóa dịch vụ tại lễ tân.');
      return;
    }

    if (selectedServiceIds.length === 0) {
      setServiceEditorError('Lịch hẹn phải có ít nhất 1 dịch vụ.');
      return;
    }

    setSavingServices(true);
    setServiceEditorError(null);
    try {
      await onUpdateServices(appointmentId, selectedServiceIds);
      await fetchDetail();
      setEditingServices(false);
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      setServiceEditorError(err?.response?.data?.message || err?.message || 'Không thể cập nhật dịch vụ.');
    } finally {
      setSavingServices(false);
    }
  };

  const handleCancelAppointment = async () => {
    if (!detail) return;
    const reason = window.prompt(`Nhập lý do hủy lịch hẹn ${detail.appointmentId}:`)?.trim();
    if (!reason) {
      alert('Vui lòng nhập lý do hủy lịch hẹn.');
      return;
    }

    setCancelingAppointment(true);
    try {
      await onCancelAppointment(appointmentId, reason);
      onClose();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err?.response?.data?.message || 'Không thể hủy lịch hẹn.');
    } finally {
      setCancelingAppointment(false);
    }
  };

  const loadTransferOptions = async () => {
    setTransferLoading(true);
    setTransferError(null);
    try {
      const options = await appointmentService.getRoomTransferOptions(appointmentId);
      setTransferOptions(options ?? []);
      const firstTarget = (options ?? []).find(option => !option.currentRoom);
      setSelectedTransferRoomId(firstTarget?.roomId ?? '');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      setTransferOptions([]);
      setSelectedTransferRoomId('');
      setTransferError(err?.response?.data?.message || err?.message || 'Không thể tải danh sách phòng điều phối.');
    } finally {
      setTransferLoading(false);
    }
  };

  const handleOpenTransfer = async () => {
    setTransferOpen(true);
    await loadTransferOptions();
  };

  const handleTransferRoom = async () => {
    if (!selectedTransferRoomId) {
      setTransferError('Vui lòng chọn phòng cần chuyển.');
      return;
    }

    setTransferringRoom(true);
    setTransferError(null);
    try {
      await onTransferRoom(appointmentId, selectedTransferRoomId, transferReason);
      await fetchDetail();
      fetchOrders();
      setTransferOpen(false);
      setTransferReason('');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      setTransferError(err?.response?.data?.message || err?.message || 'Không thể điều phối phòng.');
    } finally {
      setTransferringRoom(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 overflow-y-auto"
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-white w-full max-w-[700px] rounded-lg shadow-xl relative p-8 my-8">
        <button onClick={onClose} className="absolute right-4 top-4 text-gray-500 hover:text-black">
          <X size={22} />
        </button>

        <h2 className="text-xl font-bold text-[#1a2b3c] mb-6 border-l-4 border-black pl-3 uppercase">
          Chi tiết lịch hẹn
        </h2>

        {loading ? (
          <div className="flex justify-center py-10">
            <Loader2 className="animate-spin text-slate-500" size={28} />
          </div>
        ) : !detail ? (
          <div className="py-8 text-center text-slate-600">Không tìm thấy thông tin lịch hẹn</div>
        ) : (
          <div className="flex flex-col gap-4 text-sm">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Mã lịch hẹn" value={detail.appointmentId} />
              <Field label="Mã bệnh nhân" value={detail.patientId} />
              <Field label="Tên bệnh nhân" value={detail.patientName} />
              <Field label="Bác sĩ" value={detail.doctorName || '— Chưa phân công —'} />
              <Field label="Khoa" value={detail.departmentName || '—'} />
              <Field label="Số thứ tự" value={detail.queueNumber?.toString() || '—'} />
              <Field label="Ngày hẹn" value={formatAppointmentDate(detail.appointmentDate)} />
              <Field label="Giờ hẹn" value={detail.appointmentTime || '—'} />
              <div className="flex flex-col gap-1">
                <span className="text-xs font-bold text-gray-500 uppercase">Trạng thái</span>
                <span className={`inline-flex px-2 py-1 rounded text-xs font-semibold border w-fit ${STATUS_LABELS[detail.status]?.color ?? 'bg-gray-100 text-gray-700 border-gray-200'}`}>
                  {STATUS_LABELS[detail.status]?.label ?? formatVietnameseLabel(detail.status, APPOINTMENT_STATUS_LABELS)}
                </span>
              </div>
            </div>

            {detail.notes && (
              <div className="flex flex-col gap-1 mt-2">
                <span className="text-xs font-bold text-gray-500 uppercase">Ghi chú / Lý do khám</span>
                <p className="text-gray-700 bg-gray-50 rounded p-3 border border-gray-100">{detail.notes}</p>
              </div>
            )}

            {/* Danh sách dịch vụ: ưu tiên service orders, fallback sang detail.services */}
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <div className="text-xs font-bold uppercase text-gray-500">Phòng đang xếp</div>
                  <div className="mt-1 text-sm font-semibold text-slate-900">
                    {detail.roomName || 'Chưa phân phòng'}
                  </div>
                  <div className="mt-1 text-xs text-slate-600">
                    Chỉ điều phối sang phòng đang hoạt động và có cùng dịch vụ.
                  </div>
                </div>
                {canTransferRoom && (
                  <button
                    onClick={handleOpenTransfer}
                    disabled={transferLoading || transferringRoom}
                    className="inline-flex items-center justify-center gap-2 rounded border border-blue-200 bg-white px-3 py-2 text-xs font-bold uppercase text-blue-700 hover:bg-blue-50 disabled:opacity-50"
                  >
                    {transferLoading ? <Loader2 size={14} className="animate-spin" /> : <Shuffle size={14} />}
                    Điều phối phòng
                  </button>
                )}
              </div>

              {transferOpen && (
                <div className="mt-4 rounded border border-white bg-white p-4 shadow-sm">
                  <div className="grid gap-3 sm:grid-cols-[1fr,2fr]">
                    <div>
                      <label className="text-xs font-bold uppercase text-gray-500">Phòng mới</label>
                      <select
                        value={selectedTransferRoomId}
                        onChange={event => setSelectedTransferRoomId(event.target.value)}
                        disabled={transferLoading || transferringRoom}
                        className="mt-1 w-full rounded border border-gray-200 bg-white px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500"
                      >
                        <option value="">Chọn phòng</option>
                        {transferOptions.map(option => (
                          <option key={option.roomId} value={option.roomId} disabled={Boolean(option.currentRoom)}>
                            {option.roomName}
                            {option.currentRoom ? ' (hiện tại)' : ''}
                            {option.currentQueueLength != null ? ` - ${option.currentQueueLength} đang chờ` : ''}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="text-xs font-bold uppercase text-gray-500">Lý do điều phối</label>
                      <input
                        value={transferReason}
                        onChange={event => setTransferReason(event.target.value)}
                        disabled={transferringRoom}
                        placeholder="VD: Phòng tạm dừng, bác sĩ đổi phòng..."
                        className="mt-1 w-full rounded border border-gray-200 px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-1 focus:ring-blue-500"
                      />
                    </div>
                  </div>

                  {transferOptions.length > 0 && (
                    <div className="mt-3 max-h-40 overflow-y-auto rounded border border-gray-100">
                      {transferOptions.map(option => (
                        <div key={option.roomId} className="flex items-center justify-between gap-3 border-b border-gray-100 px-3 py-2 text-xs last:border-b-0">
                          <div>
                            <div className="font-semibold text-slate-900">{option.roomName}</div>
                            <div className="text-slate-500">
                              {option.doctorName || 'Chưa có bác sĩ'} · {option.serviceName || 'Dịch vụ phù hợp'}
                            </div>
                          </div>
                          <span className={option.currentRoom ? 'font-semibold text-blue-700' : 'text-slate-600'}>
                            {option.currentRoom ? 'Hiện tại' : `${option.currentQueueLength ?? 0} chờ`}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}

                  {transferError && (
                    <div className="mt-3 rounded border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
                      {transferError}
                    </div>
                  )}

                  <div className="mt-4 flex justify-end gap-2">
                    <button
                      onClick={() => setTransferOpen(false)}
                      disabled={transferringRoom}
                      className="rounded border border-gray-200 px-3 py-2 text-xs font-bold uppercase text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                    >
                      Hủy
                    </button>
                    <button
                      onClick={handleTransferRoom}
                      disabled={transferringRoom || transferLoading || !selectedTransferRoomId}
                      className="rounded bg-blue-600 px-3 py-2 text-xs font-bold uppercase text-white hover:bg-blue-700 disabled:opacity-50"
                    >
                      {transferringRoom ? 'Đang chuyển...' : 'Xác nhận chuyển'}
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="flex flex-col gap-2 mt-2">
              <div className="flex items-center justify-between gap-3">
                <span className="text-xs font-bold text-gray-500 uppercase">Dịch vụ đăng ký</span>
                {canEditServices && !editingServices && (
                  <button
                    onClick={handleStartEditingServices}
                    className="px-3 py-1 text-[11px] font-bold uppercase border border-gray-200 rounded text-gray-700 hover:bg-gray-50 transition-colors"
                  >
                    Thêm/Xóa dịch vụ
                  </button>
                )}
              </div>
              {ordersLoading ? (
                <div className="flex items-center gap-2 py-2 text-xs text-slate-600">
                  <Loader2 size={14} className="animate-spin" /> Đang tải dịch vụ...
                </div>
              ) : serviceOrders.length > 0 ? (
                // Đã có service orders (sau khi thanh toán) — hiển thị với trạng thái TT và nút Hủy
                <table className="w-full text-xs border border-gray-100 rounded overflow-hidden">
                  <thead className="bg-gray-50 text-gray-500 font-bold">
                    <tr>
                      <th className="px-3 py-2 text-left">Dịch vụ</th>
                      <th className="px-3 py-2 text-center">Thanh toán</th>
                      <th className="px-3 py-2 text-center w-20">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {serviceOrders.map(order => {
                      const isPaid = (order.paymentStatus ?? '').toUpperCase() === 'PAID';
                      return (
                        <tr key={order.orderId} className="border-t border-gray-100">
                          <td className="px-3 py-2 font-medium text-gray-800">{order.serviceName}</td>
                          <td className="px-3 py-2 text-center">
                            <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-semibold border ${
                              isPaid
                                ? 'bg-blue-50 text-blue-700 border-blue-200'
                                : 'bg-blue-100 text-blue-800 border-blue-200'
                            }`}>
                              {isPaid ? 'Đã thanh toán' : 'Chưa thanh toán'}
                            </span>
                          </td>
                          <td className="px-3 py-2 text-center">
                            {!isPaid ? (
                              <button
                                onClick={() => handleCancelOrder(order.orderId)}
                                disabled={cancelingId === order.orderId}
                                className="inline-flex items-center gap-1 px-2 py-1 rounded border border-red-200 text-red-600 hover:bg-red-50 disabled:opacity-50 transition-colors"
                                title="Hủy dịch vụ này"
                              >
                                {cancelingId === order.orderId
                                  ? <Loader2 size={12} className="animate-spin" />
                                  : <Trash2 size={12} />}
                                Hủy
                              </button>
                            ) : (
                              <span className="text-xs text-slate-500">—</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              ) : displayedServices.length > 0 ? (
                // Chưa có service orders — hiển thị dịch vụ gốc từ lịch hẹn (chờ thanh toán)
                <table className="w-full text-xs border border-gray-100 rounded overflow-hidden">
                  <thead className="bg-gray-50 text-gray-500 font-bold">
                    <tr>
                      <th className="px-3 py-2 text-left">Dịch vụ</th>
                      <th className="px-3 py-2 text-right">Giá</th>
                      <th className="px-3 py-2 text-center">Trạng thái</th>
                      {editingServices && <th className="px-3 py-2 text-center w-24">Thao tác</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {displayedServices.map((service) => (
                      <tr key={service.serviceId} className="border-t border-gray-100">
                        <td className="px-3 py-2 font-medium text-gray-800">{service.serviceName}</td>
                        <td className="px-3 py-2 text-right text-gray-600">
                          {formatCurrency(service.price)}
                        </td>
                        <td className="px-3 py-2 text-center">
                          <span className="inline-flex px-2 py-0.5 rounded-full text-xs font-semibold border bg-gray-50 text-gray-500 border-gray-200">
                            Chờ thanh toán
                          </span>
                        </td>
                        {editingServices && (
                          <td className="px-3 py-2 text-center">
                            <button
                              onClick={() => handleToggleService({
                                serviceId: service.serviceId,
                                serviceName: service.serviceName,
                                price: service.price,
                                department: service.departmentType ? { deptId: 0, deptName: '', deptType: service.departmentType } : undefined,
                                hasOperationalRoom: service.hasOperationalRoom,
                              })}
                              className="inline-flex items-center gap-1 px-2 py-1 rounded border border-red-200 text-red-600 hover:bg-red-50 transition-colors"
                            >
                              <Trash2 size={12} /> Xóa
                            </button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <div className="py-2 text-xs text-slate-600">Không có dịch vụ nào.</div>
              )}

              {editingServices && (
                <div className="rounded-lg border border-gray-100 bg-gray-50 p-4">
                  <div className="text-xs font-bold uppercase text-gray-500">Thêm dịch vụ</div>
                  {catalogLoading ? (
                    <div className="mt-3 flex items-center gap-2 text-xs text-slate-600">
                      <Loader2 size={14} className="animate-spin" /> Đang tải danh sách dịch vụ...
                    </div>
                  ) : availableCatalogServices.length === 0 ? (
                    <div className="mt-3 text-xs text-slate-600">Không còn dịch vụ khả dụng để thêm.</div>
                  ) : (
                    <div className="mt-3 max-h-52 overflow-y-auto rounded border border-gray-100 bg-white">
                      {availableCatalogServices.map(service => (
                        <div key={service.serviceId} className="flex items-center justify-between gap-3 border-b border-gray-100 px-3 py-2 last:border-b-0">
                          <div className="min-w-0">
                            <div className="font-medium text-gray-800">{service.serviceName}</div>
                            <div className="text-[11px] text-gray-500">
                              {normalizeDepartmentType(service.department?.deptType) === 'PARACLINICAL' ? 'Cận lâm sàng' : 'Lâm sàng'}
                              {' · '}
                              {formatCurrency(Number(service.price))}
                            </div>
                          </div>
                          <button
                            onClick={() => handleToggleService(service)}
                            className="shrink-0 rounded border border-gray-200 px-2 py-1 text-[11px] font-bold uppercase text-gray-700 hover:bg-gray-50 transition-colors"
                          >
                            Thêm
                          </button>
                        </div>
                      ))}
                    </div>
                  )}

                  {serviceEditorError && (
                    <div className="mt-3 rounded border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
                      {serviceEditorError}
                    </div>
                  )}

                  <div className="mt-4 flex items-center justify-end gap-2">
                    <button
                      onClick={handleDiscardServiceChanges}
                      disabled={savingServices}
                      className="px-3 py-2 text-[11px] font-bold uppercase border border-gray-200 rounded text-gray-700 hover:bg-gray-100 disabled:opacity-50 transition-colors"
                    >
                      Bỏ thay đổi
                    </button>
                    <button
                      onClick={handleSaveServiceChanges}
                      disabled={savingServices || catalogLoading}
                      className="px-3 py-2 text-[11px] font-bold uppercase bg-black text-white rounded hover:bg-gray-800 disabled:opacity-50 transition-colors"
                    >
                      {savingServices ? 'Đang lưu...' : 'Lưu dịch vụ'}
                    </button>
                  </div>
                </div>
              )}

              {!editingServices && !ordersLoading && serviceOrders.length > 0 && (
                <div className="text-[11px] text-slate-600">
                  Lịch hẹn đã phát sinh phiếu dịch vụ, chỉ còn thao tác hủy từng dịch vụ chưa thanh toán.
                </div>
              )}
            </div>

            <div className="mt-4 flex items-center justify-end gap-3">
              {hasPaidServiceOrder && (
                <div className="mr-auto max-w-md text-left text-[11px] font-medium text-amber-700">
                  Lịch hẹn đã thanh toán nên không thể hủy tại lễ tân. Nếu cần hủy, phải xử lý hoàn/hủy hóa đơn bằng nghiệp vụ riêng.
                </div>
              )}
              {canCancelAppointment && (
                <button
                  onClick={handleCancelAppointment}
                  disabled={cancelingAppointment || savingServices}
                  className="px-4 py-2 text-xs font-bold uppercase border border-red-200 text-red-600 rounded hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {cancelingAppointment ? 'Đang hủy...' : 'Hủy lịch hẹn'}
                </button>
              )}
              {canCheckIn && (
                <button
                  onClick={handleModalCheckIn}
                  disabled={checkingInId === appointmentId || cancelingAppointment || savingServices}
                  className="px-4 py-2 text-xs font-bold uppercase bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {checkingInId === appointmentId ? 'Đang check-in...' : 'Check-in'}
                </button>
              )}
              <button
                onClick={onClose}
                className="px-4 py-2 text-xs font-bold uppercase border border-gray-200 rounded text-gray-700 hover:bg-gray-50 transition-colors"
              >
                Đóng
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-xs font-bold text-gray-500 uppercase">{label}</span>
      <span className="text-gray-800 font-medium">{value || '—'}</span>
    </div>
  );
}

export default function ReceptionistPatientsPage() {
  const [appointments, setAppointments] = useState<AppointmentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedAppointmentId, setSelectedAppointmentId] = useState<string | null>(null);
  const [checkingInId, setCheckingInId] = useState<string | null>(null);

  const { user } = useAuthStore();
  const router = useRouter();
  const pageSize = 10;

  useEffect(() => {
    if (user && !user.roles.includes(ROLES.RECEPTIONIST) && !user.roles.includes(ROLES.CASHIER)) {
      router.push('/unauthorized');
    }
  }, [user, router]);

  const fetchAppointments = useCallback(async () => {
    setLoading(true);
    try {
      const data = await appointmentService.getAppointments(currentPage, pageSize, statusFilter || undefined);
      setAppointments(data.content ?? []);
      setTotalPages(data.totalPages ?? 0);
      setTotalElements(data.totalElements ?? 0);
    } catch (error) {
      console.error('Failed to fetch appointments:', error);
      setAppointments([]);
    } finally {
      setLoading(false);
    }
  }, [currentPage, statusFilter]);

  useEffect(() => {
    const id = setTimeout(fetchAppointments, 300);
    return () => clearTimeout(id);
  }, [fetchAppointments]);

  useEffect(() => { setCurrentPage(1); }, [statusFilter]);

  const handleCheckIn = async (appointmentId: string) => {
    setCheckingInId(appointmentId);
    try {
      await appointmentService.checkIn(appointmentId);
      await fetchAppointments();
    } catch (e: unknown) {
      console.error('Check-in failed', e);
      const err = e as { response?: { data?: { message?: string } } };
      alert(err?.response?.data?.message || 'Check-in thất bại.');
      throw e;
    } finally {
      setCheckingInId(null);
    }
  };

  const handleUpdateServices = async (appointmentId: string, serviceIds: string[]) => {
    await appointmentService.updateAppointment(appointmentId, { serviceIds });
    await fetchAppointments();
  };

  const handleTransferRoom = async (appointmentId: string, roomId: string, reason?: string) => {
    await appointmentService.transferRoom(appointmentId, roomId, reason);
    await fetchAppointments();
  };

  const handleCancelAppointment = async (appointmentId: string, reason: string) => {
    await appointmentService.cancelAppointment(appointmentId, reason);
    await fetchAppointments();
  };

  // Client-side search filter on appointmentId / patientName / patientId
  const filtered = appointments.filter(a => {
    if (!searchTerm) return true;
    const q = searchTerm.toLowerCase();
    return (
      a.appointmentId?.toLowerCase().includes(q) ||
      a.patientName?.toLowerCase().includes(q) ||
      a.patientId?.toLowerCase().includes(q) ||
      a.doctorName?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="p-8 bg-gray-50 min-h-screen font-sans text-black">
      <h1 className="text-2xl font-bold mb-8 text-black border-l-4 border-black pl-3 uppercase">
        DANH SÁCH LỊCH HẸN
      </h1>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        {/* Header */}
        <div className="flex flex-col md:flex-row justify-between items-center mb-6 gap-4">
          <h2 className="text-xl font-bold text-black">Danh sách lịch hẹn</h2>

          <div className="flex flex-col sm:flex-row items-center gap-4 w-full md:w-auto">
            {/* Search */}
            <div className="relative w-full sm:w-80">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Search className="h-4 w-4 text-slate-500" />
              </div>
              <input
                type="text"
                placeholder="Tìm theo tên, mã BN, mã lịch hẹn..."
                className="w-full rounded-lg border border-gray-200 py-2 pl-10 pr-4 text-sm text-slate-900 placeholder:text-slate-500 focus:outline-none focus:ring-1 focus:ring-black"
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
              />
            </div>

            {/* Status filter */}
            <select
              className="w-full rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-black sm:w-44"
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
            >
              <option value="">Tất cả trạng thái</option>
              <option value="REQUESTED">Chờ xác nhận</option>
              <option value="WAITING">Chờ khám</option>
              <option value="CONFIRMED">Đã xác nhận</option>
              <option value="COMPLETED">Hoàn thành</option>
              <option value="CANCELLED">Đã hủy</option>
            </select>

            {/* Create button */}
            <Link
              href="/dashboard/receptionist/create-appointment"
              className="w-full sm:w-auto bg-black text-white px-6 py-2 rounded-lg font-medium text-sm whitespace-nowrap hover:bg-gray-800 transition-colors"
            >
              + Tạo yêu cầu
            </Link>
          </div>
        </div>

        {/* Table */}
        {loading ? (
          <div className="flex items-center justify-center p-12 gap-2 text-gray-500">
            <Loader2 className="animate-spin" size={20} /> Đang tải dữ liệu...
          </div>
        ) : (
          <>
            <div className="overflow-x-auto rounded-t-lg border border-gray-100">
              <table className="min-w-full text-sm text-left">
                <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-100 text-xs uppercase">
                  <tr>
                    <th className="px-4 py-4">STT</th>
                    <th className="px-4 py-4">Mã lịch hẹn</th>
                    <th className="px-4 py-4">Mã BN</th>
                    <th className="px-4 py-4">Tên bệnh nhân</th>
                    <th className="px-4 py-4">Bác sĩ</th>
                    <th className="px-4 py-4">Ngày hẹn</th>
                    <th className="px-4 py-4">Trạng thái</th>
                    <th className="px-4 py-4">Check-in</th>
                    <th className="px-4 py-4">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 bg-white">
                  {filtered.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="px-6 py-10 text-center text-slate-600">
                        Không có lịch hẹn nào
                      </td>
                    </tr>
                  ) : (
                    filtered.map((appt, index) => {
                      const status = STATUS_LABELS[appt.status] ?? {
                        label: formatVietnameseLabel(appt.status, APPOINTMENT_STATUS_LABELS),
                        color: 'bg-gray-100 text-gray-600 border-gray-200',
                      };
                      return (
                        <tr key={appt.appointmentId} className="hover:bg-gray-50 transition-colors">
                          <td className="px-4 py-4 text-gray-500">
                            {(currentPage - 1) * pageSize + index + 1}
                          </td>
                          <td className="px-4 py-4 font-mono text-xs text-gray-700">{appt.appointmentId}</td>
                          <td className="px-4 py-4 font-medium text-black">{appt.patientId}</td>
                          <td className="px-4 py-4 font-bold text-black">{appt.patientName}</td>
                          <td className="px-4 py-4 text-gray-600">{appt.doctorName || '—'}</td>
                          <td className="px-4 py-4 text-gray-600">
                            {formatAppointmentDate(appt.appointmentDate)}
                          </td>
                          <td className="px-4 py-4">
                            <span className={`px-2 py-1 inline-flex text-xs font-semibold rounded-full border ${status.color}`}>
                              {status.label}
                            </span>
                          </td>
                          <td className="px-4 py-4">
                            {(appt.status === 'REQUESTED' || appt.status === 'CONFIRMED') && (
                              <button
                                onClick={() => handleCheckIn(appt.appointmentId)}
                                disabled={checkingInId === appt.appointmentId}
                                className="px-3 py-1 text-xs font-bold bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors whitespace-nowrap"
                              >
                                {checkingInId === appt.appointmentId ? '...' : 'CHECK-IN'}
                              </button>
                            )}
                          </td>
                          <td className="px-4 py-4">
                            <button
                              onClick={() => setSelectedAppointmentId(appt.appointmentId)}
                              className="text-xs font-bold uppercase text-black hover:underline hover:text-gray-600 transition-colors"
                            >
                              TRUY CẬP
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="flex flex-col sm:flex-row justify-between items-center px-4 py-4 mt-2 border-t border-gray-100">
              <span className="text-sm text-gray-500 mb-3 sm:mb-0">
                Hiển thị <span className="font-bold text-black">
                  {totalElements === 0 ? 0 : (currentPage - 1) * pageSize + 1}–{Math.min(currentPage * pageSize, totalElements)}
                </span> / <span className="font-bold text-black">{totalElements}</span>
              </span>
              <div className="flex gap-1">
                {Array.from({ length: Math.min(totalPages, 5) }).map((_, i) => (
                  <button
                    key={i}
                    onClick={() => setCurrentPage(i + 1)}
                    className={`w-8 h-8 flex items-center justify-center rounded-md border text-sm font-medium transition-colors ${
                      currentPage === i + 1
                        ? 'bg-black text-white border-black'
                        : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    {i + 1}
                  </button>
                ))}
                {totalPages > 5 && <span className="flex h-8 w-8 items-center justify-center text-slate-500">...</span>}
                <button
                  onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                  disabled={currentPage >= totalPages || totalPages === 0}
                  className="px-3 h-8 flex items-center justify-center rounded-md border border-gray-200 bg-white text-gray-600 text-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Detail modal */}
      {selectedAppointmentId && (
        <AppointmentDetailModal
          appointmentId={selectedAppointmentId}
          onClose={() => setSelectedAppointmentId(null)}
          onCheckIn={handleCheckIn}
          onUpdateServices={handleUpdateServices}
          onTransferRoom={handleTransferRoom}
          onCancelAppointment={handleCancelAppointment}
          checkingInId={checkingInId}
        />
      )}
    </div>
  );
}
