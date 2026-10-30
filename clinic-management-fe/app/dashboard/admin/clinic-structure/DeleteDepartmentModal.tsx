'use client';

import { AlertTriangle } from 'lucide-react';

interface DeleteDepartmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  deptName: string;
  loading?: boolean;
}

export function DeleteDepartmentModal({
  isOpen,
  onClose,
  onConfirm,
  deptName,
  loading = false,
}: DeleteDepartmentModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
      <div className="bg-white w-full max-w-md border border-slate-200 shadow-xl rounded-lg overflow-hidden p-6">
        <div className="flex flex-col items-center text-center">
          <div className="mb-4 p-3 rounded-full bg-red-50">
            <AlertTriangle className="w-10 h-10 text-red-600 stroke-[2]" />
          </div>
          <h2 className="text-lg font-bold text-slate-800 mb-2">Xác nhận xóa mềm khoa?</h2>
          <p className="text-slate-600 text-sm mb-3">
            Khoa <strong className="text-slate-800">{deptName}</strong> sẽ biến mất khỏi danh sách admin.
          </p>
          <p className="text-slate-500 text-sm mb-6">
            Bản ghi đã xóa mềm không thể mở lại trên giao diện, chỉ có thể can thiệp trực tiếp DB.
          </p>
          <div className="flex gap-3 w-full justify-center">
            <button
              onClick={onClose}
              disabled={loading}
              className="px-5 py-2 rounded font-semibold text-sm text-slate-600 bg-white border border-slate-200 hover:bg-slate-50 disabled:opacity-50 transition-colors"
            >
              Hủy
            </button>
            <button
              onClick={onConfirm}
              disabled={loading}
              className="px-5 py-2 rounded font-semibold text-sm bg-red-600 text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
            >
              {loading ? 'Đang xóa...' : 'Xóa mềm'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
