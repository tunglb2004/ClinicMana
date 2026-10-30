import axiosClient from '@/lib/axios';
import type { InsuranceLookupResponse } from '@/types/financial.types';

export interface DepartmentInfo {
  deptId: number;
  deptName: string;
  deptType: string;
}

export interface ServiceItem {
  serviceId: string;
  serviceName: string;
  price: number;
  description?: string;
  isActive?: boolean;
  maxPatients?: number;
  hasOperationalRoom?: boolean;
  department?: DepartmentInfo;
}

export type ServiceCategory = 'CLINICAL' | 'PARACLINICAL';

export interface UpdateServiceConfigRequest {
  deptId?: number;
  maxPatients?: number;
}

export interface PrepaymentRequest {
  appointmentId: string;
  paymentMethod: string;
  cccdNumber?: string;
}

export interface SupplementPaymentRequest {
  recordId: string;
  paymentMethod: string;
  cccdNumber?: string;
}

export interface PaymentInitiationResponse {
  invoiceId?: string;
  paymentUrl?: string;
  paymentStatus?: string;
  paymentMethod?: string;
  amount?: number;
  totalServiceFee?: number;
  insurancePaid?: number;
  customerPaid?: number;
  message?: string;
  insuranceBreakdown?: InsuranceLookupResponse;
}

export function getUnavailableServiceAddMessage(service?: Pick<ServiceItem, 'serviceName'>): string {
  const serviceName = service?.serviceName?.trim() || 'dịch vụ đã chọn';
  return `Chưa có phòng thực hiện hợp lệ cho dịch vụ: ${serviceName}. Vui lòng cấu hình phòng cho đúng dịch vụ trước khi thêm.`;
}

export const paymentService = {
  collectPrepayment: async (data: PrepaymentRequest): Promise<PaymentInitiationResponse> => {
    const response = await axiosClient.post<PaymentInitiationResponse>('/payments/prepayment', data);
    return response.data;
  },

  supplementPayment: async (data: SupplementPaymentRequest): Promise<PaymentInitiationResponse> => {
    const response = await axiosClient.post<PaymentInitiationResponse>('/payments/supplement-payment', data);
    return response.data;
  },
};

export const catalogService = {
  getServices: async (category?: ServiceCategory, deptId?: number): Promise<ServiceItem[]> => {
    const params = new URLSearchParams();
    if (category) params.set('category', category);
    if (deptId !== undefined) params.set('deptId', deptId.toString());
    const suffix = params.size > 0 ? `?${params.toString()}` : '';
    const response = await axiosClient.get<ServiceItem[]>(`/services${suffix}`);
    if (Array.isArray(response.data)) return response.data;
    return [];
  },

  updateServiceConfig: async (serviceId: string, body: UpdateServiceConfigRequest): Promise<ServiceItem> => {
    const response = await axiosClient.put<ServiceItem>(`/services/${serviceId}/config`, body);
    return response.data;
  },
};
