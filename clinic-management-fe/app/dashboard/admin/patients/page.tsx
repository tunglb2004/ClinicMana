'use client';

import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from 'react';
import { ConfirmationModal } from '@/components/common/ConfirmationModal';
import {
    patientService,
    type AdminPatientMutationPayload,
    type AdminPatientRecord,
} from '@/services/patient.service';
import { PatientFormModal, type PatientFormValues } from './PatientFormModal';

type StatusFilter = 'ALL' | 'ACTIVE' | 'LOCKED';

function extractErrorMessage(error: unknown): string {
    if (error && typeof error === 'object' && 'response' in error) {
        const response = (error as { response?: { data?: { message?: string } } }).response;
        if (response?.data?.message) {
            return response.data.message;
        }
    }

    if (error instanceof Error && error.message) {
        return error.message;
    }

    return 'Có lỗi xảy ra. Vui lòng thử lại.';
}

function normalizeText(value: string): string | undefined {
    const trimmed = value.trim();
    return trimmed ? trimmed : undefined;
}

function toMutationPayload(values: PatientFormValues): AdminPatientMutationPayload {
    return {
        fullName: normalizeText(values.fullName),
        email: normalizeText(values.email)?.toLowerCase(),
        phoneNumber: normalizeText(values.phoneNumber),
        cccd: normalizeText(values.cccd),
        gender: values.gender || undefined,
        dateOfBirth: values.dateOfBirth || undefined,
        bloodType: normalizeText(values.bloodType),
        healthInsuranceNumber: normalizeText(values.healthInsuranceNumber),
        allergies: normalizeText(values.allergies),
        address: normalizeText(values.address),
        height: values.height ? Number(values.height) : undefined,
        weight: values.weight ? Number(values.weight) : undefined,
        backgroundDisease: normalizeText(values.backgroundDisease),
        emergencyContactName: normalizeText(values.emergencyContactName),
        emergencyContactPhone: normalizeText(values.emergencyContactPhone),
        emergencyContactRelationship: normalizeText(values.emergencyContactRelationship),
    };
}

