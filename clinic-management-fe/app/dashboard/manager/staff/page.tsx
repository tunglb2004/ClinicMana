'use client';

import { useEffect, useMemo, useState } from 'react';
import { User } from '@/types/user.types';
import { userService, type ProfileResponse } from '@/services/user.service';

type ModalMode = 'create' | 'edit';

type StaffFormState = {
  fullName: string;
  cccd: string;
  dateOfBirth: string;
  phoneNumber: string;
  email: string;
  role: string;
  username: string;
  address: string;
};

type StaffInfoModalProps = {
  isOpen: boolean;
  mode: ModalMode;
  initialData: StaffFormState;
  submitting: boolean;
  onClose: () => void;
  onSubmit: (form: StaffFormState) => Promise<void>;
};

const ROLE_OPTIONS = [
  { value: 'ROLE_DOCTOR', label: 'Bác sĩ' },
  { value: 'ROLE_SPECIALISTDOCTOR', label: 'Bác sĩ chuyên khoa' },
  { value: 'ROLE_NURSE', label: 'Y tá' },
  { value: 'ROLE_RECEPTIONIST', label: 'Lễ tân' },
  { value: 'ROLE_CASHIER', label: 'Thu ngân' },
  { value: 'ROLE_CLINIC_MANAGER', label: 'Quản trị phòng khám' },
] as const;

const ROLE_LABELS: Record<string, string> = {
  ROLE_DOCTOR: 'BÁC SĨ',
  ROLE_SPECIALISTDOCTOR: 'BS CHUYÊN KHOA',
  ROLE_NURSE: 'Y TÁ',
  ROLE_RECEPTIONIST: 'LỄ TÂN',
  ROLE_CASHIER: 'THU NGÂN',
  ROLE_CLINIC_MANAGER: 'QUẢN TRỊ',
};

const ROLE_BADGE_STYLE: Record<string, string> = {
  ROLE_DOCTOR: 'bg-white text-slate-700',
  ROLE_SPECIALISTDOCTOR: 'bg-blue-600 text-white',
  ROLE_NURSE: 'bg-white text-slate-700',
  ROLE_RECEPTIONIST: 'bg-white text-slate-700',
  ROLE_CASHIER: 'bg-white text-slate-700',
  ROLE_CLINIC_MANAGER: 'bg-blue-600 text-white',
};

const SYSTEM_ROLES = ['ROLE_SYSTEM_ADMIN', 'ROLE_ADMIN', 'ROLE_PATIENT'];
const DOCTOR_ROLES = [
  'ROLE_DOCTOR',
  'ROLE_CLINICALDOCTOR',
  'ROLE_CLINICAL_DOCTOR',
  'ROLE_SPECIALISTDOCTOR',
  'ROLE_SPECIALIST_DOCTOR',
];

const EMPTY_FORM: StaffFormState = {
  fullName: '',
  cccd: '',
  dateOfBirth: '',
  phoneNumber: '',
  email: '',
  role: 'ROLE_DOCTOR',
  username: '',
  address: '',
};

const getRoleLabel = (role: string) => ROLE_LABELS[role] || role;

const hasAnyRole = (user: User, roles: string[]) => user.roles.some((role) => roles.includes(role));
const isDoctorUser = (user: User) => hasAnyRole(user, DOCTOR_ROLES);

const sanitizeUsername = (email: string, username: string): string => {
  const candidate = (username.trim() || email.split('@')[0] || 'user')
    .toLowerCase()
    .replace(/[^a-z0-9._]/g, '');

  if (candidate.length >= 3) {
    return candidate.slice(0, 50);
  }

  const suffix = Date.now().toString().slice(-4);
  return `${candidate || 'usr'}${suffix}`.slice(0, 50);
};

const extractApiErrorMessage = (error: unknown) => {
  const responseData = (error as {
    response?: {
      data?: {
        message?: string;
        data?: Record<string, string>;
      };
    };
  })?.response?.data;

  if (responseData?.data) {
    const firstValidationError = Object.values(responseData.data)[0];
    if (firstValidationError) return firstValidationError;
  }

  return responseData?.message || 'Thao tác thất bại. Vui lòng thử lại.';
};

