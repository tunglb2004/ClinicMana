'use client';

import { useEffect, useMemo, useState } from 'react';
import { userService } from '@/services/user.service';
import { User } from '@/types/user.types';

import { ConfirmationModal } from '@/components/common/ConfirmationModal';
import { EditUserModal } from './EditUserModal';
import { CreateUserModal } from './CreateUserModal';

export default function UserManagementPage() {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [appliedSearchTerm, setAppliedSearchTerm] = useState('');
    const [roleFilter, setRoleFilter] = useState('ALL');
    const [statusFilter, setStatusFilter] = useState('ALL'); // ALL, ACTIVE, LOCKED
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    // Status Confirmation Modal State
    const [statusModal, setStatusModal] = useState<{
        isOpen: boolean;
        userId: number | null;
        isActive: boolean;
    }>({
        isOpen: false,
        userId: null,
        isActive: false
    });

    // Delete Confirmation Modal State
    const [deleteModal, setDeleteModal] = useState<{
        isOpen: boolean;
        userId: number | null;
        fullName: string;
    }>({
        isOpen: false,
        userId: null,
        fullName: ''
    });

    const [editingUser, setEditingUser] = useState<User | null>(null);

    // Trigger search only on Enter key
    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            setAppliedSearchTerm(searchTerm);
        }
    };

    useEffect(() => {
        const fetchUsers = async () => {
            setLoading(true);
            try {
                const data = await userService.getUsers(currentPage, pageSize, appliedSearchTerm, roleFilter);
                setUsers(data.users);
                setTotalPages(data.totalPages);
            } catch (error) {
                console.error('Failed to fetch users:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchUsers();
    }, [appliedSearchTerm, roleFilter, currentPage, pageSize]);

    // Client-side status filter + exclude non-employee roles
    const filteredUsers = useMemo(() => {
        let result = users.filter(u =>
            !u.roles.some(r => r === 'ROLE_SYSTEM_ADMIN' || r === 'ROLE_ADMIN' || r === 'ROLE_PATIENT')
        );
        if (statusFilter === 'ACTIVE') result = result.filter(u => u.isActive);
        else if (statusFilter === 'LOCKED') result = result.filter(u => !u.isActive);
        return result;
    }, [users, statusFilter]);

    // Reset page when filter / search changes
    useEffect(() => {
        setCurrentPage(0);
    }, [appliedSearchTerm, roleFilter, statusFilter, pageSize]);

    const totalPagesSafe = totalPages > 0 ? totalPages : 1;

    const pageNumbers = useMemo(() => {
        const total = totalPagesSafe;
        const current = currentPage + 1; // 1-based for display
        if (total <= 7) {
            return Array.from({ length: total }, (_, i) => i + 1);
        }
        const pages: (number | string)[] = [];
        pages.push(1);
        const left = Math.max(2, current - 1);
        const right = Math.min(total - 1, current + 1);
        if (left > 2) pages.push('...');
        for (let i = left; i <= right; i++) {
            pages.push(i);
        }
        if (right < total - 1) pages.push('...');
        pages.push(total);
        return pages;
    }, [currentPage, totalPagesSafe]);

    const initiateStatusToggle = (user: User) => {
        setStatusModal({
            isOpen: true,
            userId: user.userId,
            isActive: user.isActive
        });
    };

    const handleConfirmStatusToggle = async () => {
        if (statusModal.userId === null) return;

        try {
            const newStatus = !statusModal.isActive;
            await userService.updateStatus(statusModal.userId, newStatus);
            // Update local state
            setUsers(prev => prev.map(u => u.userId === statusModal.userId ? { ...u, isActive: newStatus } : u));
            setStatusModal(prev => ({ ...prev, isOpen: false }));
        } catch (error) {
            console.error('Failed to update status:', error);
            alert('Có lỗi xảy ra khi cập nhật trạng thái!');
        }
    };

    const initiateDelete = (user: User) => {
        setDeleteModal({
            isOpen: true,
            userId: user.userId,
            fullName: user.fullName
        });
    };

    const handleConfirmDelete = async () => {
        if (!deleteModal.userId) return;

        try {
            await userService.deleteUser(deleteModal.userId);
            // Remove user from local state
            setUsers(prev => prev.filter(u => u.userId !== deleteModal.userId));
            setDeleteModal(prev => ({ ...prev, isOpen: false }));
        } catch (error) {
            console.error('Failed to delete user:', error);
            alert('Có lỗi xảy ra khi xóa người dùng!');
        }
    };

    return (
        <div className="ls-page" style={{ padding: 24, minHeight: '100vh' }}>
            <div className="page-card">
                {loading ? (
                    <div className="ls-text-muted" style={{ padding: 24, textAlign: 'center' }}>
                        Đang tải...
                    </div>
                ) : (
                    <>
                        <div className="ls-page-title" style={{ marginBottom: 16, textDecoration: 'underline', fontWeight: 700, fontSize: 22 }}>
                            Danh sách Nhân viên
                        </div>

                        {/* Actions Bar */}
                        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                            <input
                                type="text"
                                placeholder="Tìm kiếm: Mã NV, tên, email..."
                                className="form-control"
                                style={{ flex: 1, minWidth: 200, maxWidth: 420 }}
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                onKeyDown={handleKeyDown}
                            />

                            <select
                                className="form-control"
                                style={{ width: 150 }}
                                value={statusFilter}
                                onChange={(e) => setStatusFilter(e.target.value)}
                            >
                                <option value="ALL">Trạng thái</option>
                                <option value="ACTIVE">Hoạt động</option>
                                <option value="LOCKED">Đã khóa</option>
                            </select>

                            <select
                                className="form-control"
                                style={{ width: 150 }}
                                value={roleFilter}
                                onChange={(e) => setRoleFilter(e.target.value)}
                            >
                                <option value="ROLE_MANAGER">Manager</option>
                                <option value="ALL">Tất cả</option>
                                <option value="ROLE_CLINIC_MANAGER">Quản trị phòng khám</option>
                                <option value="ROLE_DOCTOR">Bác sĩ lâm sàng</option>
                                <option value="ROLE_SPECIALISTDOCTOR">Bác sĩ chuyên khoa</option>
                                <option value="ROLE_NURSE">Y tá</option>
                                <option value="ROLE_RECEPTIONIST">Lễ tân</option>
                                <option value="ROLE_CASHIER">Thu ngân</option>
                            </select>

                            <button
                                onClick={() => setIsCreateModalOpen(true)}
                                className="btn btn-primary"
                                style={{ whiteSpace: 'nowrap', fontWeight: 700 }}
                            >
                                + THÊM MỚI
                            </button>
                        </div>

                        <div style={{ overflowX: 'auto' }}>
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th>Mã NV</th>
                                        <th>Họ và Tên</th>
                                        <th>Email</th>
                                        <th>Vai trò</th>
                                        <th>Trạng thái</th>
                                        <th>Hành động</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredUsers.map((user) => (
                                        <tr key={user.userId}>
                                            <td>{user.staffId}</td>
                                            <td style={{ fontWeight: 600 }}>{user.fullName}</td>
                                            <td className="ls-text-muted">{user.email}</td>
                                            <td className="ls-text-muted">
                                                {user.roles.map(role => {
                                                    const roleMap: Record<string, string> = {
                                                        'ROLE_SYSTEM_ADMIN': 'Quản trị viên cấp cao',
                                                        'ROLE_ADMIN': 'Quản trị viên',
                                                        'ROLE_CLINIC_MANAGER': 'Quản trị phòng khám',
                                                        'ROLE_CLINICAL_DOCTOR': 'Bác sĩ lâm sàng',
                                                        'ROLE_CLINICALDOCTOR': 'Bác sĩ lâm sàng',
                                                        'ROLE_SPECIALIST_DOCTOR': 'Bác sĩ chuyên khoa',
                                                        'ROLE_SPECIALISTDOCTOR': 'Bác sĩ chuyên khoa',
                                                        'ROLE_NURSE': 'Y tá',
                                                        'ROLE_DOCTOR': 'Bác sĩ lâm sàng',
                                                        'ROLE_PATIENT': 'Bệnh nhân',
                                                        'ROLE_CASHIER': 'Thu ngân',
                                                        'ROLE_MANAGER': 'Manager',
                                                        'ROLE_RECEPTIONIST': 'Lễ Tân'
                                                    };
                                                    return roleMap[role] || role;
                                                }).join(', ')}
                                            </td>
                                            <td>
                                                <span
                                                    className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${
                                                        user.isActive
                                                            ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'
                                                            : 'bg-red-50 text-red-700 ring-1 ring-red-200'
                                                    }`}
                                                >
                                                    {user.isActive ? 'HOẠT ĐỘNG' : 'ĐÃ KHÓA'}
                                                </span>
                                            </td>
                                            <td>
                                                <div style={{ display: 'inline-flex', gap: 8, flexWrap: 'nowrap', whiteSpace: 'nowrap', alignItems: 'center' }}>
                                                    <button
                                                        onClick={() => setEditingUser(user)}
                                                        className="btn btn-secondary !h-8 !px-3 text-xs font-semibold"
                                                    >
                                                        TRUY CẬP
                                                    </button>
                                                    <button
                                                        onClick={() => initiateDelete(user)}
                                                        className="btn btn-danger-soft !h-8 !px-3 text-xs font-semibold"
                                                    >
                                                        XÓA
                                                    </button>
                                                    <button
                                                        onClick={() => initiateStatusToggle(user)}
                                                        className="btn btn-secondary !h-8 !px-3 text-xs font-semibold"
                                                    >
                                                        {user.isActive ? 'KHÓA' : 'MỞ'}
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {/* Pagination */}
                        <div className="pagination-bar">
                            <div className="pagination-summary">
                                <span className="ls-text-muted">Hien thi</span>
                                <select
                                    className="pagination-pagesize"
                                    value={pageSize}
                                    onChange={(e) => setPageSize(Number(e.target.value))}
                                    aria-label="So nhan vien moi trang"
                                >
                                    {[10, 20, 30, 40, 50].map((size) => (
                                        <option key={size} value={size}>
                                            {size}/trang
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div className="pagination">
                            <button
                                className={`pagination__item ${currentPage === 0 ? 'pagination__item--disabled' : ''}`}
                                disabled={currentPage === 0}
                                onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
                            >
                                &lt;
                            </button>

                            {pageNumbers.map((p, idx) =>
                                typeof p === 'string' ? (
                                    <span key={`ellipsis-${idx}`} className="px-2 text-sm text-slate-500">
                                        {p}
                                    </span>
                                ) : (
                                    <button
                                        key={p}
                                        className={`pagination__item ${p === currentPage + 1 ? 'pagination__item--active' : ''}`}
                                        onClick={() => setCurrentPage(p - 1)}
                                    >
                                        {p}
                                    </button>
                                )
                            )}

                            <button
                                className={`pagination__item ${currentPage >= totalPagesSafe - 1 ? 'pagination__item--disabled' : ''}`}
                                disabled={currentPage >= totalPagesSafe - 1}
                                onClick={() => setCurrentPage(p => Math.min(totalPagesSafe - 1, p + 1))}
                            >
                                &gt;
                            </button>
                            </div>
                        </div>
                    </>
                )}
            </div>
            {/* Status Confirmation Modal */}
            <ConfirmationModal
                isOpen={statusModal.isOpen}
                onClose={() => setStatusModal(prev => ({ ...prev, isOpen: false }))}
                onConfirm={handleConfirmStatusToggle}
                title={statusModal.isActive ? "XÁC NHẬN KHÓA?" : "XÁC NHẬN MỞ?"}
                message={`Bạn chắc chắn muốn ${statusModal.isActive ? "khóa" : "mở khóa"} tài khoản này?`}
                confirmText={statusModal.isActive ? "KHÓA NGAY" : "MỞ NGAY"}
                cancelText="HỦY"
                variant={statusModal.isActive ? 'blue' : 'black'}
            />

            {/* Delete Confirmation Modal */}
            <ConfirmationModal
                isOpen={deleteModal.isOpen}
                onClose={() => setDeleteModal(prev => ({ ...prev, isOpen: false }))}
                onConfirm={handleConfirmDelete}
                title="XÁC NHẬN XÓA?"
                message={`Bạn chắc chắn muốn xóa nhân viên ${deleteModal.fullName}? Hành động này không thể hoàn tác.`}
                confirmText="XÓA NGAY"
                cancelText="HỦY"
                variant="red"
            />

            {/* Edit User Modal */}
            <EditUserModal
                isOpen={!!editingUser}
                onClose={() => setEditingUser(null)}
                user={editingUser}
                onUpdateSuccess={() => {
                    setEditingUser(null);
                    setAppliedSearchTerm(prev => prev || ' ');
                }}
            />

            {/* Create User Modal */}
            <CreateUserModal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                onCreateSuccess={() => {
                    setIsCreateModalOpen(false);
                    setAppliedSearchTerm(prev => prev || ' '); // Trigger refresh
                }}
            />
        </div>
    );
}
