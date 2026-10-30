import axiosClient from '@/lib/axios';

export interface CreateAppointmentRequest {
  patientId?: string;
  newPatient?: {
    fullName: string;
    email: string;
    phoneNumber: string;
    cccd?: string;
    gender?: 'MALE' | 'FEMALE' | 'OTHER';
    dateOfBirth?: string;
    healthInsuranceNumber?: string;
    address?: string;
  };
  doctorId?: string;
  paraclinicalDoctorId?: string;
  roomId?: string;
  serviceIds?: string[];
  appointmentDate: string;
  appointmentTime: string;
  status: string;
  notes: string;
  followUpSourceRecordId?: string;
}

export interface UpdateAppointmentRequest {
  doctorId?: string;
  paraclinicalDoctorId?: string;
  roomId?: string;
  serviceIds?: string[];
  appointmentDate?: string;
  appointmentTime?: string;
  status?: string;
  notes?: string;
}

export interface ServiceInfo {
  serviceId: string;
  serviceName: string;
  price: number;
  departmentType?: string;
  assignedRoomId?: string;
  assignedRoomName?: string;
  assignedDoctorId?: string;
  assignedDoctorName?: string;
}

export interface AppointmentItem {
  appointmentId: string;
  patientId: string;
  patientName: string;
  doctorId: number;
  doctorName: string;
  departmentName: string;
  services: ServiceInfo[];
  appointmentDate: string;
  appointmentTime: string;
  status: string;
  queueNumber: number;
  priorityScore?: number | null;
  notes: string;
  cancelReason: string;
  bookedByPatientId: string | null;
  createdAt: string;
  recordId?: string | null;
  paymentStatus?: string | null;
  visitType?: string | null;
  followUpSourceRecordId?: string | null;
}

