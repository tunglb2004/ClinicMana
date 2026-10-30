'use client';

import { useState } from 'react';
import { TriangleAlert } from 'lucide-react';
import { serviceCatalogService, type ServiceDetail } from '@/services/service-catalog.service';
import { useToast } from '@/hooks/use-toast';

interface DeleteServiceModalProps {
  isOpen: boolean;
  onClose: () => void;
  service: ServiceDetail | null;
  onSuccess: () => void;
}

export function DeleteServiceModal({
  isOpen,
  onClose,
  service,
  onSuccess,
}: DeleteServiceModalProps) {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);

  if (!isOpen || !service) {
    return null;
  }

  const handleDelete = async () => {
    setLoading(true);
    try {
      await serviceCatalogService.delete(service.serviceId);
      toast({
        description: 'Đã xóa mềm dịch vụ thành công.',
        variant: 'success',
      });
      onSuccess();
      onClose();
    } catch (error: any) {
      console.error(error);
      toast({
        description: error?.response?.data?.message || 'Có lỗi xảy ra.',
        variant: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-confirm max-w-[460px] !p-0 overflow-hidden">
        <div className="border-b border-[var(--ls-border)] px-6 py-5">
          <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-[var(--ls-danger-soft-bg)] text-[var(--ls-danger)]">
            <TriangleAlert className="h-6 w-6" />
          </div>
          <h3 className="ls-modal-title">Xóa mềm dịch vụ?</h3>
          <p className="ls-text-muted ls-text-muted-14 mt-2">
            Dịch vụ <span className="font-semibold text-[var(--ls-title-page-color)]">{service.serviceName}</span>{' '}
            sẽ biến mất hoàn toàn khỏi danh sách dịch vụ của admin và khỏi các màn tạo lịch khám.
            Các hồ sơ/lượt khám cũ đã dùng dịch vụ này vẫn được giữ nguyên. Muốn mở lại sau đó
            sẽ phải can thiệp trực tiếp vào database.
          </p>
        </div>

        <div className="modal-confirm__footer border-t border-[var(--ls-border)] bg-[var(--ls-bg-table-header)] px-6 py-4">
          <button type="button" onClick={onClose} className="btn btn-secondary">
            Hủy bỏ
          </button>
          <button
            type="button"
            onClick={handleDelete}
            disabled={loading}
            className="btn btn-danger"
          >
            {loading ? 'Đang xóa...' : 'Xóa mềm'}
          </button>
        </div>
      </div>
    </div>
  );
}
