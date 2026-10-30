'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Banknote,
  CheckCircle2,
  ClipboardList,
  Loader2,
  RefreshCw,
  Send,
  WalletCards,
  XCircle,
} from 'lucide-react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useToast } from '@/hooks/use-toast';
import { financialService } from '@/services/financial.service';
import { userService, User } from '@/services/user.service';
import { useAuthStore } from '@/store/auth.store';
import { CashHandover, CashierDaySummary } from '@/types/financial.types';

const emptySummary: CashierDaySummary = {
  businessDate: '',
  totalCollected: 0,
  cashCollected: 0,
  transferCollected: 0,
  otherCollected: 0,
  pendingHandovers: 0,
  confirmedHandovers: 0,
  cashRemaining: 0,
  invoiceCount: 0,
  cashInvoiceCount: 0,
  transferInvoiceCount: 0,
  otherInvoiceCount: 0,
  methodBreakdown: [],
  recentInvoices: [],
};

const todayKey = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value || 0));

const formatNumber = (value: number) => new Intl.NumberFormat('vi-VN').format(Number(value || 0));

const formatDateTime = (value?: string | null) => {
  if (!value) return '-';
  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const methodLabel = (method?: string | null) => {
  const normalized = (method || 'UNKNOWN').toUpperCase();
  const labels: Record<string, string> = {
    CASH: 'Tiền mặt',
    QR: 'QR',
    QR_SEPAY: 'QR SePay',
    TRANSFER: 'Chuyển khoản',
    BANK_TRANSFER: 'Chuyển khoản NH',
    UNKNOWN: 'Không xác định',
  };
  return labels[normalized] || normalized;
};

const statusBadge = (status: string) => {
  const normalized = status.toUpperCase();
  const styles: Record<string, string> = {
    PENDING: 'border-amber-200 bg-amber-50 text-amber-700',
    CONFIRMED: 'border-emerald-200 bg-emerald-50 text-emerald-700',
    CANCELLED: 'border-slate-200 bg-slate-50 text-slate-600',
  };
  const labels: Record<string, string> = {
    PENDING: 'Chờ xác nhận',
    CONFIRMED: 'Đã xác nhận',
    CANCELLED: 'Đã hủy',
  };
  return (
    <span className={`inline-flex rounded-md border px-2.5 py-1 text-xs font-semibold ${styles[normalized] || styles.CANCELLED}`}>
      {labels[normalized] || status}
    </span>
  );
};

export default function CashHandoverPage() {
  const { toast } = useToast();
  const currentUser = useAuthStore((state) => state.user);
  const [businessDate, setBusinessDate] = useState(todayKey());
  const [summary, setSummary] = useState<CashierDaySummary>(emptySummary);
  const [cashiers, setCashiers] = useState<User[]>([]);
  const [outgoing, setOutgoing] = useState<CashHandover[]>([]);
  const [incoming, setIncoming] = useState<CashHandover[]>([]);
  const [selectedCashierId, setSelectedCashierId] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [actionId, setActionId] = useState<number | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [summaryData, outgoingData, incomingData, cashierData] = await Promise.all([
        financialService.getCashierDaySummary(businessDate),
        financialService.getCashHandovers(businessDate, 'mine'),
        financialService.getCashHandovers(businessDate, 'incoming'),
        userService.getUsers(0, 100, '', 'ROLE_CASHIER'),
      ]);
      setSummary(summaryData || emptySummary);
      setOutgoing(outgoingData || []);
      setIncoming(incomingData || []);
      setCashiers((cashierData.users || []).filter((cashier) => cashier.userId !== currentUser?.id && cashier.isActive));
      setAmount(String(Number(summaryData?.cashRemaining || 0)));
    } catch (error) {
      console.error('Failed to load cash handover page:', error);
      toast({ description: 'Không thể tải dữ liệu bàn giao tiền mặt.', variant: 'destructive' });
      setSummary(emptySummary);
      setOutgoing([]);
      setIncoming([]);
    } finally {
      setLoading(false);
    }
  }, [businessDate, currentUser?.id, toast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const cards = useMemo(
    () => [
      {
        label: 'Tổng thu',
        value: formatCurrency(summary.totalCollected),
        helper: `${formatNumber(summary.invoiceCount)} hóa đơn đã thanh toán`,
        icon: WalletCards,
        color: 'text-blue-700',
        bg: 'bg-blue-50',
      },
      {
        label: 'Tiền mặt',
        value: formatCurrency(summary.cashCollected),
        helper: `${formatNumber(summary.cashInvoiceCount)} hóa đơn tiền mặt`,
        icon: Banknote,
        color: 'text-emerald-700',
        bg: 'bg-emerald-50',
      },
      {
        label: 'Chuyển khoản',
        value: formatCurrency(summary.transferCollected),
        helper: `${formatNumber(summary.transferInvoiceCount)} hóa đơn chuyển khoản`,
        icon: Send,
        color: 'text-indigo-700',
        bg: 'bg-indigo-50',
      },
      {
        label: 'Còn phải bàn giao',
        value: formatCurrency(summary.cashRemaining),
        helper: `Đã/đang bàn giao ${formatCurrency(summary.pendingHandovers + summary.confirmedHandovers)}`,
        icon: ClipboardList,
        color: 'text-amber-700',
        bg: 'bg-amber-50',
      },
    ],
    [summary]
  );

  const handleCreate = async () => {
    const parsedAmount = Number(amount);
    const remainingAmount = Number(summary.cashRemaining || 0);
    if (remainingAmount <= 0) {
      toast({ description: 'Không còn tiền mặt cần bàn giao.', variant: 'destructive' });
      return;
    }
    if (cashiers.length === 0) {
      toast({ description: 'Không có thu ngân khác đang hoạt động để nhận bàn giao.', variant: 'destructive' });
      return;
    }
    if (!selectedCashierId) {
      toast({ description: 'Vui lòng chọn thu ngân nhận bàn giao.', variant: 'destructive' });
      return;
    }
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      toast({ description: 'Số tiền bàn giao không hợp lệ.', variant: 'destructive' });
      return;
    }

    if (parsedAmount > remainingAmount) {
      toast({ description: 'Số tiền bàn giao vượt quá tiền mặt còn lại.', variant: 'destructive' });
      return;
    }

    setSubmitting(true);
    try {
      await financialService.createCashHandover({
        businessDate,
        toUserId: Number(selectedCashierId),
        amount: parsedAmount,
        note: note.trim() || undefined,
      });
      toast({ description: 'Đã tạo phiếu bàn giao tiền mặt.', variant: 'success' });
      setSelectedCashierId('');
      setNote('');
      await loadData();
    } catch (error) {
      console.error('Failed to create cash handover:', error);
      toast({ description: 'Không thể tạo phiếu bàn giao tiền mặt.', variant: 'destructive' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirm = async (handoverId: number) => {
    setActionId(handoverId);
    try {
      await financialService.confirmCashHandover(handoverId);
      toast({ description: 'Đã xác nhận nhận bàn giao tiền mặt.', variant: 'success' });
      await loadData();
    } catch (error) {
      console.error('Failed to confirm cash handover:', error);
      toast({ description: 'Không thể xác nhận phiếu bàn giao.', variant: 'destructive' });
    } finally {
      setActionId(null);
    }
  };

  const handleCancel = async (handoverId: number) => {
    setActionId(handoverId);
    try {
      await financialService.cancelCashHandover(handoverId);
      toast({ description: 'Đã hủy phiếu bàn giao tiền mặt.', variant: 'success' });
      await loadData();
    } catch (error) {
      console.error('Failed to cancel cash handover:', error);
      toast({ description: 'Không thể hủy phiếu bàn giao.', variant: 'destructive' });
    } finally {
      setActionId(null);
    }
  };

  const renderHandoverTable = (items: CashHandover[], direction: 'outgoing' | 'incoming') => (
    <div className="overflow-x-auto">
      <table className="min-w-full border-collapse text-left text-sm">
        <thead>
          <tr className="border-b bg-slate-50 text-xs font-semibold uppercase text-slate-500">
            <th className="px-4 py-3">Mã</th>
            <th className="px-4 py-3">{direction === 'outgoing' ? 'Người nhận' : 'Người giao'}</th>
            <th className="px-4 py-3 text-right">Số tiền</th>
            <th className="px-4 py-3">Trạng thái</th>
            <th className="px-4 py-3">Tạo lúc</th>
            <th className="px-4 py-3 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {items.length === 0 ? (
            <tr>
              <td colSpan={6} className="px-4 py-8 text-center text-slate-500">
                Không có phiếu bàn giao
              </td>
            </tr>
          ) : (
            items.map((item) => (
              <tr key={item.handoverId} className="border-b last:border-0">
                <td className="px-4 py-3 font-semibold text-slate-900">#{item.handoverId}</td>
                <td className="px-4 py-3 text-slate-700">
                  {direction === 'outgoing' ? item.toCashierName || '-' : item.fromCashierName || '-'}
                  {item.note && <div className="mt-1 max-w-xs text-xs text-slate-500">{item.note}</div>}
                </td>
                <td className="px-4 py-3 text-right font-semibold text-slate-900">{formatCurrency(item.amount)}</td>
                <td className="px-4 py-3">{statusBadge(item.status)}</td>
                <td className="px-4 py-3 text-slate-600">{formatDateTime(item.createdAt)}</td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    {item.canConfirm && (
                      <Button size="sm" onClick={() => handleConfirm(item.handoverId)} disabled={actionId === item.handoverId}>
                        {actionId === item.handoverId ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
                        Xác nhận
                      </Button>
                    )}
                    {item.canCancel && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleCancel(item.handoverId)}
                        disabled={actionId === item.handoverId}
                      >
                        {actionId === item.handoverId ? <Loader2 className="h-4 w-4 animate-spin" /> : <XCircle className="h-4 w-4" />}
                        Hủy
                      </Button>
                    )}
                    {!item.canConfirm && !item.canCancel && <span className="text-xs text-slate-400">Không có</span>}
                  </div>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );

  const cashRemaining = Number(summary.cashRemaining || 0);
  const hasNoCashRemaining = !loading && cashRemaining <= 0;

  return (
    <div className="min-h-full">
      <DashboardHeader title="Bàn giao tiền mặt" subtitle="Tổng kết doanh thu ngày và xác nhận bàn giao giữa các ca thu ngân" />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-5 flex flex-col gap-3 rounded-lg border bg-white p-4 shadow-sm sm:flex-row sm:items-end sm:justify-between">
          <div className="w-full max-w-xs">
            <label className="mb-1 block text-sm font-semibold text-slate-700">Ngày</label>
            <Input type="date" value={businessDate} onChange={(event) => setBusinessDate(event.target.value)} />
          </div>
          <Button variant="outline" onClick={loadData} disabled={loading}>
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>

        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          {cards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.label} className="rounded-lg border bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-medium text-slate-600">{card.label}</p>
                    <p className={`mt-2 text-2xl font-bold ${card.color}`}>
                      {loading ? <Loader2 className="h-7 w-7 animate-spin text-slate-400" /> : card.value}
                    </p>
                    <p className="mt-2 text-sm text-slate-500">{card.helper}</p>
                  </div>
                  <div className={`rounded-md p-3 ${card.bg} ${card.color}`}>
                    <Icon className="h-6 w-6" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
          <section className="rounded-lg border bg-white shadow-sm">
            <div className="flex items-center justify-between border-b px-5 py-4">
              <div>
                <h2 className="font-semibold text-slate-950">Giao dịch trong ngày</h2>
                <p className="text-sm text-slate-500">Các hóa đơn đã thanh toán gần nhất</p>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full border-collapse text-left text-sm">
                <thead>
                  <tr className="border-b bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                    <th className="px-4 py-3">Hóa đơn</th>
                    <th className="px-4 py-3">Bệnh nhân</th>
                    <th className="px-4 py-3">Phương thức</th>
                    <th className="px-4 py-3 text-right">Số tiền</th>
                    <th className="px-4 py-3">Thanh toán</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.recentInvoices.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="px-4 py-10 text-center text-slate-500">
                        Không có giao dịch đã thanh toán
                      </td>
                    </tr>
                  ) : (
                    summary.recentInvoices.map((invoice) => (
                      <tr key={invoice.invoiceId} className="border-b last:border-0">
                        <td className="px-4 py-3 font-semibold text-slate-900">{invoice.invoiceId}</td>
                        <td className="px-4 py-3 text-slate-700">{invoice.patientName || '-'}</td>
                        <td className="px-4 py-3 text-slate-700">{methodLabel(invoice.paymentMethod)}</td>
                        <td className="px-4 py-3 text-right font-semibold text-slate-900">
                          {formatCurrency(invoice.customerPaid || invoice.totalServiceFee || 0)}
                        </td>
                        <td className="px-4 py-3 text-slate-600">{formatDateTime(invoice.paidAt || invoice.createdAt)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <aside className="space-y-6">
            <section className="rounded-lg border bg-white p-5 shadow-sm">
              <h2 className="font-semibold text-slate-950">Tạo phiếu bàn giao</h2>
              <div className="mt-4 space-y-4">
                <div>
                  <label className="mb-1 block text-sm font-semibold text-slate-700">Thu ngân nhận</label>
                  <select
                    value={selectedCashierId}
                    onChange={(event) => setSelectedCashierId(event.target.value)}
                    className="h-9 w-full rounded-md border border-slate-300 bg-white px-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  >
                    <option value="">Chọn người nhận</option>
                    {cashiers.map((cashier) => (
                      <option key={cashier.userId} value={cashier.userId}>
                        {cashier.fullName}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-semibold text-slate-700">Số tiền</label>
                  <Input
                    type="number"
                    min="1"
                    max={cashRemaining}
                    step="1000"
                    value={amount}
                    onChange={(event) => setAmount(event.target.value)}
                  />
                </div>
                {hasNoCashRemaining && (
                  <div role="alert" className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-medium text-amber-800">
                    Không còn tiền mặt cần bàn giao cho ngày này.
                  </div>
                )}
                <div>
                  <label className="mb-1 block text-sm font-semibold text-slate-700">Ghi chú</label>
                  <textarea
                    value={note}
                    onChange={(event) => setNote(event.target.value)}
                    rows={3}
                    className="w-full resize-none rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                    placeholder="Thông tin ca, túi tiền, chênh lệch nếu có"
                  />
                </div>
                <Button className="w-full" onClick={handleCreate} disabled={submitting || loading || summary.cashRemaining <= 0}>
                  {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                  Tạo phiếu bàn giao
                </Button>
              </div>
            </section>

            <section className="rounded-lg border bg-white p-5 shadow-sm">
              <h2 className="font-semibold text-slate-950">Theo phương thức</h2>
              <div className="mt-4 space-y-3">
                {summary.methodBreakdown.length === 0 ? (
                  <p className="text-sm text-slate-500">Không có dữ liệu</p>
                ) : (
                  summary.methodBreakdown.map((item) => (
                    <div key={item.paymentMethod} className="flex items-center justify-between rounded-md border px-3 py-2">
                      <div>
                        <p className="text-sm font-semibold text-slate-800">{methodLabel(item.paymentMethod)}</p>
                        <p className="text-xs text-slate-500">{formatNumber(item.invoiceCount)} hóa đơn</p>
                      </div>
                      <p className="text-sm font-bold text-slate-950">{formatCurrency(item.amount)}</p>
                    </div>
                  ))
                )}
              </div>
            </section>
          </aside>
        </div>

        <section className="mt-6 rounded-lg border bg-white shadow-sm">
          <div className="border-b px-5 py-4">
            <h2 className="font-semibold text-slate-950">Phiếu đã tạo</h2>
          </div>
          {renderHandoverTable(outgoing, 'outgoing')}
        </section>

        <section className="mt-6 rounded-lg border bg-white shadow-sm">
          <div className="border-b px-5 py-4">
            <h2 className="font-semibold text-slate-950">Phiếu chờ mình xác nhận</h2>
          </div>
          {renderHandoverTable(incoming, 'incoming')}
        </section>
      </main>
    </div>
  );
}
