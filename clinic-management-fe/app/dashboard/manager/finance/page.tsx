'use client';

import type { ComponentType } from 'react';
import Link from 'next/link';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { financialService } from '@/services/financial.service';
import { CashHandover, FinancialResponse, ManagerCashierSummary } from '@/types/financial.types';
import {
  Banknote,
  CheckCircle2,
  ClipboardList,
  Loader2,
  RefreshCw,
  Search,
  Send,
  WalletCards,
  XCircle,
} from 'lucide-react';
import { PAYMENT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

type TabKey = 'overview' | 'cashier' | 'handover';
type MetricTone = 'blue' | 'emerald' | 'amber' | 'violet' | 'rose' | 'slate';

const TRANSFER_METHODS = new Set(['QR', 'QR_SEPAY', 'TRANSFER', 'BANK_TRANSFER']);

const tabs: { key: TabKey; label: string }[] = [
  { key: 'overview', label: 'Tổng quan' },
  { key: 'cashier', label: 'Theo thu ngân' },
  { key: 'handover', label: 'Bàn giao' },
];

const metricToneClasses: Record<MetricTone, { card: string; icon: string; value: string }> = {
  blue: { card: 'border-blue-100 bg-blue-50/70', icon: 'text-blue-700', value: 'text-blue-950' },
  emerald: { card: 'border-emerald-100 bg-emerald-50/70', icon: 'text-emerald-700', value: 'text-emerald-950' },
  amber: { card: 'border-amber-100 bg-amber-50/80', icon: 'text-amber-700', value: 'text-amber-950' },
  violet: { card: 'border-violet-100 bg-violet-50/70', icon: 'text-violet-700', value: 'text-violet-950' },
  rose: { card: 'border-rose-100 bg-rose-50/70', icon: 'text-rose-700', value: 'text-rose-950' },
  slate: { card: 'border-slate-200 bg-white', icon: 'text-slate-500', value: 'text-slate-900' },
};

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0);

const getLocalDateKey = (date = new Date()) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

const getMonthStartKey = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
};

const getInvoiceDateKey = (invoice: FinancialResponse) =>
  (invoice.paidAt || invoice.createdAt || '').split('T')[0] ?? '';

const getInvoiceAmount = (invoice: FinancialResponse) =>
  Number(invoice.customerPaid ?? invoice.totalServiceFee ?? 0);

const normalizePaymentMethod = (value?: string | null) => (value || '').trim().toUpperCase();

const isCashMethod = (value?: string | null) => normalizePaymentMethod(value) === 'CASH';

const isTransferMethod = (value?: string | null) => TRANSFER_METHODS.has(normalizePaymentMethod(value));

const methodLabel = (value?: string | null) => {
  const method = normalizePaymentMethod(value);
  if (method === 'CASH') return 'Tiền mặt';
  if (method === 'QR' || method === 'QR_SEPAY') return 'QR';
  if (method === 'TRANSFER' || method === 'BANK_TRANSFER') return 'Chuyển khoản';
  return method || 'Khác';
};

const formatDateTime = (value?: string | null) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

const handoverStatusLabel = (status?: string | null) => {
  switch ((status || '').toUpperCase()) {
    case 'PENDING':
      return 'Chờ xác nhận';
    case 'CONFIRMED':
      return 'Đã xác nhận';
    case 'CANCELLED':
      return 'Đã hủy';
    default:
      return status || '-';
  }
};

const handoverStatusClass = (status?: string | null) => {
  switch ((status || '').toUpperCase()) {
    case 'PENDING':
      return 'bg-amber-50 text-amber-700 ring-amber-200';
    case 'CONFIRMED':
      return 'bg-emerald-50 text-emerald-700 ring-emerald-200';
    case 'CANCELLED':
      return 'bg-rose-50 text-rose-700 ring-rose-200';
    default:
      return 'bg-slate-50 text-slate-700 ring-slate-200';
  }
};

const hasCashierActivity = (cashier: ManagerCashierSummary) => {
  const summary = cashier.summary;
  return (
    Number(summary?.invoiceCount ?? 0) > 0 ||
    Number(summary?.totalCollected ?? 0) > 0 ||
    Number(summary?.cashCollected ?? 0) > 0 ||
    Number(summary?.transferCollected ?? 0) > 0 ||
    Number(summary?.cashRemaining ?? 0) > 0 ||
    (summary?.recentInvoices ?? []).length > 0
  );
};

