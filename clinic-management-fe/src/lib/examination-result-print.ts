'use client';

import type { AppointmentDetail } from '@/services/appointment.service';
import type { ServiceOrderItem } from '@/services/service-order.service';
import type { PrescriptionMedicine } from '@/types/examination.types';

const CLINIC_HOTLINE = '1900 1234';
const CLINIC_WEBSITE = 'lam-san.vn';

type SymptomsData = {
  mach?: string;
  huyetAp?: string;
  nhietDo?: string;
  nhipTim?: string;
  canNang?: string;
  chieuCao?: string;
  daiTien?: string;
  tieuTien?: string;
  moTa?: string;
};

type ExaminationResultPrintPayload = {
  appointment: AppointmentDetail;
  symptoms?: string | SymptomsData | null;
  diagnosis?: string | null;
  doctorNotes?: string | null;
  serviceOrders?: ServiceOrderItem[];
  prescription?: PrescriptionMedicine | null;
};

type ExaminationResultPrintOptions = {
  autoPrint?: boolean;
};

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

function formatPrintedAt(value = new Date()) {
  const time = value.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
  return `${time}, ngày ${formatDate(value.toISOString())}`;
}

function formatAge(value?: string | null) {
  if (!value) return '—';
  const dob = new Date(value);
  if (Number.isNaN(dob.getTime())) return '—';
  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const beforeBirthday =
    now.getMonth() < dob.getMonth() ||
    (now.getMonth() === dob.getMonth() && now.getDate() < dob.getDate());
  if (beforeBirthday) age -= 1;
  return age >= 0 ? String(age) : '—';
}

function parseSymptoms(symptoms?: string | SymptomsData | null): SymptomsData {
  if (!symptoms) return {};
  if (typeof symptoms !== 'string') return symptoms;

  try {
    return JSON.parse(symptoms) as SymptomsData;
  } catch {
    return { moTa: symptoms };
  }
}

function renderText(value?: string | number | null, fallback = '—') {
  const text = typeof value === 'string' ? value.trim() : value;
  return escapeHtml(text || fallback);
}

function renderMultiline(value?: string | null, fallback = 'Không có') {
  const text = value?.trim();
  if (!text) return escapeHtml(fallback);
  return escapeHtml(text).replace(/\r?\n/g, '<br />');
}

function resolveClinicName(appointment: AppointmentDetail) {
  const departmentName = appointment.departmentName?.trim();
  if (!departmentName) return 'PHÒNG KHÁM ĐA KHOA G2';
  if (/^phòng khám/i.test(departmentName)) {
    return departmentName.toLocaleUpperCase('vi-VN');
  }

  return `PHÒNG KHÁM ${departmentName.toLocaleUpperCase('vi-VN')}`;
}

function resolveClinicAddress(appointment: AppointmentDetail) {
  const roomName = appointment.roomName?.trim() || appointment.roomId?.trim();
  const departmentName = appointment.departmentName?.trim();
  const parts = [roomName, departmentName].filter(Boolean);
  return parts.length > 0 ? parts.join(', ') : 'Phòng khám trung tâm';
}

function medicalLogo() {
  return `
    <svg class="clinic-logo" viewBox="0 0 64 64" aria-hidden="true">
      <defs>
        <linearGradient id="clinicLogoGradient" x1="8" y1="8" x2="56" y2="56" gradientUnits="userSpaceOnUse">
          <stop stop-color="#0b2a66" />
          <stop offset="1" stop-color="#10a36f" />
        </linearGradient>
      </defs>
      <path fill="#0b2a66" d="M27 4h10a7 7 0 0 1 7 7v9h9a7 7 0 0 1 7 7v10a7 7 0 0 1-7 7h-9v9a7 7 0 0 1-7 7H27a7 7 0 0 1-7-7v-9h-9a7 7 0 0 1-7-7V27a7 7 0 0 1 7-7h9v-9a7 7 0 0 1 7-7Z" opacity=".96" />
      <path fill="#10a36f" d="M39 20h14a7 7 0 0 1 7 7v10a7 7 0 0 1-7 7H39V20Z" opacity=".92" />
      <path fill="#ffffff" d="M32 12c5.1 0 9.2 4.1 9.2 9.2 0 3.8-2.6 7.8-7.8 12l-1.4 1.1-1.4-1.1c-5.2-4.2-7.8-8.2-7.8-12C22.8 16.1 26.9 12 32 12Zm0 6.1a3.1 3.1 0 1 0 0 6.2 3.1 3.1 0 0 0 0-6.2Z" />
      <path fill="none" stroke="url(#clinicLogoGradient)" stroke-linecap="round" stroke-linejoin="round" stroke-width="4" d="M17 43c4.5 9 13.5 14 24 10 5.4-2.2 8.9-6.7 10.5-12.5" />
    </svg>`;
}

