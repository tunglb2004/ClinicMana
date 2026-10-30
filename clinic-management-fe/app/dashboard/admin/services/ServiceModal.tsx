'use client';

import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  serviceCatalogService,
  type ServiceCatalogRequest,
  type ServiceDetail,
} from '@/services/service-catalog.service';
import { departmentService, type DepartmentResponse } from '@/services/department.service';
import { useToast } from '@/hooks/use-toast';
import { SERVICE_CODE_REGEX } from '@/lib/validation-rules';

const serviceSchema = z.object({
  serviceName: z.string().min(1, 'Vui lòng nhập tên dịch vụ'),
  price: z.coerce.number().min(0, 'Giá tiền không được nhỏ hơn 0'),
  deptId: z.coerce.number().min(1, 'Vui lòng chọn khoa phụ trách'),
  maxPatients: z.coerce.number().min(1, 'Tối thiểu 1').max(10, 'Tối đa 10'),
  isActive: z.boolean(),
  bhytServiceCode: z.string().optional(),
  description: z.string().optional(),
}).superRefine((values, ctx) => {
  const serviceName = values.serviceName.trim();
  const bhytServiceCode = values.bhytServiceCode?.trim() ?? '';
  const description = values.description?.trim() ?? '';

  if (serviceName.length === 0 || serviceName.length > 200) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['serviceName'], message: 'Tên dịch vụ phải có 1-200 ký tự' });
  }
  if (Number(values.price) <= 0) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['price'], message: 'Giá tiền phải lớn hơn 0' });
  }
  if (bhytServiceCode && !SERVICE_CODE_REGEX.test(bhytServiceCode)) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['bhytServiceCode'], message: 'Mã BHYT chỉ gồm chữ, số, dấu chấm, gạch ngang hoặc gạch dưới' });
  }
  if (bhytServiceCode.length > 50) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['bhytServiceCode'], message: 'Mã BHYT tối đa 50 ký tự' });
  }
  if (description.length > 1000) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['description'], message: 'Mô tả tối đa 1000 ký tự' });
  }
});

type ServiceFormValues = z.input<typeof serviceSchema>;

interface ServiceModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  service: ServiceDetail | null;
}

function fieldClassName(hasError: boolean) {
  return `form-control ${hasError ? 'border-red-500 focus:!border-red-500' : ''}`;
}

