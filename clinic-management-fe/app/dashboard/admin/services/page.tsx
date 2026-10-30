'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  Edit,
  Filter,
  Loader2,
  Lock,
  Plus,
  Search,
  Stethoscope,
  Trash2,
  Unlock,
} from 'lucide-react';
import { departmentService, type DepartmentResponse } from '@/services/department.service';
import { serviceCatalogService, type ServiceDetail } from '@/services/service-catalog.service';
import { useToast } from '@/hooks/use-toast';

import { DeleteServiceModal } from './DeleteServiceModal';
import { ServiceModal } from './ServiceModal';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

export default function AdminServicesPage() {
  const { toast } = useToast();
  const [services, setServices] = useState<ServiceDetail[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [processingServiceId, setProcessingServiceId] = useState<string | null>(null);

  const [searchWord, setSearchWord] = useState('');
  const [selectedDept, setSelectedDept] = useState<string>('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [isServiceModalOpen, setIsServiceModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedService, setSelectedService] = useState<ServiceDetail | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [departmentResponse, serviceResponse] = await Promise.all([
        departmentService.getList(undefined, 0, 100),
        serviceCatalogService.getServices(true),
      ]);

      setDepartments(departmentResponse.departments || []);
      setServices(serviceResponse);
    } catch (error) {
      console.error('Error fetching service data:', error);
      toast({
        description: 'Không thể tải danh sách dịch vụ.',
        variant: 'error',
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleToggleStatus = async (service: ServiceDetail, nextIsActive: boolean) => {
    setProcessingServiceId(service.serviceId);
    try {
      await serviceCatalogService.updateStatus(service.serviceId, nextIsActive);
      toast({
        description: nextIsActive
          ? 'Đã mở khóa dịch vụ thành công.'
          : 'Đã khóa dịch vụ thành công.',
        variant: 'success',
      });
      await fetchData();
    } catch (error: any) {
      console.error(error);
      toast({
        description: error?.response?.data?.message || 'Không thể cập nhật trạng thái dịch vụ.',
        variant: 'error',
      });
    } finally {
      setProcessingServiceId(null);
    }
  };

  const filteredServices = services.filter((service) => {
    const search = searchWord.trim().toLowerCase();
    const matchesSearch =
      !search ||
      service.serviceName.toLowerCase().includes(search) ||
      service.serviceId.toLowerCase().includes(search);
    const matchesDepartment = selectedDept
      ? String(service.department?.deptId || '') === selectedDept
      : true;

    return matchesSearch && matchesDepartment;
  });

  const totalPages = Math.max(1, Math.ceil(filteredServices.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const paginatedServices = filteredServices.slice(
    (safeCurrentPage - 1) * pageSize,
    safeCurrentPage * pageSize
  );
  const pageNumbers = Array.from({ length: totalPages }, (_, index) => index + 1);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchWord, selectedDept, pageSize]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  return (
    <div className="ls-page min-h-full rounded-[var(--ls-radius-lg)] p-6">
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-blue-50 text-[var(--ls-primary)]">
                <Stethoscope className="h-6 w-6" />
              </div>
              <div>
                <h1 className="ls-page-title">Danh mục dịch vụ</h1>
                <p className="ls-text-muted ls-text-muted-14">
                  Quản lý dịch vụ khám, khóa/mở khóa và xóa mềm khỏi luồng tạo lịch khám.
                </p>
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              setSelectedService(null);
              setIsServiceModalOpen(true);
            }}
            className="btn btn-primary gap-2 self-start"
          >
            <Plus className="h-4 w-4" />
            Thêm dịch vụ
          </button>
        </div>

        <div className="page-card !p-4">
          <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_260px]">
            <div className="form-control-with-icon">
              <Search className="form-control-leading-icon" />
              <input
                type="text"
                placeholder="Tìm theo mã hoặc tên dịch vụ..."
                value={searchWord}
                onChange={(event) => setSearchWord(event.target.value)}
                className="form-control"
              />
            </div>

            <div className="form-control-with-icon">
              <Filter className="form-control-leading-icon" />
              <select
                value={selectedDept}
                onChange={(event) => setSelectedDept(event.target.value)}
                className="form-control"
              >
                <option value="">Tất cả chuyên khoa</option>
                {departments.map((department) => (
                  <option key={department.deptId} value={department.deptId}>
                    {department.deptName}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {filteredServices.length > 0 && (
          <div className="page-card !p-4">
            <div className="pagination-bar !mt-0">
              <div className="pagination-summary">
                <span className="text-[var(--ls-text-subtle-color)]">
                  {paginatedServices.length} / {filteredServices.length}
                </span>
                <select
                  value={pageSize}
                  onChange={(event) => setPageSize(Number(event.target.value))}
                  className="pagination-pagesize"
                  aria-label="So dich vu moi trang"
                >
                  {PAGE_SIZE_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}/trang
                    </option>
                  ))}
                </select>
              </div>

              <div className="pagination">
                <button
                  type="button"
                  disabled={safeCurrentPage === 1}
                  onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
                  className={`pagination__item ${safeCurrentPage === 1 ? 'pagination__item--disabled' : ''}`}
                >
                  &lt;
                </button>
                {pageNumbers.map((page) => (
                  <button
                    key={page}
                    type="button"
                    onClick={() => setCurrentPage(page)}
                    className={`pagination__item ${page === safeCurrentPage ? 'pagination__item--active' : ''}`}
                  >
                    {page}
                  </button>
                ))}
                <button
                  type="button"
                  disabled={safeCurrentPage === totalPages}
                  onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                  className={`pagination__item ${safeCurrentPage === totalPages ? 'pagination__item--disabled' : ''}`}
                >
                  &gt;
                </button>
              </div>
            </div>
          </div>
        )}

        <div className="page-card !p-0 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th className="!w-[108px]">Mã DV</th>
                  <th>Tên dịch vụ</th>
                  <th>Đơn giá</th>
                  <th>Khoa thực hiện</th>
                  <th className="text-center">Giới hạn</th>
                  <th className="text-center">Trạng thái</th>
                  <th className="text-right !w-[188px]">Xử lý</th>
                </tr>
              </thead>

              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={7} className="py-14 text-center text-[var(--ls-text-subtle-color)]">
                      <div className="flex items-center justify-center gap-2">
                        <Loader2 className="h-5 w-5 animate-spin text-[var(--ls-primary)]" />
                        Đang tải dữ liệu...
                      </div>
                    </td>
                  </tr>
                ) : filteredServices.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-14 text-center text-[var(--ls-text-subtle-color)]">
                      Không có dịch vụ phù hợp.
                    </td>
                  </tr>
                ) : (
                  paginatedServices.map((service) => {
                    const isProcessing = processingServiceId === service.serviceId;

                    return (
                      <tr key={service.serviceId}>
                        <td>
                          <span className="inline-flex rounded-[var(--ls-radius-sm)] bg-slate-100 px-2.5 py-1 font-mono text-xs font-semibold text-slate-700">
                            {service.serviceId}
                          </span>
                        </td>

                        <td>
                          <div className="space-y-1">
                            <div className="font-medium text-[var(--ls-title-page-color)]">
                              {service.serviceName}
                            </div>
                            {service.bhytServiceCode && (
                              <div className="text-xs text-[var(--ls-text-subtle-color)]">
                                BHYT: {service.bhytServiceCode}
                              </div>
                            )}
                          </div>
                        </td>

                        <td className="font-semibold text-[var(--ls-primary)]">
                          {new Intl.NumberFormat('vi-VN', {
                            style: 'currency',
                            currency: 'VND',
                          }).format(service.price)}
                        </td>

                        <td className="text-[var(--ls-text-color)]">
                          {service.department?.deptName || 'Chưa phân khoa'}
                        </td>

                        <td className="text-center">
                          <span className="inline-flex rounded-[var(--ls-radius-sm)] bg-slate-100 px-2.5 py-1 text-sm text-[var(--ls-text-color)]">
                            {service.maxPatients}
                            <span className="ml-1 text-xs text-[var(--ls-text-subtle-color)]">BN/ca</span>
                          </span>
                        </td>

                        <td className="text-center">
                          {service.isActive ? (
                            <span className="inline-flex items-center rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700">
                              <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-blue-600" />
                              Đang hoạt động
                            </span>
                          ) : (
                            <span className="inline-flex items-center rounded-full bg-blue-100 px-2.5 py-1 text-xs font-semibold text-blue-800">
                              <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-blue-600" />
                              Đang khóa
                            </span>
                          )}
                        </td>

                        <td>
                          <div className="flex justify-end gap-2">
                            <button
                              type="button"
                              onClick={() => {
                                setSelectedService(service);
                                setIsServiceModalOpen(true);
                              }}
                              className="btn btn-secondary !h-9 !w-9 !px-0 text-[var(--ls-primary)]"
                              title="Chỉnh sửa dịch vụ"
                            >
                              <Edit className="h-4 w-4" />
                            </button>

                            <button
                              type="button"
                              disabled={isProcessing}
                              onClick={() => handleToggleStatus(service, !service.isActive)}
                              className={`btn btn-secondary !h-9 !w-9 !px-0 ${
                                service.isActive ? 'text-blue-700' : 'text-[var(--ls-primary)]'
                              }`}
                              title={service.isActive ? 'Khóa dịch vụ' : 'Mở khóa dịch vụ'}
                            >
                              {isProcessing ? (
                                <Loader2 className="h-4 w-4 animate-spin" />
                              ) : service.isActive ? (
                                <Lock className="h-4 w-4" />
                              ) : (
                                <Unlock className="h-4 w-4" />
                              )}
                            </button>

                            <button
                              type="button"
                              onClick={() => {
                                setSelectedService(service);
                                setIsDeleteModalOpen(true);
                              }}
                              className="btn btn-danger-soft !h-9 !w-9 !px-0"
                              title="Xóa mềm dịch vụ"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          <div className="border-t border-[var(--ls-border)] bg-[var(--ls-bg-table-header)] px-6 py-4">
            Hiển thị{' '}
            <span className="font-semibold text-[var(--ls-title-page-color)]">
              {paginatedServices.length}
            </span>{' '}
            / {services.length} dịch vụ chưa bị xóa mềm
          </div>
        </div>
      </div>

      <ServiceModal
        isOpen={isServiceModalOpen}
        onClose={() => setIsServiceModalOpen(false)}
        service={selectedService}
        onSuccess={fetchData}
      />

      <DeleteServiceModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        service={selectedService}
        onSuccess={fetchData}
      />
    </div>
  );
}