export interface AppointmentPageResponse {
  content: AppointmentItem[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface MedicalRecordInfo {
  recordId: string;
  symptoms: string;
  diagnosis: string;
  doctorNotes: string;
  recordStatus: string;
  recordCreatedAt: string;
  visitType?: string | null;
  parentRecordId?: string | null;
  rootRecordId?: string | null;
}

export interface AppointmentDetail {
  appointmentId: string;
  appointmentDate: string;
  appointmentTime: string;
  status: string;
  queueNumber: number;
  notes: string;
  cancelReason: string;
  createdAt: string;
  patientId: string;
  patientName: string;
  patientPhone: string;
  patientDateOfBirth: string;
  patientBloodType: string;
  patientHealthInsuranceNumber: string;
  patientAllergies: string;
  patientBackgroundDisease: string;
  doctorId: number;
  doctorCode: string;
  doctorName: string;
  departmentName: string;
  roomId?: string;
  roomName?: string;
  specialization: string;
  degree: string;
  services: ServiceInfo[];
  medicalRecord: MedicalRecordInfo | null;
  visitType?: string | null;
  followUpSourceRecordId?: string | null;
  followUpSourceRecord?: MedicalRecordInfo | null;
}

export interface RoomTransferOption {
  roomId: string;
  roomName: string;
  departmentName?: string | null;
  location?: string | null;
  currentQueueLength?: number | null;
  baseAvgTime?: number | null;
  doctorId?: string | null;
  doctorName?: string | null;
  currentRoom?: boolean | null;
  serviceId?: string | null;
  serviceName?: string | null;
}

export interface AvailableSlot {
  slotId: number;
  shiftName: string;
  startTime: string;
  endTime: string;
  maxPatients: number;
  isFull: boolean;
  currentCount: number;
}

export interface RelativeInfo {
  patientId?: string;
  fullName: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  relationship?: string;
  email?: string;
  cccd?: string;
}

export interface BookAppointmentPayload {
  serviceIds?: string[];
  appointmentDate: string;
  appointmentTime: string;
  notes?: string;
  followUpSourceRecordId?: string;
  relative?: RelativeInfo;
}

export interface CancelAppointmentPayload {
  cancelReason: string;
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

export const appointmentService = {
  createAppointment: async (data: CreateAppointmentRequest): Promise<AppointmentItem> => {
    const response = await axiosClient.post('/appointments', data);
    return unwrapResponse<AppointmentItem>(response);
  },

  updateAppointment: async (appointmentId: string, data: UpdateAppointmentRequest): Promise<AppointmentItem> => {
    const response = await axiosClient.put(`/appointments/${appointmentId}`, data);
    return unwrapResponse<AppointmentItem>(response);
  },

  getRoomTransferOptions: async (appointmentId: string): Promise<RoomTransferOption[]> => {
    const response = await axiosClient.get(`/appointments/${appointmentId}/room-transfer-options`);
    return unwrapResponse<RoomTransferOption[]>(response);
  },

  transferRoom: async (appointmentId: string, roomId: string, reason?: string): Promise<AppointmentItem> => {
    const response = await axiosClient.patch(`/appointments/${appointmentId}/transfer-room`, { roomId, reason });
    return unwrapResponse<AppointmentItem>(response);
  },

  getAppointments: async (page: number = 1, size: number = 10, status?: string): Promise<AppointmentPageResponse> => {
    const params = new URLSearchParams({ page: (page - 1).toString(), size: size.toString() });
    if (status) params.append('status', status);
    const response = await axiosClient.get(`/appointments?${params.toString()}`);
    return unwrapResponse<AppointmentPageResponse>(response);
  },

  /** GET /api/appointments/{id} — full detail: patient info + medicalRecord.recordId */
  getAppointmentDetail: async (appointmentId: string): Promise<AppointmentDetail> => {
    const response = await axiosClient.get(`/appointments/${appointmentId}`);
    return unwrapResponse<AppointmentDetail>(response);
  },

  /** GET /api/appointments/my/{id} — patient-safe full detail for own appointment */
  getMyAppointmentDetail: async (appointmentId: string): Promise<AppointmentDetail> => {
    const response = await axiosClient.get(`/appointments/my/${appointmentId}`);
    return unwrapResponse<AppointmentDetail>(response);
  },

  /** GET /api/appointments/my — lịch hẹn của bệnh nhân đang đăng nhập */
  getMyAppointments: async (page: number = 1, size: number = 10, status?: string): Promise<AppointmentPageResponse> => {
    const params = new URLSearchParams({ page: (page - 1).toString(), size: size.toString() });
    if (status) params.append('status', status);
    const response = await axiosClient.get(`/appointments/my?${params.toString()}`);
    return unwrapResponse<AppointmentPageResponse>(response);
  },

  /** PATCH /api/appointments/my/{id}/cancel — bệnh nhân hủy lịch của mình */
  cancelMyAppointment: async (appointmentId: string, cancelReason: string): Promise<AppointmentItem> => {
    const body: CancelAppointmentPayload = { cancelReason };
    const response = await axiosClient.patch(`/appointments/my/${appointmentId}/cancel`, body);
    return unwrapResponse<AppointmentItem>(response);
  },

  /** PATCH /api/appointments/{id}/cancel — lễ tân/quản trị hủy lịch hẹn */
  cancelAppointment: async (appointmentId: string, cancelReason: string): Promise<AppointmentItem> => {
    const body: CancelAppointmentPayload = { cancelReason };
    const response = await axiosClient.patch(`/appointments/${appointmentId}/cancel`, body);
    return unwrapResponse<AppointmentItem>(response);
  },

  /** GET /api/appointments/available-slots?date=YYYY-MM-DD&serviceIds=SVC1&serviceIds=SVC2 */
  getAvailableSlots: async (date: string, serviceIds: string[] = []): Promise<AvailableSlot[]> => {
    const params = new URLSearchParams({ date });
    serviceIds
      .filter((serviceId) => serviceId.trim().length > 0)
      .forEach((serviceId) => params.append('serviceIds', serviceId));
    const response = await axiosClient.get(`/appointments/available-slots?${params.toString()}`);
    return unwrapResponse<AvailableSlot[]>(response);
  },

  /** GET /api/appointments/clinical-rooms?slotId=X&date=YYYY-MM-DD&serviceIds=SVC1 */
  getClinicalRooms: async (
    slotId?: number,
    date?: string,
    serviceIds: string[] = []
  ): Promise<{ roomId: string; roomName: string; doctorId: string; doctorName: string }[]> => {
    const params = new URLSearchParams();
    if (slotId != null) params.append('slotId', String(slotId));
    if (date) params.append('date', date);
    serviceIds
      .filter((serviceId) => serviceId.trim().length > 0)
      .forEach((serviceId) => params.append('serviceIds', serviceId));
    const response = await axiosClient.get(`/appointments/clinical-rooms?${params.toString()}`);
    return unwrapResponse<{ roomId: string; roomName: string; doctorId: string; doctorName: string }[]>(response);
  },

  /** POST /api/appointments/my/book */
  bookMyAppointment: async (body: BookAppointmentPayload): Promise<AppointmentItem> => {
    const response = await axiosClient.post('/appointments/my/book', body);
    return unwrapResponse<AppointmentItem>(response);
  },

  /** PATCH /api/appointments/{id}/checkin — lễ tân check-in bệnh nhân → WAITING */
  checkIn: async (appointmentId: string): Promise<AppointmentItem> => {
    const response = await axiosClient.patch(`/appointments/${appointmentId}/checkin`, {});
    return unwrapResponse<AppointmentItem>(response);
  },

  /** GET /api/appointments/my-patients — bác sĩ xem lịch hẹn của mình */
  getMyPatients: async (page: number = 1, size: number = 10, status?: string): Promise<AppointmentPageResponse> => {
    const params = new URLSearchParams({ page: (page - 1).toString(), size: size.toString() });
    if (status) params.append('status', status);
    const response = await axiosClient.get(`/appointments/my-patients?${params.toString()}`);
    return unwrapResponse<AppointmentPageResponse>(response);
  },

  callNextPatient: async (): Promise<AppointmentItem> => {
    const response = await axiosClient.post('/appointments/my-patients/call-next', {});
    return unwrapResponse<AppointmentItem>(response);
  },

  postponePatient: async (appointmentId: string): Promise<AppointmentItem> => {
    const response = await axiosClient.post(`/appointments/my-patients/${appointmentId}/postpone`, {});
    return unwrapResponse<AppointmentItem>(response);
  },

  returnPostponedPatientToQueue: async (appointmentId: string): Promise<AppointmentItem> => {
    const response = await axiosClient.post(`/appointments/my-patients/${appointmentId}/return-to-queue`, {});
    return unwrapResponse<AppointmentItem>(response);
  },

  /** GET /api/appointments/nurse-clinical-patients — hàng đợi lâm sàng cho y tá */
  getNurseClinicalPatients: async (page: number = 1, size: number = 10, status?: string): Promise<AppointmentPageResponse> => {
    const params = new URLSearchParams({ page: (page - 1).toString(), size: size.toString() });
    if (status) params.append('status', status);
    const response = await axiosClient.get(`/appointments/nurse-clinical-patients?${params.toString()}`);
    return unwrapResponse<AppointmentPageResponse>(response);
  },

  callNextNurseClinicalPatient: async (): Promise<AppointmentItem> => {
    const response = await axiosClient.post('/appointments/nurse-clinical-patients/call-next', {});
    return unwrapResponse<AppointmentItem>(response);
  },

  postponeNurseClinicalPatient: async (appointmentId: string): Promise<AppointmentItem> => {
    const response = await axiosClient.post(`/appointments/nurse-clinical-patients/${appointmentId}/postpone`, {});
    return unwrapResponse<AppointmentItem>(response);
  },

  returnPostponedNurseClinicalPatientToQueue: async (appointmentId: string): Promise<AppointmentItem> => {
    const response = await axiosClient.post(`/appointments/nurse-clinical-patients/${appointmentId}/return-to-queue`, {});
    return unwrapResponse<AppointmentItem>(response);
  },

  getNurseClinicalAppointmentDetail: async (appointmentId: string): Promise<AppointmentDetail> => {
    const response = await axiosClient.get(`/appointments/nurse-clinical-patients/${appointmentId}`);
    return unwrapResponse<AppointmentDetail>(response);
  },

  /** PATCH /api/appointments/{id}/complete — hoàn thành khám, lưu dữ liệu lâm sàng */
  completeExamination: async (appointmentId: string, body: { symptoms: string; diagnosis: string; doctorNotes: string }) => {
    const response = await axiosClient.patch(`/appointments/${appointmentId}/complete`, body);
    return unwrapResponse<AppointmentItem>(response);
  },

  /** PATCH /api/appointments/{id}/save-progress — lưu tiến độ khám mà chưa hoàn tất */
  saveExaminationProgress: async (appointmentId: string, body: { symptoms: string; diagnosis: string; doctorNotes: string }) => {
    const response = await axiosClient.patch(`/appointments/${appointmentId}/save-progress`, body);
    return unwrapResponse<AppointmentItem>(response);
  },

  pauseForParaclinicalResults: async (appointmentId: string, body: { symptoms: string; diagnosis: string; doctorNotes: string }) => {
    const response = await axiosClient.patch(`/appointments/${appointmentId}/pause-for-paraclinical`, body);
    return unwrapResponse<AppointmentItem>(response);
  },

  /** PATCH /api/appointments/{id}/nurse-save-progress — y tá lưu nhập hộ lâm sàng */
  nurseSaveClinicalProgress: async (appointmentId: string, body: { symptoms: string }) => {
    const response = await axiosClient.patch(`/appointments/${appointmentId}/nurse-save-progress`, body);
    return unwrapResponse<AppointmentItem>(response);
  },

  /** POST /api/xml-mapper/send-to-bhxh/{appointmentId} — gửi hồ sơ sang BHXH */
  sendToBhxh: async (appointmentId: string) => {
    const response = await axiosClient.post(`/xml-mapper/send-to-bhxh/${appointmentId}`);
    return unwrapResponse<AppointmentItem>(response);
  },
};
