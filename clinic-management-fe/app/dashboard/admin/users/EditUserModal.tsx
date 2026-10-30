"use client";

import { useEffect, useState } from "react";
import { User, UserDetail } from "@/types/user.types";
import { userService } from "@/services/user.service";
import { X } from "lucide-react";

interface EditUserModalProps {
    isOpen: boolean;
    onClose: () => void;
    user: User | null;
    onUpdateSuccess: () => void;
}

export function EditUserModal({ isOpen, onClose, user, onUpdateSuccess }: EditUserModalProps) {
    const [detail, setDetail] = useState<UserDetail | null>(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    // Form states (simplified binding)
    const [formData, setFormData] = useState<Partial<UserDetail>>({});

    useEffect(() => {
        if (isOpen && user) {
            fetchDetail(user.userId);
        } else {
            setDetail(null);
            setFormData({});
        }
    }, [isOpen, user]);

    const fetchDetail = async (id: number) => {
        setLoading(true);
        try {
            const data = await userService.getUserDetail(id);
            setDetail(data);
            setFormData(data);
        } catch (error) {
            console.error("Failed to fetch detail", error);
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (field: keyof UserDetail, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleSave = async () => {
        if (!user || !detail) return;
        setSaving(true);
        try {
            await userService.updateUser(user.userId, formData);
            onUpdateSuccess();
            onClose();
        } catch (error) {
            console.error("Update failed", error);
            alert("Cập nhật thất bại!");
        } finally {
            setSaving(false);
        }
    };

    if (!isOpen) return null;

    const isDoctor =
        user?.roles.includes("ROLE_CLINICAL_DOCTOR") ||
        user?.roles.includes("ROLE_SPECIALIST_DOCTOR") ||
        user?.roles.includes("ROLE_CLINICALDOCTOR") ||
        user?.roles.includes("ROLE_DOCTOR") ||
        user?.roles.includes("ROLE_SPECIALISTDOCTOR");
    const isNurse = user?.roles.includes("ROLE_NURSE") || user?.roles.some(r => r === "Y tá");

    /* Common Fields Render */
    const renderPersonalInfo = () => (
        <div className="section mb-8">
            <h3 className="section-title text-xl font-bold border-b border-black pb-2 mb-4">1. Thông tin cá nhân</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="form-group">
                    <label className="block text-sm font-bold mb-2">Họ và tên</label>
                    <input
                        type="text"
                        className="w-full border border-gray-400 p-2 rounded-sm"
                        value={formData.fullName || ''}
                        onChange={(e) => handleChange('fullName', e.target.value)}
                    />
                </div>
                <div className="form-group">
                    <label className="block text-sm font-bold mb-2">Giới tính</label>
                    <select
                        className="w-full border border-gray-400 p-2 rounded-sm bg-gray-50/50"
                        value={formData.gender || ''}
                        onChange={(e) => handleChange('gender', e.target.value)}
                    >
                        <option value="MALE">Nam</option>
                        <option value="FEMALE">Nữ</option>
                        <option value="OTHER">Khác</option>
                    </select>
                </div>
                <div className="form-group">
                    <label className="block text-sm font-bold mb-2">Ngày sinh</label>
                    <div className="relative">
                        <input type="date" className="w-full border border-gray-400 p-2 rounded-sm" />
                        {/* Mocking date input styling provided by browser, customize if needed */}
                    </div>
                </div>
                <div className="form-group">
                    <label className="block text-sm font-bold mb-2">Số CCCD / CMND</label>
                    <input
                        type="text"
                        className="w-full border border-gray-400 p-2 rounded-sm"
                        value={formData.cccd || ''}
                        onChange={(e) => handleChange('cccd', e.target.value)}
                    />
                </div>
                <div className="form-group">
                    <label className="block text-sm font-bold mb-2">Số điện thoại</label>
                    <input
                        type="text"
                        className="w-full border border-gray-400 p-2 rounded-sm"
                        value={formData.phoneNumber || ''}
                        onChange={(e) => handleChange('phoneNumber', e.target.value)}
                    />
                </div>
                <div className="form-group">
                    <label className="block text-sm font-bold mb-2">Email</label>
                    <input
                        type="email"
                        className="w-full border border-gray-400 p-2 rounded-sm"
                        value={formData.email || ''}
                        readOnly // Often email is not editable easily
                    />
                </div>
                <div className="form-group md:col-span-2">
                    <label className="block text-sm font-bold mb-2">Địa chỉ liên hệ</label>
                    <input type="text" className="w-full border border-gray-400 p-2 rounded-sm" />
                </div>
            </div>
        </div>
    );

    /* Detail Logic based on Role */
    const renderContent = () => {
        if (loading) return <div className="p-10 text-center">Đang tải thông tin...</div>;

        return (
            <div className="p-6 md:p-10 max-h-[85vh] overflow-y-auto custom-scrollbar">
                {/* Avatar & Basic Info Header could go here like in design but keeping it simple for modal flow */}
                <div className="flex justify-between items-start mb-6">
                    {/* Placeholder for Avatar box if needed */}
                    <div className="border border-black p-4 w-48 text-center hidden md:block mr-8">
                        <div className="w-32 h-32 rounded-full border-2 border-black mx-auto mb-2 bg-gray-100 flex items-center justify-center">
                            <span className="text-4xl text-gray-300">Avatar</span>
                        </div>
                        <h4 className="font-bold text-lg">{formData.fullName}</h4>
                        <p className="text-gray-500 text-sm">{isDoctor ? "Bác sĩ chuyên khoa" : isNurse ? "Y tá / Điều dưỡng" : "Nhân viên"}</p>
                    </div>

                    <div className="flex-1">
                        {renderPersonalInfo()}

                        {/* DOCTOR SPECIFIC */}
                        {isDoctor && (
                            <>
                                <div className="section mb-8">
                                    <h3 className="section-title text-xl font-bold border-b border-black pb-2 mb-4">2. Thông tin hành nghề (QUAN TRỌNG NHẤT)</h3>
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Mã bác sĩ</label>
                                            <input type="text" className="w-full border border-gray-400 p-2 bg-gray-100 italic cursor-not-allowed" value={formData.staffId || ''} readOnly disabled />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Khoa chuyên môn</label>
                                            <input type="text" className="w-full border border-gray-400 p-2 bg-gray-100" value={formData.specialization || 'Nội khoa'} onChange={e => handleChange('specialization', e.target.value)} />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Chức danh chuyên môn</label>
                                            <input type="text" className="w-full border border-gray-400 p-2 bg-gray-100" value={formData.position || 'Bác sĩ đa khoa'} onChange={e => handleChange('position', e.target.value)} />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Số chứng chỉ hành nghề</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.licenseNumber || ''} onChange={e => handleChange('licenseNumber', e.target.value)} />
                                        </div>
                                        {/* File upload mock */}
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">File chứng chỉ (PDF)</label>
                                            <div className="border border-gray-400 p-1 flex">
                                                <button className="border border-gray-400 bg-gray-100 px-2 text-sm">Choose File</button>
                                                <span className="ml-2 text-sm text-gray-500 self-center">No file chosen</span>
                                            </div>
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Ngày cấp chứng chỉ</label>
                                            <input type="date" className="w-full border border-gray-400 p-2" value={formData.licenseDate ? String(formData.licenseDate).slice(0, 10) : ''} onChange={e => handleChange('licenseDate', e.target.value)} />
                                        </div>
                                        <div className="form-group md:col-span-2">
                                            <label className="block text-sm font-bold mb-2">Nơi cấp chứng chỉ</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.licensePlace || ''} onChange={e => handleChange('licensePlace', e.target.value)} />
                                        </div>
                                    </div>
                                </div>

                                <div className="section mb-8">
                                    <h3 className="section-title text-xl font-bold border-b border-black pb-2 mb-4">3. Trình độ chuyên môn</h3>
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Bằng cấp cao nhất</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.degree || ''} onChange={e => handleChange('degree', e.target.value)} />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Trường đào tạo</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.school || ''} onChange={e => handleChange('school', e.target.value)} />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Năm tốt nghiệp</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.graduationYear || ''} onChange={e => handleChange('graduationYear', e.target.value)} />
                                        </div>
                                    </div>
                                </div>

                                <div className="section mb-8">
                                    <h3 className="section-title text-xl font-bold border-b border-black pb-2 mb-4">4. Kinh nghiệm công tác</h3>
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Số năm kinh nghiệm</label>
                                            <input type="number" className="w-full border border-gray-400 p-2" value={formData.experienceYears || ''} onChange={e => handleChange('experienceYears', e.target.value)} />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Nơi công tác trước đây</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.previousWorkplace || ''} onChange={e => handleChange('previousWorkplace', e.target.value)} />
                                        </div>
                                        <div className="form-group md:col-span-2">
                                            <label className="block text-sm font-bold mb-2">Lĩnh vực chuyên sâu</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" placeholder="Tim mạch, tiêu hóa..." />
                                        </div>
                                    </div>
                                </div>

                                <div className="section mb-8">
                                    <h3 className="section-title text-xl font-bold border-b border-black pb-2 mb-4">5. Thông tin làm việc tại phòng khám</h3>
                                    <div className="form-group">
                                        <label className="block text-sm font-bold mb-2">Khoa đang công tác</label>
                                        <input type="text" className="w-full border border-gray-400 p-2 bg-gray-100 cursor-not-allowed" value={formData.deptName || ''} readOnly disabled />
                                    </div>
                                </div>
                            </>
                        )}

                        {/* NURSE SPECIFIC */}
                        {isNurse && (
                            <>
                                <div className="section mb-8">
                                    <h3 className="section-title text-xl font-bold border-b border-black pb-2 mb-4">2. Thông tin nghề nghiệp</h3>
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Mã y tá</label>
                                            <input type="text" className="w-full border border-gray-400 p-2 bg-gray-100 cursor-not-allowed" value={formData.staffId || ''} readOnly disabled />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Chức danh</label>
                                            <input type="text" className="w-full border border-gray-400 p-2 bg-gray-100" value="Điều dưỡng" readOnly />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Trình độ</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.degree || 'Trung cấp'} onChange={e => handleChange('degree', e.target.value)} />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Năm tốt nghiệp</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.graduationYear || ''} onChange={e => handleChange('graduationYear', e.target.value)} />
                                        </div>
                                        <div className="form-group md:col-span-2">
                                            <label className="block text-sm font-bold mb-2">Trường đào tạo</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.school || ''} onChange={e => handleChange('school', e.target.value)} />
                                        </div>
                                    </div>
                                </div>

                                <div className="section mb-8">
                                    <h3 className="section-title text-xl font-bold border-b border-black pb-2 mb-4">3. Chứng chỉ</h3>
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        <div className="form-group md:col-span-2">
                                            <label className="block text-sm font-bold mb-2">Chứng chỉ hành nghề</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.licenseNumber || ''} onChange={e => handleChange('licenseNumber', e.target.value)} />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Ngày cấp</label>
                                            <input type="date" className="w-full border border-gray-400 p-2" value={formData.licenseDate ? String(formData.licenseDate).slice(0, 10) : ''} onChange={e => handleChange('licenseDate', e.target.value)} />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Nơi cấp</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.licensePlace || ''} onChange={e => handleChange('licensePlace', e.target.value)} />
                                        </div>
                                    </div>
                                </div>

                                <div className="section mb-8">
                                    <h3 className="section-title text-xl font-bold border-b border-black pb-2 mb-4">4. Kinh nghiệm làm việc</h3>
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Số năm kinh nghiệm</label>
                                            <input type="number" className="w-full border border-gray-400 p-2" value={formData.experienceYears ?? ''} onChange={e => handleChange('experienceYears', e.target.value ? Number(e.target.value) : undefined)} />
                                        </div>
                                        <div className="form-group">
                                            <label className="block text-sm font-bold mb-2">Khoa từng làm việc</label>
                                            <input type="text" className="w-full border border-gray-400 p-2" value={formData.previousWorkplace || ''} onChange={e => handleChange('previousWorkplace', e.target.value)} />
                                        </div>
                                        <div className="form-group md:col-span-2">
                                            <label className="block text-sm font-bold mb-2">Kỹ năng hỗ trợ</label>
                                            <textarea className="w-full border border-gray-400 p-2 rounded-sm" rows={3} value={formData.supportSkills || ''} onChange={e => handleChange('supportSkills', e.target.value)} />
                                        </div>
                                    </div>
                                </div>

                                <div className="section mb-8">
                                    <h3 className="section-title text-xl font-bold border-b border-black pb-2 mb-4">5. Thông tin làm việc tại phòng khám</h3>
                                    <div className="form-group">
                                        <label className="block text-sm font-bold mb-2">Khoa hiện tại</label>
                                        <input type="text" className="w-full border border-gray-400 p-2 bg-gray-100 cursor-not-allowed" value={formData.deptName || ''} readOnly disabled />
                                    </div>
                                </div>
                            </>
                        )}
                    </div>
                </div>

                <div className="flex justify-center md:justify-end mt-8">
                    <button
                        onClick={handleSave}
                        disabled={saving}
                        className="bg-black text-white px-8 py-3 font-bold uppercase hover:bg-gray-800 disabled:opacity-50"
                    >
                        {saving ? "Đang lưu..." : "Lưu hồ sơ"}
                    </button>
                </div>
            </div>
        );
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
            <div className="bg-white w-full max-w-6xl max-h-[90vh] shadow-2xl overflow-hidden relative flex flex-col rounded-lg">
                <button
                    onClick={onClose}
                    className="absolute top-4 right-4 p-2 hover:bg-gray-100 rounded-full z-10"
                >
                    <X className="w-6 h-6" />
                </button>
                {renderContent()}
            </div>
        </div>
    );
}
