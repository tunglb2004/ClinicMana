import axiosClient from '@/lib/axios';

export interface AppointmentReviewPageResponse {
  content: AppointmentReviewItem[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface AppointmentReviewItem {
  reviewId: number;
  patientId?: string;
  appointmentId?: string;
  doctorId?: string;
  doctorName?: string;
  departmentName?: string;
  serviceId?: string;
  serviceName?: string;
  rating: number;
  comment?: string;
  patientName?: string;
  replyContent?: string;
  replyByName?: string;
  replyAt?: string;
  createdAt: string;
}

export interface CreateAppointmentReviewPayload {
  appointmentId: string;
  doctorId: string;
  serviceId: string;
  rating: number;
  comment: string;
}

export interface ReplyReviewPayload {
  replyContent: string;
}

export const reviewService = {
  getMyReviews: async (
    page: number = 1,
    size: number = 10
  ): Promise<AppointmentReviewPageResponse> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await axiosClient.get<AppointmentReviewPageResponse>(`/reviews/my?${params.toString()}`);
    return response.data;
  },

  getMyAppointmentReviews: async (appointmentId: string): Promise<AppointmentReviewItem[]> => {
    const response = await axiosClient.get<AppointmentReviewItem[]>(`/reviews/my/appointment/${appointmentId}`);
    return response.data;
  },

  createAppointmentReview: async (
    data: CreateAppointmentReviewPayload
  ): Promise<AppointmentReviewItem> => {
    const response = await axiosClient.post<AppointmentReviewItem>('/reviews', data);
    return response.data;
  },

  getAllReviews: async (
    page: number = 1,
    size: number = 20
  ): Promise<AppointmentReviewPageResponse> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await axiosClient.get<AppointmentReviewPageResponse>(`/reviews?${params.toString()}`);
    return response.data;
  },

  replyReview: async (reviewId: number, data: ReplyReviewPayload): Promise<AppointmentReviewItem> => {
    const response = await axiosClient.put<AppointmentReviewItem>(`/reviews/${reviewId}/reply`, data);
    return response.data;
  },
};
