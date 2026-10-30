'use client';

import { useCallback, useEffect, useState } from 'react';
import { paymentHistoryService } from '@/services/payment-history.service';
import { FinancialResponse, FinancialDetailResponse } from '@/types/financial.types';
import { Loader2, Search, FileText, Receipt, CheckCircle, XCircle, Clock, X, User, Shield, Calendar, Calculator, Printer, type LucideIcon } from 'lucide-react';
import toast from 'react-hot-toast';
import { extractErrorMessage } from '@/lib/errorHandler';
import { SERVICE_ORDER_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const HISTORY_FETCH_SIZE = 200;

const PAYMENT_STATUS_LABELS: Record<string, string> = {
  SUCCESS: 'THÀNH CÔNG',
  PAID: 'THÀNH CÔNG',
  UNPAID: 'CHƯA THANH TOÁN',
  FAILED: 'THẤT BẠI',
  PENDING: 'ĐANG XỬ LÝ',
  REFUNDED: 'ĐÃ HOÀN TIỀN',
};

const PAYMENT_STATUS_COLORS: Record<string, string> = {
  SUCCESS: 'bg-green-100 text-green-700 border-green-300',
  PAID: 'bg-green-100 text-green-700 border-green-300',
  UNPAID: 'bg-gray-100 text-gray-700 border-gray-300',
  FAILED: 'bg-red-100 text-red-700 border-red-300',
  PENDING: 'bg-yellow-100 text-yellow-700 border-yellow-300',
  REFUNDED: 'bg-blue-100 text-blue-700 border-blue-300',
};

const PAYMENT_STATUS_ICONS: Record<string, LucideIcon> = {
  SUCCESS: CheckCircle,
  PAID: CheckCircle,
  UNPAID: Receipt,
  FAILED: XCircle,
  PENDING: Clock,
  REFUNDED: Receipt,
};

const RECEIPT_STATUS_LABELS: Record<string, string> = {
  NONE: 'CHƯA PHÁT HÀNH',
  PROCESSING: 'ĐANG PHÁT HÀNH',
  ISSUED: 'ĐÃ PHÁT HÀNH',
  FAILED: 'LỖI PHÁT HÀNH',
};

const RECEIPT_STATUS_COLORS: Record<string, string> = {
  NONE: 'bg-gray-100 text-gray-700 border-gray-300',
  PROCESSING: 'bg-yellow-100 text-yellow-700 border-yellow-300',
  ISSUED: 'bg-green-100 text-green-700 border-green-300',
  FAILED: 'bg-red-100 text-red-700 border-red-300',
};

const RECEIPT_STATUS_ICONS: Record<string, LucideIcon> = {
  NONE: Receipt,
  PROCESSING: Clock,
  ISSUED: CheckCircle,
  FAILED: XCircle,
};

export default function PaymentHistoryPage() {
  const [loading, setLoading] = useState(true);
  const [paymentHistory, setPaymentHistory] = useState<FinancialResponse[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedInvoice, setSelectedInvoice] = useState<FinancialDetailResponse | null>(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [printingInvoiceId, setPrintingInvoiceId] = useState<string | null>(null);

  const isPaidStatus = (status?: string | null) => status === 'SUCCESS' || status === 'PAID';

  const getNormalizedReceiptStatus = (receiptStatus?: string | null) => receiptStatus || 'NONE';

  const getDisplayStatus = (item: FinancialResponse) => {
    if (isPaidStatus(item.paymentStatus)) {
      const receiptStatus = getNormalizedReceiptStatus(item.receiptStatus);
      return {
        label: RECEIPT_STATUS_LABELS[receiptStatus] || formatVietnameseLabel(receiptStatus, RECEIPT_STATUS_LABELS).toLocaleUpperCase('vi-VN'),
        color: RECEIPT_STATUS_COLORS[receiptStatus] || 'bg-gray-100 text-gray-600 border-gray-300',
        Icon: RECEIPT_STATUS_ICONS[receiptStatus] || Receipt,
      };
    }

    return {
      label: PAYMENT_STATUS_LABELS[item.paymentStatus] || formatVietnameseLabel(item.paymentStatus, PAYMENT_STATUS_LABELS).toLocaleUpperCase('vi-VN'),
      color: PAYMENT_STATUS_COLORS[item.paymentStatus] || 'bg-gray-100 text-gray-600 border-gray-300',
      Icon: PAYMENT_STATUS_ICONS[item.paymentStatus] || Clock,
    };
  };

  const getRoomSummary = (invoice: FinancialDetailResponse | null) => {
    if (!invoice) return '';
    if (invoice.roomName) {
      return invoice.roomName;
    }
    const rooms = [...new Set(invoice.services.map((service) => service.assignedRoomName).filter(Boolean))];
    if (rooms.length > 0) {
      return rooms.join(', ');
    }
    return '';
  };

  const getListServiceSummary = (item: FinancialResponse) => {
    if (item.serviceSummary?.trim()) {
      return item.serviceSummary;
    }
    if (item.departmentName?.trim()) {
      return item.departmentName;
    }
    return 'Dịch vụ khám';
  };

  const fetchPaymentHistory = useCallback(async () => {
    setLoading(true);
    try {
      const response = await paymentHistoryService.getPaymentHistory({
        page: 1,
        size: HISTORY_FETCH_SIZE,
        paymentStatus: statusFilter || undefined,
      });

      setPaymentHistory(response.content ?? []);
    } catch (error) {
      console.error('Failed to fetch payment history:', error);
      toast.error('Không thể tải lịch sử thanh toán. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchPaymentHistory();
  }, [fetchPaymentHistory]);

  const normalizedKeyword = keyword.trim().toLowerCase();
  const filteredPaymentHistory = paymentHistory.filter((item) => {
    if (!normalizedKeyword) {
      return true;
    }

    const searchableText = [
      item.invoiceId,
      item.appointmentId,
      item.paymentMethod,
      item.departmentName,
      item.serviceSummary,
      getListServiceSummary(item),
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();

    return searchableText.includes(normalizedKeyword);
  });

  const totalCount = filteredPaymentHistory.length;
  const totalPages = Math.ceil(totalCount / pageSize);
  const pageStart = currentPage * pageSize;
  const paginatedPaymentHistory = filteredPaymentHistory.slice(pageStart, pageStart + pageSize);

  useEffect(() => {
    if (totalPages === 0 && currentPage !== 0) {
      setCurrentPage(0);
      return;
    }

    if (totalPages > 0 && currentPage > totalPages - 1) {
      setCurrentPage(totalPages - 1);
    }
  }, [currentPage, totalPages]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    setKeyword(searchInput);
  };

  const handleStatusFilter = (newStatus: string) => {
    setStatusFilter(newStatus);
    setCurrentPage(0);
  };

  const handleViewDetail = async (invoiceId: string) => {
    setDetailLoading(true);
    setShowDetailModal(true);
    setSelectedInvoice(null);
    try {
      const detail = await paymentHistoryService.getInvoiceDetail(invoiceId);
      setSelectedInvoice(detail);
    } catch (error) {
      console.error('Failed to fetch invoice detail:', error);
      toast.error('Không thể tải chi tiết hóa đơn.');
      setShowDetailModal(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const openEinvoiceUrl = (url: string) => {
    const openedWindow = window.open(url, '_blank');
    if (!openedWindow) {
      toast.error('Trình duyệt đã chặn cửa sổ hóa đơn điện tử. Hãy cho phép popup rồi thử lại.');
      return false;
    }
    openedWindow.opener = null;
    return true;
  };

  const syncReceiptInfo = (invoice: FinancialDetailResponse) => {
    setSelectedInvoice(invoice);
    setPaymentHistory((items) =>
      items.map((item) =>
        item.invoiceId === invoice.invoiceId
          ? {
              ...item,
              receiptNumber: invoice.receiptNumber,
              receiptStatus: invoice.receiptStatus,
              receiptIssuedAt: invoice.receiptIssuedAt,
            }
          : item
      )
    );
  };

  const handlePrintElectronicInvoice = async (invoice: FinancialDetailResponse) => {
    if (!isPaidStatus(invoice.paymentStatus)) {
      toast.error('Chỉ có thể in hóa đơn điện tử cho hóa đơn đã thanh toán.');
      return true;
    }

    const receiptStatus = getNormalizedReceiptStatus(invoice.receiptStatus);
    if (receiptStatus === 'PROCESSING') {
      toast.error('Hóa đơn điện tử đang được xử lý. Vui lòng thử lại sau.');
      return true;
    }

    if (receiptStatus === 'ISSUED') {
      if (invoice.eInvoiceUrl) {
        openEinvoiceUrl(invoice.eInvoiceUrl);
      } else {
        toast.error('Hóa đơn điện tử đã phát hành nhưng chưa có link file.');
      }
      return true;
    }

    let printWindow: Window | null = null;
    try {
      setPrintingInvoiceId(invoice.invoiceId);
      printWindow = window.open('', '_blank');
      if (!printWindow) {
        toast.error('Trình duyệt đã chặn cửa sổ hóa đơn điện tử. Hãy cho phép popup rồi thử lại.');
        return true;
      }
      printWindow.opener = null;

      printWindow.document.write('<p style="font-family: Arial, sans-serif; padding: 24px;">Đang chuẩn bị hóa đơn điện tử...</p>');
      const issuedInvoice = await paymentHistoryService.issueReceipt(invoice.invoiceId);
      syncReceiptInfo(issuedInvoice);

      if (issuedInvoice.eInvoiceUrl) {
        printWindow.location.href = issuedInvoice.eInvoiceUrl;
        toast.success('Đang mở hóa đơn điện tử.');
      } else {
        printWindow.close();
        toast.error('Chưa có link hóa đơn điện tử để in.');
      }
    } catch (error) {
      try {
        const refreshedInvoice = await paymentHistoryService.getInvoiceDetail(invoice.invoiceId);
        syncReceiptInfo(refreshedInvoice);
        if (getNormalizedReceiptStatus(refreshedInvoice.receiptStatus) === 'ISSUED' && refreshedInvoice.eInvoiceUrl) {
          if (printWindow) {
            printWindow.location.href = refreshedInvoice.eInvoiceUrl;
          } else {
            openEinvoiceUrl(refreshedInvoice.eInvoiceUrl);
          }
          return true;
        }
      } catch {
        // Keep the original issuance error below.
      }
      printWindow?.close();
      console.error('Failed to open electronic invoice:', error);
      toast.error(extractErrorMessage(error));
    } finally {
      setPrintingInvoiceId(null);
    }

    return true;
  };

  const handlePrintInvoice = async (invoice: FinancialDetailResponse) => {
    const handledByElectronicInvoice = await handlePrintElectronicInvoice(invoice);
    if (handledByElectronicInvoice) {
      return;
    }

    const roomSummary = getRoomSummary(invoice);
    const iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    document.body.appendChild(iframe);
    const pri = iframe.contentWindow;
    if (!pri) return;

    pri.document.open();
    pri.document.write(`
      <html>
        <head>
          <title>In Hóa Đơn</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 40px; line-height: 1.6; color: #000; }
            .header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #000; padding-bottom: 20px; }
            .title { font-size: 28px; font-weight: bold; text-transform: uppercase; margin-bottom: 5px; }
            .subtitle { font-size: 16px; color: #555; }
            .info-grid { display: flex; justify-content: space-between; margin-bottom: 30px; }
            .info-item { margin-bottom: 10px; }
            .info-label { font-weight: bold; font-size: 14px; text-transform: uppercase; color: #333; }
            .table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
            .table th, .table td { border: 1px solid #000; padding: 12px; text-align: left; }
            .table th { background: #f5f5f5; font-weight: bold; text-transform: uppercase; font-size: 14px; }
            .text-right { text-align: right; }
            .text-center { text-align: center; }
            .summary { margin-top: 30px; text-align: right; }
            .summary-item { display: flex; justify-content: flex-end; margin-bottom: 10px; font-size: 16px; }
            .summary-item .label { width: 200px; }
            .summary-item .value { font-weight: bold; width: 150px; }
            .summary-total { border-top: 2px solid #000; margin-top: 15px; padding-top: 15px; font-size: 20px; font-weight: bold; }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="title">Hóa Đơn ${invoice.invoiceId}</div>
            <div class="subtitle">Phòng Khám Đa Khoa</div>
          </div>
          <div class="info-grid">
            <div>
               <div class="info-item"><span class="info-label">Mã hóa đơn: </span> ${invoice.invoiceId}</div>
               <div class="info-item"><span class="info-label">Ngày: </span> ${new Date(invoice.createdAt).toLocaleString('vi-VN')}</div>
               <div class="info-item"><span class="info-label">Khách hàng: </span> ${invoice.patientName}</div>
            </div>
            <div>
               <div class="info-item"><span class="info-label">SĐT: </span> ${invoice.patientPhone ?? ''}</div>
               <div class="info-item"><span class="info-label">Khoa: </span> ${invoice.departmentName || ''}</div>
               <div class="info-item"><span class="info-label">Bác sĩ: </span> ${invoice.doctorName}</div>
               <div class="info-item"><span class="info-label">Phòng: </span> ${roomSummary}</div>
            </div>
          </div>
          <table class="table">
             <thead>
               <tr>
                 <th class="text-center" style="width: 5%">STT</th>
                 <th style="width: 22%">Dịch Vụ</th>
                 <th style="width: 15%">Phòng</th>
                 <th class="text-center" style="width: 7%">ĐVT</th>
                 <th class="text-center" style="width: 5%">SL</th>
                 <th class="text-right" style="width: 12%">Đơn Giá</th>
                 <th class="text-right" style="width: 12%">Thành Tiền</th>
                 <th class="text-right" style="width: 10%">Quỹ BHYT</th>
                 <th class="text-right" style="width: 12%">Khách Trả</th>
               </tr>
             </thead>
             <tbody>
               ${invoice.services.map((service, index) => `
                 <tr>
                   <td class="text-center">${index + 1}</td>
                   <td>${service.serviceName}</td>
                   <td>${service.assignedRoomName || '—'}</td>
                   <td class="text-center">Lần</td>
                   <td class="text-center">1</td>
                   <td class="text-right">${service.price.toLocaleString('vi-VN')} đ</td>
                   <td class="text-right">${service.price.toLocaleString('vi-VN')} đ</td>
                   <td class="text-right">0 đ</td>
                   <td class="text-right">${service.price.toLocaleString('vi-VN')} đ</td>
                 </tr>
               `).join('')}
             </tbody>
          </table>
          <div class="summary">
            <div class="summary-item"><div class="label">Tổng phí dịch vụ:</div><div class="value">${invoice.totalServiceFee.toLocaleString('vi-VN')} đ</div></div>
            <div class="summary-item" style="color: green;"><div class="label">BHYT chi trả:</div><div class="value">-${invoice.insurancePaid.toLocaleString('vi-VN')} đ</div></div>
            <div class="summary-item summary-total"><div class="label">Khách trả:</div><div class="value">${invoice.customerPaid.toLocaleString('vi-VN')} đ</div></div>
          </div>
        </body>
      </html>
    `);
    pri.document.close();
    setTimeout(() => {
      pri.focus();
      pri.print();
      document.body.removeChild(iframe);
    }, 250);
  };

  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize);
    setCurrentPage(0);
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleDateString('vi-VN');
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  return (
    <div className="min-h-full bg-gray-50 p-6 text-black">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-[#1a2b3c] mb-2">Thanh toán & Hóa đơn</h1>
          <p className="text-sm text-gray-500">Xem lịch sử thanh toán và chi tiết hóa đơn của bạn</p>
        </div>

        {loading ? (
          <div className="flex items-center justify-center gap-2 py-32 text-slate-600">
            <Loader2 className="animate-spin" size={22} />
            <span>Đang tải lịch sử thanh toán...</span>
          </div>
        ) : (
          <>
            {/* Filters */}
            <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 mb-6">
              <div className="flex flex-col lg:flex-row gap-4 items-start lg:items-center">
                {/* Search */}
                <form onSubmit={handleSearch} className="flex gap-2 w-full lg:flex-1">
                  <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
                    <input
                      type="text"
                      placeholder="Tìm theo mã hóa đơn, mã lịch hẹn, khoa khám..."
                      value={searchInput}
                      onChange={(e) => setSearchInput(e.target.value)}
                      className="pl-10 pr-4 py-2 w-full text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#8B5CF6] focus:border-transparent"
                    />
                  </div>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-[#8B5CF6] text-white rounded-lg hover:bg-[#7C3AED] transition-colors text-sm font-medium whitespace-nowrap"
                  >
                    Tìm kiếm
                  </button>
                </form>

                {/* Status Filter */}
                <div className="flex items-center gap-2 w-full lg:w-auto">
                  <label className="text-sm text-gray-600 whitespace-nowrap">Trạng thái:</label>
                  <select
                    value={statusFilter}
                    onChange={(e) => handleStatusFilter(e.target.value)}
                    className="px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#8B5CF6] flex-1 lg:w-40"
                  >
                    <option value="">Tất cả</option>
                    <option value="PAID">Thành công</option>
                    <option value="PENDING">Đang xử lý</option>
                    <option value="UNPAID">Chưa thanh toán</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Results Count */}
            {totalCount > 0 && (
              <div className="mb-4 text-sm text-gray-500">
                Hiển thị <span className="font-medium">{Math.min((currentPage + 1) * pageSize, totalCount)}</span> /{' '}
                <span className="font-medium">{totalCount}</span> kết quả
              </div>
            )}

            {/* Payment History Table */}
            <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
              {filteredPaymentHistory.length === 0 ? (
                <div className="text-center py-16">
                  <Receipt className="mx-auto mb-3 h-12 w-12 text-slate-400" />
                  <p className="text-base text-slate-600">Không có lịch sử thanh toán nào</p>
                  <p className="mt-1 text-sm text-slate-500">
                    {!keyword && !statusFilter
                      ? 'Lịch sử thanh toán sẽ hiển thị ở đây sau khi bạn thanh toán dịch vụ.'
                      : 'Không tìm thấy kết quả phù hợp.'}
                  </p>
                </div>
              ) : (
                <>
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-gray-200 bg-gray-50">
                          <th className="text-left py-4 px-6 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                            STT
                          </th>
                          <th className="text-left py-4 px-6 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                            Mã Phiếu thu
                          </th>
                          <th className="text-left py-4 px-6 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                            Ngày
                          </th>
                          <th className="text-left py-4 px-6 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                            Dịch vụ
                          </th>
                          <th className="text-right py-4 px-6 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                            Số tiền
                          </th>
                          <th className="text-center py-4 px-6 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                            Trạng thái
                          </th>
                          <th className="text-center py-4 px-6 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                            Thao tác
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {paginatedPaymentHistory.map((item, index) => {
                          const statusDisplay = getDisplayStatus(item);

                          return (
                            <tr key={item.invoiceId} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                              <td className="py-4 px-6 text-sm text-gray-600">{index + 1 + currentPage * pageSize}</td>
                              <td className="py-4 px-6 text-sm text-gray-900 font-mono font-medium">
                                {item.receiptNumber || item.invoiceId}
                              </td>
                              <td className="py-4 px-6 text-sm text-gray-700">{formatDate(item.paidAt || item.createdAt)}</td>
                              <td className="py-4 px-6 text-sm text-gray-700 max-w-xs truncate">
                                {getListServiceSummary(item)}
                              </td>
                              <td className="py-4 px-6 text-sm text-gray-900 font-semibold text-right">
                                {formatCurrency(item.customerPaid)}
                              </td>
                              <td className="py-4 px-6 text-center">
                                <span
                                  className={`inline-flex items-center gap-1 px-3 py-1 text-xs font-semibold rounded-full border ${statusDisplay.color}`}
                                >
                                  <statusDisplay.Icon className="w-3 h-3" />
                                  {statusDisplay.label}
                                </span>
                              </td>
                              <td className="py-4 px-6 text-center">
                                <button
                                  onClick={() => handleViewDetail(item.invoiceId)}
                                  className="inline-flex items-center gap-1 px-3 py-1.5 text-sm text-[#8B5CF6] hover:text-[#7C3AED] border border-[#8B5CF6] hover:border-[#7C3AED] rounded-lg transition-colors"
                                >
                                  <FileText className="w-4 h-4" />
                                  <span className="hidden sm:inline">Xem chi tiết</span>
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>

                  {/* Pagination */}
                  {totalPages > 1 && (
                    <div className="mt-6 p-4 border-t border-gray-200 flex flex-col sm:flex-row justify-between items-center gap-4">
                      {/* Page Size Selector */}
                      <div className="flex items-center gap-2">
                        <span className="text-sm text-gray-500">Hiển thị:</span>
                        <select
                          value={pageSize}
                          onChange={(e) => handlePageSizeChange(Number(e.target.value))}
                          className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#8B5CF6]"
                        >
                          <option value={5}>5</option>
                          <option value={10}>10</option>
                          <option value={20}>20</option>
                          <option value={50}>50</option>
                        </select>
                        <span className="text-sm text-gray-500">mỗi trang</span>
                      </div>

                      {/* Page Navigation */}
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                          disabled={currentPage === 0}
                          className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          Trang trước
                        </button>

                        <div className="flex items-center gap-1">
                          {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                            let pageNum;
                            if (totalPages <= 5) {
                              pageNum = i + 1;
                            } else if (currentPage < 3) {
                              pageNum = i + 1;
                            } else if (currentPage > totalPages - 3) {
                              pageNum = totalPages - 4 + i;
                            } else {
                              pageNum = currentPage - 1 + i;
                            }

                            return (
                              <button
                                key={pageNum}
                                onClick={() => setCurrentPage(pageNum - 1)}
                                className={`w-8 h-8 text-sm rounded-lg transition-colors ${
                                  currentPage === pageNum - 1
                                    ? 'bg-[#8B5CF6] text-white'
                                    : 'border border-gray-300 hover:bg-gray-50'
                                }`}
                              >
                                {pageNum}
                              </button>
                            );
                          })}
                        </div>

                        <button
                          onClick={() => setCurrentPage((prev) => Math.min(totalPages - 1, prev + 1))}
                          disabled={currentPage === totalPages - 1}
                          className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          Trang sau
                        </button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </>
        )}

        {/* Invoice Detail Modal - UC-27 */}
        {showDetailModal && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
              {/* Header with Title and Close Button */}
              <div className="sticky top-0 bg-gradient-to-r from-[#8B5CF6] to-[#7C3AED] text-white rounded-t-xl px-6 py-4">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <h2 className="text-xl font-bold tracking-wide">
                      CHI TIẾT HÓA ĐƠN
                    </h2>
                    <p className="text-sm text-white/80 mt-1 font-mono">
                      Mã HD: {selectedInvoice?.invoiceId || '---'}
                    </p>
                  </div>
                  <button
                    onClick={() => setShowDetailModal(false)}
                    className="flex items-center justify-center w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 transition-colors"
                    title="Đóng"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              </div>

              <div className="p-6">
                {detailLoading ? (
                  <div className="flex items-center justify-center gap-2 py-16 text-slate-600">
                    <Loader2 className="animate-spin" size={22} />
                    <span>Đang tải chi tiết...</span>
                  </div>
                ) : selectedInvoice ? (
                  <div className="space-y-6">
                    {(() => {
                      const receiptStatus = getNormalizedReceiptStatus(selectedInvoice.receiptStatus);
                      const ReceiptStatusIcon = RECEIPT_STATUS_ICONS[receiptStatus] || Receipt;
                      const receiptStatusLabel = RECEIPT_STATUS_LABELS[receiptStatus] || formatVietnameseLabel(receiptStatus, RECEIPT_STATUS_LABELS).toLocaleUpperCase('vi-VN');
                      const receiptStatusColor = RECEIPT_STATUS_COLORS[receiptStatus] || 'bg-gray-100 text-gray-600 border-gray-300';

                      return (
                        <div className="bg-gradient-to-br from-amber-50 to-orange-50 rounded-lg p-5 border border-amber-100">
                          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                              <p className="text-xs text-gray-500 uppercase tracking-wide mb-1 font-semibold">
                                Phiếu thu / Hóa đơn điện tử
                              </p>
                              <div className="flex flex-wrap items-center gap-2">
                                <span className="text-base font-bold text-gray-900 font-mono">
                                  {selectedInvoice.receiptNumber || selectedInvoice.invoiceId}
                                </span>
                                <span className={`inline-flex items-center gap-1 px-3 py-1 text-xs font-semibold rounded-full border ${receiptStatusColor}`}>
                                  <ReceiptStatusIcon className="w-3 h-3" />
                                  {receiptStatusLabel}
                                </span>
                              </div>
                            </div>
                            {selectedInvoice.eInvoiceUrl && receiptStatus === 'ISSUED' && (
                              <a
                                href={selectedInvoice.eInvoiceUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-[#8B5CF6] rounded-lg hover:bg-[#7C3AED] transition-colors"
                              >
                                <Receipt className="w-4 h-4" />
                                Mở hóa đơn điện tử
                              </a>
                            )}
                          </div>
                          {selectedInvoice.receiptErrorMessage && receiptStatus === 'FAILED' && (
                            <p className="mt-3 text-sm text-red-600">
                              {selectedInvoice.receiptErrorMessage}
                            </p>
                          )}
                        </div>
                      );
                    })()}

                    {/* BN (Tên bệnh nhân) - Section 1 */}
                    <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-lg p-5 border border-blue-100">
                      <div className="flex items-start gap-3">
                        <div className="bg-blue-100 rounded-full p-2">
                          <User className="w-5 h-5 text-blue-600" />
                        </div>
                        <div className="flex-1">
                          <p className="text-xs text-gray-500 uppercase tracking-wide mb-1 font-semibold">
                            Bệnh nhân (BN)
                          </p>
                          <p className="text-lg font-bold text-gray-900">
                            {selectedInvoice.patientName}
                          </p>
                          {selectedInvoice.patientPhone && (
                            <p className="text-sm text-gray-600 mt-1">
                              SĐT: {selectedInvoice.patientPhone}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Mã BHYT - Section 2 */}
                    <div className="bg-gradient-to-br from-green-50 to-emerald-50 rounded-lg p-5 border border-green-100">
                      <div className="flex items-start gap-3">
                        <div className="bg-green-100 rounded-full p-2">
                          <Shield className="w-5 h-5 text-green-600" />
                        </div>
                        <div className="flex-1">
                          <p className="text-xs text-gray-500 uppercase tracking-wide mb-1 font-semibold">
                            Mã BHYT
                          </p>
                          {selectedInvoice.healthInsuranceNumber ? (
                            <div className="flex items-center gap-2">
                              <p className="text-base font-bold text-gray-900 font-mono">
                                {selectedInvoice.healthInsuranceNumber}
                              </p>
                              <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-semibold bg-green-600 text-white rounded-full">
                                <CheckCircle className="w-3 h-3" />
                                Đã áp dụng
                              </span>
                            </div>
                          ) : (
                            <p className="text-sm text-gray-500 italic">Chưa đăng ký BHYT</p>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Ngày - Section 3 */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                        <div className="flex items-start gap-3">
                          <Calendar className="mt-0.5 h-5 w-5 text-slate-500" />
                          <div className="flex-1">
                            <p className="text-xs text-gray-500 uppercase tracking-wide mb-1 font-semibold">
                              Ngày thanh toán
                            </p>
                            <p className="text-base font-semibold text-gray-900">
                              {selectedInvoice.paidAt
                                ? formatDate(selectedInvoice.paidAt)
                                : formatDate(selectedInvoice.createdAt)}
                            </p>
                            {selectedInvoice.paidAt && (
                              <p className="text-xs text-gray-500 mt-1">
                                {new Date(selectedInvoice.paidAt).toLocaleTimeString('vi-VN')}
                              </p>
                            )}
                          </div>
                        </div>
                      </div>

                      <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                        <div className="flex items-start gap-3">
                          <Clock className="mt-0.5 h-5 w-5 text-slate-500" />
                          <div className="flex-1">
                            <p className="text-xs text-gray-500 uppercase tracking-wide mb-1 font-semibold">
                              Mã Phiếu thu
                            </p>
                            <p className="text-base font-semibold text-gray-900 font-mono">
                              {selectedInvoice.receiptNumber || selectedInvoice.invoiceId}
                            </p>
                            <p className="text-xs text-gray-500 mt-1">
                              {RECEIPT_STATUS_LABELS[getNormalizedReceiptStatus(selectedInvoice.receiptStatus)]}
                            </p>
                          </div>
                        </div>
                      </div>

                      {getRoomSummary(selectedInvoice) && (
                        <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                          <div className="flex items-start gap-3">
                            <FileText className="mt-0.5 h-5 w-5 text-slate-500" />
                            <div className="flex-1">
                              <p className="text-xs text-gray-500 uppercase tracking-wide mb-1 font-semibold">
                                Phòng khám
                              </p>
                              <p className="text-base font-semibold text-gray-900">
                                {getRoomSummary(selectedInvoice)}
                              </p>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Bảng Dịch vụ - Section 4 */}
                    <div>
                      <h3 className="text-sm font-bold text-gray-700 mb-3 uppercase tracking-wide flex items-center gap-2">
                        <FileText className="w-4 h-4" />
                        Bảng Dịch vụ
                      </h3>
                      <div className="border-2 border-gray-200 rounded-lg overflow-hidden">
                        <table className="w-full">
                          <thead className="bg-gradient-to-r from-gray-50 to-gray-100">
                            <tr>
                              <th className="text-left py-3 px-4 text-xs font-bold text-gray-700 uppercase tracking-wider border-b-2 border-gray-200">
                                STT
                              </th>
                              <th className="text-left py-3 px-4 text-xs font-bold text-gray-700 uppercase tracking-wider border-b-2 border-gray-200">
                                Dịch vụ
                              </th>
                              <th className="text-left py-3 px-4 text-xs font-bold text-gray-700 uppercase tracking-wider border-b-2 border-gray-200">
                                Phòng
                              </th>
                              <th className="text-right py-3 px-4 text-xs font-bold text-gray-700 uppercase tracking-wider border-b-2 border-gray-200">
                                Giá
                              </th>
                              <th className="text-center py-3 px-4 text-xs font-bold text-gray-700 uppercase tracking-wider border-b-2 border-gray-200">
                                Trạng thái
                              </th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-gray-200">
                            {selectedInvoice.services.map((service, index) => (
                              <tr key={index} className="hover:bg-gray-50 transition-colors">
                                <td className="py-3 px-4 text-sm text-gray-600 font-medium text-center">
                                  {index + 1}
                                </td>
                                <td className="py-3 px-4 text-sm text-gray-900 font-medium align-top">{service.serviceName}</td>
                                <td className="py-3 px-4 text-sm text-gray-700 align-top">{service.assignedRoomName || '—'}</td>
                                <td className="py-3 px-4 text-sm text-gray-900 font-semibold text-right">
                                  {formatCurrency(service.price)}
                                </td>
                                <td className="py-3 px-4 text-sm text-center">
                                  <span className="px-3 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-700 border border-blue-200">
                                    {formatVietnameseLabel(service.status, SERVICE_ORDER_STATUS_LABELS)}
                                  </span>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>

                    {/* Payment Summary - Financial Breakdown */}
                    <div className="bg-gradient-to-br from-[#8B5CF6]/10 to-[#7C3AED]/10 rounded-xl p-6 border-2 border-[#8B5CF6]/20">
                      <h3 className="text-sm font-bold text-[#8B5CF6] mb-4 uppercase tracking-wide flex items-center gap-2">
                        <Calculator className="w-4 h-4" />
                        Tóm tắt thanh toán
                      </h3>
                      <div className="space-y-3">
                        {/* Tổng cộng */}
                        <div className="flex justify-between items-center py-2 border-b border-gray-300">
                          <span className="text-base font-semibold text-gray-700">Tổng cộng:</span>
                          <span className="text-lg font-bold text-gray-900">
                            {formatCurrency(selectedInvoice.totalServiceFee)}
                          </span>
                        </div>

                        {/* BHYT chi trả */}
                        {selectedInvoice.insurancePaid > 0 && (
                          <div className="flex justify-between items-center py-2 border-b border-gray-300">
                            <div className="flex items-center gap-2">
                              <span className="text-base font-semibold text-green-700">BHYT chi trả:</span>
                              {selectedInvoice.insurancePaid > 0 && selectedInvoice.totalServiceFee > 0 && (
                                <span className="text-xs text-green-600 bg-green-100 px-2 py-0.5 rounded-full font-semibold">
                                  {Math.round((selectedInvoice.insurancePaid / selectedInvoice.totalServiceFee) * 100)}%
                                </span>
                              )}
                            </div>
                            <span className="text-lg font-bold text-green-700">
                              -{formatCurrency(selectedInvoice.insurancePaid)}
                            </span>
                          </div>
                        )}

                        {/* BỆNH NHÂN TRẢ */}
                        <div className="flex justify-between items-center py-3 bg-gradient-to-r from-[#8B5CF6] to-[#7C3AED] -mx-2 px-2 rounded-lg">
                          <span className="text-lg font-bold text-white uppercase tracking-wide">BỆNH NHÂN TRẢ:</span>
                          <span className="text-xl font-bold text-white">
                            {formatCurrency(selectedInvoice.customerPaid)}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex flex-col sm:flex-row gap-3 pt-4 border-t border-gray-200">
                      {/* ĐÓNG Button */}
                      <button
                        onClick={() => setShowDetailModal(false)}
                        className="flex-1 flex items-center justify-center gap-2 px-6 py-3 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-semibold border-2 border-gray-300"
                      >
                        <X className="w-5 h-5" />
                        <span>ĐÓNG</span>
                      </button>

                      {/* TẢI PHIẾU KHÁM (PDF) Button */}
                      {isPaidStatus(selectedInvoice.paymentStatus) && (
                        <button
                          onClick={() => handlePrintInvoice(selectedInvoice)}
                          disabled={printingInvoiceId === selectedInvoice.invoiceId}
                          className="flex-1 flex items-center justify-center gap-2 px-6 py-3 bg-[#1f2937] text-white rounded-lg hover:bg-black transition-all font-semibold border-2 border-[#1f2937] shadow-lg disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          {printingInvoiceId === selectedInvoice.invoiceId ? (
                            <Loader2 className="w-5 h-5 animate-spin" />
                          ) : (
                            <Printer className="w-5 h-5" />
                          )}
                          <span>
                            {printingInvoiceId === selectedInvoice.invoiceId ? 'ĐANG MỞ HÓA ĐƠN' : 'IN HÓA ĐƠN ĐIỆN TỬ'}
                          </span>
                        </button>
                      )}

                      {isPaidStatus(selectedInvoice.paymentStatus) && (
                        <div className="flex-1 flex items-center justify-center px-6 py-3 text-sm text-gray-500 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300">
                          Dữ liệu hóa đơn đã hiển thị đầy đủ trong màn chi tiết.
                        </div>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="py-8 text-center text-slate-600">
                    Không thể tải thông tin hóa đơn
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
