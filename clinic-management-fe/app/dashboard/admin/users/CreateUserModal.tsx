"use client";

import { useState } from "react";
import { userService } from "@/services/user.service";

const DEFAULT_ROLE = "ROLE_DOCTOR";

const ROLE_OPTIONS = [
    { value: "ROLE_MANAGER", label: "Manager" },
    { value: "ROLE_CASHIER", label: "Thu ngan" },
    { value: "ROLE_CLINIC_MANAGER", label: "Quản trị phòng khám" },
    { value: "ROLE_DOCTOR", label: "Bác sĩ lâm sàng" },
    { value: "ROLE_SPECIALISTDOCTOR", label: "Bác sĩ chuyên khoa" },
    { value: "ROLE_NURSE", label: "Y tá / Điều dưỡng" },
    { value: "ROLE_RECEPTIONIST", label: "Lễ tân" },
] as const;

const buildUsernameFromEmail = (email: string): string => {
    const localPart = (email.split("@")[0] || "")
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9._]/g, "");

    const base = localPart || "user";
    if (base.length >= 3) {
        return base.slice(0, 50);
    }

    const suffix = Date.now().toString().slice(-4);
    return `${base}${suffix}`.slice(0, 50);
};

const getCreateUserErrorMessage = (error: unknown): string => {
    const responseData = (error as {
        response?: {
            data?: {
                message?: string;
                data?: Record<string, string>;
            };
        };
    })?.response?.data;

    const firstValidationMessage = responseData?.data
        ? Object.values(responseData.data)[0]
        : undefined;

    return (
        firstValidationMessage ||
        responseData?.message ||
        "Tạo mới thất bại! Vui lòng kiểm tra lại thông tin."
    );
};

interface CreateUserModalProps {
    isOpen: boolean;
    onClose: () => void;
    onCreateSuccess: () => void;
}

export function CreateUserModal({ isOpen, onClose, onCreateSuccess }: CreateUserModalProps) {
    const [loading, setLoading] = useState(false);

    // Form State
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [cccd, setCccd] = useState("");
    const [role, setRole] = useState(DEFAULT_ROLE);

    const handleSave = async () => {
        if (!fullName || !email || !role) {
            alert("Vui lòng nhập đầy đủ thông tin bắt buộc!");
            return;
        }

        if (cccd && cccd.length > 20) {
            alert("CCCD không được quá 20 ký tự.");
            return;
        }

        setLoading(true);
        try {
            // Backend yêu cầu username 3-50 ký tự.
            const normalizedEmail = email.trim().toLowerCase();
            const username = buildUsernameFromEmail(normalizedEmail);
            const normalizedCccd = cccd.trim();

            const payload: {
                fullName: string;
                email: string;
                username: string;
                roleNames: string[];
                cccd?: string;
            } = {
                fullName: fullName.trim(),
                email: normalizedEmail,
                username,
                roleNames: [role]
            };

            if (normalizedCccd) {
                payload.cccd = normalizedCccd;
            }

            await userService.createUser(payload);

            // Reset form
            setFullName("");
            setEmail("");
            setCccd("");
            setRole(DEFAULT_ROLE);

            onCreateSuccess();
            onClose();
        } catch (error) {
            console.error("Failed to create user", error);
            alert(getCreateUserErrorMessage(error));
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
            <div className="bg-white w-full max-w-2xl shadow-[8px_8px_0px_0px_rgba(0,0,0,0.5)] border-2 border-black flex flex-col transform transition-all rounded-lg overflow-hidden">
                {/* Header */}
                <div className="flex justify-between items-center bg-gray-100 p-4 border-b-2 border-black">
                    <h2 className="text-xl font-bold uppercase tracking-wide">THÊM MỚI NHÂN VIÊN</h2>
                    <button onClick={onClose} className="hover:bg-gray-200 p-1 rounded transition-colors">
                        <span className="font-bold text-lg">[X]</span>
                    </button>
                </div>

                {/* Body */}
                <div className="p-8 space-y-6">
                    {/* Full Name */}
                    <div>
                        <label className="block text-sm font-bold mb-2 text-black">Họ và Tên</label>
                        <input
                            type="text"
                            className="w-full border border-black p-3 text-sm focus:outline-none focus:ring-1 focus:ring-black transition-all"
                            value={fullName}
                            onChange={(e) => setFullName(e.target.value)}
                            placeholder="Nguyễn Văn A"
                        />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {/* Email */}
                        <div>
                            <label className="block text-sm font-bold mb-2 text-black">Email</label>
                            <input
                                type="email"
                                className="w-full border border-black p-3 text-sm focus:outline-none focus:ring-1 focus:ring-black transition-all"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="example@email.com"
                            />
                        </div>

                        {/* CCCD */}
                        <div>
                            <label className="block text-sm font-bold mb-2 text-black">Số CCCD</label>
                            <input
                                type="text"
                                className="w-full border border-black p-3 text-sm focus:outline-none focus:ring-1 focus:ring-black transition-all"
                                value={cccd}
                                onChange={(e) => setCccd(e.target.value)}
                                placeholder=""
                            />
                        </div>
                    </div>

                    {/* Role */}
                    <div>
                        <label className="block text-sm font-bold mb-2 text-black">Vai trò</label>
                        <div className="relative">
                            <select
                                className="w-full border-2 border-black p-3 text-sm appearance-none font-bold focus:outline-none bg-white cursor-pointer hover:bg-gray-50 transition-colors"
                                value={role}
                                onChange={(e) => setRole(e.target.value)}
                            >
                                {ROLE_OPTIONS.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </select>
                            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-black">
                                <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
                                    <path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z" />
                                </svg>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="p-4 border-t-2 border-black flex justify-end gap-4 bg-white">
                    <button
                        onClick={onClose}
                        className="px-6 py-2 bg-white border border-black font-bold text-sm uppercase hover:bg-gray-100 transition-all"
                    >
                        HỦY BỎ
                    </button>
                    <button
                        onClick={handleSave}
                        disabled={loading}
                        className="px-8 py-2 bg-black text-white font-bold text-sm uppercase hover:bg-gray-800 transition-all disabled:opacity-70 disabled:cursor-not-allowed"
                    >
                        {loading ? "ĐANG LƯU..." : "LƯU"}
                    </button>
                </div>
            </div>
        </div>
    );
}
