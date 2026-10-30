export interface PrescriptionDetail {
  id: number;
  medicineName: string;
  dosage: string;
  quantity: string;
  frequency?: string;
  duration?: string;
  unit?: string;
  note?: string;
}

export interface PrescriptionMedicine {
  prescriptionId: string;
  medicalRecordId: string;
  notes?: string;
  createdAt: string;
  details: PrescriptionDetail[];
}

export interface DoctorReview {
  reviewId: string;
  doctorId: string;
  doctorName: string;
  doctorAvatarUrl?: string;
  departmentName?: string;
  rating: number;
  reviewText: string;
  createdAt: string;
  hasReviewed: boolean;
}

export interface ExaminationResultResponse {
  recordId: string;
  appointmentId?: string | null;
  appointmentDate?: string | null;
  appointmentTime?: string | null;
  patientId?: string | null;
  patientPhone?: string | null;
  patientHealthInsuranceNumber?: string | null;
  roomId?: string | null;
  roomName?: string | null;
  status: string;
  visitType?: string | null;
  parentRecordId?: string | null;
  rootRecordId?: string | null;
  symptoms?: string;
  diagnosis?: string;
  doctorNotes: string;
  createdAt: string;
  patient: {
    fullName: string;
    dob?: string | null;
    gender?: string | null;
  };
  doctor: {
    fullName: string;
    department?: string | null;
  };
  prescription?: PrescriptionMedicine | null;
}
