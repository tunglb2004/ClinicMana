import axiosClient from '@/lib/axios';

export interface ServiceCatalogRequest {
  serviceName: string;
  price: number;
  description?: string;
  isActive: boolean;
  bhytServiceCode?: string;
  deptId?: number;
  maxPatients: number;
}

export interface ServiceDetail {
  serviceId: string;
  serviceName: string;
  price: number;
  description?: string;
  isActive: boolean;
  bhytServiceCode?: string;
  maxPatients: number;
  department?: {
    deptId: number;
    deptName: string;
    deptType: string;
  };
}

function buildServiceListSuffix(includeInactive: boolean) {
  const params = new URLSearchParams();
  if (includeInactive) {
    params.set('includeInactive', 'true');
  }
  return params.size > 0 ? `?${params.toString()}` : '';
}

export const serviceCatalogService = {
  getServices: async (includeInactive: boolean = false): Promise<ServiceDetail[]> => {
    const response = await axiosClient.get<ServiceDetail[]>(`/services${buildServiceListSuffix(includeInactive)}`);
    return Array.isArray(response.data) ? response.data : [];
  },

  create: async (data: ServiceCatalogRequest): Promise<ServiceDetail> => {
    const response = await axiosClient.post<ServiceDetail>('/services', data);
    return response.data;
  },

  update: async (serviceId: string, data: ServiceCatalogRequest): Promise<ServiceDetail> => {
    const response = await axiosClient.put<ServiceDetail>(`/services/${encodeURIComponent(serviceId)}`, data);
    return response.data;
  },

  updateStatus: async (serviceId: string, isActive: boolean): Promise<ServiceDetail> => {
    const response = await axiosClient.patch<ServiceDetail>(`/services/${encodeURIComponent(serviceId)}/status`, { isActive });
    return response.data;
  },

  delete: async (serviceId: string): Promise<void> => {
    await axiosClient.delete(`/services/${encodeURIComponent(serviceId)}`);
  },
};