function StaffInfoModal({
  isOpen,
  mode,
  initialData,
  submitting,
  onClose,
  onSubmit,
}: StaffInfoModalProps) {
  const [form, setForm] = useState<StaffFormState>(initialData);

  if (!isOpen) return null;

  const isEdit = mode === 'edit';

  const handleSubmit = async () => {
    if (!form.fullName.trim() || !form.email.trim() || !form.role) {
      alert('Vui lòng nhập đầy đủ các trường bắt buộc.');
      return;
    }
    await onSubmit(form);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4">
      <div className="w-full max-w-4xl border border-slate-200 bg-white shadow-xl shadow-slate-900/10">
        <div className="flex items-center justify-between border-b border-slate-200 bg-blue-600 px-4 py-3 text-white">
          <h2 className="text-sm font-extrabold uppercase tracking-wider">Thông tin nhân viên</h2>
          <button type="button" onClick={onClose} className="text-sm font-bold">
            X
          </button>
        </div>

        <div className="grid grid-cols-1 gap-4 p-4 md:grid-cols-2">
          <div className="md:col-span-2">
            <label className="mb-1 block text-xs font-bold uppercase">Họ và tên (*)</label>
            <input
              className="w-full border border-slate-200 px-3 py-2 text-sm"
              value={form.fullName}
              onChange={(event) => setForm((prev) => ({ ...prev, fullName: event.target.value }))}
              placeholder="VD: Nguyễn Văn A"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold uppercase">Số CCCD / CMND (*)</label>
            <input
              className="w-full border border-slate-200 px-3 py-2 text-sm"
              value={form.cccd}
              onChange={(event) => setForm((prev) => ({ ...prev, cccd: event.target.value }))}
              placeholder="12 số định danh cá nhân"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold uppercase">Ngày sinh</label>
            <input
              type="date"
              className="w-full border border-slate-200 px-3 py-2 text-sm"
              value={form.dateOfBirth}
              onChange={(event) => setForm((prev) => ({ ...prev, dateOfBirth: event.target.value }))}
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold uppercase">Số điện thoại (*)</label>
            <input
              className="w-full border border-slate-200 px-3 py-2 text-sm"
              value={form.phoneNumber}
              onChange={(event) => setForm((prev) => ({ ...prev, phoneNumber: event.target.value }))}
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold uppercase">Email liên hệ</label>
            <input
              type="email"
              className="w-full border border-slate-200 px-3 py-2 text-sm"
              value={form.email}
              onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold uppercase">Chức vụ (*)</label>
            <select
              className="w-full border border-slate-200 px-3 py-2 text-sm"
              value={form.role}
              onChange={(event) => setForm((prev) => ({ ...prev, role: event.target.value }))}
            >
              {ROLE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold uppercase">Tên đăng nhập (*)</label>
            <input
              className="w-full border border-slate-200 px-3 py-2 text-sm"
              value={form.username}
              onChange={(event) => setForm((prev) => ({ ...prev, username: event.target.value }))}
              placeholder="username"
              disabled={isEdit}
            />
          </div>

          <div className="md:col-span-2">
            <label className="mb-1 block text-xs font-bold uppercase">Địa chỉ thường trú</label>
            <input
              className="w-full border border-slate-200 px-3 py-2 text-sm"
              value={form.address}
              onChange={(event) => setForm((prev) => ({ ...prev, address: event.target.value }))}
              placeholder="Nhập địa chỉ đầy đủ..."
            />
          </div>
        </div>

        <div className="flex items-center justify-end gap-2 border-t border-slate-200 p-4">
          <button
            type="button"
            onClick={onClose}
            className="border border-slate-200 bg-white px-5 py-2 text-sm font-bold uppercase hover:bg-white"
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={submitting}
            className="border border-slate-200 bg-blue-600 px-5 py-2 text-sm font-bold uppercase text-white hover:bg-blue-700 disabled:opacity-60"
          >
            {submitting ? 'Đang lưu...' : 'Lưu thông tin'}
          </button>
        </div>
      </div>
    </div>
  );
}

type DoctorProfileModalProps = {
  isOpen: boolean;
  loading: boolean;
  profile: ProfileResponse | null;
  error: string;
  onClose: () => void;
};

const readValue = (value?: string | number | null) => {
  if (value === undefined || value === null || value === '') return 'Chưa cập nhật';
  return String(value);
};

const getDepartmentName = (profile: ProfileResponse) => {
  const department = profile.profile.department;
  if (!department) return '';
  return typeof department === 'string' ? department : department.deptName || '';
};

function DoctorProfileModal({ isOpen, loading, profile, error, onClose }: DoctorProfileModalProps) {
  if (!isOpen) return null;

  const doctorProfile = profile?.profile;
  const fileRows = [
    { label: 'Giấy phép hành nghề', url: doctorProfile?.certificationFileUrl },
    { label: 'Bằng cấp/chứng chỉ', url: doctorProfile?.degreeFileUrl },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4">
      <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto border border-slate-200 bg-white shadow-xl shadow-slate-900/10">
        <div className="flex items-center justify-between border-b border-slate-200 bg-blue-600 px-4 py-3 text-white">
          <h2 className="text-sm font-extrabold uppercase tracking-wider">Hồ sơ bác sĩ</h2>
          <button type="button" onClick={onClose} className="text-sm font-bold">
            X
          </button>
        </div>

        <div className="space-y-5 p-4">
          {loading && <p className="py-8 text-center text-sm text-slate-600">Đang tải hồ sơ bác sĩ...</p>}
          {!loading && error && <div className="border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>}

          {!loading && profile && doctorProfile && (
            <>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-[auto_1fr]">
                <div className="h-24 w-24 overflow-hidden rounded-full bg-blue-100 text-blue-700">
                  {profile.avatarUrl ? (
                    <img src={profile.avatarUrl} alt={profile.fullName} className="h-full w-full object-cover" />
                  ) : (
                    <span className="flex h-full w-full items-center justify-center text-3xl font-bold">
                      {profile.fullName?.charAt(0)?.toUpperCase() || 'B'}
                    </span>
                  )}
                </div>
                <div>
                  <h3 className="text-xl font-bold text-slate-950">{profile.fullName}</h3>
                  <p className="text-sm text-slate-600">{profile.email}</p>
                  <p className="mt-1 text-sm text-slate-600">Mã bác sĩ: {readValue(doctorProfile.doctorId)}</p>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                <InfoItem label="Số điện thoại" value={readValue(profile.phoneNumber)} />
                <InfoItem label="CCCD/CMND" value={readValue(profile.cccd)} />
                <InfoItem label="Khoa" value={readValue(getDepartmentName(profile))} />
                <InfoItem label="Phòng khám đang trực" value={readValue(doctorProfile.room?.roomName)} />
                <InfoItem label="Chuyên khoa" value={readValue(doctorProfile.specialization)} />
                <InfoItem label="Bằng cấp" value={readValue(doctorProfile.degree)} />
                <InfoItem label="Số giấy phép hành nghề" value={readValue(doctorProfile.licenseNumber)} />
                <InfoItem label="Ngày cấp" value={readValue(doctorProfile.certificationIssueDate)} />
                <InfoItem label="Nơi cấp" value={readValue(doctorProfile.certificationPlace)} />
                <InfoItem label="Nơi đào tạo" value={readValue(doctorProfile.trainingPlace)} />
                <InfoItem label="Năm tốt nghiệp" value={readValue(doctorProfile.graduationYear)} />
                <InfoItem label="Số năm kinh nghiệm" value={readValue(doctorProfile.experienceYears)} />
                <InfoItem label="Nơi làm việc trước" value={readValue(doctorProfile.previousWorkplace)} />
                <InfoItem label="Tiểu sử / giới thiệu" value={readValue(doctorProfile.bio)} className="md:col-span-2" />
              </div>

              <div>
                <h4 className="mb-2 text-sm font-bold uppercase text-slate-800">File hồ sơ</h4>
                <div className="grid grid-cols-1 gap-2 md:grid-cols-2">
                  {fileRows.map((file) => (
                    <div key={file.label} className="border border-slate-200 p-3 text-sm">
                      <p className="font-semibold text-slate-900">{file.label}</p>
                      {file.url ? (
                        <a href={file.url} target="_blank" rel="noreferrer" className="mt-1 block truncate text-blue-600 hover:underline">
                          Xem file đã tải lên
                        </a>
                      ) : (
                        <p className="mt-1 text-slate-500">Chưa tải lên</p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function InfoItem({ label, value, className = '' }: { label: string; value: string; className?: string }) {
  return (
    <div className={`border border-slate-200 bg-slate-50 px-3 py-2 ${className}`}>
      <p className="text-[11px] font-bold uppercase text-slate-500">{label}</p>
      <p className="mt-1 text-sm font-medium text-slate-950">{value}</p>
    </div>
  );
}

export default function ManagerStaffPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<ModalMode>('create');
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [modalInitialData, setModalInitialData] = useState<StaffFormState>(EMPTY_FORM);
  const [profileModalOpen, setProfileModalOpen] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [selectedProfile, setSelectedProfile] = useState<ProfileResponse | null>(null);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await userService.getUsers(0, 500, '', 'ALL');
      const list = (response.users || []).filter((item) => !hasAnyRole(item, SYSTEM_ROLES));
      setUsers(list);
    } catch (error) {
      console.error('Failed to fetch staff users:', error);
      alert(extractApiErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const filteredUsers = useMemo(() => {
    const keyword = search.trim().toLowerCase();

    return users
      .filter((user) => {
        if (!keyword) return true;
        return [user.fullName, user.username, user.email, user.staffId]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(keyword));
      })
      .filter((user) => (roleFilter === 'ALL' ? true : user.roles.includes(roleFilter)))
      .filter((user) => {
        if (statusFilter === 'ALL') return true;
        return statusFilter === 'ACTIVE' ? user.isActive : !user.isActive;
      });
  }, [roleFilter, search, statusFilter, users]);

  const openCreateModal = () => {
    setModalMode('create');
    setEditingUser(null);
    setModalInitialData(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEditModal = (user: User) => {
    const selectedRole = ROLE_OPTIONS.find((option) => user.roles.includes(option.value))?.value || 'ROLE_DOCTOR';
    setModalMode('edit');
    setEditingUser(user);
    setModalInitialData({
      fullName: user.fullName || '',
      cccd: user.cccd || '',
      dateOfBirth: '',
      phoneNumber: user.phoneNumber || '',
      email: user.email || '',
      role: selectedRole,
      username: user.username || '',
      address: '',
    });
    setModalOpen(true);
  };

  const handleModalSubmit = async (form: StaffFormState) => {
    setSubmitting(true);
    try {
      const normalizedEmail = form.email.trim().toLowerCase();
      const normalizedCccd = form.cccd.trim();
      const normalizedPhone = form.phoneNumber.trim();

      if (modalMode === 'create') {
        const payload: {
          fullName: string;
          email: string;
          username: string;
          roleNames: string[];
          cccd?: string;
          phoneNumber?: string;
        } = {
          fullName: form.fullName.trim(),
          email: normalizedEmail,
          username: sanitizeUsername(normalizedEmail, form.username),
          roleNames: [form.role],
        };

        if (normalizedCccd) payload.cccd = normalizedCccd;
        if (normalizedPhone) payload.phoneNumber = normalizedPhone;

        await userService.createUser(payload);
      } else if (editingUser) {
        const payload: {
          fullName: string;
          email: string;
          roleNames: string[];
          cccd?: string;
          phoneNumber?: string;
        } = {
          fullName: form.fullName.trim(),
          email: normalizedEmail,
          roleNames: [form.role],
        };

        if (normalizedCccd) payload.cccd = normalizedCccd;
        if (normalizedPhone) payload.phoneNumber = normalizedPhone;

        await userService.updateUser(editingUser.userId, payload);
      }

      setModalOpen(false);
      await fetchUsers();
    } catch (error) {
      console.error('Save staff user failed:', error);
      alert(extractApiErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleStatus = async (user: User) => {
    try {
      await userService.updateStatus(user.userId, !user.isActive);
      await fetchUsers();
    } catch (error) {
      console.error('Toggle status failed:', error);
      alert(extractApiErrorMessage(error));
    }
  };

  const openDoctorProfile = async (user: User) => {
    setProfileModalOpen(true);
    setProfileLoading(true);
    setProfileError('');
    setSelectedProfile(null);

    try {
      const profile = await userService.getManagedProfile(user.userId);
      setSelectedProfile(profile);
    } catch (error) {
      console.error('Load doctor profile failed:', error);
      setProfileError(extractApiErrorMessage(error));
    } finally {
      setProfileLoading(false);
    }
  };

  const handleDelete = async (user: User) => {
    const confirmed = window.confirm(`Bạn có chắc muốn xóa nhân viên ${user.fullName}?`);
    if (!confirmed) return;

    try {
      await userService.deleteUser(user.userId);
      await fetchUsers();
    } catch (error) {
      console.error('Delete user failed:', error);
      alert(extractApiErrorMessage(error));
    }
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] p-6 text-slate-950">
      <div className="border border-slate-200 bg-white shadow-[0_18px_40px_-32px_rgb(15_23_42/0.45)]">
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <h1 className="text-lg font-extrabold uppercase tracking-wide">Danh sách nhân viên</h1>
          <button
            onClick={openCreateModal}
            className="border border-slate-200 bg-blue-600 px-4 py-2 text-xs font-bold uppercase text-white hover:bg-blue-700"
          >
            + Thêm nhân viên mới
          </button>
        </div>

        <div className="space-y-4 p-4">
          <div className="grid grid-cols-1 gap-2 md:grid-cols-[2fr_1fr_1fr]">
            <input
              className="border border-slate-200 px-3 py-2 text-sm"
              placeholder="Tìm kiếm tên, email, SĐT..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />

            <select
              className="border border-slate-200 px-3 py-2 text-sm"
              value={roleFilter}
              onChange={(event) => setRoleFilter(event.target.value)}
            >
              <option value="ALL">Tất cả chức vụ</option>
              {ROLE_OPTIONS.map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </select>

            <select
              className="border border-slate-200 px-3 py-2 text-sm"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động</option>
              <option value="LOCKED">Đã khóa</option>
            </select>
          </div>

          <div className="overflow-x-auto border border-slate-200">
            <table className="w-full min-w-[980px] text-[11px]">
              <thead className="bg-blue-600 text-white">
                <tr className="uppercase">
                  <th className="px-3 py-2 text-left">STT</th>
                  <th className="px-3 py-2 text-left">Họ và tên</th>
                  <th className="px-3 py-2 text-left">Tên đăng nhập</th>
                  <th className="px-3 py-2 text-left">Chức vụ</th>
                  <th className="px-3 py-2 text-left">Trạng thái</th>
                  <th className="px-3 py-2 text-right">Hành động</th>
                </tr>
              </thead>

              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={6} className="px-3 py-6 text-center text-sm">
                      Đang tải danh sách nhân viên...
                    </td>
                  </tr>
                ) : filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-3 py-6 text-center text-sm">
                      Không có dữ liệu phù hợp.
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map((user, index) => {
                    const role = ROLE_OPTIONS.find((item) => user.roles.includes(item.value))?.value || user.roles[0] || 'N/A';
                    return (
                      <tr key={user.userId} className="border-b border-neutral-300 last:border-b-0">
                        <td className="px-3 py-2">{index + 1}</td>
                        <td className={`px-3 py-2 font-semibold ${!user.isActive ? 'text-neutral-700' : ''}`}>{user.fullName}</td>
                        <td className={`px-3 py-2 ${!user.isActive ? 'text-neutral-700' : ''}`}>{user.username}</td>
                        <td className="px-3 py-2">
                          <span
                            className={`inline-block border border-slate-200 px-2 py-0.5 text-[10px] font-bold uppercase ${ROLE_BADGE_STYLE[role] || 'bg-white text-slate-700'}`}
                          >
                            {getRoleLabel(role)}
                          </span>
                        </td>
                        <td className={`px-3 py-2 font-semibold ${user.isActive ? 'text-slate-950' : 'text-neutral-700 line-through'}`}>
                          {user.isActive ? 'Hoạt động' : 'Đã khóa'}
                        </td>
                        <td className="px-3 py-2">
                          <div className="flex justify-end gap-1">
                            {isDoctorUser(user) && (
                              <button
                                onClick={() => openDoctorProfile(user)}
                                className="border border-slate-200 px-2 py-1 text-[10px] font-bold uppercase hover:bg-white"
                              >
                                Hồ sơ
                              </button>
                            )}
                            <button
                              onClick={() => openEditModal(user)}
                              className="border border-slate-200 px-2 py-1 text-[10px] font-bold uppercase hover:bg-white"
                            >
                              Sửa
                            </button>
                            <button
                              onClick={() => handleToggleStatus(user)}
                              className="border border-slate-200 px-2 py-1 text-[10px] font-bold uppercase hover:bg-white"
                            >
                              {user.isActive ? 'Khóa' : 'Mở khóa'}
                            </button>
                            <button
                              onClick={() => handleDelete(user)}
                              className="border border-slate-200 px-2 py-1 text-[10px] font-bold uppercase hover:bg-white"
                            >
                              Xóa
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <StaffInfoModal
        key={`${modalMode}-${editingUser?.userId ?? 'new'}-${modalOpen ? 'open' : 'closed'}`}
        isOpen={modalOpen}
        mode={modalMode}
        initialData={modalInitialData}
        submitting={submitting}
        onClose={() => setModalOpen(false)}
        onSubmit={handleModalSubmit}
      />

      <DoctorProfileModal
        isOpen={profileModalOpen}
        loading={profileLoading}
        profile={selectedProfile}
        error={profileError}
        onClose={() => setProfileModalOpen(false)}
      />
    </div>
  );
}

