'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { userService, type ProfileResponse, type UpdateProfileRequest } from '@/services/user.service';
import { extractErrorMessage } from '@/lib/errorHandler';
import { Card, CardContent } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { BadgeCheck, BriefcaseBusiness, Camera, Lock, Loader2, Mail, Upload, User, X } from 'lucide-react';
import { useAuthStore } from '@/store/auth.store';

interface PatientProfile {
  patientId: string;
  fullName: string;
  gender: string;
  dateOfBirth: string;
  cccd: string;
  phoneNumber: string;
  email: string;
  address: string;
  bloodType: string;
  healthInsuranceNumber: string;
  height: string;
  weight: string;
  allergies: string;
  backgroundDisease: string;
  emergencyContactName: string;
  emergencyContactRelationship: string;
  emergencyContactPhone: string;
}

interface NurseProfile {
  nurseId: string;
  fullName: string;
  gender: string;
  cccd: string;
  phoneNumber: string;
  email: string;
  department: string;
  position: string;
  certificationNumber: string;
  certificationIssueDate: string;
  certificationPlace: string;
  certificationFileUrl: string;
  educationLevel: string;
  trainingPlace: string;
  graduationYear: string;
  degreeFileUrl: string;
  yearsOfExperience: string;
  supportSkills: string;
  previousWorkplaces: string;
}

interface DoctorProfile {
  doctorId: string;
  fullName: string;
  gender: string;
  cccd: string;
  phoneNumber: string;
  email: string;
  licenseNumber: string;
  certificationIssueDate: string;
  certificationPlace: string;
  certificationFileUrl: string;
  degree: string;
  specialization: string;
  trainingPlace: string;
  graduationYear: string;
  degreeFileUrl: string;
  experienceYears: string;
  previousWorkplace: string;
  bio: string;
  deptName: string;
  position: string;
  isHeadOfDept: boolean;
  roomName: string;
}

type ProfileRole =
  | 'PATIENT'
  | 'NURSE'
  | 'CLINICAL_DOCTOR'
  | 'SPECIALIST_DOCTOR'
  | 'RECEPTIONIST'
  | 'CASHIER';

interface AdministrativeStaffProfile {
  userId: string;
  username: string;
  fullName: string;
  gender: string;
  cccd: string;
  phoneNumber: string;
  email: string;
  roleLabel: string;
  statusLabel: string;
}

const hasAnyRole = (roles: string[] = [], roleNames: string[]) =>
  roleNames.some((roleName) => roles.includes(roleName));

const buildAdministrativeStaffProfile = (
  data: ProfileResponse,
  roleLabel: string
): AdministrativeStaffProfile => ({
  userId: data.id?.toString() || '',
  username: data.username || '',
  fullName: data.fullName || '',
  gender: data.gender || '',
  cccd: data.cccd || '',
  phoneNumber: data.phoneNumber || '',
  email: data.email || '',
  roleLabel,
  statusLabel: data.isActive ? 'Đang hoạt động' : 'Tạm khóa',
});

