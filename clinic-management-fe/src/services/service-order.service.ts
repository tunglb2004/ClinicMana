import axiosClient from '@/lib/axios';
import { normalizeResultFileCarrier } from '@/lib/result-file';

export interface ServiceOrderItem {
  orderId: string;
  recordId: string;
  serviceId: string;
  serviceName: string;
  price?: number;
  technicianId: string | null;
  resultNote: string | null;
  resultFileUrl: string | null;
  resultFileUrls?: string[];
  indicationNote?: string | null;
  status: string; // PENDING | COMPLETED | PAID | CANCELLED
  orderType: string; // INITIAL | SUPPLEMENT
  invoiceId: string | null;
  paymentStatus: string | null;
  unpaid: boolean;
  createdAt: string;
  assignedRoomId?: string | null;
  assignedRoomName?: string | null;
  patientId?: string;
  patientName?: string;
  appointmentId?: string;
}

export interface ServiceOrderPageResponse {
  content: ServiceOrderItem[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface CreateServiceOrderRequest {
  recordId: string;
  serviceIds: string[];
  indicationNote?: string;
}

type WrappedResponse<T> = {
  data?: T | WrappedResponse<T>;
};

const normalizeServiceOrder = (item: ServiceOrderItem): ServiceOrderItem => normalizeResultFileCarrier(item);

const normalizeServiceOrders = (items: ServiceOrderItem[]): ServiceOrderItem[] => items.map(normalizeServiceOrder);

const unwrap = <T>(response: WrappedResponse<T> | T): T => {
  const raw = typeof response === 'object' && response !== null && 'data' in response
    ? response.data
    : response;

  if (typeof raw === 'object' && raw !== null && 'data' in raw) {
    return (raw as WrappedResponse<T>).data as T;
  }

  return raw as T;
};

export const serviceOrderService = {
  /** GET /api/service-orders/{orderId} */
  getById: async (orderId: string): Promise<ServiceOrderItem> => {
    const res = await axiosClient.get(`/service-orders/${orderId}`);
    return normalizeServiceOrder(unwrap<ServiceOrderItem>(res));
  },

  /** GET /api/service-orders/by-record/{recordId} */
  getByRecord: async (recordId: string): Promise<ServiceOrderItem[]> => {
    const res = await axiosClient.get(`/service-orders/by-record/${recordId}`);
    const data = unwrap<ServiceOrderItem[]>(res);
    return Array.isArray(data) ? normalizeServiceOrders(data) : [];
  },

  /** GET /api/service-orders/by-appointment/{appointmentId} */
  getByAppointment: async (appointmentId: string): Promise<ServiceOrderItem[]> => {
    const res = await axiosClient.get(`/service-orders/by-appointment/${appointmentId}`);
    const data = unwrap<ServiceOrderItem[]>(res);
    return Array.isArray(data) ? normalizeServiceOrders(data) : [];
  },

  /** GET /api/service-orders/my-record/{recordId} */
  getMyRecordResults: async (recordId: string): Promise<ServiceOrderItem[]> => {
    const res = await axiosClient.get(`/service-orders/my-record/${recordId}`);
    const data = unwrap<ServiceOrderItem[]>(res);
    return Array.isArray(data) ? normalizeServiceOrders(data) : [];
  },

  /** POST /api/service-orders */
  create: async (data: CreateServiceOrderRequest): Promise<ServiceOrderItem[]> => {
    const res = await axiosClient.post('/service-orders', data);
    const d = unwrap<ServiceOrderItem[]>(res);
    return Array.isArray(d) ? normalizeServiceOrders(d) : [];
  },

  /** POST /api/service-orders/supplement */
  createSupplement: async (data: CreateServiceOrderRequest): Promise<ServiceOrderItem[]> => {
    const res = await axiosClient.post('/service-orders/supplement', data);
    const d = unwrap<ServiceOrderItem[]>(res);
    return Array.isArray(d) ? normalizeServiceOrders(d) : [];
  },

  /** POST /api/service-orders/{orderId}/upload-result (multipart) */
  uploadResult: async (orderId: string, file: File): Promise<ServiceOrderItem> => {
    const formData = new FormData();
    formData.append('file', file);
    // QUAN TRỌNG: Phải xóa hoàn toàn Content-Type để browser/Axios tự sinh
    // header "multipart/form-data; boundary=..." với boundary đúng.
    // Nếu set thủ công (kể cả 'multipart/form-data') → thiếu boundary → Spring lỗi 400.
    const res = await axiosClient.post(
      `/service-orders/${orderId}/upload-result`,
      formData,
      {
        transformRequest: [(data, headers) => {
          if (headers) delete headers['Content-Type'];
          return data;
        }],
      }
    );
    return normalizeServiceOrder(unwrap<ServiceOrderItem>(res));
  },

  /** PUT /api/service-orders/{orderId}/result */
  updateResultNote: async (orderId: string, resultNote: string): Promise<ServiceOrderItem> => {
    const res = await axiosClient.put(`/service-orders/${orderId}/result`, { resultNote });
    return normalizeServiceOrder(unwrap<ServiceOrderItem>(res));
  },

  /** PUT /api/service-orders/{orderId}/cancel */
  cancel: async (orderId: string): Promise<ServiceOrderItem> => {
    const res = await axiosClient.put(`/service-orders/${orderId}/cancel`);
    return normalizeServiceOrder(unwrap<ServiceOrderItem>(res));
  },

  /** GET /api/service-orders/by-patient/{patientId}/completed */
  getCompletedByPatient: async (patientId: string): Promise<ServiceOrderItem[]> => {
    const res = await axiosClient.get(`/service-orders/by-patient/${patientId}/completed`);
    const data = unwrap<ServiceOrderItem[]>(res);
    return Array.isArray(data) ? normalizeServiceOrders(data) : [];
  },

  /** GET /api/service-orders/{orderId}/export → PDF blob */
  exportPdf: async (orderId: string): Promise<Blob> => {
    const res = await axiosClient.get(`/service-orders/${orderId}/export`, {
      responseType: 'blob',
    });
    return res.data;
  },

  /** GET /api/service-orders/my/{orderId}/export -> patient PDF blob */
  exportMyPdf: async (orderId: string): Promise<Blob> => {
    const res = await axiosClient.get(`/service-orders/my/${orderId}/export`, {
      responseType: 'blob',
    });
    return res.data;
  },

  /** GET /api/service-orders?status=&page=&size= — danh sách toàn bộ */
  getAll: async (page: number = 1, size: number = 20, status?: string): Promise<ServiceOrderPageResponse> => {
    const params = new URLSearchParams({ page: (page - 1).toString(), size: size.toString() });
    if (status) params.append('status', status);
    const res = await axiosClient.get(`/service-orders?${params.toString()}`);
    const raw = unwrap<ServiceOrderPageResponse>(res);
    return {
      ...raw,
      content: normalizeServiceOrders(raw.content ?? []),
    };
  },

  /** GET /api/service-orders/supplement?status=&page=&size= — danh sách chỉ định bổ sung */
  getSupplements: async (page: number = 1, size: number = 20, status?: string): Promise<ServiceOrderPageResponse> => {
    const params = new URLSearchParams({ page: (page - 1).toString(), size: size.toString() });
    if (status) params.append('status', status);
    const res = await axiosClient.get(`/service-orders/supplement?${params.toString()}`);
    const raw = unwrap<ServiceOrderPageResponse>(res);
    return {
      ...raw,
      content: normalizeServiceOrders(raw.content ?? []),
    };
  },

  /** GET /api/service-orders/my-queue — hàng đợi cận lâm sàng của bác sĩ */
  getMyQueue: async (page: number = 1, size: number = 20, status?: string): Promise<ServiceOrderPageResponse> => {
    const params = new URLSearchParams({ page: (page - 1).toString(), size: size.toString() });
    if (status) params.append('status', status);
    const res = await axiosClient.get(`/service-orders/my-queue?${params.toString()}`);
    const raw = unwrap<ServiceOrderPageResponse>(res);
    return {
      ...raw,
      content: normalizeServiceOrders(raw.content ?? []),
    };
  },

  /** GET /api/service-orders/my-queue/by-appointment/{appointmentId} */
  getMyQueueByAppointment: async (appointmentId: string): Promise<ServiceOrderItem[]> => {
    const res = await axiosClient.get(`/service-orders/my-queue/by-appointment/${appointmentId}`);
    const data = unwrap<ServiceOrderItem[]>(res);
    return Array.isArray(data) ? normalizeServiceOrders(data) : [];
  },
};
