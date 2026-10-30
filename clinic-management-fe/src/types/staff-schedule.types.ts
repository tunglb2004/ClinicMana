export interface StaffSchedule {
    id: string;
    staffId: string;
    staffName: string;
    departmentName: string;
    date: string;
    startTime: string;
    endTime: string;
    status: 'PRESENT' | 'ABSENT' | 'SCHEDULED';
    note?: string;
}

export interface StaffScheduleResponse {
    scheduleId: number;
    userId: number;
    fullName: string;
    staffId?: string;
    role: string;
    slotId: number;
    shiftName: string;
    startTime: string;
    endTime: string;
    workDate: string;
    roomId?: string | null;
    roomName?: string | null;
    note?: string | null;
    deptName?: string | null;
}

export interface StaffScheduleListResponse {
    schedules: StaffScheduleResponse[];
    currentPage: number;
    totalItems: number;
    totalPages: number;
    date: string;

    // Legacy fields kept to avoid breaking older components.
    content?: StaffSchedule[];
    totalElements?: number;
    size?: number;
    number?: number;
}

export interface StaffScheduleFilterParams {
    date?: string;
    deptId?: number;
    page?: number;
    size?: number;
}

export interface StaffScheduleCreateRequest {
    userId: number;
    slotId: number;
    workDate: string;
    roomId?: string;
    note?: string;
}

export interface StaffScheduleUpdateRequest {
    userId?: number;
    slotId?: number;
    workDate?: string;
    roomId?: string;
    note?: string;
}

export interface StaffScheduleMessageResponse {
    message: string;
}

export interface NurseDoctorAssignmentRequest {
    nurseUserId: number;
    doctorUserId: number;
    effectiveDate?: string;
    endDate?: string;
}

export interface NurseDoctorAssignmentResponse {
    assignmentId: number;
    nurseUserId: number;
    nurseFullName: string;
    nurseStaffId: string;
    doctorUserId: number;
    doctorFullName: string;
    doctorStaffId: string;
    doctorSpecialization?: string;
    effectiveDate: string;
    endDate?: string | null;
    isActive: boolean;
}
