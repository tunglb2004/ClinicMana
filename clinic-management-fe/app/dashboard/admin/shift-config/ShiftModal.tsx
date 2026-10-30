'use client';

import { useState, useEffect, useMemo } from 'react';
import { AlertTriangle } from 'lucide-react';
import { workingTimeSlotService } from '@/services/workingTimeSlot.service';
import { TimePicker } from './TimePicker';
import type { WorkingTimeSlotResponse } from '@/services/workingTimeSlot.service';

interface ShiftModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaveCreate: (payload: { shiftName: string; startTime: string; endTime: string }) => Promise<void>;
  onSaveEdit: (
    slotId: number,
    payload: { shiftName: string; startTime: string; endTime: string }
  ) => Promise<void>;
  editSlot: WorkingTimeSlotResponse | null;
  apiError: string | null;
  onClearApiError: () => void;
}

function timeToMinutes(t: string): number {
  const part = String(t).trim().slice(0, 5);
  const [h, m] = part.split(':').map(Number);
  return (h ?? 0) * 60 + (m ?? 0);
}

function doOverlap(
  start1: string,
  end1: string,
  start2: string,
  end2: string
): boolean {
  const s1 = timeToMinutes(start1);
  const e1 = timeToMinutes(end1);
  const s2 = timeToMinutes(start2);
  const e2 = timeToMinutes(end2);
  return s1 < e2 && e1 > s2;
}

export function ShiftModal({
  isOpen,
  onClose,
  onSaveCreate,
  onSaveEdit,
  editSlot,
  apiError,
  onClearApiError,
}: ShiftModalProps) {
  const [shiftName, setShiftName] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [saving, setSaving] = useState(false);
  const [otherSlots, setOtherSlots] = useState<WorkingTimeSlotResponse[]>([]);

  const isEdit = !!editSlot;

  useEffect(() => {
    if (isOpen) {
      onClearApiError();
      if (editSlot) {
        setShiftName(editSlot.shiftName ?? '');
        setStartTime(editSlot.startTime?.slice(0, 5) ?? '07:30');
        setEndTime(editSlot.endTime?.slice(0, 5) ?? '11:30');
      } else {
        setShiftName('');
        setStartTime('');
        setEndTime('');
      }
      setOtherSlots([]);
    }
  }, [isOpen, editSlot, onClearApiError]);

  useEffect(() => {
    if (!isOpen) return;
    let cancelled = false;
    workingTimeSlotService
      .getActive()
      .then((list) => {
        if (cancelled) return;
        const exclude = isEdit && editSlot
          ? (s: WorkingTimeSlotResponse) => s.slotId !== editSlot.slotId
          : () => true;
        setOtherSlots(list.filter(exclude));
      })
      .catch(() => {
        if (!cancelled) setOtherSlots([]);
      });
    return () => { cancelled = true; };
  }, [isOpen, isEdit, editSlot]);

  const startMinutes = timeToMinutes(startTime);
  const endMinutes = timeToMinutes(endTime);
  const startNotBeforeEnd = startTime && endTime && startMinutes >= endMinutes;
  const overlapError = useMemo(() => {
    const start = startTime.trim().slice(0, 5);
    const end = endTime.trim().slice(0, 5);
    if (!start || !end || startMinutes >= endMinutes) return null;
    for (const other of otherSlots) {
      const oStart = other.startTime?.slice(0, 5) ?? '';
      const oEnd = other.endTime?.slice(0, 5) ?? '';
      if (doOverlap(start, end, oStart, oEnd)) {
        return 'Khung giờ đã trùng với ca đã có.';
      }
    }
    return null;
  }, [startTime, endTime, startMinutes, endMinutes, otherSlots]);

  const timeErrorMessage =
    apiError ??
    (startNotBeforeEnd ? 'Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc.' : null) ??
    overlapError ??
    null;

  const canSave =
    !!shiftName.trim() &&
    !!startTime &&
    !!endTime &&
    startMinutes < endMinutes &&
    !overlapError &&
    !apiError &&
    !saving;

  const handleSave = async () => {
    if (!canSave) return;
    setSaving(true);
    onClearApiError();
    try {
      const payload = {
        shiftName: shiftName.trim(),
        startTime: startTime.slice(0, 5),
        endTime: endTime.slice(0, 5),
      };
      if (isEdit && editSlot) {
        await onSaveEdit(editSlot.slotId, payload);
        onClose();
      } else {
        await onSaveCreate(payload);
        onClose();
      }
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
      <div className="bg-white w-full max-w-lg border border-slate-200 shadow-xl rounded-lg overflow-hidden">
        <div className="flex justify-between items-center p-4 border-b border-slate-200 bg-slate-100">
          <h2 className="text-lg font-bold text-slate-800 uppercase tracking-wide">
            {isEdit ? 'Chi tiết ca' : 'Tạo ca mới'}
          </h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-slate-200 rounded-lg transition-colors text-slate-600 font-bold"
          >
            ✕
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-semibold mb-1 text-slate-700">Tên Ca</label>
            <input
              type="text"
              className="w-full border border-slate-200 rounded p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 text-slate-800"
              value={shiftName}
              onChange={(e) => setShiftName(e.target.value)}
              placeholder="VD: Ca Sáng"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold mb-1 text-slate-700">Bắt đầu</label>
              <TimePicker
                value={startTime}
                onChange={setStartTime}
                placeholder={isEdit ? '07:30' : 'Chọn giờ'}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold mb-1 text-slate-700">Kết thúc</label>
              <TimePicker
                value={endTime}
                onChange={setEndTime}
                placeholder={isEdit ? '11:30' : 'Chọn giờ'}
              />
            </div>
          </div>

          {timeErrorMessage && (
            <div className="flex gap-2 p-3 rounded-lg bg-blue-50 border border-blue-200">
              <AlertTriangle className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />
              <p className="text-sm text-blue-800">{timeErrorMessage}</p>
            </div>
          )}
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
            disabled={!canSave}
            className="px-4 py-2 rounded font-semibold text-sm bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {saving ? 'Đang lưu...' : 'Lưu'}
          </button>
        </div>
      </div>
    </div>
  );
}
