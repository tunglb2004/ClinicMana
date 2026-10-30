'use client';

import { useEffect, useState } from 'react';
import {
  roomService,
  type CreateRoomRequest,
  type RoomResponse,
  type UpdateRoomRequest,
} from '@/services/room.service';
import { catalogService, type ServiceItem } from '@/services/payment.service';
import { useToast } from '@/hooks/use-toast';

interface RoomModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (data: RoomResponse, isCreate: boolean) => void;
  editRoomId?: string | null;
  selectedDeptId: number | null;
  selectedDeptName: string;
}

const ROOM_ID_MAX_LENGTH = 20;

function generateRoomId(roomName: string, deptId: number | null) {
  const normalizedName = roomName
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, (match) => (match === 'đ' ? 'd' : 'D'));
  const slug = normalizedName
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toUpperCase();
  const fallback = `P-${deptId ?? 0}`;
  const suffix = Date.now().toString(36).toUpperCase().slice(-6);
  const prefixLength = ROOM_ID_MAX_LENGTH - suffix.length - 1;
  const prefix = (slug || fallback).slice(0, prefixLength).replace(/-+$/g, '') || 'P';

  return `${prefix}-${suffix}`.slice(0, ROOM_ID_MAX_LENGTH);
}

function formatRoomErrorMessage(message?: string) {
  if (!message) {
    return 'Có lỗi xảy ra. Vui lòng thử lại.';
  }

  const duplicateRoomName = message.match(/Ten phong '(.+)' da ton tai/i);
  if (duplicateRoomName) {
    return `Tên phòng "${duplicateRoomName[1]}" đã tồn tại. Vui lòng nhập tên khác.`;
  }

  const duplicateRoomId = message.match(/Ma phong '(.+)' da ton tai/i);
  if (duplicateRoomId) {
    return 'Mã phòng tự sinh bị trùng. Vui lòng bấm Lưu lại hoặc đổi tên phòng.';
  }

  return message;
}