function sectionIcon(name: 'patient' | 'clinical' | 'document' | 'flask' | 'info') {
  const paths = {
    patient: '<path d="M20 21a8 8 0 1 0-16 0" /><circle cx="12" cy="7" r="4" />',
    clinical: '<path d="M19 14c1.5-1.5 3-3.7 3-6a5 5 0 0 0-9-3 5 5 0 0 0-9 3c0 5.5 8 11 8 11s2.1-1.4 4.3-3.3" /><path d="M3.2 13h4l2-3 4 7 2-4h5.6" />',
    document: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6" /><path d="M8 13h8" /><path d="M8 17h5" />',
    flask: '<path d="M9 3h6" /><path d="M10 3v6l-5.4 9.4A2.4 2.4 0 0 0 6.7 22h10.6a2.4 2.4 0 0 0 2.1-3.6L14 9V3" /><path d="M7.5 16h9" />',
    info: '<circle cx="12" cy="12" r="10" /><path d="M12 10v7" /><path d="M12 7h.01" />',
  };

  return `
    <svg class="section-icon" viewBox="0 0 24 24" aria-hidden="true">
      ${paths[name]}
    </svg>`;
}

function vitalIcon(name: 'pulse' | 'pressure' | 'temperature' | 'heart' | 'scale' | 'height' | 'clipboard' | 'drop') {
  const paths = {
    pulse: '<path d="M3 12h4l2-5 4 10 2-5h6" />',
    pressure: '<path d="M9 21V8a3 3 0 0 1 6 0v13" /><path d="M9 12h6" /><path d="M12 3V1" />',
    temperature: '<path d="M14 14.8V5a4 4 0 0 0-8 0v9.8a6 6 0 1 0 8 0Z" /><path d="M10 7v8" />',
    heart: '<path d="M19 14c1.5-1.5 3-3.7 3-6a5 5 0 0 0-9-3 5 5 0 0 0-9 3c0 5.5 8 11 8 11s2.1-1.4 4.3-3.3" /><path d="M3.2 13h4l2-3 4 7 2-4h5.6" />',
    scale: '<path d="M6 20h12l-1-12H7L6 20Z" /><path d="M9 8a3 3 0 0 1 6 0" /><path d="M12 11v3" />',
    height: '<path d="M12 3v18" /><path d="m8 7 4-4 4 4" /><path d="m8 17 4 4 4-4" /><path d="M5 21h14" />',
    clipboard: '<path d="M9 3h6l1 2h3v16H5V5h3l1-2Z" /><path d="M9 11h6" /><path d="M9 15h4" />',
    drop: '<path d="M12 2s7 7.1 7 12a7 7 0 0 1-14 0c0-4.9 7-12 7-12Z" />',
  };

  return `
    <svg class="vital-icon" viewBox="0 0 24 24" aria-hidden="true">
      ${paths[name]}
    </svg>`;
}

function renderPatientItem(label: string, value?: string | number | null, fallback = '—') {
  return `
    <div class="patient-item">
      <span class="patient-label">${escapeHtml(label)}:</span>
      <span class="patient-value">${renderText(value, fallback)}</span>
    </div>`;
}

function renderVitalItem(
  iconName: 'pulse' | 'pressure' | 'temperature' | 'heart' | 'scale' | 'height' | 'clipboard' | 'drop',
  label: string,
  value?: string | null,
  unit?: string,
  fallback = '—',
) {
  const hasValue = Boolean(value?.trim());
  return `
    <div class="vital-item">
      ${vitalIcon(iconName)}
      <span class="vital-label">${escapeHtml(label)}:</span>
      <span class="${hasValue ? 'vital-value' : 'vital-missing'}">${renderText(value, fallback)}</span>
      ${unit ? `<span class="vital-unit">${escapeHtml(unit)}</span>` : ''}
    </div>`;
}

