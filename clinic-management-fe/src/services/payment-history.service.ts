import axiosClient from '@/lib/axios';
import {
  FinancialResponse,
  FinancialDetailResponse,
  PageResponse,
} from '@/types/financial.types';

export interface PaymentHistorySearchParams {
  page?: number;
  size?: number;
  keyword?: string; // Client-side search only
  paymentStatus?: string; // Filter by payment status
}

export const paymentHistoryService = {
  /**
   * Get patient payment history with pagination and filtering
   */
  getPaymentHistory: async (
    params?: PaymentHistorySearchParams
  ): Promise<PageResponse<FinancialResponse>> => {
    const queryParams = new URLSearchParams();

    queryParams.append('page', String(params?.page ?? 1));
    queryParams.append('size', String(params?.size ?? 10));
    if (params?.paymentStatus) {
      queryParams.append('paymentStatus', params.paymentStatus);
    }

    const url = `/customer/invoices${queryParams.toString() ? `?${queryParams.toString()}` : ''}`;

     
    const response = await axiosClient.get<any>(url);
    return response.data;
  },

  /**
   * Get detailed invoice information
   */
  getInvoiceDetail: async (invoiceId: string): Promise<FinancialDetailResponse> => {
     
    const response = await axiosClient.get<any>(`/customer/invoices/${invoiceId}`);
    return response.data;
  },

  /**
   * Issue the patient's electronic invoice/receipt, then return its detail.
   */
  issueReceipt: async (invoiceId: string): Promise<FinancialDetailResponse> => {
     
    const response = await axiosClient.post<any>(`/customer/invoices/${invoiceId}/receipt`);
    return response.data;
  },
};
