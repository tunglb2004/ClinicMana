'use client';

import { useState, useEffect, use, Suspense, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { financialService } from '@/services/financial.service';
import { FinancialDetailResponse, InsuranceLookupResponse } from '@/types/financial.types';
import { appointmentService, ServiceInfo } from '@/services/appointment.service';
import { paymentService, type PaymentInitiationResponse } from '@/services/payment.service';
import { useToast } from '@/hooks/use-toast';
import { useAuthStore } from '@/store/auth.store';
import { formatAppointmentDate } from '@/lib/date';
import { hasAnyRole, ROLES } from '@/lib/roleRedirect';
import { ArrowLeft } from 'lucide-react';
import { PAYMENT_STATUS_LABELS, RECEIPT_STATUS_LABELS, formatVietnameseLabel } from '@/lib/vietnamese-labels';

const PAYMENT_METHOD_LABEL: Record<string, string> = {
  CASH: 'Tiền mặt',
  TIEN_MAT: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  CHUYEN_KHOAN: 'Chuyển khoản',
  QR: 'QR Code',
};

const PAID_STATUS_CLS: Record<string, string> = {
  PAID: 'bg-green-50 text-green-700 border-green-300',
  PENDING: 'bg-yellow-50 text-yellow-700 border-yellow-300',
  FAILED: 'bg-red-50 text-red-700 border-red-300',
};

const RECEIPT_STATUS_LABEL: Record<string, string> = {
  NONE: 'Chưa phát hành',
  PROCESSING: 'Đang phát hành',
  ISSUED: 'Đã phát hành',
  FAILED: 'Lỗi phát hành',
};

const RECEIPT_STATUS_CLS: Record<string, string> = {
  NONE: 'bg-gray-100 text-gray-700 border-gray-300',
  PROCESSING: 'bg-yellow-50 text-yellow-700 border-yellow-300',
  ISSUED: 'bg-green-50 text-green-700 border-green-300',
  FAILED: 'bg-red-50 text-red-700 border-red-300',
};

export function InvoiceDetailContent({ id, forceHistory = false }: { id: string; forceHistory?: boolean }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const isHistory = forceHistory || searchParams.get('mode') === 'history';
  const isSupplement = searchParams.get('type') === 'supplement';

  const { user } = useAuthStore();
  const { toast } = useToast();

  const [detail, setDetail] = useState<FinancialDetailResponse | null>(null);
  const [appointmentServices, setAppointmentServices] = useState<ServiceInfo[]>([]);
  const [appointmentStatus, setAppointmentStatus] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'QR'>('CASH');
  const [appliedBhyt, setAppliedBhyt] = useState(false);
  const [cccdInput, setCccdInput] = useState('');
  const [insuranceLookupLoading, setInsuranceLookupLoading] = useState(false);
  const [insuranceResult, setInsuranceResult] = useState<InsuranceLookupResponse | null>(null);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [qrPaymentUrl, setQrPaymentUrl] = useState<string | null>(null);
  const [showPaymentConfirm, setShowPaymentConfirm] = useState(false);
  const paymentStatusPollingRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const displayInsurancePaid = appliedBhyt ? (insuranceResult?.totalInsurancePaid ?? detail?.insurancePaid ?? 0) : 0;
  const displayCustomerPaid = appliedBhyt
    ? (insuranceResult?.totalPatientPaid ?? (detail?.totalServiceFee ?? 0) - displayInsurancePaid)
    : (detail?.totalServiceFee ?? 0);
  const displayServices: Array<{
    serviceId: string;
    serviceName: string;
    price: number;
    status?: string;
    assignedRoomId?: string | null;
    assignedRoomName?: string | null;
  }> = detail?.services?.length ? detail.services : appointmentServices;
  const isPaidReceipt = detail?.paymentStatus === 'PAID' || detail?.paymentStatus === 'SUCCESS';
  const resolvedPaidAt = detail?.paidAt
    ?? ((detail?.paymentStatus === 'PAID' || detail?.paymentStatus === 'SUCCESS') ? detail?.createdAt : null);
  const isAppointmentCompleted = appointmentStatus === 'COMPLETED';

  useEffect(() => {
    if (
      user &&
      !hasAnyRole(user.roles, [ROLES.CASHIER, ROLES.SYSTEM_ADMIN, ROLES.CLINIC_MANAGER, ROLES.MANAGER])
    ) {
      router.push('/unauthorized');
    }
  }, [user, router]);

  useEffect(() => {
    return () => {
      if (paymentStatusPollingRef.current) {
        clearInterval(paymentStatusPollingRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        if (isSupplement) {
          // Load supplement service orders directly by recordId
          const res = await financialService.getSupplementOrders(id);
          setAppointmentServices(
            res.orders.map((o) => ({
              serviceId: o.serviceId,
              serviceName: o.serviceName,
              price: o.price ?? 0,
              assignedRoomId: o.assignedRoomId ?? undefined,
              assignedRoomName: o.assignedRoomName ?? undefined,
            }))
          );
          // Build unique room names for display
          const roomNames = [...new Set(res.orders.map(o => o.assignedRoomName).filter(Boolean))];
          const roomDisplay = roomNames.length > 0 ? roomNames.join(', ') : null;
          const existingInvoiceId = res.orders.find((o) => o.invoiceId)?.invoiceId ?? '';
          let existingInvoiceDetail: FinancialDetailResponse | null = null;
          if (existingInvoiceId) {
            try {
              existingInvoiceDetail = await financialService.getInvoiceDetail(existingInvoiceId);
              if ((existingInvoiceDetail.insurancePaid ?? 0) > 0) {
                setAppliedBhyt(true);
              }
            } catch (invoiceError) {
              console.error('Failed to fetch pending supplement invoice detail', invoiceError);
            }
          }
          setDetail({
            invoiceId: existingInvoiceDetail?.invoiceId ?? existingInvoiceId,
            receiptNumber: existingInvoiceDetail?.receiptNumber ?? null,
            eInvoiceCode: existingInvoiceDetail?.eInvoiceCode ?? null,
            eInvoiceUrl: existingInvoiceDetail?.eInvoiceUrl ?? null,
            receiptStatus: existingInvoiceDetail?.receiptStatus ?? 'NONE',
            receiptIssuedAt: existingInvoiceDetail?.receiptIssuedAt ?? null,
            receiptErrorMessage: existingInvoiceDetail?.receiptErrorMessage ?? null,
            cashierId: existingInvoiceDetail?.cashierId ?? '',
            patientId: res.patientId,
            patientName: res.patientName,
            patientPhone: res.phoneNumber ?? null,
            patientEmail: existingInvoiceDetail?.patientEmail ?? null,
            patientAddress: existingInvoiceDetail?.patientAddress ?? null,
            patientDateOfBirth: existingInvoiceDetail?.patientDateOfBirth ?? null,
            patientCccd: existingInvoiceDetail?.patientCccd ?? null,
            healthInsuranceNumber: res.healthInsuranceNumber ?? null,
            appointmentId: existingInvoiceDetail?.appointmentId ?? '',
            appointmentDate: res.appointmentDate ?? '',
            appointmentTime: res.appointmentTime ?? '',
            doctorId: existingInvoiceDetail?.doctorId ?? '',
            doctorName: res.doctorName ?? '',
            departmentName: res.departmentName ?? null,
            roomId: existingInvoiceDetail?.roomId ?? null,
            roomName: existingInvoiceDetail?.roomName ?? roomDisplay,
            services: res.orders.map(o => ({
              serviceId: o.serviceId,
              serviceName: o.serviceName,
              price: o.price ?? 0,
              status: o.status ?? 'PENDING',
              assignedRoomId: o.assignedRoomId ?? null,
              assignedRoomName: o.assignedRoomName ?? null,
            })),
            totalServiceFee: existingInvoiceDetail?.totalServiceFee ?? res.totalAmount,
            insurancePaid: existingInvoiceDetail?.insurancePaid ?? 0,
            customerPaid: existingInvoiceDetail?.customerPaid ?? res.totalAmount,
            paymentStatus: existingInvoiceDetail?.paymentStatus ?? 'PENDING',
            paymentMethod: existingInvoiceDetail?.paymentMethod ?? '',
            transactionId: existingInvoiceDetail?.transactionId ?? null,
            paidAt: existingInvoiceDetail?.paidAt ?? null,
            createdAt: existingInvoiceDetail?.createdAt ?? new Date().toISOString(),
          });
          return;
        }

        const res = await financialService.getInvoiceDetail(id);
        if (res && res.invoiceId) {
          setDetail(res);
          try {
            const apt = await appointmentService.getAppointmentDetail(res.appointmentId);
            setAppointmentServices(apt.services ?? []);
            setAppointmentStatus(apt.status ?? null);
          } catch (e) {
            console.error('Failed to fetch appointment services', e);
            setAppointmentStatus(null);
          }
        }
      } catch {
        // Invoice not found — this ID may be an appointmentId (unpaid record)
        try {
          const apt = await appointmentService.getAppointmentDetail(id);
          if (apt && apt.appointmentId) {
            const totalServiceFee = apt.services?.reduce((sum: number, s: { price: number }) => sum + s.price, 0) ?? 0;
            setDetail({
              invoiceId: '',
              receiptNumber: null,
              eInvoiceCode: null,
              eInvoiceUrl: null,
              receiptStatus: 'NONE',
              receiptIssuedAt: null,
              receiptErrorMessage: null,
              cashierId: '',
              patientId: apt.patientId,
              patientName: apt.patientName,
              patientPhone: apt.patientPhone ?? null,
              patientEmail: null,
              patientAddress: null,
              patientDateOfBirth: apt.patientDateOfBirth ?? null,
              patientCccd: null,
              healthInsuranceNumber: apt.patientHealthInsuranceNumber ?? null,
              appointmentId: apt.appointmentId,
              appointmentDate: apt.appointmentDate,
              appointmentTime: apt.appointmentTime,
              doctorId: String(apt.doctorId),
              doctorName: apt.doctorName,
              departmentName: apt.departmentName ?? null,
              roomId: apt.roomId ?? null,
              roomName: apt.roomName ?? null,
              services: apt.services?.map((s: ServiceInfo) => ({
                serviceId: s.serviceId,
                serviceName: s.serviceName,
                price: s.price,
                status: 'PENDING',
                assignedRoomId: s.assignedRoomId ?? null,
                assignedRoomName: s.assignedRoomName ?? null,
              })) ?? [],
              totalServiceFee,
              insurancePaid: 0,
              customerPaid: totalServiceFee,
              paymentStatus: 'PENDING',
              paymentMethod: '',
              transactionId: null,
              paidAt: null,
              createdAt: apt.createdAt,
            });
            setAppointmentServices(apt.services ?? []);
            setAppointmentStatus(apt.status ?? null);
          }
        } catch (e2) {
          console.error('Failed to fetch appointment detail', e2);
          setAppointmentStatus(null);
        }
      } finally {
        setLoading(false);
      }
    };
    fetchDetail();
  }, [id, isSupplement]);

  const handlePrint = (receiptDetail?: FinancialDetailResponse | null) => {
    const printableDetail = receiptDetail ?? detail;
    if (!printableDetail) return;
    const printableServices = printableDetail.services?.length ? printableDetail.services : displayServices;
    const printableRoomSummary = (() => {
      const serviceRooms = [...new Set(printableServices.map((service) => service.assignedRoomName).filter(Boolean))];
      if (serviceRooms.length > 0) {
        return serviceRooms.join(', ');
      }
      return printableDetail.roomName ?? '';
    })();
    const iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    document.body.appendChild(iframe);
    const pri = iframe.contentWindow;
    if (!pri) return;
    pri.document.open();
    pri.document.write(`
      <html>
        <head>
          <title>In Phiếu Khám</title>
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
            <div class="title">Phiếu Khám ${printableDetail.invoiceId}</div>
            <div class="subtitle">Phòng Khám Đa Khoa</div>
          </div>
          <div class="info-grid">
            <div>
               <div class="info-item"><span class="info-label">Mã số PK: </span> ${printableDetail.invoiceId}</div>
               <div class="info-item"><span class="info-label">Ngày: </span> ${new Date(printableDetail.createdAt).toLocaleString('vi-VN')}</div>
               <div class="info-item"><span class="info-label">Khách hàng: </span> ${printableDetail.patientName}</div>
            </div>
            <div>
               <div class="info-item"><span class="info-label">SĐT: </span> ${printableDetail.patientPhone ?? ''}</div>
               <div class="info-item"><span class="info-label">Khoa: </span> ${printableDetail.departmentName || ''}</div>
               <div class="info-item"><span class="info-label">Bác sĩ: </span> ${printableDetail.doctorName}</div>
               <div class="info-item"><span class="info-label">Phòng: </span> ${printableRoomSummary}</div>
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
               ${printableServices?.map((svc, i) => {
                 const breakdown = insuranceResult?.serviceBreakdowns?.find(b => b.serviceId === svc.serviceId);
                 const bhytPaid = appliedBhyt ? (breakdown?.insurancePaid ?? 0) : 0;
                 const patientPaid = svc.price - bhytPaid;
                 return `
                 <tr>
                   <td class="text-center">${i + 1}</td>
                   <td>${svc.serviceName}</td>
                   <td>${svc.assignedRoomName || '—'}</td>
                   <td class="text-center">Lần</td>
                   <td class="text-center">1</td>
                   <td class="text-right">${svc.price?.toLocaleString('vi-VN')} đ</td>
                   <td class="text-right">${svc.price?.toLocaleString('vi-VN')} đ</td>
                   <td class="text-right" style="color: green;">${bhytPaid.toLocaleString('vi-VN')} đ</td>
                   <td class="text-right" style="font-weight: bold;">${patientPaid.toLocaleString('vi-VN')} đ</td>
                 </tr>
               `}).join('') || '<tr><td colspan="9" class="text-center">Không có dịch vụ</td></tr>'}
             </tbody>
          </table>
          <div class="summary">
            <div class="summary-item"><div class="label">Tổng phí dịch vụ:</div><div class="value">${printableDetail.totalServiceFee?.toLocaleString('vi-VN')} đ</div></div>
            <div class="summary-item" style="color: green;"><div class="label">BHYT chi trả:</div><div class="value">-${displayInsurancePaid.toLocaleString('vi-VN')} đ</div></div>
            <div class="summary-item summary-total"><div class="label">Khách trả:</div><div class="value">${displayCustomerPaid.toLocaleString('vi-VN')} đ</div></div>
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

  const handleLookupInsurance = async () => {
    if (!cccdInput.trim()) {
      toast({ description: 'Vui lòng nhập số CCCD để tra cứu BHYT', variant: 'error' });
      return;
    }
    setInsuranceLookupLoading(true);
    setInsuranceResult(null);
    setAppliedBhyt(false);
    try {
      const result = isSupplement
        ? await financialService.lookupInsuranceByRecord(cccdInput.trim(), id)
        : await financialService.lookupInsurance(cccdInput.trim(), detail!.appointmentId);
      setInsuranceResult(result);
      setAppliedBhyt(true);
    } catch {
      toast({ description: 'Không tìm thấy thông tin BHYT hoặc có lỗi xảy ra', variant: 'error' });
    } finally {
      setInsuranceLookupLoading(false);
    }
  };

  const stopPaymentStatusPolling = () => {
    if (paymentStatusPollingRef.current) {
      clearInterval(paymentStatusPollingRef.current);
      paymentStatusPollingRef.current = null;
    }
  };

  const mergePaymentResultIntoDetail = (result: PaymentInitiationResponse, invoiceId: string) => {
    const paid = result.paymentStatus === 'PAID' || result.paymentStatus === 'SUCCESS';
    if (result.insuranceBreakdown) {
      setInsuranceResult(result.insuranceBreakdown);
      setAppliedBhyt(true);
    } else if ((result.insurancePaid ?? 0) > 0) {
      setAppliedBhyt(true);
    }

    setDetail((prev) => prev ? {
      ...prev,
      invoiceId,
      paymentStatus: result.paymentStatus ?? prev.paymentStatus,
      paymentMethod: result.paymentMethod ?? paymentMethod,
      totalServiceFee: result.totalServiceFee ?? prev.totalServiceFee,
      insurancePaid: result.insurancePaid ?? prev.insurancePaid,
      customerPaid: result.customerPaid ?? result.amount ?? prev.customerPaid,
      paidAt: paid ? (prev.paidAt ?? new Date().toISOString()) : prev.paidAt,
    } : prev);
  };

  const handlePaidPaymentResult = async (result: PaymentInitiationResponse) => {
    const invoiceId = result.invoiceId ?? detail?.invoiceId;
    if (!invoiceId) {
      toast({ description: 'Không nhận được mã phiếu khám để in phiếu', variant: 'error' });
      return;
    }

    mergePaymentResultIntoDetail(result, invoiceId);

    let latestDetail: FinancialDetailResponse | null = null;
    try {
      latestDetail = await financialService.getInvoiceDetail(invoiceId);
      if (isSupplement) {
        setDetail((prev) => prev ? {
          ...prev,
          invoiceId: latestDetail!.invoiceId,
          paymentStatus: latestDetail!.paymentStatus,
          paymentMethod: latestDetail!.paymentMethod,
          totalServiceFee: latestDetail!.totalServiceFee,
          insurancePaid: latestDetail!.insurancePaid,
          customerPaid: latestDetail!.customerPaid,
          transactionId: latestDetail!.transactionId,
          paidAt: latestDetail!.paidAt,
          receiptNumber: latestDetail!.receiptNumber,
          eInvoiceCode: latestDetail!.eInvoiceCode,
          eInvoiceUrl: latestDetail!.eInvoiceUrl,
          receiptStatus: latestDetail!.receiptStatus,
          receiptIssuedAt: latestDetail!.receiptIssuedAt,
        } : prev);
      } else {
        setDetail(latestDetail);
      }
    } catch (refreshError) {
      console.error('Failed to refresh receipt detail after payment', refreshError);
    }

    toast({ description: 'Thanh toán thành công!', variant: 'success' });
    router.refresh();
    setTimeout(() => handlePrint(isSupplement ? null : latestDetail), 800);
  };

  const startPaymentStatusPolling = (invoiceId: string) => {
    stopPaymentStatusPolling();
    let attempts = 0;

    paymentStatusPollingRef.current = setInterval(async () => {
      attempts += 1;
      try {
        const statusRes = await financialService.getInvoiceStatus(invoiceId);
        const paymentStatus = statusRes?.paymentStatus;

        if (paymentStatus === 'PAID') {
          stopPaymentStatusPolling();
          let latestDetail: FinancialDetailResponse | null = null;
          try {
            latestDetail = await financialService.getInvoiceDetail(invoiceId);
            if (isSupplement) {
              // Preserve supplement-only services; only merge payment fields
              setDetail((prev) => prev ? {
                ...prev,
                invoiceId: latestDetail!.invoiceId,
                paymentStatus: latestDetail!.paymentStatus,
                paymentMethod: latestDetail!.paymentMethod,
                transactionId: latestDetail!.transactionId,
                paidAt: latestDetail!.paidAt,
                receiptNumber: latestDetail!.receiptNumber,
                eInvoiceCode: latestDetail!.eInvoiceCode,
                eInvoiceUrl: latestDetail!.eInvoiceUrl,
                receiptStatus: latestDetail!.receiptStatus,
                receiptIssuedAt: latestDetail!.receiptIssuedAt,
              } : prev);
            } else {
              setDetail(latestDetail);
            }
          } catch (refreshError) {
            console.error('Failed to refresh receipt detail after payment', refreshError);
            setDetail((prev) => prev ? {
              ...prev,
              invoiceId,
              paymentStatus: 'PAID',
              paidAt: prev.paidAt ?? new Date().toISOString(),
            } : prev);
          }
          toast({ description: 'Thanh toán thành công!', variant: 'success' });
          router.refresh();
          // For supplement, print with original detail (supplement services only)
          setTimeout(() => handlePrint(isSupplement ? null : latestDetail), 800);
          return;
        }

        if (paymentStatus && paymentStatus !== 'PENDING' && paymentStatus !== 'PROCESSING') {
          stopPaymentStatusPolling();
          toast({ description: 'Thanh toán thất bại', variant: 'error' });
          return;
        }

        if (attempts >= 36) {
          stopPaymentStatusPolling();
          toast({ description: 'Chưa nhận được xác nhận chuyển khoản. Nếu đã chuyển tiền, vui lòng tải lại phiếu sau ít phút.', variant: 'error' });
        }
      } catch {
        if (attempts >= 36) {
          stopPaymentStatusPolling();
          toast({ description: 'Chưa kiểm tra được trạng thái thanh toán. Vui lòng tải lại phiếu sau ít phút.', variant: 'error' });
        }
      }
    }, 5000);
  };

  const handleSwitchToQR = async () => {
    setPaymentMethod('QR');
    if (!detail) return;
    setPaymentLoading(true);
    setQrPaymentUrl(null);
    try {
      const res = isSupplement
        ? await paymentService.supplementPayment({
            recordId: id,
            paymentMethod: 'QR',
            cccdNumber: cccdInput.trim() || undefined,
          })
        : await paymentService.collectPrepayment({
            appointmentId: detail.appointmentId,
            paymentMethod: 'QR',
            cccdNumber: cccdInput.trim() || undefined,
          });
      const invoiceId = res?.invoiceId ?? null;
      const url = res?.paymentUrl ?? null;
      if (invoiceId) {
        mergePaymentResultIntoDetail(res, invoiceId);
        if (res?.paymentStatus === 'PAID' || res?.paymentStatus === 'SUCCESS') {
          await handlePaidPaymentResult(res);
          setQrPaymentUrl(url);
          return;
        }
        if (!res?.paymentStatus || res.paymentStatus === 'PENDING' || res.paymentStatus === 'PROCESSING') {
          startPaymentStatusPolling(invoiceId);
        }
      }
      setQrPaymentUrl(url);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast({
        description: err?.response?.data?.message || 'Có lỗi khi khởi tạo thanh toán QR',
        variant: 'error',
      });
    } finally {
      setPaymentLoading(false);
    }
  };

  const handlePayment = async () => {
    if (!detail) return;
    if (paymentMethod === 'QR') {
      toast({ description: 'Đang chờ xác nhận thanh toán QR', variant: 'default' });
      return;
    }
    setPaymentLoading(true);
    try {
      let res;
      if (isSupplement) {
        res = await paymentService.supplementPayment({
          recordId: id,
          paymentMethod: 'CASH',
          cccdNumber: cccdInput.trim() || undefined,
        });
      } else {
        res = await paymentService.collectPrepayment({
          appointmentId: detail.appointmentId,
          paymentMethod: 'CASH',
          cccdNumber: cccdInput.trim() || undefined,
        });
      }
      const invoiceId = res?.invoiceId ?? detail.invoiceId;
      if (!invoiceId) {
        toast({ description: 'Không nhận được mã phiếu khám để kiểm tra trạng thái', variant: 'error' });
        return;
      }
      mergePaymentResultIntoDetail(res, invoiceId);
      if (res?.paymentStatus === 'PAID' || res?.paymentStatus === 'SUCCESS') {
        await handlePaidPaymentResult(res);
        return;
      }
      if (!res?.paymentStatus || res.paymentStatus === 'PENDING' || res.paymentStatus === 'PROCESSING') {
        startPaymentStatusPolling(invoiceId);
      }
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast({
        description: err?.response?.data?.message || 'Có lỗi xảy ra khi thanh toán',
        variant: 'error',
      });
    } finally {
      setPaymentLoading(false);
    }
  };

  const handlePrintEinvoice = () => {
    if (!isAppointmentCompleted) {
      toast({
        description: 'Ca khám chưa hoàn tất, chưa thể in lại hóa đơn điện tử.',
        variant: 'error',
      });
      return;
    }

    if (normalizedReceiptStatus === 'PROCESSING') {
      toast({
        description: 'Hóa đơn điện tử đang được xử lý. Vui lòng thử lại sau.',
        variant: 'error',
      });
      return;
    }

    if (!detail?.eInvoiceUrl) {
      toast({
        description: 'Chưa có hóa đơn điện tử để in lại.',
        variant: 'error',
      });
      return;
    }

    window.open(detail.eInvoiceUrl, '_blank', 'noopener,noreferrer');
  };

  const numberToWords = (num: number): string => {
    if (num === 0) return 'Không đồng';
    return num.toLocaleString('vi-VN') + ' đồng';
  };

  const normalizedReceiptStatus = detail?.receiptStatus || 'NONE';
  const canPrintEinvoice = isHistory && isPaidReceipt;

  if (loading) {
    return (
      <div className="p-8 bg-gray-50 min-h-screen flex items-center justify-center">
        <div className="bg-white p-8 rounded border-2 border-black font-bold uppercase shadow-[4px_4px_0_0_rgba(0,0,0,1)]">
          Đang tải...
        </div>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="p-8 bg-gray-50 min-h-screen flex flex-col items-center justify-center gap-4">
        <p className="text-gray-500 font-medium">Không tìm thấy phiếu khám.</p>
        <button
          onClick={() => router.back()}
          className="flex items-center gap-2 border-2 border-black bg-white hover:bg-gray-100 px-4 py-2 font-bold uppercase text-sm transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          Quay lại
        </button>
      </div>
    );
  }

  return (
    <div className="p-6 bg-gray-50 min-h-screen font-sans text-black">
      {/* Page Header */}
      <div className="flex items-center gap-4 mb-6">
        <button
          onClick={() => router.back()}
          className="flex items-center gap-2 border-2 border-black bg-white hover:bg-gray-100 px-4 py-2 font-bold uppercase text-sm transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          Quay lại
        </button>
        <h1 className="text-2xl font-bold text-black border-l-4 border-black pl-3 uppercase">
          {isHistory ? 'LỊCH SỬ GIAO DỊCH' : 'CHI TIẾT PHIẾU KHÁM'} — {detail.invoiceId}
        </h1>
      </div>

      {/* Content */}
      <div className="w-full bg-[#fdfdfd] text-black font-sans border-2 border-black">
        <div className="p-4 flex flex-col lg:flex-row gap-6">

          {/* LEFT COLUMN - Box 1: Patient Info */}
          <div className="lg:w-1/2 flex flex-col">
            <div className="border-2 border-black flex flex-col h-full bg-white">
              <div className="border-b-2 border-black p-3 font-bold uppercase text-[15px] bg-white text-black tracking-wide">
                1. THÔNG TIN BỆNH NHÂN
              </div>

              <div className="p-5 flex flex-col gap-4">
                <div className="grid grid-cols-2 gap-4 text-sm font-semibold">
                  <div className="flex flex-col gap-1.5">
                    <label>Họ và tên</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed font-bold" value={detail.patientName || ''} />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label>Số điện thoại</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.patientPhone || ''} />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 text-sm font-semibold">
                  <div className="flex flex-col gap-1.5">
                    <label>CCCD / CMND</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.patientCccd || ''} />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label>Ngày sinh</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.patientDateOfBirth ? new Date(detail.patientDateOfBirth).toLocaleDateString('vi-VN') : ''} />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 text-sm font-semibold">
                  <div className="flex flex-col gap-1.5">
                    <label>Loại bảo hiểm</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.healthInsuranceNumber ? 'Có BHYT' : 'Dịch vụ'} />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label>Số BHYT</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.healthInsuranceNumber || ''} />
                  </div>
                </div>

                <div className="flex flex-col gap-1.5 text-sm font-semibold">
                  <label>Địa chỉ</label>
                  <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.patientAddress || ''} />
                </div>

                <div className="grid grid-cols-2 gap-4 text-sm font-semibold">
                  <div className="flex flex-col gap-1.5">
                    <label>Bác sĩ</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.doctorName || ''} />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label>Khoa</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.departmentName || ''} />
                  </div>
                </div>

                {detail.roomName && (
                  <div className="grid grid-cols-2 gap-4 text-sm font-semibold">
                    <div className="flex flex-col gap-1.5">
                      <label>Phòng khám</label>
                      <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.roomName || ''} />
                    </div>
                  </div>
                )}

                <div className="grid grid-cols-2 gap-4 text-sm font-semibold">
                  <div className="flex flex-col gap-1.5">
                    <label>Ngày khám</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.appointmentDate ? formatAppointmentDate(detail.appointmentDate) : ''} />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label>Giờ khám</label>
                    <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.appointmentTime || ''} />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN - Box 2 + Box 3 */}
          <div className="lg:w-1/2 flex flex-col gap-6">

            {/* Box 2: Services */}
            <div className="border-2 border-black flex flex-col bg-white">
              <div className="border-b-2 border-black p-3 font-bold uppercase text-[15px] bg-white tracking-wide">
                2. DỊCH VỤ ĐÃ SỬ DỤNG
              </div>

              <div className="p-4 flex flex-col gap-4">
                <div className="overflow-x-auto border border-gray-300">
                  <table className="w-full text-xs text-left">
                    <thead className="bg-white border-b border-gray-300 font-bold uppercase">
                      <tr>
                        <th className="p-2 border-r border-gray-300 text-center">STT</th>
                        <th className="p-2 border-r border-gray-300">Dịch vụ</th>
                        <th className="p-2 border-r border-gray-300">Phòng</th>
                        <th className="p-2 border-r border-gray-300 text-center">ĐVT</th>
                        <th className="p-2 border-r border-gray-300 text-center">SL</th>
                        <th className="p-2 border-r border-gray-300 text-right">Đơn giá</th>
                        <th className="p-2 border-r border-gray-300 text-right">Thành tiền</th>
                        <th className="p-2 border-r border-gray-300 text-right">Quỹ BHYT</th>
                        <th className="p-2 text-right">Khách trả</th>
                      </tr>
                    </thead>
                    <tbody>
                      {displayServices.length > 0 ? displayServices.map((svc, i) => {
                        const breakdown = insuranceResult?.serviceBreakdowns.find(b => b.serviceId === svc.serviceId);
                        const bhytPaid = appliedBhyt ? (breakdown?.insurancePaid ?? 0) : 0;
                        const patientPaid = svc.price - bhytPaid;
                        return (
                          <tr key={i} className="border-b border-gray-200">
                            <td className="p-2 border-r border-gray-300 text-center">{i + 1}</td>
                            <td className="p-2 border-r border-gray-300 align-top font-medium text-gray-900">{svc.serviceName}</td>
                            <td className="p-2 border-r border-gray-300 align-top text-gray-700">{svc.assignedRoomName || '—'}</td>
                            <td className="p-2 border-r border-gray-300 text-center">Lần</td>
                            <td className="p-2 border-r border-gray-300 text-center">1</td>
                            <td className="p-2 border-r border-gray-300 text-right">{svc.price?.toLocaleString('vi-VN')}</td>
                            <td className="p-2 border-r border-gray-300 text-right">{svc.price?.toLocaleString('vi-VN')}</td>
                            <td className="p-2 border-r border-gray-300 text-right text-green-700">{bhytPaid.toLocaleString('vi-VN')}</td>
                            <td className="p-2 text-right font-bold">{patientPaid.toLocaleString('vi-VN')}</td>
                          </tr>
                        );
                      }) : (
                        <tr><td colSpan={9} className="p-4 text-center">Không có dịch vụ</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>

                <div className="text-sm border-t border-dashed border-gray-400 pt-3 flex flex-col gap-1 font-semibold">
                  <div className="font-bold">Tổng kết:</div>
                  <div className="flex justify-between w-64 text-gray-700">
                    <span>- Tổng phí dịch vụ:</span>
                    <span className="font-bold text-black">{detail.totalServiceFee?.toLocaleString('vi-VN')} VNĐ</span>
                  </div>
                  <div className="flex justify-between w-64 text-gray-700">
                    <span>- BHYT chi trả:</span>
                    <span className="font-bold text-green-700">-{displayInsurancePaid.toLocaleString('vi-VN')} VNĐ</span>
                  </div>
                  <div className="flex justify-between w-64 text-gray-700 border-t border-gray-300 pt-1 mt-1">
                    <span>- Khách phải trả:</span>
                    <span className="font-bold text-black">{displayCustomerPaid.toLocaleString('vi-VN')} VNĐ</span>
                  </div>
                  <div className="mt-1 text-gray-600 text-xs">
                    Bằng chữ: <span className="font-medium">{numberToWords(displayCustomerPaid)}</span>.
                  </div>
                </div>

                {/* BHYT lookup — only in payment mode */}
                {!isHistory && (
                  <div className="mt-3 flex flex-col gap-3">
                    <div className="font-bold text-sm text-black flex items-center gap-2">
                      <span className="w-28 shrink-0">Tra cứu BHYT:</span>
                      <div className="flex-1 flex gap-2">
                        <input
                          type="text"
                          placeholder="Nhập số CCCD của bệnh nhân"
                          className="border border-gray-400 p-2 flex-1 text-sm outline-none focus:border-black"
                          value={cccdInput}
                          onChange={(e) => setCccdInput(e.target.value)}
                          onKeyDown={(e) => { if (e.key === 'Enter') handleLookupInsurance(); }}
                        />
                        <button
                          onClick={handleLookupInsurance}
                          disabled={insuranceLookupLoading}
                          className="bg-black text-white px-4 py-2 font-bold hover:bg-gray-800 transition disabled:opacity-50 disabled:cursor-not-allowed text-sm"
                        >
                          {insuranceLookupLoading ? 'Đang tra cứu...' : 'Tra cứu'}
                        </button>
                        {insuranceResult && (
                          <button
                            onClick={() => { setInsuranceResult(null); setAppliedBhyt(false); setCccdInput(''); }}
                            className="border border-gray-400 text-black px-4 py-2 font-bold hover:bg-gray-100 transition text-sm"
                          >Xóa</button>
                        )}
                      </div>
                    </div>

                    {/* Insurance result panel */}
                    {insuranceResult && (
                      <div className="border-2 border-black bg-white text-xs">
                        <div className="bg-black text-white p-2 font-bold uppercase tracking-wide">
                          Kết quả tra cứu BHYT — Xác nhận thông tin khớp với bệnh nhân
                        </div>
                        <div className="p-3 grid grid-cols-2 gap-x-4 gap-y-1 font-semibold border-b border-dashed border-gray-300">
                          <div>
                            <span className="text-gray-500">Họ tên: </span>
                            <span className={`font-bold ${insuranceResult.citizenInfo.fullName.toUpperCase() === detail.patientName?.toUpperCase() ? 'text-green-700' : 'text-red-600'}`}>
                              {insuranceResult.citizenInfo.fullName}
                            </span>
                          </div>
                          <div><span className="text-gray-500">Mã BHYT: </span><span className="font-bold">{insuranceResult.citizenInfo.insuranceCode}</span></div>
                          <div><span className="text-gray-500">Ngày sinh: </span>{insuranceResult.citizenInfo.dateOfBirth}</div>
                          <div><span className="text-gray-500">Giới tính: </span>{insuranceResult.citizenInfo.genderName}</div>
                          <div><span className="text-gray-500">Địa chỉ: </span>{insuranceResult.citizenInfo.address}</div>
                          <div>
                            <span className="text-gray-500">Hiệu lực đến: </span>
                            <span className="text-blue-700 font-bold">{insuranceResult.citizenInfo.validTo}</span>
                          </div>
                          <div className="col-span-2">
                            <span className="text-gray-500">Đúng tuyến: </span>
                            <span className={insuranceResult.citizenInfo.isDungTuyen ? 'text-green-700 font-bold' : 'text-red-600 font-bold'}>
                              {insuranceResult.citizenInfo.isDungTuyen ? `Có (${insuranceResult.citizenInfo.tuyenRatio}%)` : 'Không'}
                            </span>
                          </div>
                          <div className="col-span-2 text-green-700 font-bold">{insuranceResult.citizenInfo.message}</div>
                        </div>

                        <div className="p-3 pt-2 flex gap-2">
                          <div className="flex-1 px-4 py-2 bg-green-50 border border-green-300 text-green-700 font-bold text-sm uppercase text-center">
                            ✓ Đã áp dụng BHYT vào bảng dịch vụ
                          </div>
                          <button
                            onClick={() => setAppliedBhyt(!appliedBhyt)}
                            className="border border-gray-400 text-black px-4 py-2 font-bold hover:bg-gray-100 transition text-sm"
                          >{appliedBhyt ? 'Bỏ áp dụng' : 'Áp dụng lại'}</button>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Box 3: Payment info */}
            <div className="border-2 border-black flex flex-col bg-white">
              <div className="border-b-2 border-black p-3 font-bold uppercase text-[15px] bg-white tracking-wide">
                {isHistory ? '3. THÔNG TIN THANH TOÁN' : '3. THANH TOÁN (PREPAYMENT)'}
              </div>

              <div className="p-4 flex flex-col bg-white">
                {isHistory || isPaidReceipt ? (
                  /* ── HISTORY MODE: read-only payment summary ── */
                  <>
                    <div className="grid grid-cols-2 gap-4 text-sm font-semibold mb-4">
                      <div className="flex flex-col gap-1.5">
                        <label>Trạng thái</label>
                        <span className={`inline-flex px-3 py-2 border text-xs font-bold uppercase w-fit ${PAID_STATUS_CLS[detail.paymentStatus] ?? 'bg-gray-100 text-gray-700 border-gray-300'}`}>
                          {formatVietnameseLabel(detail.paymentStatus, PAYMENT_STATUS_LABELS)}
                        </span>
                      </div>
                      <div className="flex flex-col gap-1.5">
                        <label>Trạng thái phiếu thu</label>
                        <span className={`inline-flex px-3 py-2 border text-xs font-bold uppercase w-fit ${RECEIPT_STATUS_CLS[normalizedReceiptStatus] ?? 'bg-gray-100 text-gray-700 border-gray-300'}`}>
                          {formatVietnameseLabel(normalizedReceiptStatus, { ...RECEIPT_STATUS_LABELS, ...RECEIPT_STATUS_LABEL })}
                        </span>
                      </div>
                      <div className="flex flex-col gap-1.5">
                        <label>Phương thức thanh toán</label>
                        <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={PAYMENT_METHOD_LABEL[detail.paymentMethod] ?? detail.paymentMethod ?? 'N/A'} />
                      </div>
                      <div className="flex flex-col gap-1.5">
                        <label>Ngày tạo hóa đơn</label>
                        <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={detail.createdAt ? new Date(detail.createdAt).toLocaleString('vi-VN') : ''} />
                      </div>
                      <div className="flex flex-col gap-1.5">
                        <label>Thời gian thanh toán</label>
                        <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed" value={resolvedPaidAt ? new Date(resolvedPaidAt).toLocaleString('vi-VN') : 'N/A'} />
                      </div>
                      {detail.transactionId && (
                        <div className="flex flex-col gap-1.5 col-span-2">
                          <label>Mã giao dịch</label>
                          <input type="text" disabled className="w-full border border-gray-300 bg-gray-100 p-2 text-black cursor-not-allowed font-mono" value={detail.transactionId} />
                        </div>
                      )}
                      {detail.eInvoiceUrl && normalizedReceiptStatus === 'ISSUED' && (
                        <div className="flex flex-col gap-1.5 col-span-2">
                          <label>Hóa đơn điện tử</label>
                          <a
                            href={detail.eInvoiceUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex w-fit items-center gap-2 border border-black bg-white px-4 py-2 text-sm font-bold uppercase text-black transition-colors hover:bg-gray-100"
                          >
                            Mở hóa đơn điện tử
                          </a>
                        </div>
                      )}
                      {detail.receiptErrorMessage && normalizedReceiptStatus === 'FAILED' && (
                        <div className="flex flex-col gap-1.5 col-span-2">
                          <label>Lý do lỗi phát hành</label>
                          <textarea disabled className="min-h-[88px] w-full resize-none border border-red-300 bg-red-50 p-2 text-sm text-red-700 cursor-not-allowed" value={detail.receiptErrorMessage} />
                        </div>
                      )}
                    </div>

                    <div className="border-t-2 border-black pt-4 flex justify-between items-center mb-5">
                      <div className="font-bold text-lg uppercase">TỔNG ĐÃ THANH TOÁN:</div>
                      <div className="font-bold text-xl text-green-700">{detail.customerPaid?.toLocaleString('vi-VN')} đ</div>
                    </div>

                    <div className="flex gap-3">
                      <button
                        onClick={() => router.back()}
                        className="border-2 border-black bg-white hover:bg-gray-100 px-6 py-3 font-bold uppercase text-sm transition-colors text-black"
                      >
                        ĐÓNG
                      </button>
                      {canPrintEinvoice && (
                        <button
                          onClick={handlePrintEinvoice}
                          className="border-2 border-black bg-white hover:bg-gray-100 px-6 py-3 font-bold uppercase text-sm transition-colors text-black disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          IN HÓA ĐƠN
                        </button>
                      )}
                      <button
                        onClick={() => handlePrint()}
                        className="flex-1 bg-black hover:bg-gray-900 text-white font-bold uppercase text-sm py-3 transition-colors border-2 border-black"
                      >
                        {isHistory ? 'IN PHIẾU THU' : 'IN LẠI PHIẾU THU'}
                      </button>
                    </div>
                  </>
                ) : (
                  /* ── PAYMENT MODE: tabs + QR + action buttons ── */
                  <>
                    <div className="flex w-full mb-4 border-b-2 border-black">
                      <button
                        onClick={() => setPaymentMethod('CASH')}
                        className={`flex-1 py-2 font-bold text-sm tracking-wide ${paymentMethod === 'CASH' ? 'bg-black text-white' : 'bg-white text-black hover:bg-gray-100'}`}
                      >
                        TIỀN MẶT
                      </button>
                      <button
                        onClick={handleSwitchToQR}
                        className={`flex-1 py-2 border-l-2 border-black font-bold text-sm tracking-wide ${paymentMethod === 'QR' ? 'bg-black text-white' : 'bg-white text-black hover:bg-gray-100'}`}
                      >
                        CHUYỂN KHOẢN / QR
                      </button>
                    </div>

                    <div className="flex-1 border border-gray-300 border-dashed p-4 flex flex-col items-center justify-center min-h-[160px] bg-gray-50/50 mb-4">
                      {paymentMethod === 'QR' ? (
                        qrPaymentUrl ? (
                          <img
                            src={qrPaymentUrl}
                            alt="QR Mã Thanh Toán"
                            className="w-full h-[300px] object-contain"
                          />
                        ) : (
                          <div className="text-sm italic text-slate-600">
                            {paymentLoading ? 'Đang tạo mã QR...' : 'Không tải được mã QR'}
                          </div>
                        )
                      ) : (
                        <div className="text-sm italic text-slate-600">[Thanh toán bằng tiền mặt]</div>
                      )}
                    </div>

                    <div className="flex justify-between items-center mb-5">
                      <div className="font-bold text-lg uppercase">TỔNG CỘNG:</div>
                      <div className="font-bold text-xl">{displayCustomerPaid.toLocaleString('vi-VN')} đ</div>
                    </div>

                    <div className="grid grid-cols-[120px_1fr_1fr] gap-4">
                      <button
                        onClick={() => router.back()}
                        className="border-2 border-black bg-white hover:bg-gray-100 py-3 font-bold uppercase text-sm transition-colors text-black"
                      >
                        ĐÓNG
                      </button>
                      <button
                        disabled
                        className="border-2 border-black bg-white py-3 font-bold uppercase text-sm text-black opacity-50 cursor-not-allowed"
                        title="Phiếu thu/hóa đơn điện tử được xử lý khi lịch hẹn đã hoàn tất"
                      >
                        XUẤT SAU HOÀN TẤT
                      </button>
                      <button
                        onClick={() => {
                          if (paymentMethod === 'CASH') {
                            setShowPaymentConfirm(true);
                          } else {
                            handlePayment();
                          }
                        }}
                        disabled={paymentLoading}
                        className="bg-black hover:bg-gray-900 text-white font-bold uppercase text-sm py-3 transition-colors border-2 border-black disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {paymentLoading ? 'ĐANG XỬ LÝ...' : 'THANH TOÁN & IN PHIẾU'}
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>

          </div>
        </div>
      </div>

      {/* Confirmation dialog for cash payment */}
      {showPaymentConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white border-2 border-black p-6 max-w-md w-full mx-4 shadow-[4px_4px_0_0_rgba(0,0,0,1)]">
            <h3 className="text-lg font-bold uppercase mb-4 border-b-2 border-black pb-3">Xác nhận thanh toán</h3>
            <p className="text-sm mb-2">Bạn có chắc chắn muốn thanh toán bằng <strong>tiền mặt</strong>?</p>
            <p className="text-sm mb-1">Số tiền: <strong className="text-lg">{displayCustomerPaid.toLocaleString('vi-VN')} đ</strong></p>
            <p className="text-sm mb-6 text-gray-600">Bệnh nhân: <strong>{detail.patientName}</strong></p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowPaymentConfirm(false)}
                className="flex-1 border-2 border-black bg-white hover:bg-gray-100 py-3 font-bold uppercase text-sm transition-colors"
              >
                HỦY
              </button>
              <button
                onClick={() => {
                  setShowPaymentConfirm(false);
                  handlePayment();
                }}
                className="flex-1 bg-black hover:bg-gray-900 text-white font-bold uppercase text-sm py-3 transition-colors border-2 border-black"
              >
                XÁC NHẬN
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function InvoiceDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return (
    <Suspense fallback={
      <div className="p-8 bg-gray-50 min-h-screen flex items-center justify-center">
        <div className="bg-white p-8 rounded border-2 border-black font-bold uppercase shadow-[4px_4px_0_0_rgba(0,0,0,1)]">
          Đang tải...
        </div>
      </div>
    }>
      <InvoiceDetailContent id={id} />
    </Suspense>
  );
}
