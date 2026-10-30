/**
 * Health Record Types
 * Based on UC-25 - View Personal Health Record
 */

export interface HealthRecordInfo {
  patientId: string;
  fullName: string;
  birthYear: number;
  healthInsuranceNumber?: string;
}

export interface MedicalHistoryItem {
  id: string;
  visitDate: string;
  patientId?: string | null;
  patientName?: string | null;
  diagnosis: string;
  doctorName: string;
  documents: MedicalDocument[];
  departmentName?: string;
  notes?: string;
  status?: string;
  visitType?: string;
  parentRecordId?: string | null;
  rootRecordId?: string | null;
  canFollowUp?: boolean;
  clinicalServiceId?: string | null;
  clinicalServiceName?: string | null;
  followUps?: MedicalHistoryItem[];
}

export interface DoctorMedicalRecordItem {
  recordId: string;
  examinationDate: string;
  patientId?: string | null;
  patientName?: string | null;
  doctorName?: string | null;
  diagnosisSummary?: string | null;
  status: string;
  visitType?: string | null;
  parentRecordId?: string | null;
  rootRecordId?: string | null;
  clinicalServiceId?: string | null;
  clinicalServiceName?: string | null;
  departmentName?: string | null;
}

export interface MedicalDocument {
  documentId: string;
  orderId?: string;
  fileName: string;
  fileUrl: string;
  fileType: string;
  uploadedAt: string;
}

export interface HealthRecordResponse {
  patientInfo: HealthRecordInfo;
  medicalHistory: MedicalHistoryItem[];
  totalCount: number;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface MedicalHistorySearchParams {
  page?: number;
  size?: number;
  keyword?: string; // Search by diagnosis or doctor name
}

export interface FollowUpOption {
  recordId: string;
  examinationDate: string;
  status: string;
  visitType?: string;
  patientId?: string;
  patientName?: string;
  doctorId?: string;
  doctorName?: string;
  departmentName?: string;
  clinicalServiceId: string;
  clinicalServiceName: string;
  diagnosisSummary?: string;
}
