import axiosClient from '@/lib/axios';

export interface PatientResponse {
    patientId: string;
    userId: number;
    fullName: string;
    email: string;
    phoneNumber: string;
    cccd: string;
    gender: string;
    avatarUrl: string;
    dateOfBirth: string;
    bloodType: string;
    healthInsuranceNumber: string;
    allergies: string;
    address: string;
    height: number;
    weight: number;
    backgroundDisease: string;
    emergencyContactName: string;
    emergencyContactPhone: string;
    emergencyContactRelationship: string;
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
}

  export interface AdminPatientRecord {
    userId: number;
    patientId?: string;
    username?: string;
    email: string;
    fullName: string;
    phoneNumber?: string;
    cccd?: string;
    gender?: 'MALE' | 'FEMALE' | 'OTHER' | string;
    avatarUrl?: string;
    isActive: boolean;
    roles?: string[];
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
    createdAt?: string;
    updatedAt?: string;
  }

  export interface AdminPatientMutationPayload {
    fullName?: string;
    email?: string;
    phoneNumber?: string;
    cccd?: string;
    gender?: 'MALE' | 'FEMALE' | 'OTHER';
    avatarUrl?: string;
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
  }

export interface PageResponse<T> {
    content: T[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
    first: boolean;
}

export const patientService = {
  getPatients: async (
    page: number = 1,
    size: number = 10,
    keyword: string = '',
    sortBy: string = 'user.userId',
    sortDir: string = 'asc'
  ): Promise<PageResponse<PatientResponse>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (keyword) {
      params.append('keyword', keyword);
    }
    if (sortBy) {
        params.append('sortBy', sortBy);
    }
    if (sortDir) {
        params.append('sortDir', sortDir);
    }

     
    const response = await axiosClient.get<any>(`/patients?${params.toString()}`);
    return response.data;
  },

  getAllPatientsForDoctor: async (
    page: number = 0,
    size: number = 10,
    keyword: string = '',
    sortBy: string = 'userId',
    sortDir: string = 'desc'
  ): Promise<PageResponse<PatientResponse>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortBy: sortBy,
      sortDir: sortDir,
    });
    if (keyword) params.append('keyword', keyword);
    
     
    const response = await axiosClient.get<any>(`/admin/patients/list?${params.toString()}`);
    return response.data;
  },

  getPatientById: async (patientId: string): Promise<PatientResponse> => {
     
    const response = await axiosClient.get<any>(`/admin/patients/${patientId}`);
    return response.data;
  },

  updatePatient: async (patientId: string, data: any): Promise<PatientResponse> => {
     
    const response = await axiosClient.put<any>(`/admin/patients/${patientId}`, data);
    return response.data;
  },

  // Uses /api/patients/{patientId} (Patient-scoped endpoint, accepts UpdatePatientRequest)
  updatePatientByPatientId: async (patientId: string, data: any): Promise<any> => {
     
    const response = await axiosClient.put<any>(`/patients/${patientId}`, data);
    return response.data ?? response;
  },

  createPatient: async (data: any): Promise<any> => {
     
    const response = await axiosClient.post<any>(`/admin/patients/create`, data);
    return response.data;
  },

  getAdminPatients: async (
    page: number = 0,
    size: number = 10,
    keyword: string = '',
    sortBy: string = 'userId',
    sortDir: string = 'desc',
    isActive?: boolean
  ): Promise<PageResponse<AdminPatientRecord>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortBy,
      sortDir,
    });

    if (keyword.trim()) {
      params.append('keyword', keyword.trim());
    }

    if (typeof isActive === 'boolean') {
      params.append('isActive', String(isActive));
    }

    const response = await axiosClient.get<PageResponse<AdminPatientRecord>>(
      `/admin/patients/list?${params.toString()}`
    );
    return response.data;
  },

  getAdminPatientById: async (userId: number): Promise<AdminPatientRecord> => {
    const response = await axiosClient.get<AdminPatientRecord>(`/admin/patients/${userId}`);
    return response.data;
  },

  createAdminPatient: async (data: AdminPatientMutationPayload): Promise<AdminPatientRecord> => {
    const response = await axiosClient.post<AdminPatientRecord>('/admin/patients/create', data);
    return response.data;
  },

  updateAdminPatient: async (
    userId: number,
    data: AdminPatientMutationPayload
  ): Promise<AdminPatientRecord> => {
    const response = await axiosClient.put<AdminPatientRecord>(`/admin/patients/${userId}`, data);
    return response.data;
  },

  lockAdminPatient: async (userId: number): Promise<void> => {
    await axiosClient.patch(`/admin/patients/${userId}/lock`);
  },

  unlockAdminPatient: async (userId: number): Promise<void> => {
    await axiosClient.patch(`/admin/patients/${userId}/unlock`);
  },

  deleteAdminPatient: async (userId: number): Promise<void> => {
    await axiosClient.delete(`/admin/patients/${userId}`);
  }
};
