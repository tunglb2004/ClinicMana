import axiosClient from '@/lib/axios';

/**
 * Request body for updating user profile
 */
export interface UpdateProfileRequest {
  fullName?: string;
  cccd?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  phoneNumber?: string;
  dateOfBirth?: string;
  bloodType?: string;
  healthInsuranceNumber?: string;
  allergies?: string;
  address?: string;
  height?: number;
  weight?: number;
  backgroundDisease?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  licenseNumber?: string;
  specialization?: string;
  degree?: string;
  certificationIssueDate?: string;
  certificationPlace?: string;
  certificationFileUrl?: string;
  trainingPlace?: string;
  graduationYear?: string;
  degreeFileUrl?: string;
  experienceYears?: number;
  previousWorkplace?: string;
  bio?: string;
  position?: string;
}

/**
 * Request body for sending OTP
 */
export interface SendOtpRequest {
  email: string;
  type: 'CHANGE_EMAIL' | 'RESET_PASSWORD' | 'REGISTER';
}

/**
 * Request body for changing email
 */
export interface ChangeEmailRequest {
  newEmail: string;
  otpCode: string;
}

/**
 * Response from profile API
 * Profile structure varies based on user role
 */
export interface ProfileResponse {
  id: number;
  username: string;
  email: string;
  fullName: string;
  cccd?: string;
  gender?: string;
  phoneNumber?: string;
  avatarUrl?: string;
  isActive: boolean;
  roles: string[];
  profile: {
    // Patient fields
    patientId?: string;
    dateOfBirth?: string;
    bloodType?: string;
    healthInsuranceNumber?: string;
    allergies?: string;
    address?: string;
    height?: number;
    weight?: number;
    backgroundDisease?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    emergencyContactRelationship?: string;

    // Nurse fields
    nurseId?: string;
    certificationNumber?: string;
    certificationIssueDate?: string;
    certificationPlace?: string;
    certificationFileUrl?: string;
    educationLevel?: string;
    yearsOfExperience?: number;
    supportSkills?: string;
    previousWorkplaces?: string;

    // Doctor fields
    doctorId?: string;
    licenseNumber?: string;
    specialization?: string;
    degree?: string;
    experienceYears?: number;
    previousWorkplace?: string;
    bio?: string;
    isHeadOfDept?: boolean;
    room?: {
      roomName?: string;
    };

    // Common fields for both Nurse and Doctor
    department?: string | {
      deptId?: number;
      deptName?: string;
      deptType?: string;
    };
    position?: string;
    trainingPlace?: string;
    graduationYear?: number | string;
    degreeFileUrl?: string;
  };
}

/**
 * User type for user management
 */
export interface User {
  userId: number;
  username: string;
  email: string;
  fullName: string;
  staffId?: string;
  patientId?: string;
  doctorId?: string;
  roomId?: string;
  deptId?: number | null;
  deptName?: string;
  deptType?: string;
  cccd?: string;
  phoneNumber?: string;
  isActive: boolean;
  roles: string[];
}

/**
 * Response from get users API
 */
