import axiosClient from '@/lib/axios';
import type { AppointmentReviewItem } from './review.service';

export interface Department {
    deptId: number;
    deptName: string;
}

export interface Staff {
    userId: number;
    fullName: string;
    username: string;
}

export interface ManagerDashboardMetrics {
    totalStaff: number;
    doctorCount: number;
    nurseCount: number;
    activeStaff: number;
    todayAppointments: number;
    waitingAppointments: number;
    completedAppointments: number;
    monthRevenue: number;
    monthPaidInvoices: number;
    averageRating: number;
    pendingReviews: number;
}

export interface ManagerRevenueDay {
    date: string;
    label: string;
    value: number;
}

export interface ManagerDashboardSummary {
    metrics: ManagerDashboardMetrics;
    revenueByDay: ManagerRevenueDay[];
    recentReviews: AppointmentReviewItem[];
}

export const managementService = {
    getDepartments: async (): Promise<Department[]> => {
        const response = await axiosClient.get<Department[]>('/manager/departments');
        return response.data;
    },

    getStaff: async (): Promise<Staff[]> => {
        const response = await axiosClient.get<Staff[]>('/manager/staff');
        return response.data;
    },

    getDashboardSummary: async (): Promise<ManagerDashboardSummary> => {
        const response = await axiosClient.get<ManagerDashboardSummary>('/manager/dashboard-summary');
        return response.data;
    },
};