export function ServiceModal({ isOpen, onClose, onSuccess, service }: ServiceModalProps) {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ServiceFormValues>({
    resolver: zodResolver(serviceSchema),
    defaultValues: {
      serviceName: '',
      price: 0,
      deptId: undefined,
      maxPatients: 8,
      isActive: true,
      bhytServiceCode: '',
      description: '',
    },
  });

  const isEdit = !!service;

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    departmentService.getList(undefined, 0, 100).then((data) => {
      setDepartments(data.departments || []);
    });

    if (isEdit && service) {
      reset({
        serviceName: service.serviceName,
        price: service.price,
        deptId: service.department?.deptId || undefined,
        maxPatients: service.maxPatients,
        isActive: service.isActive,
        bhytServiceCode: service.bhytServiceCode || '',
        description: service.description || '',
      });
      return;
    }

    reset({
      serviceName: '',
      price: 0,
      deptId: undefined,
      maxPatients: 8,
      isActive: true,
      bhytServiceCode: '',
      description: '',
    });
  }, [isEdit, isOpen, reset, service]);

  const onSubmit = async (values: ServiceFormValues) => {
    setLoading(true);
    try {
      const payload: ServiceCatalogRequest = {
        serviceName: values.serviceName.trim(),
        price: Number(values.price),
        deptId: values.deptId ? Number(values.deptId) : undefined,
        maxPatients: Number(values.maxPatients),
        isActive: values.isActive,
        bhytServiceCode: values.bhytServiceCode?.trim() || undefined,
        description: values.description?.trim() || undefined,
      };

      if (isEdit && service) {
        await serviceCatalogService.update(service.serviceId, payload);
        toast({ description: 'Cập nhật dịch vụ thành công.', variant: 'success' });
      } else {
        await serviceCatalogService.create(payload);
        toast({ description: 'Thêm mới dịch vụ thành công.', variant: 'success' });
      }

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

  if (!isOpen) {
    return null;
  }

  return (
    <div className="modal-backdrop">
      <div className="w-full max-w-3xl overflow-hidden rounded-[var(--ls-radius-lg)] border border-[var(--ls-border)] bg-white shadow-[0_24px_60px_rgba(16,24,40,0.18)]">
        <div className="flex items-center justify-between border-b border-[var(--ls-border)] bg-[var(--ls-bg-table-header)] px-6 py-4">
          <div>
            <h2 className="ls-modal-title">
              {isEdit ? 'Chỉnh sửa dịch vụ' : 'Thêm mới dịch vụ'}
            </h2>
            <p className="ls-text-muted ls-text-muted-14 mt-1">
              {isEdit
                ? 'Cập nhật thông tin hiển thị và trạng thái sử dụng của dịch vụ.'
                : 'Thiết lập dịch vụ mới để hiển thị trong các luồng chọn dịch vụ.'}
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="btn btn-secondary !h-9 !w-9 !px-0"
            aria-label="Đóng"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="grid gap-5 p-6 sm:grid-cols-2">
            <div className="space-y-2 sm:col-span-1">
              <label className="text-sm font-semibold text-[var(--ls-table-header-color)]">
                Tên dịch vụ <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                {...register('serviceName')}
                className={fieldClassName(!!errors.serviceName)}
                placeholder="Khám cận lâm sàng..."
              />
              {errors.serviceName && (
                <span className="text-xs text-red-500">{errors.serviceName.message}</span>
              )}
            </div>

            <div className="space-y-2 sm:col-span-1">
              <label className="text-sm font-semibold text-[var(--ls-table-header-color)]">
                Giá dịch vụ (đ) <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                {...register('price')}
                className={fieldClassName(!!errors.price)}
                placeholder="200000"
              />
              {errors.price && (
                <span className="text-xs text-red-500">{errors.price.message}</span>
              )}
            </div>

            <div className="space-y-2 sm:col-span-1">
              <label className="text-sm font-semibold text-[var(--ls-table-header-color)]">
                Khoa phụ trách
              </label>
              <select {...register('deptId')} className="form-control">
                <option value="">-- Chọn khoa (tùy chọn) --</option>
                {departments.map((department) => (
                  <option key={department.deptId} value={department.deptId}>
                    {department.deptName}
                  </option>
                ))}
              </select>
              {errors.deptId && (
                <span className="text-xs text-red-500">{errors.deptId.message}</span>
              )}
            </div>

            <div className="space-y-2 sm:col-span-1">
              <label className="text-sm font-semibold text-[var(--ls-table-header-color)]">
                Giới hạn BN / ca <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                {...register('maxPatients')}
                className={fieldClassName(!!errors.maxPatients)}
              />
              {errors.maxPatients && (
                <span className="text-xs text-red-500">{errors.maxPatients.message}</span>
              )}
            </div>

            <div className="space-y-2 sm:col-span-2">
              <label className="text-sm font-semibold text-[var(--ls-table-header-color)]">
                Mã BHYT
              </label>
              <input
                type="text"
                {...register('bhytServiceCode')}
                className={fieldClassName(!!errors.bhytServiceCode)}
                placeholder="Ví dụ: 01A03.0111"
              />
              {errors.bhytServiceCode && (
                <span className="text-xs text-red-500">{errors.bhytServiceCode.message}</span>
              )}
            </div>

            <div className="space-y-2 sm:col-span-2">
              <label className="text-sm font-semibold text-[var(--ls-table-header-color)]">
                Mô tả chi tiết
              </label>
              <textarea
                {...register('description')}
                rows={4}
                className={`${fieldClassName(!!errors.description)} !h-28 resize-none py-2.5`}
                placeholder="Ghi chú về dịch vụ..."
              />
              {errors.description && (
                <span className="text-xs text-red-500">{errors.description.message}</span>
              )}
            </div>

            <div className="sm:col-span-2">
              <label className="flex items-center gap-3 rounded-[var(--ls-radius-sm)] border border-[var(--ls-border)] bg-slate-50 px-4 py-3 text-sm text-[var(--ls-text-color)]">
                <input
                  type="checkbox"
                  {...register('isActive')}
                  className="h-4 w-4 accent-[var(--ls-primary)]"
                />
                <span>
                  Đang hoạt động
                  <span className="ml-1 text-[var(--ls-text-subtle-color)]">
                    (dịch vụ này sẽ xuất hiện ở các luồng chọn dịch vụ)
                  </span>
                </span>
              </label>
            </div>
          </div>

          <div className="flex justify-end gap-3 border-t border-[var(--ls-border)] bg-[var(--ls-bg-table-header)] px-6 py-4">
            <button type="button" onClick={onClose} className="btn btn-secondary">
              Hủy bỏ
            </button>
            <button type="submit" disabled={loading} className="btn btn-primary">
              {loading ? 'Đang lưu...' : isEdit ? 'Lưu thay đổi' : 'Tạo dịch vụ'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
