/**
 * Example: Generic Service Pattern
 * Template for creating new services with proper type safety
 */

import axiosClient from '@/lib/axios';

// Define your request/response types
interface CreateAppointmentRequest {
  patientId: number;
  doctorId: number;
  date: string;
  timeSlot: string;
  reason: string;
}

interface Appointment {
  id: number;
  patientName: string;
  doctorName: string;
  date: string;
  timeSlot: string;
  status: string;
}

interface AppointmentListResponse {
  appointments: Appointment[];
  total: number;
  page: number;
}

interface MessageResponse {
  message: string;
}

/**
 * Appointment Service
 * All methods automatically receive unwrapped data from axios interceptor
 */
export const appointmentService = {
  /**
   * GET /api/appointments
   * Lấy danh sách lịch hẹn
   */
  getList: async (params?: {
    page?: number;
    size?: number;
    status?: string;
  }): Promise<AppointmentListResponse> => {
    const response = await axiosClient.get<AppointmentListResponse>('/appointments', {
      params,
    });
    return response.data; // ✅ Already unwrapped by interceptor
  },

  /**
   * GET /api/appointments/:id
   * Lấy chi tiết lịch hẹn
   */
  getById: async (id: number): Promise<Appointment> => {
    const response = await axiosClient.get<Appointment>(`/appointments/${id}`);
    return response.data; // ✅ Already unwrapped by interceptor
  },

  /**
   * POST /api/appointments
   * Tạo lịch hẹn mới
   */
  create: async (data: CreateAppointmentRequest): Promise<Appointment> => {
    const response = await axiosClient.post<Appointment>('/appointments', data);
    return response.data; // ✅ Already unwrapped by interceptor
  },

  /**
   * PUT /api/appointments/:id
   * Cập nhật lịch hẹn
   */
  update: async (id: number, data: Partial<CreateAppointmentRequest>): Promise<Appointment> => {
    const response = await axiosClient.put<Appointment>(`/appointments/${id}`, data);
    return response.data; // ✅ Already unwrapped by interceptor
  },

  /**
   * DELETE /api/appointments/:id
   * Hủy lịch hẹn (soft delete)
   */
  cancel: async (id: number): Promise<MessageResponse> => {
    const response = await axiosClient.delete<MessageResponse>(`/appointments/${id}`);
    return response.data; // ✅ Already unwrapped by interceptor
  },

  /**
   * POST /api/appointments/:id/reschedule
   * Dời lịch hẹn
   */
  reschedule: async (
    id: number,
    data: { date: string; timeSlot: string }
  ): Promise<Appointment> => {
    const response = await axiosClient.post<Appointment>(`/appointments/${id}/reschedule`, data);
    return response.data; // ✅ Already unwrapped by interceptor
  },
};

/**
 * Usage Example in Component:
 * 
 * import { appointmentService } from '@/services/appointment.service';
 * import { extractErrorMessage, extractErrorCode } from '@/lib/errorHandler';
 * import { ErrorCode } from '@/types/api.types';
 * 
 * const CreateAppointmentComponent = () => {
 *   const handleCreate = async () => {
 *     try {
 *       const appointment = await appointmentService.create({
 *         patientId: 1,
 *         doctorId: 2,
 *         date: '2026-02-15',
 *         timeSlot: '09:00',
 *         reason: 'Khám tổng quát',
 *       });
 *       
 *       toast({ description: 'Đặt lịch khám thành công' });
 *       router.push(`/appointments/${appointment.id}`);
 *     } catch (error) {
 *       const errorCode = extractErrorCode(error);
 *       
 *       if (errorCode === ErrorCode.CREATE_APPOINTMENT_FULL) {
 *         // Handle full time slot
 *         setShowAlternativeSlots(true);
 *       } else if (errorCode === ErrorCode.CREATE_APPOINTMENT_MISSING_FIELDS) {
 *         // Handle missing fields
 *         formRef.current?.focus();
 *       }
 *       
 *       toast({ 
 *         variant: 'destructive',
 *         description: extractErrorMessage(error) 
 *       });
 *     }
 *   };
 * };
 */
