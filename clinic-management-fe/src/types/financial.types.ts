export interface FinancialResponse {
  invoiceId: string;
  recordId?: string;
  patientId: string;
  patientName: string;
  healthInsuranceNumber: string;
  departmentName: string;
  serviceSummary?: string;
  appointmentId: string;
  totalServiceFee: number;
  insurancePaid: number;
  customerPaid: number;
  paymentStatus: string;
  paymentMethod: string;
  receiptNumber?: string | null;
  receiptStatus?: string | null;
  receiptIssuedAt?: string | null;
  paidAt: string;
  createdAt: string;
  cashierId?: string | null;
}

export interface PaymentMethodBreakdown {
  paymentMethod: string;
  amount: number;
  invoiceCount: number;
}

export interface CashierDaySummary {
  businessDate: string;
  totalCollected: number;
  cashCollected: number;
  transferCollected: number;
  otherCollected: number;
  pendingHandovers: number;
  confirmedHandovers: number;
  cashRemaining: number;
  invoiceCount: number;
  cashInvoiceCount: number;
  transferInvoiceCount: number;
  otherInvoiceCount: number;
  methodBreakdown: PaymentMethodBreakdown[];
  recentInvoices: FinancialResponse[];
}

export interface CashHandover {
  handoverId: number;
  businessDate: string;
  fromCashierUserId: number;
  fromCashierName?: string | null;
  toCashierUserId: number;
  toCashierName?: string | null;
  amount: number;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | string;
  note?: string | null;
  createdAt: string;
  confirmedAt?: string | null;
  cancelledAt?: string | null;
  canConfirm: boolean;
  canCancel: boolean;
}

export interface CashHandoverRequest {
  businessDate: string;
  toUserId: number;
  amount: number;
  note?: string;
}

export interface ManagerCashierSummary {
  cashierUserId: number;
  cashierName: string;
  cashierStaffId?: string | null;
  summary: CashierDaySummary;
}

export interface ServiceOrderInfo {
  serviceId: string;
  serviceName: string;
  price: number;
  status: string;
  assignedRoomId?: string | null;
  assignedRoomName?: string | null;
}

export interface FinancialDetailResponse {
  invoiceId: string;
  receiptNumber: string | null;
  eInvoiceCode: string | null;
  eInvoiceUrl: string | null;
  receiptStatus: string;
  receiptErrorMessage?: string | null;
  receiptIssuedAt: string | null;
  cashierId: string;
  patientId: string;
  patientName: string;
  patientPhone: string | null;
  patientEmail: string | null;
  patientAddress: string | null;
  patientDateOfBirth: string | null;
  patientCccd: string | null;
  healthInsuranceNumber: string | null;
  appointmentId: string;
  appointmentDate: string;
  appointmentTime: string;
  doctorId: string;
  doctorName: string;
  departmentName: string | null;
  roomId: string | null;
  roomName: string | null;
  services: ServiceOrderInfo[];
  totalServiceFee: number;
  insurancePaid: number;
  customerPaid: number;
  paymentStatus: string;
  paymentMethod: string;
  transactionId: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface InsuranceCitizenInfo {
  cccdNumber: string;
  insuranceCode: string;
  fullName: string;
  dateOfBirth: string;
  gender: number;
  genderName: string;
  address: string;
  coverageRate: number | null;
  maDkbd: string;
  benefitLevel: number;
  provinceCode: string;
  isDungTuyen: boolean;
  tuyenRatio: number;
  originalCoverageRate: number;
  finalCoverageRate: number;
  validTo: string;
  status: string;
  message: string;
}

export interface InsuranceServiceBreakdown {
  serviceId: string;
  serviceName: string;
  clinicPrice: number;
  ceilingPrice: number;
  basePrice: number;
  cardCoverageRate: number;
  serviceCoverageRatio: number;
  insurancePaid: number;
  patientPaid: number;
  isCoveredByInsurance: boolean;
}

export interface InsuranceLookupResponse {
  citizenInfo: InsuranceCitizenInfo;
  serviceBreakdowns: InsuranceServiceBreakdown[];
  totalClinicPrice: number;
  totalInsurancePaid: number;
  totalPatientPaid: number;
}