function renderParaclinicalRows(serviceOrders: ServiceOrderItem[]) {
  const activeOrders = serviceOrders.filter(order => (order.status ?? '').toUpperCase() !== 'CANCELLED');

  if (activeOrders.length === 0) {
    return `
      <tr>
        <td colspan="3" class="empty">Không có kết quả cận lâm sàng.</td>
      </tr>`;
  }

  return activeOrders
    .map((order, index) => {
      return `
        <tr>
          <td class="center">${index + 1}</td>
          <td>${renderText(order.serviceName)}</td>
          <td>${renderMultiline(order.resultNote, 'Không có')}</td>
        </tr>`;
    })
    .join('');
}

export function openExaminationResultPrintWindow() {
  if (typeof window === 'undefined') return null;
  return window.open('', '_blank', 'width=960,height=720');
}

export function printExaminationResultSlip(
  payload: ExaminationResultPrintPayload,
  printWindow?: Window | null,
  options: ExaminationResultPrintOptions = {},
) {
  const targetWindow = printWindow ?? openExaminationResultPrintWindow();
  if (!targetWindow) return false;

  const { appointment, diagnosis, doctorNotes, serviceOrders = [] } = payload;
  const shouldAutoPrint = options.autoPrint ?? true;
  const symptoms = parseSymptoms(payload.symptoms);
  const clinicName = resolveClinicName(appointment);
  const clinicAddress = resolveClinicAddress(appointment);
  const clinicRoom = appointment.roomName || appointment.roomId || clinicAddress;
  const recordId = appointment.medicalRecord?.recordId;

  const html = `<!DOCTYPE html>
  <html lang="vi">
    <head>
      <meta charset="UTF-8" />
      <title>Phiếu kết quả khám${recordId ? ` - ${escapeHtml(recordId)}` : ''}</title>
      <style>
        * { box-sizing: border-box; }
        body {
          margin: 0;
          padding: 18px;
          font-family: Arial, Helvetica, sans-serif;
          color: #10224a;
          background: #ffffff;
        }
        .print-actions {
          position: sticky;
          top: 0;
          z-index: 20;
          display: flex;
          justify-content: flex-end;
          gap: 8px;
          max-width: 980px;
          margin: 0 auto 12px;
          padding: 8px 0;
          background: #ffffff;
        }
        .print-action {
          border: 1px solid #0b2a66;
          border-radius: 6px;
          background: #0b2a66;
          color: #ffffff;
          cursor: pointer;
          font: 700 13px Arial, Helvetica, sans-serif;
          padding: 9px 14px;
        }
        .print-action.secondary {
          background: #ffffff;
          color: #0b2a66;
        }
        .sheet {
          max-width: 980px;
          margin: 0 auto;
          padding: 6px 6px 0;
        }
        .header {
          display: grid;
          grid-template-columns: minmax(0, 1fr) auto;
          gap: 24px;
          align-items: center;
          border-bottom: 3px solid #0b2a66;
          padding-bottom: 16px;
          margin-bottom: 14px;
        }
        .clinic {
          display: grid;
          grid-template-columns: 76px minmax(0, 1fr);
          gap: 16px;
          align-items: center;
        }
        .clinic-logo {
          width: 76px;
          height: 76px;
          display: block;
        }
        .clinic-name {
          margin: 0 0 8px;
          color: #0b2a66;
          font-size: 24px;
          line-height: 1.1;
          font-weight: 800;
          text-transform: uppercase;
        }
        .clinic-meta {
          color: #10224a;
          font-size: 15px;
          line-height: 1.55;
        }
        .title {
          text-align: right;
          min-width: 360px;
        }
        .title h1 {
          margin: 0;
          color: #0b2a66;
          font-size: 36px;
          line-height: 1.1;
          font-weight: 900;
          text-transform: uppercase;
        }
        .title p {
          margin: 14px 0 0;
          color: #5d6b86;
          font-size: 16px;
        }
        .section {
          border: 1px solid #cfd8e6;
          border-radius: 10px;
          padding: 18px;
          margin-bottom: 12px;
        }
        .section-title {
          display: flex;
          align-items: center;
          gap: 10px;
          margin: 0 0 18px;
          color: #0b2a66;
          font-size: 21px;
          line-height: 1.2;
          font-weight: 800;
          text-transform: uppercase;
        }
        .section-icon {
          width: 26px;
          height: 26px;
          color: #10a36f;
          stroke: currentColor;
          fill: none;
          stroke-width: 2.2;
          stroke-linecap: round;
          stroke-linejoin: round;
          flex: 0 0 auto;
        }
        .patient-grid {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          column-gap: 72px;
          row-gap: 18px;
          font-size: 16px;
        }
        .patient-item {
          display: grid;
          grid-template-columns: 150px minmax(0, 1fr);
          gap: 20px;
          align-items: baseline;
        }
        .patient-label {
          color: #33456c;
          font-weight: 700;
        }
        .patient-value {
          color: #10224a;
          font-weight: 500;
        }
        .vitals-grid {
          display: grid;
          grid-template-columns: repeat(3, minmax(0, 1fr));
          gap: 24px 42px;
          font-size: 16px;
        }
        .vital-item {
          display: grid;
          grid-template-columns: 28px auto auto minmax(0, 1fr);
          gap: 10px;
          align-items: center;
          min-height: 30px;
        }
        .vital-icon {
          width: 23px;
          height: 23px;
          color: #0b2a66;
          stroke: currentColor;
          fill: none;
          stroke-width: 2;
          stroke-linecap: round;
          stroke-linejoin: round;
        }
        .vital-label {
          color: #22365f;
          white-space: nowrap;
        }
        .vital-value {
          color: #10a36f;
          font-size: 20px;
          font-weight: 800;
          white-space: nowrap;
        }
        .vital-unit {
          color: #10224a;
          white-space: nowrap;
        }
        .vital-missing {
          color: #10224a;
          font-weight: 500;
          white-space: nowrap;
        }
        .text-section {
          padding: 14px 18px 16px;
        }
        .text-section .section-title {
          margin-bottom: 12px;
          font-size: 18px;
          text-transform: none;
        }
        .text-content {
          margin-left: 32px;
          color: #10224a;
          font-size: 16px;
          line-height: 1.6;
          min-height: 24px;
        }
        table {
          width: 100%;
          border-collapse: separate;
          border-spacing: 0;
          overflow: hidden;
          border: 1px solid #cfd8e6;
          border-radius: 6px;
          font-size: 15px;
        }
        th, td {
          border-right: 1px solid #cfd8e6;
          border-bottom: 1px solid #cfd8e6;
          padding: 14px 12px;
          text-align: left;
          vertical-align: top;
        }
        tr:last-child td {
          border-bottom: 0;
        }
        th:last-child, td:last-child {
          border-right: 0;
        }
        th {
          background: #f8fafc;
          color: #0b2a66;
          text-align: center;
          text-transform: uppercase;
          font-size: 14px;
          font-weight: 800;
        }
        .center {
          text-align: center;
        }
        a {
          color: #0058d6;
          font-weight: 600;
          text-decoration: none;
        }
        .empty {
          color: #64748b;
          text-align: center;
        }
        .signatures {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          min-height: 165px;
          text-align: center;
        }
        .signature-box {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: space-between;
          padding: 12px 24px 4px;
          color: #0b2a66;
          font-size: 16px;
        }
        .signature-title {
          font-weight: 800;
          text-transform: uppercase;
        }
        .signature-space {
          height: 74px;
          width: 220px;
        }
        .signature-name {
          color: #10224a;
          font-weight: 500;
        }
        .signature-note {
          color: #10224a;
          font-style: italic;
        }
        .notice {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 12px;
          border: 1px solid #a7e3d1;
          border-radius: 8px;
          background: #eefcf8;
          color: #0d6b59;
          padding: 12px 16px;
          font-size: 15px;
          margin-top: 16px;
        }
        @page {
          size: A4;
          margin: 12mm;
        }
        @media print {
          body { padding: 0; }
          .print-actions { display: none; }
          .sheet { max-width: none; padding: 0; }
          a { color: #0058d6; }
        }
      </style>
    </head>
    <body>
      <div class="print-actions">
        <button class="print-action" type="button" onclick="window.print()">Tải / In PDF</button>
        <button class="print-action secondary" type="button" onclick="window.close()">Đóng</button>
      </div>
      <div class="sheet">
        <div class="header">
          <div class="clinic">
            ${medicalLogo()}
            <div>
              <h2 class="clinic-name">${escapeHtml(clinicName)}</h2>
              <div class="clinic-meta">
                <div>${escapeHtml(clinicAddress)}</div>
                <div>Hotline: ${escapeHtml(CLINIC_HOTLINE)} | Website: ${escapeHtml(CLINIC_WEBSITE)}</div>
              </div>
            </div>
          </div>
          <div class="title">
            <h1>Phiếu kết quả khám</h1>
            <p>In lúc: ${escapeHtml(formatPrintedAt())}</p>
          </div>
        </div>

        <section class="section">
          <h2 class="section-title">${sectionIcon('patient')}Thông tin bệnh nhân</h2>
          <div class="patient-grid">
            ${renderPatientItem('Mã lịch hẹn', appointment.appointmentId)}
            ${renderPatientItem('Mã hồ sơ', appointment.medicalRecord?.recordId)}
            ${renderPatientItem('Mã bệnh nhân', appointment.patientId)}
            ${renderPatientItem('Họ tên', appointment.patientName)}
            ${renderPatientItem('Ngày sinh', formatDate(appointment.patientDateOfBirth))}
            ${renderPatientItem('Tuổi', formatAge(appointment.patientDateOfBirth))}
            ${renderPatientItem('Số điện thoại', appointment.patientPhone)}
            ${renderPatientItem('BHYT', appointment.patientHealthInsuranceNumber, 'Không có')}
            ${renderPatientItem('Ngày khám', formatDate(appointment.appointmentDate))}
            ${renderPatientItem('Giờ khám', appointment.appointmentTime)}
            ${renderPatientItem('Bác sĩ khám', appointment.doctorName)}
            ${renderPatientItem('Khoa', appointment.departmentName)}
            <div></div>
            ${renderPatientItem('Phòng khám', clinicRoom)}
          </div>
        </section>

        <section class="section">
          <h2 class="section-title">${sectionIcon('clinical')}Thông tin lâm sàng</h2>
          <div class="vitals-grid">
            ${renderVitalItem('pulse', 'Mạch', symptoms.mach, 'lần/phút')}
            ${renderVitalItem('pressure', 'Huyết áp', symptoms.huyetAp, 'mmHg')}
            ${renderVitalItem('clipboard', 'Đại tiện', symptoms.daiTien, undefined, 'Không ghi nhận')}
            ${renderVitalItem('temperature', 'Nhiệt độ', symptoms.nhietDo, '°C')}
            ${renderVitalItem('heart', 'Nhịp tim', symptoms.nhipTim, 'lần/phút')}
            ${renderVitalItem('drop', 'Tiểu tiện', symptoms.tieuTien, undefined, 'Không ghi nhận')}
            ${renderVitalItem('scale', 'Cân nặng', symptoms.canNang, 'kg')}
            ${renderVitalItem('height', 'Chiều cao', symptoms.chieuCao, 'cm')}
          </div>
        </section>

        <section class="section text-section">
          <h2 class="section-title">${sectionIcon('document')}Mô tả triệu chứng</h2>
          <div class="text-content">${renderMultiline(symptoms.moTa, 'Không có')}</div>
        </section>

        <section class="section text-section">
          <h2 class="section-title">${sectionIcon('document')}Kết luận khám</h2>
          <div class="text-content">${renderMultiline(diagnosis, 'Không có')}</div>
        </section>

        <section class="section text-section">
          <h2 class="section-title">${sectionIcon('document')}Dặn dò / Ghi chú</h2>
          <div class="text-content">${renderMultiline(doctorNotes, 'Không có')}</div>
        </section>

        <section class="section">
          <h2 class="section-title">${sectionIcon('flask')}Kết quả cận lâm sàng</h2>
          <table>
            <thead>
              <tr>
                <th style="width: 9%;">STT</th>
                <th style="width: 36%;">Dịch vụ</th>
                <th>Kết quả / Nhận xét</th>
              </tr>
            </thead>
            <tbody>
              ${renderParaclinicalRows(serviceOrders)}
            </tbody>
          </table>
        </section>

        <section class="section signatures">
          <div class="signature-box">
            <div class="signature-title">Bác sĩ khám</div>
            <div class="signature-space"></div>
            <div class="signature-name">${renderText(appointment.doctorName)}</div>
          </div>
          <div class="signature-box">
            <div class="signature-title">Bệnh nhân / Người nhà</div>
            <div class="signature-space"></div>
            <div class="signature-note">(Ký và ghi rõ họ tên)</div>
          </div>
        </section>

        <div class="notice">
          ${sectionIcon('info')}
          <span>Phiếu kết quả chỉ có giá trị tại thời điểm khám. Vui lòng liên hệ phòng khám nếu có thắc mắc.</span>
        </div>
      </div>
    </body>
  </html>`;

  targetWindow.document.open();
  targetWindow.document.write(html);
  targetWindow.document.close();
  if (shouldAutoPrint) {
    targetWindow.setTimeout(() => {
      targetWindow.focus();
      targetWindow.print();
    }, 250);
  }
  return true;
}