export default function PatientManagementPage() {
    const [patients, setPatients] = useState<AdminPatientRecord[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [appliedSearchTerm, setAppliedSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [error, setError] = useState<string | null>(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [editingPatient, setEditingPatient] = useState<AdminPatientRecord | null>(null);
    const [saving, setSaving] = useState(false);
    const [statusModal, setStatusModal] = useState<{ isOpen: boolean; patient: AdminPatientRecord | null }>({
        isOpen: false,
        patient: null,
    });
    const [deleteModal, setDeleteModal] = useState<{ isOpen: boolean; patient: AdminPatientRecord | null }>({
        isOpen: false,
        patient: null,
    });

    useEffect(() => {
        const timer = window.setTimeout(() => {
            setAppliedSearchTerm(searchTerm.trim());
        }, 400);

        return () => window.clearTimeout(timer);
    }, [searchTerm]);

    useEffect(() => {
        setCurrentPage(0);
    }, [appliedSearchTerm, pageSize, statusFilter]);

    const fetchPatients = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            const data = await patientService.getAdminPatients(
                currentPage,
                pageSize,
                appliedSearchTerm,
                'userId',
                'desc',
                statusFilter === 'ALL' ? undefined : statusFilter === 'ACTIVE'
            );

            setPatients(data.content ?? []);
            setTotalPages(data.totalPages ?? 0);
            setTotalElements(data.totalElements ?? 0);
        } catch (fetchError) {
            setPatients([]);
            setError(extractErrorMessage(fetchError));
        } finally {
            setLoading(false);
        }
    }, [appliedSearchTerm, currentPage, pageSize, statusFilter]);

    useEffect(() => {
        fetchPatients();
    }, [fetchPatients]);

    const totalPagesSafe = totalPages > 0 ? totalPages : 1;

    const pageNumbers = useMemo(() => {
        const total = totalPagesSafe;
        const current = currentPage + 1;
        if (total <= 7) {
            return Array.from({ length: total }, (_, index) => index + 1);
        }

        const pages: Array<number | string> = [1];
        const left = Math.max(2, current - 1);
        const right = Math.min(total - 1, current + 1);

        if (left > 2) {
            pages.push('...');
        }

        for (let page = left; page <= right; page += 1) {
            pages.push(page);
        }

        if (right < total - 1) {
            pages.push('...');
        }

        pages.push(total);
        return pages;
    }, [currentPage, totalPagesSafe]);

    const handlePageSizeChange = (event: ChangeEvent<HTMLSelectElement>) => {
        setPageSize(Number(event.target.value) || 10);
    };

    const handleCreatePatient = async (values: PatientFormValues) => {
        setSaving(true);

        try {
            await patientService.createAdminPatient(toMutationPayload(values));
            setIsCreateModalOpen(false);
            await fetchPatients();
        } catch (submitError) {
            setError(extractErrorMessage(submitError));
        } finally {
            setSaving(false);
        }
    };

    const openEditModal = async (patient: AdminPatientRecord) => {
        setSaving(true);
        setError(null);

        try {
            const detail = await patientService.getAdminPatientById(patient.userId);
            setEditingPatient(detail);
        } catch (fetchError) {
            setError(extractErrorMessage(fetchError));
        } finally {
            setSaving(false);
        }
    };

    const handleUpdatePatient = async (values: PatientFormValues) => {
        if (!editingPatient) {
            return;
        }

        setSaving(true);

        try {
            await patientService.updateAdminPatient(editingPatient.userId, toMutationPayload(values));
            setEditingPatient(null);
            await fetchPatients();
        } catch (submitError) {
            setError(extractErrorMessage(submitError));
        } finally {
            setSaving(false);
        }
    };

    const handleConfirmStatus = async () => {
        if (!statusModal.patient) {
            return;
        }

        setSaving(true);

        try {
            if (statusModal.patient.isActive) {
                await patientService.lockAdminPatient(statusModal.patient.userId);
            } else {
                await patientService.unlockAdminPatient(statusModal.patient.userId);
            }

            setStatusModal({ isOpen: false, patient: null });
            await fetchPatients();
        } catch (submitError) {
            setError(extractErrorMessage(submitError));
        } finally {
            setSaving(false);
        }
    };

    const handleConfirmDelete = async () => {
        if (!deleteModal.patient) {
            return;
        }

        setSaving(true);

        try {
            await patientService.deleteAdminPatient(deleteModal.patient.userId);
            setDeleteModal({ isOpen: false, patient: null });
            await fetchPatients();
        } catch (submitError) {
            setError(extractErrorMessage(submitError));
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="ls-page" style={{ padding: 24, minHeight: '100vh' }}>
            <div className="page-card">
                <div className="ls-page-title" style={{ marginBottom: 16 }}>
                    Quản lý Bệnh nhân
                </div>

                {error ? (
                    <div style={{ marginBottom: 16, padding: 12, borderRadius: 8, background: '#FFF1F2', color: '#B42318' }}>
                        {error}
                    </div>
                ) : null}

                <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                    <input
                        type="text"
                        placeholder="Tìm theo tên, email, SĐT, mã BN..."
                        className="form-control"
                        style={{ flex: 1, minWidth: 240, maxWidth: 420 }}
                        value={searchTerm}
                        onChange={(event) => setSearchTerm(event.target.value)}
                    />

                    <select
                        className="form-control"
                        style={{ width: 160 }}
                        value={statusFilter}
                        onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
                    >
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="ACTIVE">Hoạt động</option>
                        <option value="LOCKED">Đã khóa</option>
                    </select>

                    <button className="btn btn-primary" onClick={() => setIsCreateModalOpen(true)}>
                        + Thêm bệnh nhân
                    </button>
                </div>

                <div style={{ overflowX: 'auto' }}>
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>STT</th>
                                <th>Mã BN</th>
                                <th>Họ và tên</th>
                                <th>SĐT</th>
                                <th>Email</th>
                                <th>Ngày sinh</th>
                                <th>CCCD</th>
                                <th>Trạng thái</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan={9} style={{ textAlign: 'center', padding: 24 }}>
                                        Đang tải...
                                    </td>
                                </tr>
                            ) : patients.length === 0 ? (
                                <tr>
                                    <td colSpan={9} style={{ textAlign: 'center', padding: 24 }}>
                                        Không có bệnh nhân phù hợp.
                                    </td>
                                </tr>
                            ) : (
                                patients.map((patient, index) => (
                                    <tr key={patient.userId}>
                                        <td>{currentPage * pageSize + index + 1}</td>
                                        <td>{patient.patientId || patient.userId}</td>
                                        <td style={{ fontWeight: 600 }}>{patient.fullName}</td>
                                        <td>{patient.phoneNumber || '-'}</td>
                                        <td>{patient.email}</td>
                                        <td>{patient.dateOfBirth ? String(patient.dateOfBirth).slice(0, 10) : '-'}</td>
                                        <td>{patient.cccd || '-'}</td>
                                        <td>
                                            <span
                                                style={{
                                                    display: 'inline-flex',
                                                    alignItems: 'center',
                                                    padding: '2px 10px',
                                                    borderRadius: 999,
                                                    fontSize: 12,
                                                    lineHeight: '18px',
                                                    fontWeight: 600,
                                                    background: patient.isActive ? '#ECFDF3' : '#FFF1F2',
                                                    color: patient.isActive ? '#027A48' : '#B42318',
                                                    border: `1px solid ${patient.isActive ? '#ABEFC6' : 'var(--ls-danger-soft-border)'}`,
                                                }}
                                            >
                                                {patient.isActive ? 'Hoạt động' : 'Đã khóa'}
                                            </span>
                                        </td>
                                        <td>
                                            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                                <button className="btn btn-secondary" style={{ height: 32, padding: '0 12px' }} onClick={() => openEditModal(patient)}>
                                                    Sửa
                                                </button>
                                                <button
                                                    className="btn btn-secondary"
                                                    style={{ height: 32, padding: '0 12px' }}
                                                    onClick={() => setStatusModal({ isOpen: true, patient })}
                                                >
                                                    {patient.isActive ? 'Khóa' : 'Mở'}
                                                </button>
                                                <button
                                                    className="btn btn-danger-soft"
                                                    style={{ height: 32, padding: '0 12px' }}
                                                    onClick={() => setDeleteModal({ isOpen: true, patient })}
                                                >
                                                    Xóa
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="pagination-bar">
                    <div className="pagination-summary">
                        <span>Hiển thị</span>
                        <select className="pagination-pagesize" value={pageSize} onChange={handlePageSizeChange}>
                            <option value={10}>10</option>
                            <option value={20}>20</option>
                            <option value={30}>30</option>
                            <option value={40}>40</option>
                            <option value={50}>50</option>
                        </select>
                        <span>trong {totalElements} bản ghi</span>
                    </div>

                    <div className="pagination">
                        <button
                            className={`pagination__item ${currentPage === 0 ? 'pagination__item--disabled' : ''}`}
                            disabled={currentPage === 0}
                            onClick={() => setCurrentPage((page) => Math.max(0, page - 1))}
                        >
                            ‹
                        </button>

                        {pageNumbers.map((page, index) =>
                            typeof page === 'string' ? (
                                <span key={`ellipsis-${index}`} className="ls-text-muted-12" style={{ padding: '0 4px' }}>
                                    {page}
                                </span>
                            ) : (
                                <button
                                    key={page}
                                    className={`pagination__item ${page === currentPage + 1 ? 'pagination__item--active' : ''}`}
                                    onClick={() => setCurrentPage(page - 1)}
                                >
                                    {page}
                                </button>
                            )
                        )}

                        <button
                            className={`pagination__item ${currentPage >= totalPagesSafe - 1 ? 'pagination__item--disabled' : ''}`}
                            disabled={currentPage >= totalPagesSafe - 1}
                            onClick={() => setCurrentPage((page) => Math.min(totalPagesSafe - 1, page + 1))}
                        >
                            ›
                        </button>
                    </div>
                </div>
            </div>

            <PatientFormModal
                isOpen={isCreateModalOpen}
                mode="create"
                loading={saving}
                onClose={() => setIsCreateModalOpen(false)}
                onSubmit={handleCreatePatient}
            />

            <PatientFormModal
                isOpen={!!editingPatient}
                mode="edit"
                initialData={editingPatient}
                loading={saving}
                onClose={() => setEditingPatient(null)}
                onSubmit={handleUpdatePatient}
            />

            <ConfirmationModal
                isOpen={statusModal.isOpen}
                onClose={() => setStatusModal({ isOpen: false, patient: null })}
                onConfirm={handleConfirmStatus}
                title={statusModal.patient?.isActive ? 'Khóa tài khoản?' : 'Mở khóa tài khoản?'}
                message={statusModal.patient ? `Bạn có chắc muốn ${statusModal.patient.isActive ? 'khóa' : 'mở khóa'} bệnh nhân "${statusModal.patient.fullName}" không?` : ''}
                confirmText={statusModal.patient?.isActive ? 'KHÓA' : 'MỞ KHÓA'}
                cancelText="HỦY"
                variant="blue"
            />

            <ConfirmationModal
                isOpen={deleteModal.isOpen}
                onClose={() => setDeleteModal({ isOpen: false, patient: null })}
                onConfirm={handleConfirmDelete}
                title="Xóa bệnh nhân?"
                message={deleteModal.patient ? `Tài khoản bệnh nhân "${deleteModal.patient.fullName}" sẽ bị xóa mềm.` : ''}
                confirmText="XÓA"
                cancelText="HỦY"
                variant="red"
            />
        </div>
    );
}
