import axiosClient from '@/lib/axios';

export interface WorkingTimeSlotResponse {
  slotId: number;
  shiftName: string;
  startTime: string;
  endTime: string;
  maxPatients: number;
  description: string | null;
  isActive: boolean | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateWorkingTimeSlotRequest {
  shiftName: string;
  startTime: string;
  endTime: string;
  maxPatients: number;
  description?: string | null;
  isActive?: boolean | null;
}

export interface UpdateWorkingTimeSlotRequest {
  shiftName: string;
  startTime: string;
  endTime: string;
  maxPatients: number;
  description?: string | null;
  isActive?: boolean | null;
}

function normalizeTime(t: string): string {
  if (!t) return t;
  const part = String(t).trim().slice(0, 5);
  return part.length === 5 ? part : t;
}

export const workingTimeSlotService = {
  /** GET /api/working-time-slots/active - danh sách ca đang active (dùng cho danh sách, không phân trang) */
  getActive: async (): Promise<WorkingTimeSlotResponse[]> => {
    const res = await axiosClient.get<WorkingTimeSlotResponse[]>('/working-time-slots/active');
    const list = Array.isArray(res.data) ? res.data : [];
    return list.map((s) => ({
      ...s,
      startTime: normalizeTime(s.startTime),
      endTime: normalizeTime(s.endTime),
    }));
  },

  /** POST /api/working-time-slots - mặc định description: "", isActive: true */
  create: async (body: CreateWorkingTimeSlotRequest): Promise<WorkingTimeSlotResponse> => {
    const payload = {
      shiftName: body.shiftName,
      startTime: body.startTime.length === 5 ? `${body.startTime}:00` : body.startTime,
      endTime: body.endTime.length === 5 ? `${body.endTime}:00` : body.endTime,
      maxPatients: body.maxPatients,
      description: body.description != null ? String(body.description) : '',
      isActive: body.isActive !== undefined && body.isActive !== null ? Boolean(body.isActive) : true,
    };
    const res = await axiosClient.post<WorkingTimeSlotResponse>('/working-time-slots', payload);
    const s = res.data;
    return {
      ...s,
      startTime: normalizeTime(s.startTime),
      endTime: normalizeTime(s.endTime),
    };
  },

  /** PUT /api/working-time-slots/:slotId - mặc định description: "", isActive: true */
  update: async (
    slotId: number,
    body: UpdateWorkingTimeSlotRequest
  ): Promise<WorkingTimeSlotResponse> => {
    const payload = {
      shiftName: body.shiftName,
      startTime: body.startTime.length === 5 ? `${body.startTime}:00` : body.startTime,
      endTime: body.endTime.length === 5 ? `${body.endTime}:00` : body.endTime,
      maxPatients: body.maxPatients,
      description: body.description != null ? String(body.description) : '',
      isActive: body.isActive !== undefined && body.isActive !== null ? Boolean(body.isActive) : true,
    };
    const res = await axiosClient.put<WorkingTimeSlotResponse>(
      `/working-time-slots/${slotId}`,
      payload
    );
    const s = res.data;
    return {
      ...s,
      startTime: normalizeTime(s.startTime),
      endTime: normalizeTime(s.endTime),
    };
  },

  /** DELETE /api/working-time-slots/:slotId */
  delete: async (slotId: number): Promise<void> => {
    await axiosClient.delete(`/working-time-slots/${slotId}`);
  },
};
