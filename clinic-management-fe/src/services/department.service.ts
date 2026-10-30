import axiosClient from '@/lib/axios';

export type DeptType = 'CLINICAL' | 'PARACLINICAL' | 'ADMINISTRATIVE' | 'PHARMACY';

export interface DepartmentResponse {
  deptId: number;
  deptName: string;
  deptType: string;
  description?: string;
  isActive: boolean;
  doctorCount: number;
  nurseCount: number;
}

export interface DepartmentDetailResponse {
  deptId: number;
  deptName: string;
  deptType: string;
  description?: string;
  isActive: boolean;
  doctors: unknown[];
  nurses: unknown[];
}

export interface CreateDepartmentRequest {
  deptName: string;
  deptType: DeptType;
  description?: string;
}

export interface UpdateDepartmentRequest {
  deptName?: string;
  deptType?: DeptType;
  description?: string;
}

export interface DepartmentsListResponse {
  departments: DepartmentResponse[];
  currentPage: number;
  totalItems: number;
  totalPages: number;
}

const detailCache = new Map<number, DepartmentDetailResponse>();

export const departmentService = {
  getList: async (
    keyword?: string,
    page = 0,
    size = 50,
    includeInactive = false
  ): Promise<DepartmentsListResponse> => {
    const params = new URLSearchParams();
    if (keyword?.trim()) params.append('keyword', keyword.trim());
    params.append('page', String(page));
    params.append('size', String(size));
    if (includeInactive) params.append('includeInactive', 'true');
    const res = await axiosClient.get<DepartmentsListResponse>(
      `/admin/departments/list?${params.toString()}`
    );
    return res.data;
  },

  getDetail: async (deptId: number): Promise<DepartmentDetailResponse> => {
    const cached = detailCache.get(deptId);
    if (cached) return cached;
    const res = await axiosClient.get<DepartmentDetailResponse>(
      `/admin/departments/detail/${deptId}`
    );
    detailCache.set(deptId, res.data);
    return res.data;
  },

  create: async (data: CreateDepartmentRequest): Promise<DepartmentResponse> => {
    const res = await axiosClient.post<DepartmentResponse>(
      '/admin/departments/create',
      data
    );
    return res.data;
  },

  update: async (
    deptId: number,
    data: UpdateDepartmentRequest
  ): Promise<DepartmentResponse> => {
    const res = await axiosClient.put<DepartmentResponse>(
      `/admin/departments/update/${deptId}`,
      data
    );
    detailCache.delete(deptId);
    return res.data;
  },

  updateStatus: async (deptId: number, isActive: boolean): Promise<DepartmentResponse> => {
    const res = await axiosClient.patch<DepartmentResponse>(
      `/admin/departments/${deptId}/status`,
      { isActive }
    );
    detailCache.delete(deptId);
    return res.data;
  },

  delete: async (deptId: number): Promise<void> => {
    await axiosClient.delete(`/admin/departments/delete/${deptId}`);
    detailCache.delete(deptId);
  },
};