function MetricCard({
  title,
  value,
  description,
  icon: Icon,
  tone = 'slate',
}: {
  title: string;
  value: string | number;
  description: string;
  icon: ComponentType<{ className?: string }>;
  tone?: MetricTone;
}) {
  const colors = metricToneClasses[tone];
  return (
    <div className={`rounded-lg border p-5 shadow-sm ${colors.card}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase text-slate-500">{title}</p>
          <p className={`mt-2 break-words text-2xl font-bold ${colors.value}`}>{value}</p>
        </div>
        <Icon className={`h-5 w-5 shrink-0 ${colors.icon}`} />
      </div>
      <p className="mt-1 text-xs text-slate-600">{description}</p>
    </div>
  );
}

export default function ManagerFinancePage() {
  const cashierDateRangeTouched = useRef(false);
  const handoverDateRangeTouched = useRef(false);
  const selectedCashierTouched = useRef(false);
  const [activeTab, setActiveTab] = useState<TabKey>('overview');

  const [overviewRows, setOverviewRows] = useState<FinancialResponse[]>([]);
  const [overviewLoading, setOverviewLoading] = useState(true);
  const [overviewError, setOverviewError] = useState<string | null>(null);
  const [fromDate, setFromDate] = useState(getMonthStartKey());
  const [toDate, setToDate] = useState(getLocalDateKey());
  const [keyword, setKeyword] = useState('');
  const [paymentStatus, setPaymentStatus] = useState('PAID');
  const [overviewCashierId, setOverviewCashierId] = useState('');

  const [cashierFromDate, setCashierFromDate] = useState(getLocalDateKey());
  const [cashierToDate, setCashierToDate] = useState(getLocalDateKey());
  const [cashierSummaries, setCashierSummaries] = useState<ManagerCashierSummary[]>([]);
  const [selectedCashierId, setSelectedCashierId] = useState('');
  const [cashierDetail, setCashierDetail] = useState<ManagerCashierSummary | null>(null);
  const [cashierLoading, setCashierLoading] = useState(true);
  const [cashierDetailLoading, setCashierDetailLoading] = useState(false);
  const [cashierError, setCashierError] = useState<string | null>(null);

  const [handoverFromDate, setHandoverFromDate] = useState(getLocalDateKey());
  const [handoverToDate, setHandoverToDate] = useState(getLocalDateKey());
  const [handoverCashierId, setHandoverCashierId] = useState('');
  const [handoverStatus, setHandoverStatus] = useState('');
  const [handovers, setHandovers] = useState<CashHandover[]>([]);
  const [handoverLoading, setHandoverLoading] = useState(true);
  const [handoverError, setHandoverError] = useState<string | null>(null);

  const loadOverview = useCallback(async () => {
    setOverviewLoading(true);
    setOverviewError(null);
    try {
      const response = await financialService.getInvoices(
        1,
        1000,
        keyword.trim() || undefined,
        paymentStatus || undefined
      );
      setOverviewRows(response.content ?? []);
    } catch (fetchError) {
      console.error('Không thể tải dữ liệu tài chính:', fetchError);
      setOverviewRows([]);
      setOverviewError('Không thể tải danh sách hóa đơn.');
    } finally {
      setOverviewLoading(false);
    }
  }, [keyword, paymentStatus]);

  const loadCashierSummaries = useCallback(async () => {
    setCashierLoading(true);
    setCashierError(null);
    try {
      const summaries = await financialService.getManagerCashierSummaries(cashierFromDate, cashierToDate);
      setCashierSummaries(summaries);
    } catch (fetchError) {
      console.error('Không thể tải tổng kết theo thu ngân:', fetchError);
      setCashierSummaries([]);
      setCashierError('Không thể tải danh sách tổng kết theo thu ngân.');
    } finally {
      setCashierLoading(false);
    }
  }, [cashierFromDate, cashierToDate]);

  const loadCashierDetail = useCallback(async () => {
    if (!selectedCashierId) {
      setCashierDetail(null);
      return;
    }

    setCashierDetailLoading(true);
    setCashierError(null);
    try {
      const detail = await financialService.getManagerCashierDaySummary(
        cashierFromDate,
        cashierToDate,
        Number(selectedCashierId)
      );
      setCashierDetail(detail);
    } catch (fetchError) {
      console.error('Không thể tải chi tiết thu ngân:', fetchError);
      setCashierDetail(null);
      setCashierError('Không thể tải chi tiết thu ngân đã chọn.');
    } finally {
      setCashierDetailLoading(false);
    }
  }, [cashierFromDate, cashierToDate, selectedCashierId]);

  const loadHandovers = useCallback(async () => {
    setHandoverLoading(true);
    setHandoverError(null);
    try {
      const rows = await financialService.getManagerCashHandovers(
        handoverFromDate,
        handoverToDate,
        handoverCashierId ? Number(handoverCashierId) : undefined,
        handoverStatus || undefined
      );
      setHandovers(rows);
    } catch (fetchError) {
      console.error('Không thể tải phiếu bàn giao:', fetchError);
      setHandovers([]);
      setHandoverError('Không thể tải danh sách phiếu bàn giao.');
    } finally {
      setHandoverLoading(false);
    }
  }, [handoverCashierId, handoverFromDate, handoverStatus, handoverToDate]);

  useEffect(() => {
    loadOverview();
  }, [loadOverview]);

  useEffect(() => {
    loadCashierSummaries();
  }, [loadCashierSummaries]);

  const cashierOptions = useMemo(() => {
    return [...cashierSummaries].sort((left, right) => {
      const activityDiff = Number(hasCashierActivity(right)) - Number(hasCashierActivity(left));
      if (activityDiff !== 0) return activityDiff;

      const revenueDiff = Number(right.summary?.totalCollected ?? 0) - Number(left.summary?.totalCollected ?? 0);
      if (revenueDiff !== 0) return revenueDiff;

      return left.cashierName.localeCompare(right.cashierName, 'vi');
    });
  }, [cashierSummaries]);

  useEffect(() => {
    if (!cashierSummaries.length) {
      setSelectedCashierId('');
      return;
    }

    const selectedCashier = cashierSummaries.find((cashier) => String(cashier.cashierUserId) === selectedCashierId);
    const defaultCashier = cashierOptions.find(hasCashierActivity) ?? cashierOptions[0];
    const shouldPreferActiveCashier =
      !selectedCashierTouched.current &&
      defaultCashier &&
      hasCashierActivity(defaultCashier) &&
      (!selectedCashier || !hasCashierActivity(selectedCashier));

    if (!selectedCashierId || !selectedCashier || shouldPreferActiveCashier) {
      if (!defaultCashier || String(defaultCashier.cashierUserId) === selectedCashierId) {
        return;
      }
      setSelectedCashierId(String(defaultCashier.cashierUserId));
    }
  }, [cashierOptions, cashierSummaries, selectedCashierId]);

  useEffect(() => {
    loadCashierDetail();
  }, [loadCashierDetail]);

  useEffect(() => {
    loadHandovers();
  }, [loadHandovers]);

  const filteredOverviewRows = useMemo(() => {
    return overviewRows.filter((row) => {
      const dateKey = getInvoiceDateKey(row);
      const byFrom = fromDate ? dateKey >= fromDate : true;
      const byTo = toDate ? dateKey <= toDate : true;
      const selectedCashier = cashierSummaries.find((cashier) => String(cashier.cashierUserId) === overviewCashierId);
      const rowCashierId = String(row.cashierId ?? '');
      const byCashier = !overviewCashierId
        || rowCashierId === overviewCashierId
        || (!!selectedCashier?.cashierStaffId && rowCashierId === selectedCashier.cashierStaffId);
      return byFrom && byTo && byCashier;
    });
  }, [cashierSummaries, fromDate, overviewCashierId, overviewRows, toDate]);

  const getCashierDisplayName = useCallback((cashierId?: string | null) => {
    if (!cashierId) return '-';
    const cashier = cashierSummaries.find(
      (item) => String(item.cashierUserId) === cashierId || item.cashierStaffId === cashierId
    );
    return cashier?.cashierName || cashierId;
  }, [cashierSummaries]);

  const latestOverviewBusinessDate = useMemo(() => {
    return overviewRows
      .map(getInvoiceDateKey)
      .filter(Boolean)
      .sort()
      .at(-1);
  }, [overviewRows]);

  const cashierSummaryHasActivity = useMemo(() => cashierSummaries.some(hasCashierActivity), [cashierSummaries]);

  useEffect(() => {
    if (overviewLoading || !latestOverviewBusinessDate) {
      return;
    }

    const today = getLocalDateKey();
    if (!latestOverviewBusinessDate || latestOverviewBusinessDate === today) {
      return;
    }

    if (
      !cashierDateRangeTouched.current &&
      cashierFromDate === today &&
      cashierToDate === today &&
      !cashierLoading &&
      !cashierSummaryHasActivity
    ) {
      setCashierFromDate(latestOverviewBusinessDate);
      setCashierToDate(latestOverviewBusinessDate);
    }

    if (
      !handoverDateRangeTouched.current &&
      handoverFromDate === today &&
      handoverToDate === today &&
      !handoverLoading &&
      handovers.length === 0
    ) {
      setHandoverFromDate(latestOverviewBusinessDate);
      setHandoverToDate(latestOverviewBusinessDate);
    }
  }, [
    cashierFromDate,
    cashierLoading,
    cashierSummaryHasActivity,
    cashierToDate,
    handoverFromDate,
    handoverLoading,
    handovers.length,
    handoverToDate,
    latestOverviewBusinessDate,
    overviewLoading,
  ]);

  const overviewTotals = useMemo(() => {
    const paidRows = filteredOverviewRows.filter((row) => row.paymentStatus === 'PAID');
    return paidRows.reduce(
      (acc, row) => {
        const amount = getInvoiceAmount(row);
        acc.totalRevenue += amount;
        acc.invoiceCount += 1;
        if (isCashMethod(row.paymentMethod)) {
          acc.cash += amount;
        } else if (isTransferMethod(row.paymentMethod)) {
          acc.transfer += amount;
        } else {
          acc.other += amount;
        }
        return acc;
      },
      { totalRevenue: 0, invoiceCount: 0, cash: 0, transfer: 0, other: 0 }
    );
  }, [filteredOverviewRows]);

  const selectedSummary = cashierDetail?.summary;

  return (
    <div className="min-h-[calc(100vh-4rem)] bg-slate-50 p-6 text-slate-950">
      <div className="mx-auto max-w-7xl space-y-5">
        <div className="flex flex-col justify-between gap-3 md:flex-row md:items-end">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Tài chính</h1>
            <p className="mt-1 text-sm text-slate-600">Quản lý doanh thu, thu ngân và bàn giao tiền mặt.</p>
          </div>
          <div className="flex rounded-lg border bg-white p-1 shadow-sm">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                type="button"
                onClick={() => setActiveTab(tab.key)}
                className={`h-9 rounded-md px-4 text-sm font-semibold transition ${
                  activeTab === tab.key ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-600 hover:bg-blue-50 hover:text-blue-700'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {activeTab === 'overview' && (
          <section className="space-y-5">
            {overviewError && (
              <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{overviewError}</div>
            )}

            <div className="grid grid-cols-1 gap-4 xl:grid-cols-4">
              <MetricCard
                title="Tổng doanh thu"
                value={overviewLoading ? '...' : formatCurrency(overviewTotals.totalRevenue)}
                description="Hóa đơn đã thanh toán trong khoảng ngày"
                icon={WalletCards}
                tone="blue"
              />
              <MetricCard
                title="Số hóa đơn"
                value={overviewLoading ? '...' : overviewTotals.invoiceCount}
                description="Chỉ tính hóa đơn đã thanh toán"
                icon={ClipboardList}
                tone="violet"
              />
              <MetricCard
                title="Tiền mặt"
                value={overviewLoading ? '...' : formatCurrency(overviewTotals.cash)}
                description="Phương thức CASH"
                icon={Banknote}
                tone="emerald"
              />
              <MetricCard
                title="Chuyển khoản / QR"
                value={overviewLoading ? '...' : formatCurrency(overviewTotals.transfer)}
                description="QR, Sepay hoặc chuyển khoản"
                icon={Send}
                tone="amber"
              />
            </div>

            <div className="rounded-lg border bg-white p-4 shadow-sm">
              <div className="grid grid-cols-1 gap-3 md:grid-cols-[1fr_1fr_1fr_1.4fr_1.4fr_auto]">
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Từ ngày</span>
                  <input
                    type="date"
                    className="h-10 w-full rounded-md border px-3"
                    value={fromDate}
                    onChange={(event) => setFromDate(event.target.value)}
                  />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Đến ngày</span>
                  <input
                    type="date"
                    className="h-10 w-full rounded-md border px-3"
                    value={toDate}
                    onChange={(event) => setToDate(event.target.value)}
                  />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Trạng thái</span>
                  <select
                    className="h-10 w-full rounded-md border px-3"
                    value={paymentStatus}
                    onChange={(event) => setPaymentStatus(event.target.value)}
                  >
                    <option value="">Tất cả</option>
                    <option value="PAID">Đã thanh toán</option>
                    <option value="PENDING">Chờ thanh toán</option>
                    <option value="UNPAID">Chưa thanh toán</option>
                  </select>
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Thu ngân</span>
                  <select
                    className="h-10 w-full rounded-md border px-3"
                    value={overviewCashierId}
                    onChange={(event) => setOverviewCashierId(event.target.value)}
                  >
                    <option value="">Tất cả thu ngân</option>
                    {cashierOptions.map((cashier) => (
                      <option key={cashier.cashierUserId} value={cashier.cashierUserId}>
                        {cashier.cashierName}
                        {cashier.cashierStaffId ? ` - ${cashier.cashierStaffId}` : ''}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Mã phiếu / tên BN</span>
                  <div className="relative">
                    <input
                      className="h-10 w-full rounded-md border py-2 pl-9 pr-3"
                      placeholder="Nhập từ khóa..."
                      value={keyword}
                      onChange={(event) => setKeyword(event.target.value)}
                    />
                    <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  </div>
                </label>
                <button
                  type="button"
                  onClick={loadOverview}
                  className="mt-5 inline-flex h-10 items-center justify-center gap-2 rounded-md border px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  <RefreshCw className="h-4 w-4" />
                  Tải lại
                </button>
              </div>
            </div>

            <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[1180px] text-sm">
                  <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                    <tr>
                      <th className="px-4 py-3">STT</th>
                      <th className="px-4 py-3">Mã hóa đơn</th>
                      <th className="px-4 py-3">Thời gian</th>
                      <th className="px-4 py-3">Khách hàng</th>
                      <th className="px-4 py-3">Thu ngân</th>
                      <th className="px-4 py-3">Dịch vụ</th>
                      <th className="px-4 py-3 text-right">Tổng tiền</th>
                      <th className="px-4 py-3">Phương thức</th>
                      <th className="px-4 py-3">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {overviewLoading ? (
                      <tr>
                        <td colSpan={9} className="px-4 py-10 text-center text-slate-500">
                          <Loader2 className="inline h-5 w-5 animate-spin" />
                        </td>
                      </tr>
                    ) : filteredOverviewRows.length === 0 ? (
                      <tr>
                        <td colSpan={9} className="px-4 py-8 text-center text-slate-500">
                          Không có hóa đơn phù hợp.
                        </td>
                      </tr>
                    ) : (
                      filteredOverviewRows.map((row, index) => (
                        <tr key={row.invoiceId} className="hover:bg-slate-50">
                          <td className="px-4 py-3">{index + 1}</td>
                          <td className="px-4 py-3 font-mono text-xs">
                            <Link
                              href={`/dashboard/manager/finance/invoices/${encodeURIComponent(row.invoiceId)}?mode=history`}
                              className="font-semibold text-blue-700 hover:underline"
                              title="Xem chi tiết hóa đơn"
                            >
                              {row.invoiceId}
                            </Link>
                          </td>
                          <td className="px-4 py-3">{formatDateTime(row.paidAt || row.createdAt)}</td>
                          <td className="px-4 py-3 font-medium">{row.patientName || '-'}</td>
                          <td className="px-4 py-3">{getCashierDisplayName(row.cashierId)}</td>
                          <td className="max-w-[320px] px-4 py-3">{row.serviceSummary || row.departmentName || '-'}</td>
                          <td className="px-4 py-3 text-right font-semibold">{formatCurrency(getInvoiceAmount(row))}</td>
                          <td className="px-4 py-3">{methodLabel(row.paymentMethod)}</td>
                          <td className="px-4 py-3">
                            <span className="rounded-full bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">
                              {formatVietnameseLabel(row.paymentStatus, PAYMENT_STATUS_LABELS)}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}

        {activeTab === 'cashier' && (
          <section className="space-y-5">
            {cashierError && (
              <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{cashierError}</div>
            )}

            <div className="rounded-lg border bg-white p-4 shadow-sm">
              <div className="grid grid-cols-1 gap-3 md:grid-cols-[1fr_1fr_2fr_auto]">
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Từ ngày</span>
                  <input
                    type="date"
                    className="h-10 w-full rounded-md border px-3"
                    value={cashierFromDate}
                    onChange={(event) => {
                      cashierDateRangeTouched.current = true;
                      setCashierFromDate(event.target.value);
                    }}
                  />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Đến ngày</span>
                  <input
                    type="date"
                    className="h-10 w-full rounded-md border px-3"
                    value={cashierToDate}
                    onChange={(event) => {
                      cashierDateRangeTouched.current = true;
                      setCashierToDate(event.target.value);
                    }}
                  />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Thu ngân</span>
                  <select
                    className="h-10 w-full rounded-md border px-3"
                    value={selectedCashierId}
                    onChange={(event) => {
                      selectedCashierTouched.current = true;
                      setSelectedCashierId(event.target.value);
                    }}
                  >
                    {cashierSummaries.length === 0 ? (
                      <option value="">Không có thu ngân</option>
                    ) : (
                      cashierOptions.map((cashier) => (
                        <option key={cashier.cashierUserId} value={cashier.cashierUserId}>
                          {cashier.cashierName}
                          {cashier.cashierStaffId ? ` - ${cashier.cashierStaffId}` : ''}
                        </option>
                      ))
                    )}
                  </select>
                </label>
                <button
                  type="button"
                  onClick={() => Promise.all([loadCashierSummaries(), loadCashierDetail()])}
                  className="mt-5 inline-flex h-10 items-center justify-center gap-2 rounded-md border px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  <RefreshCw className="h-4 w-4" />
                  Tải lại
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-5 xl:grid-cols-[360px_1fr]">
              <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
                <div className="border-b px-4 py-3">
                  <h2 className="font-semibold text-slate-900">Danh sách thu ngân</h2>
                </div>
                <div className="max-h-[520px] divide-y overflow-y-auto">
                  {cashierLoading ? (
                    <div className="px-4 py-10 text-center text-slate-500">
                      <Loader2 className="inline h-5 w-5 animate-spin" />
                    </div>
                  ) : cashierSummaries.length === 0 ? (
                    <div className="px-4 py-8 text-center text-sm text-slate-500">Không có dữ liệu.</div>
                  ) : (
                    cashierOptions.map((cashier) => {
                      const active = String(cashier.cashierUserId) === selectedCashierId;
                      return (
                        <button
                          key={cashier.cashierUserId}
                          type="button"
                          onClick={() => {
                            selectedCashierTouched.current = true;
                            setSelectedCashierId(String(cashier.cashierUserId));
                          }}
                          className={`block w-full px-4 py-3 text-left transition ${
                            active ? 'bg-blue-600 text-white' : 'hover:bg-blue-50'
                          }`}
                        >
                          <span className="block text-sm font-semibold">{cashier.cashierName}</span>
                          <span className={active ? 'text-xs text-slate-200' : 'text-xs text-slate-500'}>
                            {formatCurrency(Number(cashier.summary.totalCollected ?? 0))} | còn{' '}
                            {formatCurrency(Number(cashier.summary.cashRemaining ?? 0))}
                          </span>
                        </button>
                      );
                    })
                  )}
                </div>
              </div>

              <div className="space-y-5">
                <div className="grid grid-cols-1 gap-4 lg:grid-cols-4">
                  <MetricCard
                    title="Tổng thu"
                    value={cashierDetailLoading ? '...' : formatCurrency(Number(selectedSummary?.totalCollected ?? 0))}
                    description={cashierDetail?.cashierName || 'Thu ngân đã chọn'}
                    icon={WalletCards}
                    tone="blue"
                  />
                  <MetricCard
                    title="Tiền mặt"
                    value={cashierDetailLoading ? '...' : formatCurrency(Number(selectedSummary?.cashCollected ?? 0))}
                    description={`${selectedSummary?.cashInvoiceCount ?? 0} hóa đơn CASH`}
                    icon={Banknote}
                    tone="emerald"
                  />
                  <MetricCard
                    title="Chuyển khoản"
                    value={cashierDetailLoading ? '...' : formatCurrency(Number(selectedSummary?.transferCollected ?? 0))}
                    description={`${selectedSummary?.transferInvoiceCount ?? 0} hóa đơn QR/CK`}
                    icon={Send}
                    tone="amber"
                  />
                  <MetricCard
                    title="Còn bàn giao"
                    value={cashierDetailLoading ? '...' : formatCurrency(Number(selectedSummary?.cashRemaining ?? 0))}
                    description="Sau phiếu chờ và đã xác nhận"
                    icon={ClipboardList}
                    tone="violet"
                  />
                </div>

                <div className="rounded-lg border bg-white p-4 shadow-sm">
                  <h2 className="font-semibold text-slate-900">Breakdown phương thức</h2>
                  <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-3">
                    {(selectedSummary?.methodBreakdown ?? []).map((item) => (
                      <div key={item.paymentMethod} className="rounded-md border p-3">
                        <p className="text-xs font-semibold uppercase text-slate-500">{methodLabel(item.paymentMethod)}</p>
                        <p className="mt-1 text-lg font-bold text-slate-900">{formatCurrency(Number(item.amount ?? 0))}</p>
                        <p className="text-xs text-slate-500">{item.invoiceCount} hóa đơn</p>
                      </div>
                    ))}
                    {!cashierDetailLoading && (selectedSummary?.methodBreakdown ?? []).length === 0 && (
                      <p className="text-sm text-slate-500">Chưa có giao dịch trong khoảng ngày.</p>
                    )}
                  </div>
                </div>

                <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
                  <div className="border-b px-4 py-3">
                    <h2 className="font-semibold text-slate-900">Hóa đơn gần nhất</h2>
                  </div>
                  <div className="overflow-x-auto">
                    <table className="w-full min-w-[820px] text-sm">
                      <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                        <tr>
                          <th className="px-4 py-3">Mã hóa đơn</th>
                          <th className="px-4 py-3">Thời gian</th>
                          <th className="px-4 py-3">Khách hàng</th>
                          <th className="px-4 py-3 text-right">Số tiền</th>
                          <th className="px-4 py-3">Phương thức</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y">
                        {cashierDetailLoading ? (
                          <tr>
                            <td colSpan={5} className="px-4 py-10 text-center text-slate-500">
                              <Loader2 className="inline h-5 w-5 animate-spin" />
                            </td>
                          </tr>
                        ) : (selectedSummary?.recentInvoices ?? []).length === 0 ? (
                          <tr>
                            <td colSpan={5} className="px-4 py-8 text-center text-slate-500">
                              Không có hóa đơn.
                            </td>
                          </tr>
                        ) : (
                          (selectedSummary?.recentInvoices ?? []).map((row) => (
                            <tr key={row.invoiceId} className="hover:bg-slate-50">
                              <td className="px-4 py-3 font-mono text-xs">{row.invoiceId}</td>
                              <td className="px-4 py-3">{formatDateTime(row.paidAt || row.createdAt)}</td>
                              <td className="px-4 py-3 font-medium">{row.patientName || '-'}</td>
                              <td className="px-4 py-3 text-right font-semibold">{formatCurrency(getInvoiceAmount(row))}</td>
                              <td className="px-4 py-3">{methodLabel(row.paymentMethod)}</td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </section>
        )}

        {activeTab === 'handover' && (
          <section className="space-y-5">
            {handoverError && (
              <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{handoverError}</div>
            )}

            <div className="rounded-lg border bg-white p-4 shadow-sm">
              <div className="grid grid-cols-1 gap-3 md:grid-cols-[1fr_1fr_2fr_1fr_auto]">
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Từ ngày</span>
                  <input
                    type="date"
                    className="h-10 w-full rounded-md border px-3"
                    value={handoverFromDate}
                    onChange={(event) => {
                      handoverDateRangeTouched.current = true;
                      setHandoverFromDate(event.target.value);
                    }}
                  />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Đến ngày</span>
                  <input
                    type="date"
                    className="h-10 w-full rounded-md border px-3"
                    value={handoverToDate}
                    onChange={(event) => {
                      handoverDateRangeTouched.current = true;
                      setHandoverToDate(event.target.value);
                    }}
                  />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Thu ngân</span>
                  <select
                    className="h-10 w-full rounded-md border px-3"
                    value={handoverCashierId}
                    onChange={(event) => setHandoverCashierId(event.target.value)}
                  >
                    <option value="">Tất cả thu ngân</option>
                    {cashierOptions.map((cashier) => (
                      <option key={cashier.cashierUserId} value={cashier.cashierUserId}>
                        {cashier.cashierName}
                        {cashier.cashierStaffId ? ` - ${cashier.cashierStaffId}` : ''}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase text-slate-500">Trạng thái</span>
                  <select
                    className="h-10 w-full rounded-md border px-3"
                    value={handoverStatus}
                    onChange={(event) => setHandoverStatus(event.target.value)}
                  >
                    <option value="">Tất cả</option>
                    <option value="PENDING">Chờ xác nhận</option>
                    <option value="CONFIRMED">Đã xác nhận</option>
                    <option value="CANCELLED">Đã hủy</option>
                  </select>
                </label>
                <button
                  type="button"
                  onClick={loadHandovers}
                  className="mt-5 inline-flex h-10 items-center justify-center gap-2 rounded-md border px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  <RefreshCw className="h-4 w-4" />
                  Tải lại
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
              <MetricCard
                title="Tổng phiếu"
                value={handoverLoading ? '...' : handovers.length}
                description="Theo bộ lọc hiện tại"
                icon={ClipboardList}
                tone="blue"
              />
              <MetricCard
                title="Đang chờ"
                value={handoverLoading ? '...' : handovers.filter((item) => item.status === 'PENDING').length}
                description="Chờ thu ngân nhận xác nhận"
                icon={Loader2}
                tone="amber"
              />
              <MetricCard
                title="Đã xác nhận"
                value={handoverLoading ? '...' : handovers.filter((item) => item.status === 'CONFIRMED').length}
                description="Đã hoàn tất bàn giao"
                icon={CheckCircle2}
                tone="emerald"
              />
              <MetricCard
                title="Đã hủy"
                value={handoverLoading ? '...' : handovers.filter((item) => item.status === 'CANCELLED').length}
                description="Không còn hiệu lực"
                icon={XCircle}
                tone="rose"
              />
            </div>

            <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[920px] text-sm">
                  <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                    <tr>
                      <th className="px-4 py-3">Mã phiếu</th>
                      <th className="px-4 py-3">Ngày</th>
                      <th className="px-4 py-3">Người giao</th>
                      <th className="px-4 py-3">Người nhận</th>
                      <th className="px-4 py-3 text-right">Số tiền</th>
                      <th className="px-4 py-3">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {handoverLoading ? (
                      <tr>
                        <td colSpan={6} className="px-4 py-10 text-center text-slate-500">
                          <Loader2 className="inline h-5 w-5 animate-spin" />
                        </td>
                      </tr>
                    ) : handovers.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="px-4 py-8 text-center text-slate-500">
                          Không có phiếu bàn giao phù hợp.
                        </td>
                      </tr>
                    ) : (
                      handovers.map((handover) => (
                        <tr key={handover.handoverId} className="hover:bg-slate-50">
                          <td className="px-4 py-3 font-mono text-xs">#{handover.handoverId}</td>
                          <td className="px-4 py-3">{handover.businessDate}</td>
                          <td className="px-4 py-3 font-medium">{handover.fromCashierName || handover.fromCashierUserId}</td>
                          <td className="px-4 py-3 font-medium">{handover.toCashierName || handover.toCashierUserId}</td>
                          <td className="px-4 py-3 text-right font-semibold">{formatCurrency(Number(handover.amount ?? 0))}</td>
                          <td className="px-4 py-3">
                            <span className={`rounded-full px-2 py-1 text-xs font-semibold ring-1 ${handoverStatusClass(handover.status)}`}>
                              {handoverStatusLabel(handover.status)}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