export interface GetUsersResponse {
  users: User[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
}

export interface UserMutationPayload {
  fullName?: string;
  email?: string;
  username?: string;
  cccd?: string;
  phoneNumber?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  avatarUrl?: string;
  roleNames?: string[];
  isActive?: boolean;
}

export type UserDetailResponse = User & {
  createdAt?: string;
  updatedAt?: string;
  [key: string]: unknown;
};

export interface SyncExistingStaffProfilesResponse {
  totalUsersScanned: number;
  usersUpdated: number;
  doctorProfilesCreated: number;
  nurseProfilesCreated: number;
  doctorRolesAdded: number;
  nurseRolesAdded: number;
}

/**
 * User Service
 * Handles all API calls related to user profile
 * 
 * Note: The axios interceptor automatically unwraps ApiResponse<T> to T
 * So response.data directly contains the data field from backend
 * Backend: { success, message, data: T, error, timestamp }
 * Frontend receives: T (the data field)
 */
export const userService = {
  /**
   * GET /api/user/profile
   * Lấy thông tin profile của user hiện tại
   */
  getProfile: async (): Promise<ProfileResponse> => {
    const response = await axiosClient.get<ProfileResponse>('/user/profile');
    return response.data; // Already unwrapped by interceptor
  },

  getManagedProfile: async (userId: number): Promise<ProfileResponse> => {
    const response = await axiosClient.get<ProfileResponse>(`/users/${userId}/profile`);
    return response.data;
  },

  /**
   * PUT /api/user/profile
   * Cập nhật thông tin profile của user hiện tại
   */
  updateProfile: async (data: UpdateProfileRequest): Promise<ProfileResponse> => {
    const response = await axiosClient.put<ProfileResponse>('/user/profile', data);
    return response.data; // Already unwrapped by interceptor
  },

  uploadAvatar: async (file: File): Promise<ProfileResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosClient.post<ProfileResponse>('/user/profile/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  uploadProfileFile: async (file: File): Promise<{ fileUrl: string }> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosClient.post<{ fileUrl: string }>('/user/profile/files', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  /**
   * POST /api/otp/send
   * Gửi OTP tới email mới
   */
  sendOtp: async (data: SendOtpRequest): Promise<{ message: string }> => {
    const response = await axiosClient.post<{ message: string }>('/otp/send', data);
    return response.data; // Already unwrapped by interceptor
  },

  /**
   * POST /api/user/change-email
   * Xác nhận đổi email với OTP
   */
  changeEmail: async (data: ChangeEmailRequest): Promise<{ message: string }> => {
    const response = await axiosClient.post<{ message: string }>('/user/change-email', data);
    return response.data; // Already unwrapped by interceptor
  },

  /**
   * GET /api/admin/users
   * Lấy danh sách users (Admin only)
   * @param page - Page number (0-indexed)
   * @param size - Number of items per page
   * @param search - Search term for username/email/fullName
   * @param roleFilter - Filter by role (ALL, STAFF_ONLY, PATIENT_ONLY)
   */
  getUsers: async (
    page: number = 0,
    size: number = 10,
    search: string = '',
    roleFilter: string = 'ALL'
  ): Promise<GetUsersResponse> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (search) {
      params.append('keyword', search);
    }

    if (roleFilter && roleFilter !== 'ALL') {
      params.append('role', roleFilter);
    }

    const response = await axiosClient.get<GetUsersResponse>(`/users?${params.toString()}`);
    return response.data;
  },

  /**
   * POST /api/admin/users
   * Tạo mới user
   */
  createUser: async (data: UserMutationPayload): Promise<UserDetailResponse> => {
    const response = await axiosClient.post<UserDetailResponse>('/users', data);
    return response.data;
  },

  /**
   * GET /api/admin/users/{id}
   * Lấy chi tiết user
   */
  getUserDetail: async (userId: number): Promise<UserDetailResponse> => {
    const response = await axiosClient.get<UserDetailResponse>(`/users/${userId}`);
    return response.data;
  },

  /**
   * PUT /api/admin/users/{id}
   * Cập nhật thông tin user
   */
  updateUser: async (userId: number, data: UserMutationPayload): Promise<UserDetailResponse> => {
    const response = await axiosClient.put<UserDetailResponse>(`/users/${userId}`, data);
    return response.data;
  },

  /**
   * PATCH /api/admin/users/status/{id}
   * Cập nhật trạng thái user (Active/Inactive)
   */
  updateStatus: async (userId: number, isActive: boolean): Promise<void> => {
    if (isActive) {
      await axiosClient.patch(`/users/${userId}/unlock`);
    } else {
      await axiosClient.patch(`/users/${userId}/lock`);
    }
  },

  /**
   * DELETE /api/admin/users/delete/{id}
   * Xóa user
   */
  deleteUser: async (userId: number): Promise<void> => {
    await axiosClient.delete(`/users/${userId}`);
  },

  /**
   * POST /api/admin/users/sync-existing-staff-profiles
   * Đồng bộ user cũ: role <-> doctor/nurse profile
   */
  syncExistingStaffProfiles: async (): Promise<SyncExistingStaffProfilesResponse> => {
    const response = await axiosClient.post<SyncExistingStaffProfilesResponse>(
      '/users/sync-existing-staff-profiles'
    );
    return response.data;
  },
};
