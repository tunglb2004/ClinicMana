export interface User {
    userId: number;
    username: string;
    email: string;
    fullName: string;
    staffId?: string;
    patientId?: string;
    cccd?: string;
    phoneNumber?: string;
    isActive: boolean;
    roles: string[];
}

export interface UserDetail extends User {
    patientId?: string;
    doctorId?: string;
    nurseId?: string;
    roomId?: string;
    deptId?: number | null;
    deptName?: string;
    deptType?: string;
    cccd?: string;
    gender?: 'MALE' | 'FEMALE' | 'OTHER';
    avatarUrl?: string;
    createdAt?: string;
    updatedAt?: string;

    // Doctor/Nurse specific fields (Conceptual - for mapping)
    specialization?: string; // Khoa/Chuyên môn
    position?: string; // Chức danh
    degree?: string; // Trình độ/Bằng cấp
    licenseNumber?: string; // Số chứng chỉ hành nghề
    licenseDate?: string; // Ngày cấp chứng chỉ
    licensePlace?: string; // Nơi cấp chứng chỉ
    school?: string; // Trường đào tạo
    graduationYear?: string; // Năm tốt nghiệp
    experienceYears?: number; // Số năm kinh nghiệm
    previousWorkplace?: string; // Nơi công tác trước đây
    supportSkills?: string;

    bio?: string; // Giới thiệu/Tiểu sử
}

export interface UserListResponse {
    users: User[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
}
