import axiosClient from '@/lib/axios';
import {
  ExaminationResultResponse,
} from '@/types/examination.types';
import { ServiceOrderItem } from '@/services/service-order.service';

export const examinationService = {
  /**
   * Get examination result details by record ID
   */
  getExaminationResult: async (
    recordId: string
  ): Promise<ExaminationResultResponse> => {
    const response = await axiosClient.get<ExaminationResultResponse>(`/medical-examination/records/my-detail/${recordId}`);
    return response.data;
  },

  getRecordDetailForStaff: async (
    recordId: string
  ): Promise<ExaminationResultResponse> => {
    const response = await axiosClient.get<ExaminationResultResponse>(`/medical-examination/records/view-detail/${recordId}`);
    return response.data;
  },

  /**
   * Get paraclinical/service-order results of current patient's record
   */
  getRecordServiceResults: async (recordId: string): Promise<ServiceOrderItem[]> => {
    const response = await axiosClient.get<ServiceOrderItem[]>(`/service-orders/my-record/${recordId}`);
    return response.data;
  },

  exportMedicalRecordPdf: async (recordId: string): Promise<Blob> => {
    const response = await axiosClient.get(`/medical-examination/records/my/${recordId}/export`, {
      responseType: 'blob',
    });
    return response.data;
  },

  /**
   * Create doctor review
   */
  createDoctorReview: async (data: {
    examinationRecordId: string;
    doctorId: string;
    rating: number;
    reviewText: string;
  }): Promise<{ reviewId: string }> => {
     
    const response = await axiosClient.post<any>(`/patients/reviews/doctors`, data);
    return response.data;
  },
};
