'use client';

import { useState, useEffect, useCallback, useMemo } from 'react';
import { ConfirmationModal } from '@/components/common/ConfirmationModal';
import { ShiftModal } from './ShiftModal';
import { workingTimeSlotService } from '@/services/workingTimeSlot.service';
import type { WorkingTimeSlotResponse } from '@/services/workingTimeSlot.service';

function extractErrorMessage(e: unknown): string {
  if (e && typeof e === 'object') {
    const err = e as { response?: { data?: { message?: string }; status?: number }; message?: string };
    const msg = err.response?.data?.message ?? err.message;
    if (typeof msg === 'string') return msg;
  }
  return 'Đã xảy ra lỗi.';
}

export default function ShiftConfigPage() {
  const [slots, setSlots] = useState<WorkingTimeSlotResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [keyword, setKeyword] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editSlot, setEditSlot] = useState<WorkingTimeSlotResponse | null>(null);
  const [deleteSlot, setDeleteSlot] = useState<WorkingTimeSlotResponse | null>(null);
  const [slotFormError, setSlotFormError] = useState<string | null>(null);

  const fetchSlots = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await workingTimeSlotService.getActive();
      setSlots(list ?? []);
    } catch (e) {
      setError(extractErrorMessage(e));
      setSlots([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSlots();
  }, [fetchSlots]);

  const filteredSlots = useMemo(() => {
    if (!keyword.trim()) return slots;
    const k = keyword.trim().toLowerCase();
    return slots.filter((s) => (s.shiftName ?? '').toLowerCase().includes(k));
  }, [slots, keyword]);

  /** Thêm mới: gửi description "", isActive true. Đóng popup ngay sau khi tạo thành công. */
  const handleSaveCreate = async (payload: {
    shiftName: string;
    startTime: string;
    endTime: string;
  }) => {
    setSlotFormError(null);
    try {
      await workingTimeSlotService.create({
        shiftName: payload.shiftName.trim(),
        startTime: payload.startTime.slice(0, 5),
        endTime: payload.endTime.slice(0, 5),
        maxPatients: 8,
        description: '',
        isActive: true,
      });
      setIsCreateOpen(false);
      await fetchSlots();
    } catch (e) {
      setSlotFormError(extractErrorMessage(e));
    }
  };

  /** Cập nhật: gửi description "", isActive true. Đóng popup ngay sau khi cập nhật thành công. */
  const handleSaveEdit = async (
    slotId: number,
    payload: { shiftName: string; startTime: string; endTime: string }
  ) => {
    setSlotFormError(null);
    try {
      const currentSlot = slots.find((slot) => slot.slotId === slotId);
      await workingTimeSlotService.update(slotId, {
        shiftName: payload.shiftName.trim(),
        startTime: payload.startTime.slice(0, 5),
        endTime: payload.endTime.slice(0, 5),
        maxPatients: currentSlot?.maxPatients ?? 8,
        description: '',
        isActive: true,
      });
      setEditSlot(null);
      await fetchSlots();
    } catch (e) {
      setSlotFormError(extractErrorMessage(e));
    }
  };

  /** Xóa ca. Đóng popup xác nhận ngay sau khi xóa thành công. */
  const handleConfirmDelete = async () => {
    if (!deleteSlot) return;
    setError(null);
    try {
      await workingTimeSlotService.delete(deleteSlot.slotId);
      setDeleteSlot(null);
      await fetchSlots();
    } catch (e) {
      setError(extractErrorMessage(e));
    }
  };

  return (
    <div className="p-6 bg-slate-50 min-h-screen font-sans text-slate-800">
      <h1 className="text-2xl font-bold mb-6 text-slate-800 border-b-2 border-blue-600 inline-block pb-2">
        Danh sách Ca làm việc
      </h1>

      {error && (
        <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-800 text-sm">
          {error}
        </div>
      )}

      <div className="flex flex-wrap items-center gap-3 mb-4">
        <input
          type="text"
          className="border border-slate-200 rounded p-2 text-sm text-slate-800 bg-white min-w-[180px]"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Tìm theo tên ca"
        />
        <button
          onClick={() => setIsCreateOpen(true)}
          className="px-4 py-2 bg-slate-800 text-white font-semibold text-sm uppercase hover:bg-slate-900 transition-colors rounded"
        >
          + Tạo ca mới
        </button>
        <p className="text-xs text-slate-500">
          Sức chứa bệnh nhân hiện được cấu hình theo từng dịch vụ, không còn đặt trên ca làm việc.
        </p>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">STT</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Tên Ca</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Giờ bắt đầu</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Giờ kết thúc</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-slate-500">Đang tải...</td>
                </tr>
              ) : filteredSlots.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-slate-500">
                    {keyword.trim() ? 'Không có ca nào trùng khớp.' : 'Chưa có ca nào.'}
                  </td>
                </tr>
              ) : (
                filteredSlots.map((slot, idx) => (
                  <tr key={slot.slotId} className="hover:bg-slate-50 text-slate-700 transition-colors">
                    <td className="px-4 py-3 text-sm">{idx + 1}</td>
                    <td className="px-4 py-3 text-sm font-medium">{slot.shiftName ?? '-'}</td>
                    <td className="px-4 py-3 text-sm">{slot.startTime?.slice(0, 5) ?? '-'}</td>
                    <td className="px-4 py-3 text-sm">{slot.endTime?.slice(0, 5) ?? '-'}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        <button
                          onClick={() => setEditSlot(slot)}
                          className="px-3 py-1.5 text-xs font-semibold rounded border border-slate-300 bg-white hover:bg-slate-100 transition-colors uppercase"
                        >
                          Sửa
                        </button>
                        <button
                          onClick={() => setDeleteSlot(slot)}
                          className="px-3 py-1.5 text-xs font-semibold rounded border border-slate-300 bg-white hover:bg-red-50 text-red-600 transition-colors uppercase"
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <ShiftModal
        isOpen={isCreateOpen}
        onClose={() => { setIsCreateOpen(false); setSlotFormError(null); }}
        onSaveCreate={handleSaveCreate}
        onSaveEdit={handleSaveEdit}
        editSlot={null}
        apiError={slotFormError}
        onClearApiError={() => setSlotFormError(null)}
      />

      <ShiftModal
        isOpen={!!editSlot}
        onClose={() => { setEditSlot(null); setSlotFormError(null); }}
        onSaveCreate={handleSaveCreate}
        onSaveEdit={handleSaveEdit}
        editSlot={editSlot}
        apiError={slotFormError}
        onClearApiError={() => setSlotFormError(null)}
      />

      <ConfirmationModal
        isOpen={!!deleteSlot}
        onClose={() => setDeleteSlot(null)}
        onConfirm={handleConfirmDelete}
        title="XÁC NHẬN XÓA?"
        message={deleteSlot ? `Bạn có chắc chắn xóa ca "${deleteSlot.shiftName ?? ''}" không?` : ''}
        confirmText="XÓA"
        cancelText="HỦY"
        variant="red"
      />
    </div>
  );
}
