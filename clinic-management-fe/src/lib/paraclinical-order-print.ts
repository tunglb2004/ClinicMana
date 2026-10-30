'use client';

import type { AppointmentDetail } from '@/services/appointment.service';
import type { ServiceOrderItem } from '@/services/service-order.service';

function escapeHtml(value?: string | number | null) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatDate(value?: string | null) {
  if (!value) return '—';
  const datePart = value.split('T')[0];
  const [year, month, day] = datePart.split('-');
  if (!year || !month || !day) return value;
  return `${day}/${month}/${year}`;
}

function formatDateTime(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('vi-VN');
}

function formatMoney(value?: number | null) {
  if (typeof value !== 'number' || Number.isNaN(value)) return '—';
  return `${value.toLocaleString('vi-VN')} đ`;
}

function resolveExecutionRoomLabel(order: ServiceOrderItem, appointment: AppointmentDetail) {
  const orderType = (order.orderType ?? '').trim().toUpperCase();
  const paymentStatus = (order.paymentStatus ?? '').trim().toUpperCase();
  const isUnpaidSupplement = orderType === 'SUPPLEMENT'
    && (paymentStatus === '' || paymentStatus === 'UNPAID' || paymentStatus === 'PENDING');

  if (isUnpaidSupplement) {
    return 'Chờ thanh toán';
  }

  return order.assignedRoomName || appointment.roomName || 'Chưa phân phòng';
}

export function openParaclinicalOrderPrintWindow() {
  if (typeof window === 'undefined') return null;
  return window.open('', '_blank', 'width=960,height=720');
}