export default function ProfilePage() {
  const router = useRouter();
  const { user, setUser } = useAuthStore();
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');
  const [userRole, setUserRole] = useState<ProfileRole>('PATIENT');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [selectedAvatarFile, setSelectedAvatarFile] = useState<File | null>(null);
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState('');
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [doctorFileUploads, setDoctorFileUploads] = useState<{
    certificationFile: File | null;
    degreeFile: File | null;
  }>({
    certificationFile: null,
    degreeFile: null,
  });
  
  // Change email state
  const [showChangeEmail, setShowChangeEmail] = useState(false);
  const [newEmail, setNewEmail] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [isSendingOtp, setIsSendingOtp] = useState(false);
  const [isChangingEmail, setIsChangingEmail] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [emailError, setEmailError] = useState('');
  const [emailSuccess, setEmailSuccess] = useState('');
  
  // Form state - Patient
  const [formData, setFormData] = useState<PatientProfile>({
    patientId: '',
    fullName: '',
    gender: '',
    dateOfBirth: '',
    cccd: '',
    phoneNumber: '',
    email: '',
    address: '',
    bloodType: '',
    healthInsuranceNumber: '',
    height: '',
    weight: '',
    allergies: '',
    backgroundDisease: '',
    emergencyContactName: '',
    emergencyContactRelationship: '',
    emergencyContactPhone: '',
  });

  // Form state - Nurse
  const [nurseData, setNurseData] = useState<NurseProfile>({
    nurseId: '',
    fullName: '',
    gender: '',
    cccd: '',
    phoneNumber: '',
    email: '',
    department: '',
    position: '',
    certificationNumber: '',
    certificationIssueDate: '',
    certificationPlace: '',
    certificationFileUrl: '',
    educationLevel: '',
    trainingPlace: '',
    graduationYear: '',
    degreeFileUrl: '',
    yearsOfExperience: '',
    supportSkills: '',
    previousWorkplaces: '',
  });

  // Form state - Doctor
  const [doctorData, setDoctorData] = useState<DoctorProfile>({
    doctorId: '',
    fullName: '',
    gender: '',
    cccd: '',
    phoneNumber: '',
    email: '',
    licenseNumber: '',
    certificationIssueDate: '',
    certificationPlace: '',
    certificationFileUrl: '',
    degree: '',
    specialization: '',
    trainingPlace: '',
    graduationYear: '',
    degreeFileUrl: '',
    experienceYears: '',
    previousWorkplace: '',
    bio: '',
    deptName: '',
    position: '',
    isHeadOfDept: false,
    roomName: '',
  });

  const [staffData, setStaffData] = useState<AdministrativeStaffProfile>({
    userId: '',
    username: '',
    fullName: '',
    gender: '',
    cccd: '',
    phoneNumber: '',
    email: '',
    roleLabel: '',
    statusLabel: '',
  });

  // Fetch profile from API
  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setIsLoading(true);
        const data = await userService.getProfile();
        setAvatarUrl(data.avatarUrl || '');
        
        // Determine user role
        if (data.roles.includes('ROLE_NURSE')) {
          setUserRole('NURSE');
          setNurseData({
            nurseId: data.profile.nurseId || '',
            fullName: data.fullName || '',
            gender: data.gender || '',
            cccd: data.cccd || '',
            phoneNumber: data.phoneNumber || '',
            email: data.email || '',
            department: typeof data.profile.department === 'string' ? data.profile.department : '',
            position: data.profile.position || '',
            certificationNumber: data.profile.certificationNumber || '',
            certificationIssueDate: data.profile.certificationIssueDate || '',
            certificationPlace: data.profile.certificationPlace || '',
            certificationFileUrl: data.profile.certificationFileUrl || '',
            educationLevel: data.profile.educationLevel || '',
            trainingPlace: data.profile.trainingPlace || '',
            graduationYear: data.profile.graduationYear?.toString() || '',
            degreeFileUrl: data.profile.degreeFileUrl || '',
            yearsOfExperience: data.profile.yearsOfExperience?.toString() || '',
            supportSkills: data.profile.supportSkills || '',
            previousWorkplaces: data.profile.previousWorkplaces || '',
          });
        } else if (
          data.roles.includes('ROLE_CLINICAL_DOCTOR') ||
          data.roles.includes('ROLE_CLINICALDOCTOR') ||
          data.roles.includes('ROLE_DOCTOR')
        ) {
          setUserRole('CLINICAL_DOCTOR');
          setDoctorData({
            doctorId: data.profile.doctorId || '',
            fullName: data.fullName || '',
            gender: data.gender || '',
            cccd: data.cccd || '',
            phoneNumber: data.phoneNumber || '',
            email: data.email || '',
            licenseNumber: data.profile.licenseNumber || '',
            certificationIssueDate: data.profile.certificationIssueDate || '',
            certificationPlace: data.profile.certificationPlace || '',
            certificationFileUrl: data.profile.certificationFileUrl || '',
            degree: data.profile.degree || '',
            specialization: data.profile.specialization || '',
            trainingPlace: data.profile.trainingPlace || '',
            graduationYear: data.profile.graduationYear?.toString() || '',
            degreeFileUrl: data.profile.degreeFileUrl || '',
            experienceYears: data.profile.experienceYears?.toString() || '',
            previousWorkplace: data.profile.previousWorkplace || '',
            bio: data.profile.bio || '',
            deptName: typeof data.profile.department === 'object' ? data.profile.department?.deptName || '' : '',
            position: data.profile.position || '',
            isHeadOfDept: data.profile.isHeadOfDept || false,
            roomName: data.profile.room?.roomName || '',
          });
        } else if (
          data.roles.includes('ROLE_SPECIALIST_DOCTOR') ||
          data.roles.includes('ROLE_SPECIALISTDOCTOR')
        ) {
          setUserRole('SPECIALIST_DOCTOR');
          setDoctorData({
            doctorId: data.profile.doctorId || '',
            fullName: data.fullName || '',
            gender: data.gender || '',
            cccd: data.cccd || '',
            phoneNumber: data.phoneNumber || '',
            email: data.email || '',
            licenseNumber: data.profile.licenseNumber || '',
            certificationIssueDate: data.profile.certificationIssueDate || '',
            certificationPlace: data.profile.certificationPlace || '',
            certificationFileUrl: data.profile.certificationFileUrl || '',
            degree: data.profile.degree || '',
            specialization: data.profile.specialization || '',
            trainingPlace: data.profile.trainingPlace || '',
            graduationYear: data.profile.graduationYear?.toString() || '',
            degreeFileUrl: data.profile.degreeFileUrl || '',
            experienceYears: data.profile.experienceYears?.toString() || '',
            previousWorkplace: data.profile.previousWorkplace || '',
            bio: data.profile.bio || '',
            deptName: typeof data.profile.department === 'object' ? data.profile.department?.deptName || '' : '',
            position: data.profile.position || '',
            isHeadOfDept: data.profile.isHeadOfDept || false,
            roomName: data.profile.room?.roomName || '',
          });
        } else if (hasAnyRole(data.roles, ['ROLE_RECEPTIONIST', 'RECEPTIONIST'])) {
          setUserRole('RECEPTIONIST');
          setStaffData(buildAdministrativeStaffProfile(data, 'Lễ tân'));
        } else if (hasAnyRole(data.roles, ['ROLE_CASHIER', 'CASHIER'])) {
          setUserRole('CASHIER');
          setStaffData(buildAdministrativeStaffProfile(data, 'Thu ngân'));
        } else {
          // Default to PATIENT
          setUserRole('PATIENT');
          setFormData({
            patientId: data.profile.patientId || '',
            fullName: data.fullName || '',
            gender: data.gender || '',
            dateOfBirth: data.profile.dateOfBirth || '',
            cccd: data.cccd || '',
            phoneNumber: data.phoneNumber || '',
            email: data.email || '',
            address: data.profile.address || '',
            bloodType: data.profile.bloodType || '',
            healthInsuranceNumber: data.profile.healthInsuranceNumber || '',
            height: data.profile.height?.toString() || '',
            weight: data.profile.weight?.toString() || '',
            allergies: data.profile.allergies || '',
            backgroundDisease: data.profile.backgroundDisease || '',
            emergencyContactName: data.profile.emergencyContactName || '',
            emergencyContactRelationship: data.profile.emergencyContactRelationship || '',
            emergencyContactPhone: data.profile.emergencyContactPhone || '',
          });
        }
      } catch (err: unknown) {
        // Nếu 401 thì axios interceptor đã redirect rồi
        setError(extractErrorMessage(err));
      } finally {
        setIsLoading(false);
      }
    };

    fetchProfile();
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleDoctorInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setDoctorData(prev => ({ ...prev, [name]: value }));
  };

  const handleStaffInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setStaffData(prev => ({ ...prev, [name]: value }));
  };

  const handleDoctorFileChange = (
    field: 'certificationFile' | 'degreeFile',
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = e.target.files?.[0] ?? null;
    setDoctorFileUploads(prev => ({ ...prev, [field]: file }));
  };

  // Handle send OTP for email change
  const handleSendOtp = async () => {
    if (!newEmail) {
      setEmailError('Vui lòng nhập email mới');
      return;
    }

    setEmailError('');
    setIsSendingOtp(true);

    try {
      await userService.sendOtp({ email: newEmail, type: 'CHANGE_EMAIL' });
      setOtpSent(true);
      setEmailSuccess('Đã gửi mã OTP tới email mới. Vui lòng kiểm tra hộp thư!');
      setTimeout(() => setEmailSuccess(''), 5000);
    } catch (err: unknown) {
      setEmailError(extractErrorMessage(err));
    } finally {
      setIsSendingOtp(false);
    }
  };

  // Handle confirm email change
  const handleChangeEmail = async () => {
    if (!newEmail || !otpCode) {
      setEmailError('Vui lòng nhập đầy đủ email và mã OTP');
      return;
    }

    setEmailError('');
    setIsChangingEmail(true);

    try {
      await userService.changeEmail({ newEmail, otpCode });
      setFormData(prev => ({ ...prev, email: newEmail }));
      setShowChangeEmail(false);
      setNewEmail('');
      setOtpCode('');
      setOtpSent(false);
      setSuccess('Email đã được thay đổi thành công!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: unknown) {
      setEmailError(extractErrorMessage(err));
    } finally {
      setIsChangingEmail(false);
    }
  };

  // Cancel email change
  const handleCancelEmailChange = () => {
    setShowChangeEmail(false);
    setNewEmail('');
    setOtpCode('');
    setOtpSent(false);
    setEmailError('');
    setEmailSuccess('');
  };

  const currentFullName =
    userRole === 'PATIENT'
      ? formData.fullName
      : userRole === 'NURSE'
        ? nurseData.fullName
        : userRole === 'RECEPTIONIST' || userRole === 'CASHIER'
          ? staffData.fullName
          : doctorData.fullName;
  const avatarInitial = currentFullName ? currentFullName.charAt(0).toUpperCase() : 'U';
  const displayedAvatarUrl = avatarPreviewUrl || avatarUrl;

  const handleAvatarFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    setSelectedAvatarFile(file);
    setAvatarPreviewUrl(URL.createObjectURL(file));
  };

  const handleCloseAvatarModal = () => {
    setIsAvatarModalOpen(false);
    setSelectedAvatarFile(null);
    setAvatarPreviewUrl('');
  };

  const handleUploadAvatar = async () => {
    if (!selectedAvatarFile) {
      return;
    }

    setIsUploadingAvatar(true);
    setError('');
    try {
      const response = await userService.uploadAvatar(selectedAvatarFile);
      const nextAvatarUrl = response.avatarUrl || '';
      setAvatarUrl(nextAvatarUrl);
      if (user) {
        setUser({ ...user, avatar: nextAvatarUrl });
      }
      setSuccess('Ảnh đại diện đã được cập nhật thành công!');
      handleCloseAvatarModal();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: unknown) {
      setError(extractErrorMessage(err));
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setIsSaving(true);
    
    try {
      let requestData: UpdateProfileRequest;

      if (userRole === 'CLINICAL_DOCTOR' || userRole === 'SPECIALIST_DOCTOR') {
        let certificationFileUrl = doctorData.certificationFileUrl;
        let degreeFileUrl = doctorData.degreeFileUrl;

        if (doctorFileUploads.certificationFile) {
          const upload = await userService.uploadProfileFile(doctorFileUploads.certificationFile);
          certificationFileUrl = upload.fileUrl;
        }

        if (doctorFileUploads.degreeFile) {
          const upload = await userService.uploadProfileFile(doctorFileUploads.degreeFile);
          degreeFileUrl = upload.fileUrl;
        }

        requestData = {
          fullName: doctorData.fullName || undefined,
          cccd: doctorData.cccd || undefined,
          phoneNumber: doctorData.phoneNumber || undefined,
          licenseNumber: doctorData.licenseNumber,
          certificationIssueDate: doctorData.certificationIssueDate || undefined,
          certificationPlace: doctorData.certificationPlace,
          certificationFileUrl: certificationFileUrl || undefined,
          degree: doctorData.degree,
          specialization: doctorData.specialization,
          trainingPlace: doctorData.trainingPlace,
          graduationYear: doctorData.graduationYear,
          degreeFileUrl: degreeFileUrl || undefined,
          experienceYears: doctorData.experienceYears ? parseInt(doctorData.experienceYears, 10) : undefined,
          previousWorkplace: doctorData.previousWorkplace,
          bio: doctorData.bio,
        };

        setDoctorData(prev => ({
          ...prev,
          certificationFileUrl,
          degreeFileUrl,
        }));
        setDoctorFileUploads({ certificationFile: null, degreeFile: null });
      } else if (userRole === 'RECEPTIONIST' || userRole === 'CASHIER') {
        requestData = {
          fullName: staffData.fullName || undefined,
          cccd: staffData.cccd || undefined,
          gender: staffData.gender as 'MALE' | 'FEMALE' | 'OTHER' || undefined,
          phoneNumber: staffData.phoneNumber || undefined,
        };
      } else {
        requestData = {
          fullName: formData.fullName || undefined,
          cccd: formData.cccd || undefined,
          gender: formData.gender as 'MALE' | 'FEMALE' | 'OTHER' || undefined,
          phoneNumber: formData.phoneNumber || undefined,
          dateOfBirth: formData.dateOfBirth || undefined,
          bloodType: formData.bloodType || undefined,
          healthInsuranceNumber: formData.healthInsuranceNumber || undefined,
          allergies: formData.allergies || undefined,
          address: formData.address || undefined,
          height: formData.height ? parseFloat(formData.height) : undefined,
          weight: formData.weight ? parseFloat(formData.weight) : undefined,
          backgroundDisease: formData.backgroundDisease || undefined,
          emergencyContactName: formData.emergencyContactName || undefined,
          emergencyContactPhone: formData.emergencyContactPhone || undefined,
          emergencyContactRelationship: formData.emergencyContactRelationship || undefined,
        };
      }

      Object.keys(requestData).forEach(key => {
        if (requestData[key as keyof UpdateProfileRequest] === undefined) {
          delete requestData[key as keyof UpdateProfileRequest];
        }
      });

      await userService.updateProfile(requestData);
      setSuccess('Thông tin đã được cập nhật thành công!');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: unknown) {
      setError(extractErrorMessage(err));
    } finally {
      setIsSaving(false);
    }
  };

  // Show loading while fetching profile
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-blue-600 border-r-transparent"></div>
          <p className="mt-4 text-gray-600">Đang tải...</p>
        </div>
      </div>
    );
  }

  return (
    <>
        {error && (
          <div className="mb-4 rounded-lg bg-red-50 border border-red-200 p-4 text-sm text-red-700">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-4 rounded-lg bg-blue-50 border border-blue-200 p-4 text-sm text-blue-700">
            {success}
          </div>
        )}

        {/* Two Column Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Left Sidebar - 25% */}
          <div className="lg:col-span-1">
            <Card>
              <CardContent className="pt-6">
                {/* Avatar */}
                <div className="flex flex-col items-center space-y-4">
                  <button
                    type="button"
                    onClick={() => setIsAvatarModalOpen(true)}
                    className="relative h-32 w-32 overflow-hidden rounded-full bg-gradient-to-br from-blue-500 to-blue-800 text-white shadow-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
                    aria-label="Chon anh dai dien"
                  >
                    {displayedAvatarUrl ? (
                      <img src={displayedAvatarUrl} alt={currentFullName} className="h-full w-full object-cover" />
                    ) : (
                      <span className="flex h-full w-full items-center justify-center text-4xl font-bold">
                        {avatarInitial}
                      </span>
                    )}
                    <span className="absolute inset-x-0 bottom-0 flex items-center justify-center bg-black/55 py-2">
                      <Camera className="h-4 w-4" />
                    </span>
                  </button>

                  {/* User Info */}
                  <div className="text-center">
                    <h2 className="text-xl font-bold text-gray-900">
                      {userRole === 'PATIENT' && formData.fullName}
                      {userRole === 'NURSE' && nurseData.fullName}
                      {(userRole === 'RECEPTIONIST' || userRole === 'CASHIER') && staffData.fullName}
                      {(userRole === 'CLINICAL_DOCTOR' || userRole === 'SPECIALIST_DOCTOR') && doctorData.fullName}
                    </h2>
                    <p className="text-sm text-gray-500 mt-1">
                      {userRole === 'PATIENT' && 'Bệnh nhân'}
                      {userRole === 'NURSE' && 'Y tá'}
                      {(userRole === 'CLINICAL_DOCTOR' || userRole === 'SPECIALIST_DOCTOR') && (userRole === 'CLINICAL_DOCTOR' ? 'Bác sĩ lâm sàng' : 'Bác sĩ chuyên khoa')}
                      {(userRole === 'RECEPTIONIST' || userRole === 'CASHIER') && staffData.roleLabel}
                    </p>
                  </div>

                  {/* Action Buttons */}
                  <div className="w-full space-y-2 pt-4">
                    <Button 
                      className="w-full flex items-center justify-start bg-blue-600 hover:bg-blue-700 text-white"
                      onClick={() => router.push('/dashboard/change-password')}
                    >
                      <Lock className="h-4 w-4 mr-2 flex-shrink-0" />
                      <span className="truncate">Đổi mật khẩu</span>
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Right Content - 75% */}
          <div className="lg:col-span-3">
            {/* ADMINISTRATIVE STAFF FORM */}
            {(userRole === 'RECEPTIONIST' || userRole === 'CASHIER') && (
              <form onSubmit={handleSubmit}>
                <Card>
                  <CardContent className="pt-6 space-y-8">
                    <div>
                      <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                        <User className="h-5 w-5 text-blue-600" />
                        <h3 className="text-lg font-semibold text-gray-900">1. Thông tin cá nhân</h3>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label htmlFor="staffFullName">Họ và tên</Label>
                          <Input
                            id="staffFullName"
                            name="fullName"
                            value={staffData.fullName}
                            onChange={handleStaffInputChange}
                            placeholder="Nhập họ và tên"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="staffGender">Giới tính</Label>
                          <Input
                            id="staffGender"
                            value={staffData.gender}
                            disabled
                            className="bg-gray-100 text-gray-900 font-medium cursor-not-allowed"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="staffCccd">CCCD/CMND</Label>
                          <Input
                            id="staffCccd"
                            name="cccd"
                            value={staffData.cccd}
                            onChange={handleStaffInputChange}
                            placeholder="Nhập số CCCD/CMND"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="staffPhone">Số điện thoại</Label>
                          <Input
                            id="staffPhone"
                            name="phoneNumber"
                            type="tel"
                            value={staffData.phoneNumber}
                            onChange={handleStaffInputChange}
                            placeholder="Nhập số điện thoại"
                          />
                        </div>

                        <div className="space-y-2 md:col-span-2">
                          <Label htmlFor="staffEmail">Email</Label>
                          <Input
                            id="staffEmail"
                            value={staffData.email}
                            disabled
                            className="bg-gray-100"
                          />
                        </div>
                      </div>
                    </div>

                    <div>
                      <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                        <BriefcaseBusiness className="h-5 w-5 text-blue-600" />
                        <h3 className="text-lg font-semibold text-gray-900">2. Thông tin công việc</h3>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label htmlFor="staffUserId">Mã tài khoản</Label>
                          <Input
                            id="staffUserId"
                            value={staffData.userId}
                            disabled
                            className="bg-gray-100 text-gray-900 font-medium cursor-not-allowed"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="staffUsername">Tên đăng nhập</Label>
                          <Input
                            id="staffUsername"
                            value={staffData.username}
                            disabled
                            className="bg-gray-100"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="staffRole">Vai trò</Label>
                          <Input
                            id="staffRole"
                            value={staffData.roleLabel}
                            disabled
                            className="bg-gray-100 text-gray-900 font-medium"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="staffStatus">Trạng thái</Label>
                          <div className="flex h-10 items-center gap-2 rounded-md border border-input bg-gray-100 px-3 text-sm text-gray-900">
                            <BadgeCheck className="h-4 w-4 text-blue-600" />
                            <span className="font-medium">{staffData.statusLabel}</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="flex justify-end pt-4 border-t">
                      <Button type="submit" size="lg" className="min-w-[200px]" disabled={isSaving}>
                        {isSaving ? (
                          <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            Đang lưu...
                          </>
                        ) : (
                          'Lưu thông tin'
                        )}
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </form>
            )}

            {/* PATIENT FORM */}
            {userRole === 'PATIENT' && (
              <form onSubmit={handleSubmit}>
                <Card>
                  <CardContent className="pt-6 space-y-8">{/* Section 1: Personal Information */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <User className="h-5 w-5 text-blue-600" />
                      <h3 className="text-lg font-semibold text-gray-900">1. Thông tin cá nhân</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Patient ID - Read Only */}
                      <div className="space-y-2">
                        <Label htmlFor="patientId">Mã bệnh nhân</Label>
                        <Input
                          id="patientId"
                          name="patientId"
                          value={formData.patientId}
                          disabled
                          className="bg-gray-100 text-gray-900 font-medium cursor-not-allowed"
                        />
                      </div>

                      {/* Full Name */}
                      <div className="space-y-2">
                        <Label htmlFor="fullName">Họ và tên</Label>
                        <Input
                          id="fullName"
                          name="fullName"
                          value={formData.fullName}
                          onChange={handleInputChange}
                        />
                      </div>

                      {/* Gender - Read Only */}
                      <div className="space-y-2">
                        <Label htmlFor="gender">Giới tính</Label>
                        <Input
                          id="gender"
                          name="gender"
                          value={formData.gender}
                          disabled
                          className="bg-gray-100 text-gray-900 font-medium cursor-not-allowed"
                        />
                      </div>

                      {/* Date of Birth */}
                      <div className="space-y-2">
                        <Label htmlFor="dateOfBirth">Ngày sinh</Label>
                        <Input
                          id="dateOfBirth"
                          name="dateOfBirth"
                          type="date"
                          value={formData.dateOfBirth}
                          onChange={handleInputChange}
                        />
                      </div>

                      {/* ID Card/CCCD */}
                      <div className="space-y-2">
                        <Label htmlFor="cccd">CCCD/CMND</Label>
                        <Input
                          id="cccd"
                          name="cccd"
                          value={formData.cccd}
                          onChange={handleInputChange}
                          placeholder="Nhập số CCCD/CMND"
                        />
                      </div>

                      {/* Phone Number */}
                      <div className="space-y-2">
                        <Label htmlFor="phoneNumber">Số điện thoại</Label>
                        <Input
                          id="phoneNumber"
                          name="phoneNumber"
                          type="tel"
                          value={formData.phoneNumber}
                          onChange={handleInputChange}
                          placeholder="Nhập số điện thoại"
                        />
                      </div>

                      {/* Email - Read Only with Change Button */}
                      <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="email">Email</Label>
                        <div className="flex gap-2">
                          <Input
                            id="email"
                            name="email"
                            type="email"
                            value={formData.email}
                            disabled
                            className="bg-gray-100 flex-1"
                          />
                          <Button
                            type="button"
                            onClick={() => setShowChangeEmail(true)}
                            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white whitespace-nowrap"
                          >
                            <Mail className="h-4 w-4" />
                            Đổi email
                          </Button>
                        </div>
                      </div>

                      {/* Change Email Modal */}
                      {showChangeEmail && (
                        <div className="md:col-span-2 p-4 border border-blue-200 rounded-lg bg-blue-50">
                          <div className="flex items-center gap-2 mb-4">
                            <Mail className="h-5 w-5 text-blue-600" />
                            <h4 className="font-semibold text-gray-900">Thay đổi địa chỉ email</h4>
                          </div>

                          {emailError && (
                            <div className="mb-3 rounded-lg bg-red-50 border border-red-200 p-3 text-sm text-red-700">
                              {emailError}
                            </div>
                          )}

                          {emailSuccess && (
                            <div className="mb-3 rounded-lg bg-blue-50 border border-blue-200 p-3 text-sm text-blue-700">
                              {emailSuccess}
                            </div>
                          )}

                          <div className="space-y-4">
                            {/* New Email Input */}
                            <div className="space-y-2">
                              <Label htmlFor="newEmail">Email mới</Label>
                              <div className="flex gap-2">
                                <Input
                                  id="newEmail"
                                  type="email"
                                  value={newEmail}
                                  onChange={(e) => setNewEmail(e.target.value)}
                                  placeholder="Nhập email mới"
                                  className="flex-1"
                                  disabled={otpSent}
                                />
                                <Button
                                  type="button"
                                  onClick={handleSendOtp}
                                  disabled={isSendingOtp || !newEmail || otpSent}
                                  className="bg-blue-600 hover:bg-blue-700 text-white whitespace-nowrap"
                                >
                                  {isSendingOtp ? 'Đang gửi...' : otpSent ? 'Đã gửi OTP' : 'Lấy OTP'}
                                </Button>
                              </div>
                            </div>

                            {/* OTP Input - Show after OTP sent */}
                            {otpSent && (
                              <div className="space-y-2">
                                <Label htmlFor="otpCode">Mã OTP</Label>
                                <Input
                                  id="otpCode"
                                  type="text"
                                  value={otpCode}
                                  onChange={(e) => setOtpCode(e.target.value)}
                                  placeholder="Nhập mã OTP từ email"
                                  maxLength={6}
                                />
                              </div>
                            )}

                            {/* Action Buttons */}
                            <div className="flex gap-2 justify-end pt-2">
                              <Button
                                type="button"
                                variant="outline"
                                onClick={handleCancelEmailChange}
                                className="border-gray-300 hover:bg-gray-100"
                              >
                                Hủy
                              </Button>
                              {otpSent && (
                                <Button
                                  type="button"
                                  onClick={handleChangeEmail}
                                  disabled={isChangingEmail || !otpCode}
                                  className="bg-blue-600 hover:bg-blue-700 text-white"
                                >
                                  {isChangingEmail ? 'Đang xử lý...' : 'Lưu thay đổi'}
                                </Button>
                              )}
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Address - Full Width */}
                      <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="address">Địa chỉ</Label>
                        <Input
                          id="address"
                          name="address"
                          value={formData.address}
                          onChange={handleInputChange}
                          placeholder="Nhập địa chỉ đầy đủ"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 2: Basic Medical Information */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">2. Thông tin y tế cơ bản</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Blood Type */}
                      <div className="space-y-2">
                        <Label htmlFor="bloodType">Nhóm máu</Label>
                        <Input
                          id="bloodType"
                          name="bloodType"
                          value={formData.bloodType}
                          onChange={handleInputChange}
                          placeholder="Ví dụ: A+, B-, O+, AB+"
                        />
                      </div>

                      {/* Health Insurance Number */}
                      <div className="space-y-2">
                        <Label htmlFor="healthInsuranceNumber">Số BHYT</Label>
                        <Input
                          id="healthInsuranceNumber"
                          name="healthInsuranceNumber"
                          value={formData.healthInsuranceNumber}
                          onChange={handleInputChange}
                          placeholder="Nhập số bảo hiểm y tế"
                        />
                      </div>

                      {/* Height */}
                      <div className="space-y-2">
                        <Label htmlFor="height">Chiều cao (cm)</Label>
                        <Input
                          id="height"
                          name="height"
                          type="number"
                          value={formData.height}
                          onChange={handleInputChange}
                          placeholder="Nhập chiều cao"
                        />
                      </div>

                      {/* Weight */}
                      <div className="space-y-2">
                        <Label htmlFor="weight">Cân nặng (kg)</Label>
                        <Input
                          id="weight"
                          name="weight"
                          type="number"
                          step="0.1"
                          value={formData.weight}
                          onChange={handleInputChange}
                          placeholder="Nhập cân nặng"
                        />
                      </div>

                      {/* Drug Allergies - Full Width */}
                      <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="allergies">Dị ứng thuốc</Label>
                        <Input
                          id="allergies"
                          name="allergies"
                          value={formData.allergies}
                          onChange={handleInputChange}
                          placeholder="Nhập các loại thuốc gây dị ứng (nếu có)"
                        />
                      </div>

                      {/* Background Diseases - Full Width */}
                      <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="backgroundDisease">Bệnh nền</Label>
                        <Input
                          id="backgroundDisease"
                          name="backgroundDisease"
                          value={formData.backgroundDisease}
                          onChange={handleInputChange}
                          placeholder="Nhập các bệnh nền (nếu có)"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 3: Emergency Contact */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">3. Người liên hệ khẩn cấp</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Emergency Contact Name */}
                      <div className="space-y-2">
                        <Label htmlFor="emergencyContactName">Họ và tên</Label>
                        <Input
                          id="emergencyContactName"
                          name="emergencyContactName"
                          value={formData.emergencyContactName}
                          onChange={handleInputChange}
                          placeholder="Nhập họ tên người liên hệ"
                        />
                      </div>

                      {/* Relationship */}
                      <div className="space-y-2">
                        <Label htmlFor="emergencyContactRelationship">Mối quan hệ</Label>
                        <Input
                          id="emergencyContactRelationship"
                          name="emergencyContactRelationship"
                          value={formData.emergencyContactRelationship}
                          onChange={handleInputChange}
                          placeholder="Ví dụ: Vợ/Chồng, Con, Anh/Chị/Em"
                        />
                      </div>

                      {/* Emergency Contact Phone */}
                      <div className="space-y-2">
                        <Label htmlFor="emergencyContactPhone">Số điện thoại</Label>
                        <Input
                          id="emergencyContactPhone"
                          name="emergencyContactPhone"
                          type="tel"
                          value={formData.emergencyContactPhone}
                          onChange={handleInputChange}
                          placeholder="Nhập số điện thoại liên hệ"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Footer Actions */}
                  <div className="flex justify-end pt-4 border-t">
                    <Button type="submit" size="lg" className="min-w-[200px]" disabled={isSaving}>
                      {isSaving ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          Đang lưu...
                        </>
                      ) : (
                        'Lưu thông tin'
                      )}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </form>
            )}

            {/* NURSE FORM */}
            {userRole === 'NURSE' && (
              <Card>
                <CardContent className="pt-6 space-y-8">
                  {/* Section 1: Personal Information */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <User className="h-5 w-5 text-blue-600" />
                      <h3 className="text-lg font-semibold text-gray-900">1. Thông tin cá nhân</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Nurse ID - Read Only */}
                      <div className="space-y-2">
                        <Label htmlFor="nurseId">Mã Y tá</Label>
                        <Input
                          id="nurseId"
                          value={nurseData.nurseId}
                          disabled
                          className="bg-gray-100 text-gray-900 font-medium cursor-not-allowed"
                        />
                      </div>

                      {/* Full Name */}
                      <div className="space-y-2">
                        <Label htmlFor="nurseFullName">Họ và tên</Label>
                        <Input
                          id="nurseFullName"
                          value={nurseData.fullName}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Gender */}
                      <div className="space-y-2">
                        <Label htmlFor="nurseGender">Giới tính</Label>
                        <Input
                          id="nurseGender"
                          value={nurseData.gender}
                          disabled
                          className="bg-gray-100 text-gray-900 font-medium cursor-not-allowed"
                        />
                      </div>

                      {/* CCCD */}
                      <div className="space-y-2">
                        <Label htmlFor="nurseCccd">CCCD/CMND</Label>
                        <Input
                          id="nurseCccd"
                          value={nurseData.cccd}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Phone Number */}
                      <div className="space-y-2">
                        <Label htmlFor="nursePhone">Số điện thoại</Label>
                        <Input
                          id="nursePhone"
                          value={nurseData.phoneNumber}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Email */}
                      <div className="space-y-2">
                        <Label htmlFor="nurseEmail">Email</Label>
                        <Input
                          id="nurseEmail"
                          value={nurseData.email}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 2: Professional Information */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">2. Thông tin nghề nghiệp</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Education Level */}
                      <div className="space-y-2">
                        <Label htmlFor="educationLevel">Trình độ học vấn</Label>
                        <Input
                          id="educationLevel"
                          value={nurseData.educationLevel}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Training Place */}
                      <div className="space-y-2">
                        <Label htmlFor="trainingPlace">Nơi đào tạo</Label>
                        <Input
                          id="trainingPlace"
                          value={nurseData.trainingPlace}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Graduation Year */}
                      <div className="space-y-2">
                        <Label htmlFor="graduationYear">Năm tốt nghiệp</Label>
                        <Input
                          id="graduationYear"
                          value={nurseData.graduationYear}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Degree File URL */}
                      <div className="space-y-2">
                        <Label htmlFor="degreeFileUrl">File bằng cấp</Label>
                        <Input
                          id="degreeFileUrl"
                          value={nurseData.degreeFileUrl}
                          disabled
                          className="bg-gray-100"
                          placeholder="URL file bằng cấp"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 3: Certification */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">3. Chứng chỉ</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Certification Number */}
                      <div className="space-y-2">
                        <Label htmlFor="certificationNumber">Số chứng chỉ hành nghề</Label>
                        <Input
                          id="certificationNumber"
                          value={nurseData.certificationNumber}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Certification Issue Date */}
                      <div className="space-y-2">
                        <Label htmlFor="certificationIssueDate">Ngày cấp chứng chỉ</Label>
                        <Input
                          id="certificationIssueDate"
                          value={nurseData.certificationIssueDate}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Certification Place */}
                      <div className="space-y-2">
                        <Label htmlFor="certificationPlace">Nơi cấp chứng chỉ</Label>
                        <Input
                          id="certificationPlace"
                          value={nurseData.certificationPlace}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Certification File URL */}
                      <div className="space-y-2">
                        <Label htmlFor="certificationFileUrl">File chứng chỉ</Label>
                        <Input
                          id="certificationFileUrl"
                          value={nurseData.certificationFileUrl}
                          disabled
                          className="bg-gray-100"
                          placeholder="URL file chứng chỉ"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 4: Work Experience */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">4. Kinh nghiệm làm việc</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Years of Experience */}
                      <div className="space-y-2">
                        <Label htmlFor="yearsOfExperience">Số năm kinh nghiệm</Label>
                        <Input
                          id="yearsOfExperience"
                          value={nurseData.yearsOfExperience}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Support Skills - Full Width */}
                      <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="supportSkills">Kỹ năng hỗ trợ</Label>
                        <Input
                          id="supportSkills"
                          value={nurseData.supportSkills}
                          disabled
                          className="bg-gray-100"
                          placeholder="VD: VITALS, ASSIST"
                        />
                      </div>

                      {/* Previous Workplaces - Full Width */}
                      <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="previousWorkplaces">Nơi làm việc trước đây</Label>
                        <Input
                          id="previousWorkplaces"
                          value={nurseData.previousWorkplaces}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 5: Current Work Info at Clinic */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">5. Thông tin làm việc tại phòng khám</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Department */}
                      <div className="space-y-2">
                        <Label htmlFor="department">Khoa</Label>
                        <Input
                          id="department"
                          value={nurseData.department}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>

                      {/* Position */}
                      <div className="space-y-2">
                        <Label htmlFor="position">Chức vụ</Label>
                        <Input
                          id="position"
                          value={nurseData.position}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* DOCTOR FORM */}
            {(userRole === 'CLINICAL_DOCTOR' || userRole === 'SPECIALIST_DOCTOR') && (
              <form onSubmit={handleSubmit}>
              <Card>
                <CardContent className="pt-6 space-y-8">
                  {/* Section 1: Personal Information */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <User className="h-5 w-5 text-blue-600" />
                      <h3 className="text-lg font-semibold text-gray-900">1. Thông tin cá nhân</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Doctor ID - Read Only */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorId">Mã Bác sĩ</Label>
                        <Input
                          id="doctorId"
                          value={doctorData.doctorId}
                          readOnly
                          disabled
                          className="bg-gray-100 text-gray-900 font-medium cursor-not-allowed"
                        />
                      </div>

                      {/* Full Name */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorFullName">Họ và tên</Label>
                        <Input
                          id="doctorFullName"
                          name="fullName"
                          value={doctorData.fullName}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Gender */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorGender">Giới tính</Label>
                        <Input
                          id="doctorGender"
                          value={doctorData.gender}
                          disabled
                          className="bg-gray-100 text-gray-900 font-medium cursor-not-allowed"
                        />
                      </div>

                      {/* CCCD */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorCccd">CCCD/CMND</Label>
                        <Input
                          id="doctorCccd"
                          name="cccd"
                          value={doctorData.cccd}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Phone Number */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorPhone">Số điện thoại</Label>
                        <Input
                          id="doctorPhone"
                          name="phoneNumber"
                          type="tel"
                          value={doctorData.phoneNumber}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Email */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorEmail">Email</Label>
                        <Input
                          id="doctorEmail"
                          value={doctorData.email}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 2: Practice License (MOST IMPORTANT) */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-red-500">
                      <svg className="h-5 w-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">2. Thông tin hành nghề <span className="text-red-600">(QUAN TRỌNG)</span></h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* License Number */}
                      <div className="space-y-2">
                        <Label htmlFor="licenseNumber">Số giấy phép hành nghề</Label>
                        <Input
                          id="licenseNumber"
                          name="licenseNumber"
                          value={doctorData.licenseNumber}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Certification Issue Date */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorCertIssueDate">Ngày cấp</Label>
                        <Input
                          id="doctorCertIssueDate"
                          name="certificationIssueDate"
                          type="date"
                          value={doctorData.certificationIssueDate}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Certification Place */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorCertPlace">Nơi cấp</Label>
                        <Input
                          id="doctorCertPlace"
                          name="certificationPlace"
                          value={doctorData.certificationPlace}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Certification File URL */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorCertFileUrl">File giấy phép</Label>
                        <label
                          htmlFor="doctorCertFileUrl"
                          className="flex cursor-pointer items-center gap-3 rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-700 hover:border-blue-400 hover:bg-blue-50"
                        >
                          <Upload className="h-5 w-5 shrink-0 text-blue-600" />
                          <span className="min-w-0">
                            <span className="block font-medium text-slate-900">Tải lên giấy phép hành nghề</span>
                            <span className="block text-xs text-slate-500">
                              Hỗ trợ PDF, DOC, DOCX, JPG, JPEG, PNG
                            </span>
                          </span>
                          <input
                            id="doctorCertFileUrl"
                            type="file"
                            accept=".pdf,.png,.jpg,.jpeg,.doc,.docx"
                            onChange={(e) => handleDoctorFileChange('certificationFile', e)}
                            className="sr-only"
                          />
                        </label>
                        {doctorData.certificationFileUrl && (
                          <a
                            href={doctorData.certificationFileUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="block truncate text-sm font-medium text-blue-600 hover:underline"
                          >
                            Xem file hiện tại
                          </a>
                        )}
                        {doctorFileUploads.certificationFile && (
                          <p className="text-xs text-slate-500">{doctorFileUploads.certificationFile.name}</p>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Section 3: Professional Qualifications */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">3. Trình độ chuyên môn</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Degree */}
                      <div className="space-y-2">
                        <Label htmlFor="degree">Bằng cấp</Label>
                        <Input
                          id="degree"
                          name="degree"
                          value={doctorData.degree}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Specialization */}
                      <div className="space-y-2">
                        <Label htmlFor="specialization">Chuyên khoa</Label>
                        <Input
                          id="specialization"
                          name="specialization"
                          value={doctorData.specialization}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Training Place */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorTrainingPlace">Nơi đào tạo</Label>
                        <Input
                          id="doctorTrainingPlace"
                          name="trainingPlace"
                          value={doctorData.trainingPlace}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Graduation Year */}
                      <div className="space-y-2">
                        <Label htmlFor="doctorGraduationYear">Năm tốt nghiệp</Label>
                        <Input
                          id="doctorGraduationYear"
                          name="graduationYear"
                          type="number"
                          value={doctorData.graduationYear}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Degree File URL */}
                      <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="doctorDegreeFileUrl">File bằng cấp</Label>
                        <label
                          htmlFor="doctorDegreeFileUrl"
                          className="flex cursor-pointer items-center gap-3 rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-700 hover:border-blue-400 hover:bg-blue-50"
                        >
                          <Upload className="h-5 w-5 shrink-0 text-blue-600" />
                          <span className="min-w-0">
                            <span className="block font-medium text-slate-900">Tải lên bằng cấp/chứng chỉ</span>
                            <span className="block text-xs text-slate-500">
                              Hỗ trợ PDF, DOC, DOCX, JPG, JPEG, PNG
                            </span>
                          </span>
                          <input
                            id="doctorDegreeFileUrl"
                            type="file"
                            accept=".pdf,.png,.jpg,.jpeg,.doc,.docx"
                            onChange={(e) => handleDoctorFileChange('degreeFile', e)}
                            className="sr-only"
                          />
                        </label>
                        {doctorData.degreeFileUrl && (
                          <a
                            href={doctorData.degreeFileUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="block truncate text-sm font-medium text-blue-600 hover:underline"
                          >
                            Xem file hiện tại
                          </a>
                        )}
                        {doctorFileUploads.degreeFile && (
                          <p className="text-xs text-slate-500">{doctorFileUploads.degreeFile.name}</p>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Section 4: Work Experience */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">4. Kinh nghiệm công tác</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Experience Years */}
                      <div className="space-y-2">
                        <Label htmlFor="experienceYears">Số năm kinh nghiệm</Label>
                        <Input
                          id="experienceYears"
                          name="experienceYears"
                          type="number"
                          min="0"
                          value={doctorData.experienceYears}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Previous Workplace */}
                      <div className="space-y-2">
                        <Label htmlFor="previousWorkplace">Nơi làm việc trước</Label>
                        <Input
                          id="previousWorkplace"
                          name="previousWorkplace"
                          value={doctorData.previousWorkplace}
                          onChange={handleDoctorInputChange}
                        />
                      </div>

                      {/* Bio - Full Width */}
                      <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="bio">Tiểu sử / Giới thiệu</Label>
                        <Input
                          id="bio"
                          name="bio"
                          value={doctorData.bio}
                          onChange={handleDoctorInputChange}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 5: Current Work Info at Clinic */}
                  <div>
                    <div className="flex items-center gap-2 mb-4 pb-2 border-b-2 border-blue-500">
                      <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                      </svg>
                      <h3 className="text-lg font-semibold text-gray-900">5. Thông tin làm việc tại phòng khám</h3>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Department */}
                      <div className="space-y-2">
                        <Label htmlFor="deptName">Khoa</Label>
                        <Input
                          id="deptName"
                          value={doctorData.deptName}
                          readOnly
                          disabled
                          className="bg-gray-100 cursor-not-allowed"
                        />
                      </div>

                      {/* Room Name */}
                      <div className="space-y-2">
                        <Label htmlFor="roomName">Phòng khám đang trực</Label>
                        <Input
                          id="roomName"
                          value={doctorData.roomName || 'Chưa có lịch trực hôm nay'}
                          disabled
                          className="bg-gray-100 text-gray-900 font-medium"
                        />
                      </div>

                      {/* Head of Department */}
                      <div className="space-y-2">
                        <Label htmlFor="isHeadOfDept">Trưởng khoa</Label>
                        <Input
                          id="isHeadOfDept"
                          value={doctorData.isHeadOfDept ? 'Có' : 'Không'}
                          disabled
                          className="bg-gray-100"
                        />
                      </div>
                    </div>
                  </div>
                  <div className="flex justify-end pt-4 border-t">
                    <Button type="submit" size="lg" className="min-w-[200px]" disabled={isSaving}>
                      {isSaving ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          Đang lưu...
                        </>
                      ) : (
                        'Lưu thông tin'
                      )}
                    </Button>
                  </div>
                </CardContent>
              </Card>
              </form>
            )}
          </div>
        </div>

        {isAvatarModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
            <div className="w-full max-w-md rounded-lg bg-white shadow-xl">
              <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
                <h3 className="text-base font-semibold text-slate-900">Cập nhật ảnh đại diện</h3>
                <button
                  type="button"
                  onClick={handleCloseAvatarModal}
                  className="inline-flex h-8 w-8 items-center justify-center rounded-full text-slate-500 hover:bg-slate-100"
                  aria-label="Dong"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <div className="space-y-4 px-5 py-5">
                <div className="mx-auto h-32 w-32 overflow-hidden rounded-full bg-gradient-to-br from-blue-500 to-blue-800 text-white">
                  {displayedAvatarUrl ? (
                    <img src={displayedAvatarUrl} alt={currentFullName} className="h-full w-full object-cover" />
                  ) : (
                    <span className="flex h-full w-full items-center justify-center text-4xl font-bold">
                      {avatarInitial}
                    </span>
                  )}
                </div>

                <label className="flex cursor-pointer flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 px-4 py-5 text-sm text-slate-600 hover:border-blue-400 hover:bg-blue-50">
                  <Upload className="mb-2 h-5 w-5 text-blue-600" />
                  <span>Chọn ảnh từ máy</span>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleAvatarFileChange}
                    className="sr-only"
                  />
                </label>
              </div>

              <div className="flex justify-end gap-3 border-t border-slate-200 px-5 py-4">
                <Button type="button" variant="outline" onClick={handleCloseAvatarModal}>
                  Hủy
                </Button>
                <Button
                  type="button"
                  onClick={handleUploadAvatar}
                  disabled={!selectedAvatarFile || isUploadingAvatar}
                  className="bg-blue-600 text-white hover:bg-blue-700"
                >
                  {isUploadingAvatar ? (
                    <span className="inline-flex items-center gap-2">
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Đang lưu
                    </span>
                  ) : (
                    'Lưu ảnh'
                  )}
                </Button>
              </div>
            </div>
          </div>
        )}
      </>
  );
}
