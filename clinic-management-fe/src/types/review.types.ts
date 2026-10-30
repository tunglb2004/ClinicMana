/**
 * Review Types
 * Based on UC-28 - View Doctor and Clinic Reviews
 */

export interface ReviewType {
  DOCTOR: 'DOCTOR';
  CLINIC: 'CLINIC';
}

export interface DoctorReviewItem {
  reviewId: string;
  doctorId: string;
  doctorName: string;
  doctorAvatarUrl?: string;
  departmentName?: string;
  appointmentDate: string;
  rating: number; // 1-5 stars
  patientName: string;
  reviewText: string;
  createdAt: string;
  updatedAt: string;
}

export interface ClinicReviewItem {
  reviewId: string;
  clinicId: string;
  clinicName: string;
  clinicLogoUrl?: string;
  appointmentDate: string;
  rating: number; // 1-5 stars
  patientName: string;
  reviewText: string;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface ReviewSearchParams {
  page?: number;
  size?: number;
}