export function printParaclinicalOrderSlip(
  appointment: AppointmentDetail,
  orders: ServiceOrderItem[],
  indicationNote?: string,
  printWindow?: Window | null,
) {
  const targetWindow = printWindow ?? openParaclinicalOrderPrintWindow();
  if (!targetWindow) return false;

  const resolvedIndicationNote = indicationNote?.trim() || orders[0]?.indicationNote?.trim() || '';

  const totalAmount = orders.reduce((sum, order) => sum + (order.price ?? 0), 0);
  const servicesMarkup = orders
    .map(
      (order, index) => `
        <tr>
          <td>${index + 1}</td>
          <td>${escapeHtml(order.orderId)}</td>
          <td>${escapeHtml(order.serviceName)}</td>
          <td>${escapeHtml(resolveExecutionRoomLabel(order, appointment))}</td>
          <td class="amount">${formatMoney(order.price ?? null)}</td>
        </tr>`,
    )
    .join('');

  const html = `<!DOCTYPE html>
  <html lang="vi">
    <head>
      <meta charset="UTF-8" />
      <title>Phiếu Chỉ Định Cận Lâm Sàng</title>
      <style>
        * { box-sizing: border-box; }
        body {
          margin: 0;
          padding: 24px;
          font-family: Arial, Helvetica, sans-serif;
          color: #111827;
          background: #ffffff;
        }
        .sheet {
          max-width: 860px;
          margin: 0 auto;
          border: 1px solid #d1d5db;
          padding: 28px;
        }
        .header {
          display: flex;
          justify-content: space-between;
          gap: 24px;
          border-bottom: 2px solid #111827;
          padding-bottom: 16px;
          margin-bottom: 18px;
        }
        .brand {
          font-size: 13px;
          line-height: 1.6;
        }
        .title {
          text-align: right;
        }
        .title h1 {
          margin: 0;
          font-size: 26px;
          text-transform: uppercase;
          letter-spacing: 0.08em;
        }
        .title p {
          margin: 6px 0 0;
          font-size: 13px;
          color: #4b5563;
        }
        .section {
          margin-bottom: 18px;
        }
        .section h2 {
          margin: 0 0 10px;
          font-size: 15px;
          text-transform: uppercase;
          letter-spacing: 0.04em;
        }
        .grid {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          gap: 10px 22px;
          font-size: 14px;
        }
        .label {
          font-weight: 700;
        }
        table {
          width: 100%;
          border-collapse: collapse;
          font-size: 14px;
        }
        th, td {
          border: 1px solid #d1d5db;
          padding: 10px 12px;
          text-align: left;
          vertical-align: top;
        }
        th {
          background: #f3f4f6;
          text-transform: uppercase;
          font-size: 12px;
          letter-spacing: 0.04em;
        }
        .amount {
          text-align: right;
          white-space: nowrap;
        }
        .summary {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 12px;
          font-size: 14px;
        }
        .note {
          margin-top: 18px;
          border: 1px dashed #9ca3af;
          padding: 14px;
          font-size: 13px;
          line-height: 1.6;
          background: #f9fafb;
        }
        .signatures {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          gap: 32px;
          margin-top: 34px;
          text-align: center;
          font-size: 14px;
        }
        .signatures strong {
          display: block;
          margin-bottom: 70px;
        }
        @media print {
          body { padding: 0; }
          .sheet { border: none; padding: 0; }
        }
      </style>
    </head>
    <body>
      <div class="sheet">
        <div class="header">
          <div class="brand">
            <div><strong>PHONG KHAM</strong></div>
            <div>Phieu huong dan thanh toan can lam sang</div>
          </div>
          <div class="title">
            <h1>Phiếu chỉ định cận lâm sàng</h1>
            <p>In lúc: ${escapeHtml(formatDateTime(new Date().toISOString()))}</p>
          </div>
        </div>

        <div class="section">
          <h2>Thông tin bệnh nhân</h2>
          <div class="grid">
            <div><span class="label">Mã lịch hẹn:</span> ${escapeHtml(appointment.appointmentId)}</div>
            <div><span class="label">Mã bệnh nhân:</span> ${escapeHtml(appointment.patientId)}</div>
            <div><span class="label">Họ tên:</span> ${escapeHtml(appointment.patientName)}</div>
            <div><span class="label">Số điện thoại:</span> ${escapeHtml(appointment.patientPhone || '—')}</div>
            <div><span class="label">Ngày khám:</span> ${escapeHtml(formatDate(appointment.appointmentDate))}</div>
            <div><span class="label">Giờ khám:</span> ${escapeHtml(appointment.appointmentTime || '—')}</div>
            <div><span class="label">Bác sĩ chỉ định:</span> ${escapeHtml(appointment.doctorName || '—')}</div>
            <div><span class="label">Khoa khám:</span> ${escapeHtml(appointment.departmentName || '—')}</div>
          </div>
        </div>

        <div class="section">
          <h2>Dịch vụ cận lâm sàng được chỉ định</h2>
          <table>
            <thead>
              <tr>
                <th>STT</th>
                <th>Mã chỉ định</th>
                <th>Dịch vụ</th>
                <th>Phòng thực hiện</th>
                <th>Đơn giá</th>
              </tr>
            </thead>
            <tbody>
              ${servicesMarkup}
            </tbody>
          </table>
          <div class="summary">
            <div><span class="label">Số lượng dịch vụ:</span> ${orders.length}</div>
            <div><span class="label">Tổng tạm tính:</span> ${formatMoney(totalAmount)}</div>
          </div>
        </div>

        <div class="note">
          <div><span class="label">Lưu ý:</span> Bệnh nhân mang phiếu này đến quầy lễ tân hoặc thu ngân để thanh toán trước khi thực hiện cận lâm sàng.</div>
          <div><span class="label">Ghi chú chỉ định:</span> ${escapeHtml(resolvedIndicationNote || 'Không có')}</div>
          <div><span class="label">Ghi chú khám:</span> ${escapeHtml(appointment.notes || 'Không có')}</div>
        </div>

        <div class="signatures">
          <div>
            <strong>Bác sĩ chỉ định</strong>
            <div>${escapeHtml(appointment.doctorName || '—')}</div>
          </div>
          <div>
            <strong>Bệnh nhân / Người nhà</strong>
            <div>(Ký và ghi rõ họ tên)</div>
          </div>
        </div>
      </div>
    </body>
  </html>`;

  targetWindow.document.open();
  targetWindow.document.write(html);
  targetWindow.document.close();
  window.setTimeout(() => {
    targetWindow.focus();
    targetWindow.print();
  }, 250);
  return true;
}