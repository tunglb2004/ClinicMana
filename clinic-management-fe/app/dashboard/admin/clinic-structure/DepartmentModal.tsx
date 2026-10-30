'use client';

import { useEffect, useState } from 'react';
import {
  departmentService,
  type CreateDepartmentRequest,
  type DepartmentResponse,
  type DeptType,
  type UpdateDepartmentRequest,
} from '@/services/department.service';
import { ErrorCode, ERROR_CODE_MESSAGES } from '@/types/api.types';
import { useToast } from '@/hooks/use-toast';

interface DepartmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (data: DepartmentResponse, isCreate: boolean) => void;
  editDeptId?: number | null;
}

const DEPT_TYPE_OPTIONS: { value: DeptType; label: string }[] = [
  { value: 'CLINICAL', label: 'Lâm sàng' },
  { value: 'PARACLINICAL', label: 'Cận lâm sàng' },
  { value: 'ADMINISTRATIVE', label: 'Hành chính' },
];

export function DepartmentModal({
  isOpen,
  onClose,
  onSuccess,
  editDeptId,
}: DepartmentModalProps) {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [deptName, setDeptName] = useState('');
  const [deptType, setDeptType] = useState<DeptType>('CLINICAL');
  const [description, setDescription] = useState('');

  const isEdit = typeof editDeptId === 'number';

  useEffect(() => {
    if (!isOpen) return;

    if (isEdit && editDeptId != null) {
      let cancelled = false;
      departmentService.getDetail(editDeptId).then((department) => {
        if (!cancelled) {
          setDeptName(department.deptName);
          setDeptType(department.deptType as DeptType);
          setDescription(department.description || '');
        }
      });
      return () => {
        cancelled = true;
      };
    }

    setDeptName('');
    setDeptType('CLINICAL');
    setDescription('');
  }, [editDeptId, isEdit, isOpen]);

  const handleSave = async () => {
    if (!deptName.trim()) {
      toast({ description: 'Vui lòng nhập tên khoa.', variant: 'error' });
      return;
    }

    setLoading(true);
    try {
      if (isEdit && editDeptId) {
        const payload: UpdateDepartmentRequest = {
          deptName: deptName.trim(),
          deptType,
          description: description.trim() || undefined,
        };
        const data = await departmentService.update(editDeptId, payload);
        onClose();
        onSuccess(data, false);
        toast({
          description: ERROR_CODE_MESSAGES[ErrorCode.UPDATE_DEPT_SUCCESS] ?? 'Cập nhật khoa thành công.',
          variant: 'success',
        });
      } else {
        const payload: CreateDepartmentRequest = {
          deptName: deptName.trim(),
          deptType,
          description: description.trim() || undefined,
        };
        const data = await departmentService.create(payload);
        onClose();
        onSuccess(data, true);
        toast({
          description: ERROR_CODE_MESSAGES[ErrorCode.CREATE_DEPT_SUCCESS] ?? 'Tạo khoa thành công.',
          variant: 'success',
        });
      }
    } catch (error: unknown) {
      console.error(error);
      const message =
        error && typeof error === 'object' && 'response' in error
          ? (error as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Có lỗi xảy ra. Vui lòng thử lại.';
      toast({ description: message ?? 'Có lỗi xảy ra. Vui lòng thử lại.', variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
      <div className="bg-white w-full max-w-lg border border-slate-200 shadow-xl rounded-lg overflow-hidden">
        <div className="flex justify-between items-center p-4 border-b border-slate-200 bg-blue-50">
          <h2 className="text-lg font-bold text-slate-800">
            {isEdit ? 'Chi tiết khoa' : 'Thêm mới khoa'}
          </h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-slate-200 rounded-lg transition-colors text-slate-600"
          >
            ✕
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-semibold mb-1 text-slate-700">Tên khoa</label>
            <input
              type="text"
              className="w-full border border-slate-200 rounded p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 text-slate-800"
              value={deptName}
              onChange={(event) => setDeptName(event.target.value)}
              placeholder="VD: X-Quang"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold mb-2 text-slate-700">Loại phòng ban</label>
            <div className="flex flex-wrap gap-4">
              {DEPT_TYPE_OPTIONS.map((option) => (
                <label key={option.value} className="flex items-center gap-2 cursor-pointer text-slate-700">
                  <input
                    type="radio"
                    name="deptType"
                    checked={deptType === option.value}
                    onChange={() => setDeptType(option.value)}
                    className="w-4 h-4 text-blue-600"
                  />
                  <span className="text-sm font-medium">{option.label}</span>
                </label>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold mb-1 text-slate-700">Mô tả</label>
            <textarea
              className="w-full border border-slate-200 rounded p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 resize-none text-slate-800"
              rows={2}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Mô tả khoa (tùy chọn)"
            />
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
            disabled={loading}
            className="px-4 py-2 rounded font-semibold text-sm bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            {loading ? 'Đang lưu...' : 'Lưu'}
          </button>
        </div>
      </div>
    </div>
  );
}