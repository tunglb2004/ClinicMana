import axiosClient from '@/lib/axios';
import { ExaminationResultResponse, PrescriptionMedicine } from '@/types/examination.types';

export interface PrescriptionDetailPayload {
  medicineName: string;
  dosage?: string;
  quantity: string;
}

export interface PrescriptionSavePayload {
  medicalRecordId: string;
  notes?: string;
  details: PrescriptionDetailPayload[];
}

function unwrapResponse<T>(response: unknown): T {
  const topLevel = response as { data?: unknown };
  const raw = topLevel.data ?? response;

  if (raw && typeof raw === 'object' && 'data' in raw) {
    const nested = raw as { data?: T };
    return (nested.data ?? raw) as T;
  }

  return raw as T;
}

export const prescriptionService = {
  getMedicalRecordDetail: async (recordId: string): Promise<ExaminationResultResponse> => {
    const response = await axiosClient.get(`/medical-examination/records/view-detail/${recordId}`);
    return unwrapResponse<ExaminationResultResponse>(response);
  },

  issuePrescription: async (payload: PrescriptionSavePayload): Promise<PrescriptionMedicine> => {
    const response = await axiosClient.post('/medical-examination/prescriptions/issue', payload);
    return unwrapResponse<PrescriptionMedicine>(response);
  },

  updatePrescription: async (prescriptionId: string, payload: PrescriptionSavePayload): Promise<PrescriptionMedicine> => {
    const response = await axiosClient.put(`/medical-examination/prescriptions/update-detail/${prescriptionId}`, payload);
    return unwrapResponse<PrescriptionMedicine>(response);
  },

  exportPrescriptionDocument: async (prescriptionId: string): Promise<string> => {
    const response = await axiosClient.get(`/medical-examination/prescriptions/export-document/${prescriptionId}`);
    return unwrapResponse<string>(response);
  },
};
