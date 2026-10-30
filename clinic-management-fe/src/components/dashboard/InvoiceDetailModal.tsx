import { useState, useEffect } from 'react';
import { financialService } from '@/services/financial.service';
import { FinancialDetailResponse } from '@/types/financial.types';
import { patientService, PatientResponse } from '@/services/patient.service';
import { useToast } from '@/hooks/use-toast';

interface InvoiceDetailModalProps {
  invoiceId: string;
  onClose: () => void;
}

export function InvoiceDetailModal({ invoiceId, onClose }: InvoiceDetailModalProps) {
  const [detail, setDetail] = useState<FinancialDetailResponse | null>(null);
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [paymentMethod, setPaymentMethod] = useState<'TIEN_MAT' | 'CHUYEN_KHOAN'>('TIEN_MAT');
  const [bhytCode, setBhytCode] = useState('');
  const [showPaymentConfirm, setShowPaymentConfirm] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        const res = await financialService.getInvoiceDetail(invoiceId);
        if (res && res.invoiceId) {
          setDetail(res);
          if (res.patientId) {
            try {
               const pRes = await patientService.getPatientById(res.patientId);
               setPatient(pRes);
            } catch (err) {
               console.error(err);
            }
          }
        }
      } catch (error) {
        console.error('Failed to fetch invoice detail', error);
      } finally {
        setLoading(false);
      }
    };
    fetchDetail();
  }, [invoiceId]);

  const handlePrint = () => {
    if (!detail) return;
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
            <div class="title">Phiếu Khám ${detail.invoiceId}</div>
            <div class="subtitle">Phòng Khám Đa Khoa</div>
          </div>
          <div class="info-grid">
            <div>
               <div class="info-item"><span class="info-label">Mã số PK: </span> ${detail.invoiceId}</div>
               <div class="info-item"><span class="info-label">Ngày: </span> ${new Date(detail.createdAt).toLocaleString('vi-VN')}</div>
               <div class="info-item"><span class="info-label">Khách hàng: </span> ${detail.patientName}</div>
            </div>
            <div>
               <div class="info-item"><span class="info-label">SĐT: </span> ${detail.patientPhone}</div>
               <div class="info-item"><span class="info-label">Khoa: </span> ${detail.departmentName || ''}</div>
               <div class="info-item"><span class="info-label">Bác sĩ: </span> ${detail.doctorName}</div>
               <div class="info-item"><span class="info-label">Phòng: </span> ${detail.roomName || ''}</div>
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
               ${detail.services?.map((svc, i) => {
                 const bhytPaid = 0;
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
            <div class="summary-item"><div class="label">Tổng phí dịch vụ:</div><div class="value">${detail.totalServiceFee?.toLocaleString('vi-VN')} đ</div></div>
            <div class="summary-item" style="color: green;"><div class="label">BHYT chi trả:</div><div class="value">-${(detail.insurancePaid ?? 0).toLocaleString('vi-VN')} đ</div></div>
            <div class="summary-item summary-total"><div class="label">Khách trả:</div><div class="value">${detail.customerPaid?.toLocaleString('vi-VN')} đ</div></div>
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

  const handleExportReceipt = async () => {
    try {
      await financialService.exportReceipt(invoiceId);
      toast({
        description: 'Xuất phiếu thu thành công',
        variant: 'success'
      });
    } catch (e: unknown) {
       const errorMessage =
         typeof e === 'object' &&
         e !== null &&
         'response' in e &&
         typeof e.response === 'object' &&
         e.response !== null &&
         'data' in e.response &&
         typeof e.response.data === 'object' &&
         e.response.data !== null &&
         'message' in e.response.data &&
         typeof e.response.data.message === 'string'
           ? e.response.data.message
           : 'Có lỗi xảy ra khi xuất phiếu thu';
       toast({
         description: errorMessage,
         variant: 'error'
       });
    }
  };

  const numberToWords = (num: number): string => {
    if (num === 0) return 'Không đồng';
    const str = num.toLocaleString('vi-VN');
    return str + ' đồng';
  };

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  if (loading) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4">
        <div className="bg-white p-8 rounded border border-slate-200 font-bold uppercase shadow-xl shadow-slate-900/10">Đang tải...</div>
      </div>
    );
  }

  if (!detail) return null;

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 lg:p-10 overflow-y-auto"
      onClick={handleBackdropClick}
    >
      <div className="w-full max-w-[1280px] bg-[#fdfdfd] text-slate-950 font-sans shadow-2xl flex flex-col relative max-h-full overflow-y-auto border border-slate-200" onClick={e => e.stopPropagation()}>
        
        {/* Content wrapper */}
        <div className="p-4 flex flex-col lg:flex-row gap-6">
          
          {/* LEFT COLUMN - Box 1 */}
          <div className="lg:w-1/2 flex flex-col">
            <div className="border border-slate-200 flex flex-col h-full bg-white relative">
              {/* Header Box 1 */}
              <div className="border-b border-slate-200 p-3 font-bold uppercase text-[15px] bg-white text-slate-950 tracking-wide">
                1. THÔNG TIN BỆNH NHÂN
              </div>
              
              <div className="p-5 flex flex-col gap-4">
                {/* Row 1 */}
                <div className="grid grid-cols-2 gap-4 text-sm font-semibold">
                  <div className="flex flex-col gap-1.5">
                    <label>Họ và tên (*)</label>
                    <input 
                      type="text" 
                      disabled 
                      className="w-full border border-gray-300 bg-gray-100 p-2 text-slate-950 cursor-not-allowed font-bold"
                      value={detail.patientName || ''}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label>Số điện thoại (*)</label>
                    <input 
                      type="text" 
                      disabled 
                      className="w-full border border-gray-300 bg-gray-100 p-2 text-slate-950 cursor-not-allowed"
                      value={detail.patientPhone || patient?.phoneNumber || ''}
                    />
                  </div>
                </div>

                {/* Row 2 */}
                <div className="grid grid-cols-2 gap-4 text-sm font-semibold">
                  <div className="flex flex-col gap-1.5">
                    <label>CCCD / CMND (*)</label>
                    <input 
                      type="text" 
                      disabled 
                      className="w-full border border-gray-300 bg-gray-100 p-2 text-slate-950 cursor-not-allowed"
                      value={patient?.cccd || ''}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label>Ngày sinh</label>
                    <input 
                      type="text" 
                      disabled 
                      className="w-full border border-gray-300 bg-gray-100 p-2 text-slate-950 cursor-not-allowed"
                      value={patient?.dateOfBirth ? new Date(patient.dateOfBirth).toLocaleDateString('en-US') : ''}
                    />
                  </div>
                </div>

                {/* Row 3 */}
                <div className="grid grid-cols-2 gap-4 text-sm font-semibold">
                  <div className="flex flex-col gap-1.5">
                    <label>Giới tính</label>
                    <input 
                      type="text" 
                      disabled 
                      className="w-full border border-gray-300 bg-gray-100 p-2 text-slate-950 cursor-not-allowed"
                      value={patient?.gender === 'MALE' ? 'Nam' : (patient?.gender === 'FEMALE' ? 'Nữ' : 'Nam')}
                    />
                  </div>
                </div>

                {/* Row 4 */}
                <div className="flex flex-col gap-1.5 text-sm font-semibold">
                  <label>Địa chỉ</label>
                  <input 
                    type="text" 
                    disabled 
                    className="w-full border border-gray-300 bg-gray-100 p-2 text-slate-950 cursor-not-allowed"
                    value={patient?.address || ''}
                  />
                </div>

                {/* Row 5 */}
                <div className="flex flex-col gap-1.5 text-sm font-semibold">
                  <label>Triệu chứng / Lý do khám (*)</label>
                  <textarea 
                    disabled 
                    className="w-full border border-gray-300 bg-gray-100 p-2 text-slate-950 cursor-not-allowed resize-none h-24"
                    value={patient?.backgroundDisease || 'Đau đầu, buồn nôn.'}
                  />
                </div>



              </div>
            </div>
          </div>

          {/* RIGHT COLUMNS - Box 2 & 3 */}
          <div className="lg:w-1/2 flex flex-col gap-6">
            
            {/* Box 2 */}
            <div className="border border-slate-200 flex flex-col bg-white">
              <div className="border-b border-slate-200 p-3 font-bold uppercase text-[15px] bg-white tracking-wide flex justify-between">
                <span>2. DỊCH VỤ & CHỈ ĐỊNH PHÒNG</span>
              </div>
              
              <div className="p-4 flex flex-col gap-4">
                {/* Table */}
                <div className="overflow-x-auto border border-gray-300">
                  <table className="w-full text-xs text-left">
                    <thead className="bg-white border-b border-gray-300 font-bold uppercase">
                      <tr>
                        <th className="p-2 border-r border-gray-300 text-center">STT</th>
                        <th className="p-2 border-r border-gray-300">Dịch vụ đã chọn</th>
                        <th className="p-2 border-r border-gray-300">Phòng</th>
                        <th className="p-2 border-r border-gray-300 text-center">ĐVT</th>
                        <th className="p-2 border-r border-gray-300 text-center">Số lượng</th>
                        <th className="p-2 border-r border-gray-300 text-right">Đơn giá</th>
                        <th className="p-2 border-r border-gray-300 text-right">Thành tiền (Đồng)</th>
                        <th className="p-2 border-r border-gray-300 text-right">Quỹ BHYT</th>
                        <th className="p-2 text-right">Giá</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detail.services?.map((svc, i) => (
                        <tr key={i} className="border-b border-gray-200">
                          <td className="p-2 border-r border-gray-300 text-center">{i + 1}</td>
                          <td className="p-2 border-r border-gray-300 align-top font-medium text-gray-900">{svc.serviceName}</td>
                        <td className="p-2 border-r border-gray-300 align-top text-gray-700">{svc.assignedRoomName || '—'}</td>
                          <td className="p-2 border-r border-gray-300 text-center">Lần</td>
                          <td className="p-2 border-r border-gray-300 text-center">1</td>
                          <td className="p-2 border-r border-gray-300 text-right">{svc.price?.toLocaleString('vi-VN')}</td>
                          <td className="p-2 border-r border-gray-300 text-right">{svc.price?.toLocaleString('vi-VN')}</td>
                          <td className="p-2 border-r border-gray-300 text-right">0</td>
                          <td className="p-2 text-right font-bold">{svc.price?.toLocaleString('vi-VN')}</td>
                        </tr>
                      )) || (
                        <tr><td colSpan={9} className="p-4 text-center">Không có dịch vụ</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>

                <div className="flex justify-end font-bold text-sm">
                  <span className="mr-8">Tổng tiền</span>
                  <span>{detail.totalServiceFee?.toLocaleString('vi-VN')} VNĐ</span>
                </div>

                {/* BHYT */}
                <div className="font-bold text-sm text-slate-950 flex items-center gap-2 mt-2">
                  <span className="w-20">Bảo hiểm:</span>
                  <div className="flex-1 flex gap-2">
                    <input 
                      type="text" 
                      placeholder="Nhập mã BHYT" 
                      className="border border-gray-400 p-2 flex-1 text-sm outline-none focus:border-blue-500"
                      value={bhytCode}
                      onChange={e => setBhytCode(e.target.value)}
                    />
                    <button className="bg-blue-600 text-white px-4 py-2 font-bold hover:bg-gray-800 transition">Áp dụng</button>
                    <button className="border border-gray-400 text-slate-950 px-4 py-2 font-bold hover:bg-gray-100 transition">Làm mới</button>
                  </div>
                </div>

                {/* Summary Box */}
                <div className="text-sm border-t border-dashed border-gray-400 mt-2 pt-3 flex flex-col gap-1 font-semibold">
                  <div className="font-bold">Tổng kết:</div>
                  <div className="flex justify-between w-64 text-gray-700">
                    <span>- Số tiền BHYT chi trả:</span>
                    <span className="font-bold text-slate-950">{detail.insurancePaid?.toLocaleString('vi-VN') || 0} VNĐ</span>
                  </div>
                  <div className="flex justify-between w-64 text-gray-700">
                    <span>- Người bệnh phải trả:</span>
                    <span className="font-bold text-slate-950">{detail.customerPaid?.toLocaleString('vi-VN')} VNĐ</span>
                  </div>
                  <div className="mt-1 text-gray-700">
                    Số tiền phải đóng chữ: <span className="font-medium">{numberToWords(detail.customerPaid || 0)}</span>.
                  </div>
                </div>
              </div>
            </div>

            {/* Box 3 */}
            <div className="border border-slate-200 flex flex-col bg-white">
              <div className="border-b border-slate-200 p-3 font-bold uppercase text-[15px] bg-white tracking-wide">
                3. THANH TOÁN (PREPAYMENT)
              </div>
              
              <div className="p-4 flex flex-col h-full bg-white">
                
                {/* Tabs */}
                <div className="flex w-full mb-4 border-b border-slate-200">
                  <button 
                    onClick={() => setPaymentMethod('TIEN_MAT')}
                    className={`flex-1 py-2 font-bold text-sm tracking-wide ${paymentMethod === 'TIEN_MAT' ? 'bg-blue-600 text-white' : 'bg-white text-slate-950 hover:bg-gray-100'}`}
                  >
                    TIỀN MẶT
                  </button>
                  <button 
                    onClick={() => setPaymentMethod('CHUYEN_KHOAN')}
                    className={`flex-1 py-2 border-l border-slate-200 font-bold text-sm tracking-wide ${paymentMethod === 'CHUYEN_KHOAN' ? 'bg-blue-600 text-white' : 'bg-white text-slate-950 hover:bg-gray-100'}`}
                  >
                    CHUYỂN KHOẢN / QR
                  </button>
                </div>

                {/* Content */}
                <div className="flex-1 border border-gray-300 border-dashed p-4 flex flex-col items-center justify-center min-h-[160px] bg-gray-50/50 mb-4">
                  {paymentMethod === 'CHUYEN_KHOAN' ? (
                     <img 
                        src={`https://img.vietqr.io/image/970422-0987654321-compact2.png?amount=${detail.customerPaid || 0}&addInfo=${detail.invoiceId}&accountName=PHONG KHAM G2`} 
                        alt="QR Mã Thanh Toán" 
                        className="w-full h-[300px] object-contain"
                     />
                  ) : (
                     <div className="text-sm italic text-slate-600">[Thanh toán bằng tiền mặt]</div>
                  )}
                </div>

                <div className="flex justify-between items-center mb-6">
                  <div className="font-bold text-lg uppercase">TỔNG CỘNG:</div>
                  <div className="font-bold text-xl">{detail.customerPaid?.toLocaleString('vi-VN')} đ</div>
                </div>

                <div className="grid grid-cols-[120px_1fr_1fr] gap-4">
                  <button 
                    onClick={onClose}
                    className="border border-slate-200 bg-white hover:bg-gray-100 py-3 font-bold uppercase text-sm transition-colors text-slate-950"
                  >
                    ĐÓNG
                  </button>
                  <button 
                    onClick={handleExportReceipt}
                    className="border border-slate-200 bg-white hover:bg-gray-100 py-3 font-bold uppercase text-sm transition-colors text-slate-950"
                  >
                    XUẤT PHIẾU THU
                  </button>
                  <button 
                    onClick={() => {
                      if (paymentMethod === 'TIEN_MAT') {
                        setShowPaymentConfirm(true);
                      } else {
                        handlePrint();
                      }
                    }}
                    className="bg-blue-600 hover:bg-blue-700 text-white font-bold uppercase text-sm py-3 transition-colors border border-slate-200"
                  >
                    THANH TOÁN & IN PHIẾU
                  </button>
                </div>

              </div>
            </div>
          </div>

        </div>
      </div>

      {/* Confirmation dialog for cash payment */}
      {showPaymentConfirm && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/45" onClick={e => e.stopPropagation()}>
          <div className="bg-white border border-slate-200 p-6 max-w-md w-full mx-4 shadow-xl shadow-slate-900/10">
            <h3 className="text-lg font-bold uppercase mb-4 border-b border-slate-200 pb-3">Xác nhận thanh toán</h3>
            <p className="text-sm mb-2">Bạn có chắc chắn muốn thanh toán bằng <strong>tiền mặt</strong>?</p>
            <p className="text-sm mb-1">Số tiền: <strong className="text-lg">{detail.customerPaid?.toLocaleString('vi-VN')} đ</strong></p>
            <p className="text-sm mb-6 text-gray-600">Bệnh nhân: <strong>{detail.patientName}</strong></p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowPaymentConfirm(false)}
                className="flex-1 border border-slate-200 bg-white hover:bg-gray-100 py-3 font-bold uppercase text-sm transition-colors"
              >
                HỦY
              </button>
              <button
                onClick={() => {
                  setShowPaymentConfirm(false);
                  handlePrint();
                }}
                className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-bold uppercase text-sm py-3 transition-colors border border-slate-200"
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

