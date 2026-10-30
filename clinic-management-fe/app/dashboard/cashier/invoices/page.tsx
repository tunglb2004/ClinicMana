'use client';

import { useEffect, useState, useCallback } from 'react';
import { financialService } from '@/services/financial.service';
import { FinancialResponse } from '@/types/financial.types';
import { useAuthStore } from '@/store/auth.store';
import { useRouter } from 'next/navigation';
import { ROLES } from '@/lib/roleRedirect';
import { RefreshCw } from 'lucide-react';
import { PAYMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

type Tab = 'pending' | 'history';
type DateFilter = 'today' | 'recent';

const STATUS_LABEL: Record<string, { label: string; cls: string }> = {
    PAID:    { label: 'Đã thanh toán',   cls: 'bg-green-50 text-green-700 border-green-300' },
    PENDING: { label: 'Chờ thanh toán',  cls: 'bg-yellow-50 text-yellow-700 border-yellow-300' },
    UNPAID:  { label: 'Chờ thanh toán',  cls: 'bg-yellow-50 text-yellow-700 border-yellow-300' },
    FAILED:  { label: 'Giao dịch thất bại', cls: 'bg-red-50 text-red-700 border-red-300 border-dashed' },
    PROCESSING: { label: 'Đang xử lý',  cls: 'bg-gray-100 text-gray-700 border-gray-400' },
};

function statusBadge(status: string) {
    const s = STATUS_LABEL[status] ?? {
        label: formatVietnameseLabel(status, PAYMENT_STATUS_LABELS),
        cls: 'bg-gray-100 text-gray-600 border-gray-300',
    };
    return (
        <span className={`px-3 py-1 inline-flex text-xs font-medium border rounded ${s.cls}`}>
            {s.label}
        </span>
    );
}

export default function CashierInvoicesPage() {
    const [activeTab, setActiveTab] = useState<Tab>('pending');
    const [feeType, setFeeType] = useState<'initial' | 'supplement'>('initial');
    const [invoices, setInvoices] = useState<FinancialResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshTick, setRefreshTick] = useState(0);
    const [searchInput, setSearchInput] = useState('');
    const [searchTerm, setSearchTerm] = useState('');
    const [insuranceFilter, setInsuranceFilter] = useState('');
    const [dateFilter, setDateFilter] = useState<DateFilter>('today');
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(0);

    const { user } = useAuthStore();
    const router = useRouter();
    const pageSize = 10;

    useEffect(() => {
        if (user && !user.roles.includes(ROLES.CASHIER) && !user.roles.includes(ROLES.SYSTEM_ADMIN)) {
            router.push('/unauthorized');
        }
    }, [user, router]);

    useEffect(() => {
        const id = setTimeout(() => setSearchTerm(searchInput.trim()), 300);
        return () => clearTimeout(id);
    }, [searchInput]);

    useEffect(() => {
        const fetchInvoices = async () => {
            setLoading(true);
            try {
                let data;
                const todayOnly = dateFilter === 'today';
                if (activeTab === 'pending') {
                    data = feeType === 'supplement'
                        ? await financialService.getSupplementRecords(currentPage, pageSize, searchTerm || undefined, todayOnly)
                        : await financialService.getUnpaidRecords(currentPage, pageSize, searchTerm || undefined, todayOnly);
                } else {
                    data = await financialService.getInvoices(
                        currentPage,
                        pageSize,
                        searchTerm || undefined,
                        'PAID',
                        todayOnly,
                        todayOnly ? undefined : 3
                    );
                }
                if (data && Array.isArray(data.content)) {
                    setInvoices(activeTab === 'history' ? data.content.filter((invoice) => invoice.paymentStatus === 'PAID') : data.content);
                    setTotalPages(data.totalPages);
                } else {
                    setInvoices([]);
                    setTotalPages(0);
                }
            } catch (error) {
                console.error('Failed to fetch invoices:', error);
                setInvoices([]);
            } finally {
                setLoading(false);
            }
        };

        const id = setTimeout(fetchInvoices, 400);
        return () => clearTimeout(id);
    }, [activeTab, feeType, searchTerm, currentPage, refreshTick, dateFilter]);

    // Reset page when filters change
    useEffect(() => { setCurrentPage(1); }, [activeTab, feeType, searchTerm, dateFilter]);

    const handleTabChange = (tab: Tab) => {
        setActiveTab(tab);
        setFeeType('initial');
        setSearchInput('');
        setSearchTerm('');
        setInsuranceFilter('');
        setDateFilter('today');
    };

    const handleSearch = () => {
        const keyword = searchInput.trim();
        setSearchInput(keyword);
        setSearchTerm(keyword);
        setCurrentPage(1);
    };
    const handleRefresh = useCallback(() => setRefreshTick(t => t + 1), []);

    const filteredInvoices = insuranceFilter === 'BHYT'
        ? invoices.filter(inv => inv.healthInsuranceNumber)
        : insuranceFilter === 'NONE'
        ? invoices.filter(inv => !inv.healthInsuranceNumber)
        : invoices;

    return (
        <div className="p-8 bg-gray-50 min-h-screen font-sans text-black">
            <h1 className="text-2xl font-bold mb-6 text-black border-l-4 border-black pl-3 uppercase">
                {activeTab === 'pending' ? 'CHỜ THANH TOÁN' : 'LỊCH SỬ GIAO DỊCH'}
            </h1>

            <div className="bg-white border border-gray-200 shadow-sm">
                {/* Tabs */}
                <div className="flex border-b border-gray-200">
                    <button
                        onClick={() => handleTabChange('pending')}
                        className={`px-8 py-3 text-sm font-bold uppercase transition-colors border-r border-gray-200 ${
                            activeTab === 'pending'
                                ? 'bg-black text-white'
                                : 'bg-white text-black hover:bg-gray-50'
                        }`}
                    >
                        Chờ Thanh Toán
                    </button>
                    <button
                        onClick={() => handleTabChange('history')}
                        className={`px-8 py-3 text-sm font-bold uppercase transition-colors border-r border-gray-200 ${
                            activeTab === 'history'
                                ? 'bg-black text-white'
                                : 'bg-white text-black hover:bg-gray-50'
                        }`}
                    >
                        Lịch Sử Giao Dịch
                    </button>
                    {/* Spacer + Refresh */}
                    <div className="flex-1 flex items-center justify-end px-4">
                        <button
                            onClick={handleRefresh}
                            disabled={loading}
                            title="Làm mới dữ liệu"
                            className="flex items-center gap-1.5 px-3 py-1.5 border border-gray-300 bg-white hover:bg-gray-50 text-gray-700 text-xs font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
                            Làm mới
                        </button>
                    </div>
                </div>

                {/* Fee Type Segmented Control — only in pending tab */}
                {activeTab === 'pending' && (
                    <div className="flex items-center gap-2 px-6 py-3 border-b border-gray-100 bg-gray-50">
                        <span className="text-xs font-semibold text-gray-500 uppercase tracking-wide mr-1">Loại phí:</span>
                        <button
                            onClick={() => setFeeType('initial')}
                            className={`px-4 py-1.5 text-sm font-semibold border transition-colors ${
                                feeType === 'initial'
                                    ? 'bg-black text-white border-black'
                                    : 'bg-white text-black border-gray-300 hover:bg-gray-100'
                            }`}
                        >
                            Phí khám ban đầu
                        </button>
                        <button
                            onClick={() => setFeeType('supplement')}
                            className={`px-4 py-1.5 text-sm font-semibold border transition-colors ${
                                feeType === 'supplement'
                                    ? 'bg-black text-white border-black'
                                    : 'bg-white text-black border-gray-300 hover:bg-gray-100'
                            }`}
                        >
                            Thanh toán bổ sung
                        </button>
                    </div>
                )}

                {/* Search Bar */}
                <div className="flex items-center gap-3 px-6 py-4 border-b border-gray-200">
                    <span className="text-sm font-semibold text-gray-700 whitespace-nowrap flex items-center gap-1">
                        🔍 Tìm kiếm:
                    </span>
                    <input
                        type="text"
                        placeholder="Nhập tên bệnh nhân..."
                        className="border border-gray-300 px-3 py-2 text-sm w-72 focus:outline-none focus:border-black"
                        value={searchInput}
                        onChange={(e) => setSearchInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                    />
                    <select
                        className="border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-black bg-white min-w-[120px]"
                        value={insuranceFilter}
                        onChange={(e) => setInsuranceFilter(e.target.value)}
                    >
                        <option value="">Bảo hiểm</option>
                        <option value="BHYT">Có BHYT</option>
                        <option value="NONE">Dịch vụ</option>
                    </select>
                    <select
                        className="border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-black bg-white min-w-[150px]"
                        value={dateFilter}
                        onChange={(e) => setDateFilter(e.target.value as DateFilter)}
                    >
                        <option value="today">Hôm nay</option>
                        <option value="recent">{activeTab === 'history' ? '3 ngày gần nhất' : 'Tất cả'}</option>
                    </select>
                    <button
                        onClick={handleSearch}
                        className="px-5 py-2 bg-white border border-gray-400 text-sm font-bold hover:bg-gray-100 transition-colors"
                    >
                        Tìm
                    </button>
                </div>

                {/* Table */}
                {loading ? (
                    <div className="flex items-center justify-center py-16 text-gray-500 text-sm font-medium">
                        Đang tải dữ liệu...
                    </div>
                ) : (
                    <>
                        <div className="overflow-x-auto">
                            <table className="min-w-full text-sm text-left border-collapse">
                                <thead>
                                    <tr className="border-b-2 border-gray-300 bg-white">
                                        <th className="px-4 py-3 font-bold text-black border border-gray-200 text-center w-14">STT</th>
                                        <th className="px-4 py-3 font-bold text-black border border-gray-200">Ngày GD</th>
                                        <th className="px-4 py-3 font-bold text-black border border-gray-200">Tên Bệnh nhân</th>
                                        <th className="px-4 py-3 font-bold text-black border border-gray-200">Loại Bảo hiểm</th>
                                        <th className="px-4 py-3 font-bold text-black border border-gray-200">Hạng mục khám</th>
                                        <th className="px-4 py-3 font-bold text-black border border-gray-200 text-right">Tổng tiền</th>
                                        <th className="px-4 py-3 font-bold text-black border border-gray-200 text-center">Trạng thái</th>
                                        <th className="px-4 py-3 font-bold text-black border border-gray-200 text-center">Hành động</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredInvoices.length === 0 ? (
                                        <tr>
                                            <td colSpan={8} className="border border-gray-200 px-4 py-10 text-center text-slate-600">
                                                Không có dữ liệu
                                            </td>
                                        </tr>
                                    ) : (
                                        filteredInvoices.map((inv, index) => (
                                            <tr key={inv.invoiceId} className="hover:bg-gray-50 border-b border-gray-200">
                                                <td className="px-4 py-3 border border-gray-200 text-center text-gray-500">
                                                    {(currentPage - 1) * pageSize + index + 1}
                                                </td>
                                                <td className="px-4 py-3 border border-gray-200 text-gray-700">
                                                    {inv.paidAt || inv.createdAt
                                                        ? new Date(inv.paidAt || inv.createdAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
                                                        : '-'}
                                                </td>
                                                <td className="px-4 py-3 border border-gray-200 font-bold text-black">
                                                    {inv.patientName || 'N/A'}
                                                </td>
                                                <td className="px-4 py-3 border border-gray-200 text-gray-700">
                                                    {inv.healthInsuranceNumber ? 'BHYT (Đúng tuyến)' : 'Tự nguyện'}
                                                </td>
                                                <td className="px-4 py-3 border border-gray-200 text-gray-700 max-w-[180px] whitespace-normal">
                                                    {inv.serviceSummary || inv.departmentName || 'N/A'}
                                                </td>
                                                <td className="px-4 py-3 border border-gray-200 font-semibold text-right text-black">
                                                    {inv.totalServiceFee?.toLocaleString('vi-VN')}đ
                                                </td>
                                                <td className="px-4 py-3 border border-gray-200 text-center">
                                                    {statusBadge(inv.paymentStatus)}
                                                </td>
                                                <td className="px-4 py-3 border border-gray-200 text-center">
                                                    <button
                                                        onClick={() => router.push(
                                                            `/dashboard/cashier/invoices/${feeType === 'supplement' ? inv.recordId : inv.invoiceId}` +
                                                            (activeTab === 'history' ? '?mode=history' : feeType === 'supplement' ? '?type=supplement' : '')
                                                        )}
                                                        className="text-sm font-semibold text-black underline hover:text-gray-600 transition-colors"
                                                    >
                                                        {activeTab === 'pending' ? 'Thanh Toán' : 'Chi Tiết'}
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>

                        {/* Pagination */}
                        <div className="flex justify-end items-center gap-1 px-6 py-4 border-t border-gray-200">
                            <button
                                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                                disabled={currentPage <= 1}
                                className="w-8 h-8 flex items-center justify-center border border-gray-300 text-sm font-medium hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                            >
                                &lt;
                            </button>
                            {Array.from({ length: Math.min(totalPages, 5) }).map((_, i) => (
                                <button
                                    key={i}
                                    onClick={() => setCurrentPage(i + 1)}
                                    className={`w-8 h-8 flex items-center justify-center border text-sm font-medium transition-colors ${
                                        currentPage === i + 1
                                            ? 'bg-black text-white border-black'
                                            : 'bg-white text-black border-gray-300 hover:bg-gray-50'
                                    }`}
                                >
                                    {i + 1}
                                </button>
                            ))}
                            {totalPages > 5 && (
                                <span className="flex h-8 w-8 items-center justify-center text-sm text-slate-500">...</span>
                            )}
                            <button
                                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                                disabled={currentPage >= totalPages || totalPages === 0}
                                className="w-8 h-8 flex items-center justify-center border border-gray-300 text-sm font-medium hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                            >
                                &gt;
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
