import axiosClient from '@/lib/axios';
import {
    NurseDoctorAssignmentRequest,
    NurseDoctorAssignmentResponse,
    StaffScheduleCreateRequest,
    StaffScheduleFilterParams,
    StaffScheduleListResponse,
    StaffScheduleMessageResponse,
    StaffScheduleResponse,
    StaffScheduleUpdateRequest,
} from '@/types/staff-schedule.types';

/**
 * Staff Schedule Service
 * Handles API calls related to staff schedules
 */
export const staffScheduleService = {
    /**
     * GET /manager/staff-schedules/list
     * Lấy danh sách lịch làm việc của nhân viên
     */
    getStaffSchedules: async (params: StaffScheduleFilterParams): Promise<StaffScheduleListResponse> => {
        const response = await axiosClient.get<StaffScheduleListResponse>('/manager/staff-schedules/list', {
            params,
        });
        return response.data;
    },

    /**
     * POST /manager/staff-schedules/create
     * Tạo lịch làm việc mới cho nhân viên
     */
    createSchedule: async (
        data: StaffScheduleCreateRequest | Record<string, unknown>
    ): Promise<StaffScheduleResponse> => {
        const response = await axiosClient.post<StaffScheduleResponse>('/manager/staff-schedules/create', data);
        return response.data;
    },

    /**
     * PUT /manager/staff-schedules/update/{scheduleId}
     * Cập nhật lịch làm việc hiện có
     */
    updateSchedule: async (
        scheduleId: number,
        data: StaffScheduleUpdateRequest
    ): Promise<StaffScheduleResponse> => {
        const response = await axiosClient.put<StaffScheduleResponse>(
            `/manager/staff-schedules/update/${scheduleId}`,
            data
        );
        return response.data;
    },

    /**
     * DELETE /manager/staff-schedules/delete/{scheduleId}
     * Xóa lịch làm việc
     */
    deleteSchedule: async (scheduleId: number): Promise<StaffScheduleMessageResponse> => {
        const response = await axiosClient.delete<StaffScheduleMessageResponse>(
            `/manager/staff-schedules/delete/${scheduleId}`
        );
        return response.data;
    },

    /**
     * POST /manager/staff-schedules/assign-nurse-to-doctor
     * Gán y tá theo dõi bác sĩ
     */
    assignNurseToDoctor: async (
        data: NurseDoctorAssignmentRequest
    ): Promise<NurseDoctorAssignmentResponse> => {
        const response = await axiosClient.post<NurseDoctorAssignmentResponse>(
            '/manager/staff-schedules/assign-nurse-to-doctor',
            data
        );
        return response.data;
    },

    /**
     * GET /manager/staff-schedules/nurses-following/{doctorUserId}
     * Lấy danh sách y tá đang theo dõi bác sĩ
     */
    getNursesFollowingDoctor: async (doctorUserId: number): Promise<NurseDoctorAssignmentResponse[]> => {
        const response = await axiosClient.get<NurseDoctorAssignmentResponse[]>(
            `/manager/staff-schedules/nurses-following/${doctorUserId}`
        );
        return response.data;
    },
};
