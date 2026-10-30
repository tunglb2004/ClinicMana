import axiosClient from '@/lib/axios';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  SendOtpRequest,
  MessageResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
  UserInfoResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
} from '@/types/auth.types';

/**
 * Authentication Service
 * Handles all API calls related to authentication
 * 
 * Note: The axios interceptor automatically unwraps ApiResponse<T> to T
 * So response.data directly contains the data field from backend
 * Backend: { success, message, data: T, error, timestamp }
 * Frontend receives: T (the data field)
 */
export const authService = {
  /**
   * POST /api/auth/login
   * Đăng nhập - Xác thực identifier (email/phone) và password rồi trả về JWT Token
   */
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await axiosClient.post<LoginResponse>('/auth/login', data);
    return response.data; // Already unwrapped by interceptor
  },

  /**
   * POST /api/auth/register/send-otp
   * Gửi OTP đến email của người dùng (Bước 1 của đăng ký)
   */
  sendOtp: async (data: SendOtpRequest): Promise<MessageResponse> => {
    const response = await axiosClient.post<MessageResponse>('/auth/register/send-otp', data);
    return response.data; // Already unwrapped by interceptor
  },

  /**
   * POST /api/auth/register
   * Đăng ký tài khoản mới với OTP (Bước 2 của đăng ký)
   */
  register: async (data: RegisterRequest): Promise<MessageResponse> => {
    const response = await axiosClient.post<MessageResponse>('/auth/register', data);
    return response.data; // Already unwrapped by interceptor
  },

  /**
   * POST /api/auth/logout
   * Đăng xuất - Xóa Refresh Token khỏi hệ thống
   */
  logout: async (data: RefreshTokenRequest): Promise<MessageResponse> => {
    const response = await axiosClient.post<MessageResponse>('/auth/logout', data);
    return response.data; // Already unwrapped by interceptor
  },

  /**
   * POST /api/auth/refresh-token
   * Làm mới token - Cấp lại Access Token mới khi token cũ hết hạn
   */
  refreshToken: async (data: RefreshTokenRequest): Promise<RefreshTokenResponse> => {
    const response = await axiosClient.post<RefreshTokenResponse>('/auth/refresh-token', data);
    return response.data; // Already unwrapped by interceptor
  },

  /**
   * GET /api/auth/me
   * Lấy thông tin chi tiết của người đang đăng nhập
   */
  getCurrentUser: async (): Promise<UserInfoResponse> => {
    const response = await axiosClient.get<UserInfoResponse>('/auth/me');
    return response.data; // Already unwrapped by interceptor
  },

  /**
   * POST /api/auth/forgot-password
   * Gửi OTP đến email để đặt lại mật khẩu
   */
  sendOtpForReset: async (data: ForgotPasswordRequest): Promise<MessageResponse> => {
    const response = await axiosClient.post<MessageResponse>('/auth/forgot-password', data);
    return response.data;
  },

  /**
   * POST /api/auth/reset-password
   * Đặt lại mật khẩu bằng OTP
   */
  resetPassword: async (data: ResetPasswordRequest): Promise<MessageResponse> => {
    const response = await axiosClient.post<MessageResponse>('/auth/reset-password', data);
    return response.data;
  },

  /**
   * POST /api/user/change-password
   * Đổi mật khẩu cho người dùng đang đăng nhập
   */
  changePassword: async (data: ChangePasswordRequest): Promise<MessageResponse> => {
    const response = await axiosClient.post<MessageResponse>('/user/change-password', data);
    return response.data;
  },
};
