import {
  CashHandover,
  CashHandoverRequest,
  CashierDaySummary,
  FinancialResponse,
  FinancialDetailResponse,
  ManagerCashierSummary,
  PageResponse,
  InsuranceLookupResponse,
} from '@/types/financial.types';

import axiosClient from '@/lib/axios';

const API_ENDPOINT = '/financial';

export interface FinancialSummary {
  totalRevenue: number;
  totalInsurancePaid: number;
  totalInvoices: number;
  paidCount: number;
  unpaidCount: number;
  pendingCount: number;
}

export interface CashierDashboardSummary {
  pendingInitial: number;
  pendingSupplement: number;
  paidToday: number;
  revenueToday: number;
}

export const financialService = {
  getCashierDaySummary: async (date?: string): Promise<CashierDaySummary> => {
    try {
      const params = new URLSearchParams();
      if (date) params.append('date', date);
      const suffix = params.toString() ? `?${params.toString()}` : '';
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/cashier-day-summary${suffix}`);
      return response.data ?? response;
    } catch (error) {
      console.error('Loi khi lay tong ket thu tien trong ngay:', error);
      throw error;
    }
  },

  createCashHandover: async (payload: CashHandoverRequest): Promise<CashHandover> => {
    try {
      const response = await axiosClient.post<any>(`${API_ENDPOINT}/cash-handovers`, payload);
      return response.data ?? response;
    } catch (error) {
      console.error('Loi khi tao phieu ban giao tien mat:', error);
      throw error;
    }
  },

  getCashHandovers: async (date?: string, scope: 'mine' | 'incoming' = 'mine'): Promise<CashHandover[]> => {
    try {
      const params = new URLSearchParams({ scope });
      if (date) params.append('date', date);
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/cash-handovers?${params.toString()}`);
      return response.data ?? response ?? [];
    } catch (error) {
      console.error('Loi khi lay danh sach phieu ban giao tien mat:', error);
      throw error;
    }
  },

  confirmCashHandover: async (handoverId: number): Promise<CashHandover> => {
    try {
      const response = await axiosClient.patch<any>(`${API_ENDPOINT}/cash-handovers/${handoverId}/confirm`);
      return response.data ?? response;
    } catch (error) {
      console.error('Loi khi xac nhan phieu ban giao tien mat:', error);
      throw error;
    }
  },

  cancelCashHandover: async (handoverId: number): Promise<CashHandover> => {
    try {
      const response = await axiosClient.patch<any>(`${API_ENDPOINT}/cash-handovers/${handoverId}/cancel`);
      return response.data ?? response;
    } catch (error) {
      console.error('Loi khi huy phieu ban giao tien mat:', error);
      throw error;
    }
  },

  getManagerCashierDaySummary: async (
    fromDate: string | undefined,
    toDate: string | undefined,
    cashierId: number
  ): Promise<ManagerCashierSummary> => {
    try {
      const params = new URLSearchParams({ cashierId: cashierId.toString() });
      if (fromDate) params.append('fromDate', fromDate);
      if (toDate) params.append('toDate', toDate);
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/manager/cashier-day-summary?${params.toString()}`);
      return response.data ?? response;
    } catch (error) {
      console.error('Loi khi lay tong ket thu tien cua thu ngan:', error);
      throw error;
    }
  },

  getManagerCashierSummaries: async (fromDate?: string, toDate?: string): Promise<ManagerCashierSummary[]> => {
    try {
      const params = new URLSearchParams();
      if (fromDate) params.append('fromDate', fromDate);
      if (toDate) params.append('toDate', toDate);
      const suffix = params.toString() ? `?${params.toString()}` : '';
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/manager/cashier-summaries${suffix}`);
      return response.data ?? response ?? [];
    } catch (error) {
      console.error('Loi khi lay tong ket thu tien cua tat ca thu ngan:', error);
      throw error;
    }
  },

  getManagerCashHandovers: async (
    fromDate?: string,
    toDate?: string,
    cashierId?: number,
    status?: string
  ): Promise<CashHandover[]> => {
    try {
      const params = new URLSearchParams();
      if (fromDate) params.append('fromDate', fromDate);
      if (toDate) params.append('toDate', toDate);
      if (cashierId) params.append('cashierId', cashierId.toString());
      if (status) params.append('status', status);
      const suffix = params.toString() ? `?${params.toString()}` : '';
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/manager/cash-handovers${suffix}`);
      return response.data ?? response ?? [];
    } catch (error) {
      console.error('Loi khi lay phieu ban giao tien mat cua manager:', error);
      throw error;
    }
  },

  getCashierDashboard: async (): Promise<CashierDashboardSummary> => {
    try {
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/cashier-dashboard`);
      return response.data ?? response;
    } catch (error) {
      console.error('Lá»—i khi láº¥y dashboard thu ngÃ¢n:', error);
      throw error;
    }
  },

  getSummary: async (): Promise<FinancialSummary> => {
    try {
      const response = await axiosClient.get<FinancialSummary>(`${API_ENDPOINT}/summary`);
      return response.data;
    } catch (error) {
      console.error('Lỗi khi lấy tổng quan tài chính:', error);
      throw error;
    }
  },

  getUnpaidRecords: async (
    page: number = 1,
    size: number = 10,
    keyword?: string,
    todayOnly?: boolean
  ): Promise<PageResponse<FinancialResponse>> => {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });
      if (keyword) params.append('keyword', keyword);
      if (todayOnly) params.append('todayOnly', 'true');
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/unpaid-records?${params.toString()}`);
      return response.data ?? response;
    } catch (error) {
      console.error('Lỗi khi lấy danh sách chờ thanh toán:', error);
      throw error;
    }
  },

  getInvoices: async (
    page: number = 1,
    size: number = 10,
    keyword?: string,
    paymentStatus?: string,
    todayOnly?: boolean,
    recentDays?: number
  ): Promise<PageResponse<FinancialResponse>> => {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });
      
      if (keyword) params.append('keyword', keyword);
      if (paymentStatus) params.append('paymentStatus', paymentStatus);
      if (todayOnly) params.append('todayOnly', 'true');
      if (recentDays) params.append('recentDays', recentDays.toString());

      // axiosClient likely returns the 'data' field directly from ApiResponse or res.data
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/invoices?${params.toString()}`);
      return response.data ?? response; 
    } catch (error) {
      console.error('Lỗi khi lấy danh sách hóa đơn:', error);
      throw error;
    }
  },

  getInvoiceDetail: async (invoiceId: string): Promise<FinancialDetailResponse> => {
    try {
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/invoices/${invoiceId}/receipt`);
      return response.data ?? response;
    } catch (error) {
      console.error('Lỗi khi lấy chi tiết hóa đơn:', error);
      throw error;
    }
  },

  getInvoiceStatus: async (invoiceId: string): Promise<{ invoiceId: string; paymentStatus: string }> => {
    try {
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/invoices/${invoiceId}/status`);
      return response.data ?? response;
    } catch (error) {
      console.error('Lỗi khi kiểm tra trạng thái hóa đơn:', error);
      throw error;
    }
  },
  
  exportReceipt: async (invoiceId: string): Promise<any> => {
    try {
      const response = await axiosClient.post<any>(`${API_ENDPOINT}/invoices/${invoiceId}/receipt`);
      return response.data ?? response;
    } catch (error) {
      console.error('Lỗi khi xuất phiếu thu:', error);
      throw error;
    }
  },

  getSupplementRecords: async (
    page: number = 1,
    size: number = 10,
    keyword?: string,
    todayOnly?: boolean
  ): Promise<PageResponse<FinancialResponse>> => {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });
      if (keyword) params.append('keyword', keyword);
      if (todayOnly) params.append('todayOnly', 'true');
      const response = await axiosClient.get<any>(`${API_ENDPOINT}/supplement-records?${params.toString()}`);
      return response.data ?? response;
    } catch (error) {
      console.error('Lỗi khi lấy danh sách phí cận lâm sàng:', error);
      throw error;
    }
  },

  getSupplementOrders: async (recordId: string): Promise<{
    recordId: string;
    patientId: string;
    patientName: string;
    phoneNumber: string;
    healthInsuranceNumber: string | null;
    doctorName: string | null;
    departmentName: string | null;
    appointmentDate: string | null;
    appointmentTime: string | null;
    totalAmount: number;
    orders: {
      orderId: string;
      invoiceId?: string | null;
      serviceId: string;
      serviceName: string;
      price: number;
      status: string;
      assignedRoomId: string | null;
      assignedRoomName: string | null;
    }[];
  }> => {
    try {
      const response = await axiosClient.get<any>(`/service-orders/supplement/by-record/${recordId}`);
      const raw = response.data ?? response;
      return raw.data ?? raw;
    } catch (error) {
      console.error('Lỗi khi lấy dịch vụ thanh toán bổ sung:', error);
      throw error;
    }
  },

  lookupInsurance: async (cccd: string, appointmentId: string): Promise<InsuranceLookupResponse> => {
    try {
      const response = await axiosClient.get<any>(`/citizen-insurance/lookup/cccd/${cccd}/appointment/${appointmentId}`);
      return response.data ?? response;
    } catch (error) {
      console.error('Lỗi khi tra cứu BHYT:', error);
      throw error;
    }
  },

  lookupInsuranceByRecord: async (cccd: string, recordId: string): Promise<InsuranceLookupResponse> => {
    try {
      const response = await axiosClient.get<any>(`/citizen-insurance/lookup/cccd/${cccd}/record/${recordId}`);
      return response.data ?? response;
    } catch (error) {
      console.error('Lỗi khi tra cứu BHYT theo record:', error);
      throw error;
    }
  },
};