export function RoomModal({
  isOpen,
  onClose,
  onSuccess,
  editRoomId,
  selectedDeptId,
  selectedDeptName,
}: RoomModalProps) {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [roomName, setRoomName] = useState('');
  const [displayDeptName, setDisplayDeptName] = useState('');
  const [availableServices, setAvailableServices] = useState<ServiceItem[]>([]);
  const [selectedServices, setSelectedServices] = useState<string[]>([]);
  const [servicesLoading, setServicesLoading] = useState(false);

  const isEdit = !!editRoomId;

  useEffect(() => {
    if (!isOpen) return;

    let cancelled = false;

    const loadData = async () => {
      try {
        setServicesLoading(true);
        let currentDeptId = selectedDeptId;

        if (isEdit && editRoomId) {
          const roomData = await roomService.getDetail(editRoomId);
          if (cancelled) return;

          setRoomName(roomData.roomName);
          setDisplayDeptName(roomData.deptName ?? '');
          currentDeptId = roomData.deptId;

          try {
            const mappedServices = await roomService.getServicesByRoom(editRoomId);
            if (!cancelled) {
              setSelectedServices(mappedServices.services?.map((service) => service.serviceId) || []);
            }
          } catch (error) {
            if (!cancelled) {
              console.error('Failed to fetch services for room:', error);
              setSelectedServices([]);
            }
          }
        } else {
          if (cancelled) return;
          setRoomName('');
          setDisplayDeptName(selectedDeptName);
          setSelectedServices([]);
        }

        if (currentDeptId != null) {
          const services = await catalogService.getServices(undefined, currentDeptId);
          if (!cancelled) {
            setAvailableServices(services || []);
          }
        } else if (!cancelled) {
          setAvailableServices([]);
        }
      } catch (error) {
        if (!cancelled) {
          console.error(error);
        }
      } finally {
        if (!cancelled) {
          setServicesLoading(false);
        }
      }
    };

    void loadData();

    return () => {
      cancelled = true;
    };
  }, [editRoomId, isEdit, isOpen, selectedDeptId, selectedDeptName]);

  const handleSave = async () => {
    if (!roomName.trim()) {
      toast({ description: 'Vui lòng nhập tên phòng.', variant: 'error' });
      return;
    }

    if (!isEdit && selectedDeptId == null) {
      toast({ description: 'Vui lòng chọn khoa trước khi thêm phòng.', variant: 'error' });
      return;
    }

    if (selectedServices.length === 0) {
      toast({ description: 'Vui lòng chọn ít nhất một dịch vụ cho phòng.', variant: 'error' });
      return;
    }

    setLoading(true);
    try {
      let savedRoom: RoomResponse | null = null;
      let createdRoomId: string | null = null;

      if (isEdit && editRoomId) {
        const payload: UpdateRoomRequest = {
          roomName: roomName.trim(),
        };
        savedRoom = await roomService.update(editRoomId, payload);
      } else {
        const roomId = generateRoomId(roomName.trim(), selectedDeptId);
        const payload: CreateRoomRequest = {
          roomId,
          roomName: roomName.trim(),
          deptId: selectedDeptId ?? undefined,
        };
        savedRoom = await roomService.create(payload);
        createdRoomId = savedRoom.roomId;
      }

      if (savedRoom) {
        try {
          await roomService.assignServicesToRoom(savedRoom.roomId, {
            serviceIds: selectedServices,
            confirmReplaceAll: true,
          });
        } catch (error) {
          if (!isEdit && createdRoomId) {
            try {
              await roomService.delete(createdRoomId);
            } catch (rollbackError) {
              console.error('Failed to rollback created room:', rollbackError);
            }
          }
          throw error;
        }
      }

      onClose();
      if (savedRoom) {
        onSuccess(savedRoom, !isEdit);
      }
      toast({
        description: isEdit ? 'Cập nhật phòng thành công.' : 'Tạo phòng thành công.',
        variant: 'success',
      });
    } catch (error: unknown) {
      console.error(error);
      const message =
        error && typeof error === 'object' && 'response' in error
          ? (error as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Có lỗi xảy ra. Vui lòng thử lại.';
      toast({ description: formatRoomErrorMessage(message), variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
      <div className="bg-white w-full max-w-md border border-slate-200 shadow-xl rounded-lg overflow-hidden">
        <div className="flex justify-between items-center p-4 border-b border-slate-200 bg-blue-50">
          <h2 className="text-lg font-bold text-slate-800">
            {isEdit ? 'Chi tiết phòng' : 'Thêm phòng'}
          </h2>
          <button onClick={onClose} className="p-2 hover:bg-slate-200 rounded-lg transition-colors text-slate-600">
            ✕
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-semibold mb-1 text-slate-700">Tên phòng</label>
            <input
              type="text"
              className="w-full border border-slate-200 rounded p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 text-slate-800"
              value={roomName}
              onChange={(event) => setRoomName(event.target.value)}
              placeholder="VD: BE-305"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold mb-1 text-slate-700">Thuộc khoa</label>
            <input
              type="text"
              readOnly
              className="w-full border border-slate-200 rounded p-2.5 text-sm bg-slate-100 text-slate-600 cursor-not-allowed"
              value={displayDeptName}
              tabIndex={-1}
              aria-readonly
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="block text-sm font-semibold text-slate-700 mt-2">Dịch vụ thực hiện tại phòng</label>
            {servicesLoading ? (
              <div className="text-sm text-slate-500 py-2">Đang tải danh sách dịch vụ...</div>
            ) : availableServices.length === 0 ? (
              <div className="text-sm text-slate-500 py-2 border rounded border-slate-200 bg-slate-100 px-3">
                Khoa này chưa có dịch vụ nào.
              </div>
            ) : (
              <div className="border border-slate-200 rounded-md p-3 bg-slate-50 min-h-32 max-h-48 overflow-y-auto space-y-1">
                {availableServices.map((service) => (
                  <label
                    key={service.serviceId}
                    className="flex items-center gap-3 cursor-pointer hover:bg-slate-200 p-2 rounded -mx-1 transition-colors"
                  >
                    <input
                      type="checkbox"
                      className="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                      checked={selectedServices.includes(service.serviceId)}
                      onChange={(event) => {
                        if (event.target.checked) {
                          setSelectedServices((prev) => [...prev, service.serviceId]);
                        } else {
                          setSelectedServices((prev) => prev.filter((id) => id !== service.serviceId));
                        }
                      }}
                    />
                    <span className="text-sm text-slate-700">{service.serviceName}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="flex justify-end gap-2 p-4 border-t border-slate-200 bg-slate-50">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded font-semibold text-sm text-slate-600 bg-white border border-slate-200 hover:bg-slate-100 transition-colors"
          >
            Hủy bỏ
          </button>
          <button
            onClick={handleSave}
            disabled={loading || servicesLoading}
            className="px-6 py-2 rounded font-semibold text-sm bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            {loading ? 'Đang lưu...' : 'Lưu'}
          </button>
        </div>
      </div>
    </div>
  );
}
