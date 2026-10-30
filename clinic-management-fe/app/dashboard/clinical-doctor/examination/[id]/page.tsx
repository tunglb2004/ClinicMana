'use client';

import { useState, useEffect } from 'react';
import { DashboardHeader } from '@/components/layout/DashboardHeader';
import { useParams } from 'next/navigation';
import { patientService, PatientResponse } from '@/services/patient.service';

export default function ExaminationPage() {
  const params = useParams();
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPatient = async () => {
      if (!params.id) return;
      try {
        const data = await patientService.getPatientById(params.id as string);
        setPatient(data);
      } catch (error) {
        console.error('Error fetching patient details:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchPatient();
  }, [params.id]);

  const specialties = [
    { id: 1, name: 'Đa khoa' },
    { id: 2, name: 'Tim mạch' },
    { id: 3, name: 'Huyết học' },
    { id: 4, name: 'Thần kinh học' },
  ];

  if (loading) return <div className="p-8 text-center">Đang tải dữ liệu bệnh nhân...</div>;
  if (!patient) return <div className="p-8 text-center text-red-500">Khong tìm thấy thông tin bệnh nhân!</div>;

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      <DashboardHeader 
        title="Khám bệnh" 
        subtitle="Chi tiết khám bệnh của bệnh nhân"
      />

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">
        
        {/* THÔNG TIN BỆNH NHÂN */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4 uppercase">Thông tin bệnh nhân</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mb-4">
            <div><span className="font-medium">Mã BN:</span> BN{patient.userId}</div>
            <div><span className="font-medium">Họ tên:</span> {patient.fullName}</div>
            <div><span className="font-medium">Giới tính:</span> {patient.gender === 'M' ? 'Nam' : patient.gender === 'F' ? 'Nữ' : 'Chưa rõ'}</div>
            <div><span className="font-medium">Tuổi:</span> {patient.dateOfBirth ? new Date().getFullYear() - new Date(patient.dateOfBirth).getFullYear() : 'N/A'}</div>
            <div className="col-span-2"><span className="font-medium">SĐT:</span> {patient.phoneNumber || 'N/A'}</div>
          </div>
          <div className="text-sm space-y-2">
            <div>
              <span className="font-medium">Tiền sử bệnh:</span> {patient.backgroundDisease || 'Không ghi nhận'}
              <br/>
              <span className="font-medium">Dị ứng:</span> {patient.allergies || 'Không ghi nhận'}
            </div>
            <div className="flex items-center gap-2">
              <span className="font-medium">Trạng thái:</span>
              <span className="border text-xs font-medium px-2 py-1 rounded border-black text-black">ĐANG KHÁM</span>
            </div>
          </div>
        </div>

        {/* Khám lâm sàng */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4">Khám lâm sàng</h2>
          <div className="mb-4">
            <h3 className="font-medium text-sm mb-3">Triệu chứng lâm sàng</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <input type="text" placeholder="Mạch (lần/phút)" className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
              <input type="text" placeholder="Huyết áp (vd: 120/80)" className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
              <input type="text" placeholder="Nhiệt độ (°C)" className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
              <input type="text" placeholder="Nhịp tim" className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
              <input type="text" placeholder="Cân nặng (kg)" className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
              <input type="text" placeholder="Chiều cao (cm)" className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
              <input type="text" placeholder="Đại tiện" className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
              <input type="text" placeholder="Tiểu tiện" className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
            </div>
          </div>
          <textarea 
            placeholder="Mô tả chung..." 
            className="w-full border rounded-md px-3 py-2 text-sm min-h-[100px] focus:outline-blue-500"
          ></textarea>
        </div>

        {/* CHỈ ĐỊNH CHUYÊN KHOA */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4 uppercase">Chỉ định chuyên khoa</h2>
          <table className="w-full text-left text-sm mb-4">
            <thead className="border-b">
              <tr>
                <th className="py-2 font-medium text-gray-500 uppercase">STT</th>
                <th className="py-2 font-medium text-gray-500 uppercase">Tên chuyên khoa</th>
                <th className="py-2 font-medium text-gray-500 uppercase text-center w-24">Chọn</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {specialties.map((spec, index) => (
                <tr key={spec.id}>
                  <td className="py-3 text-gray-600">{index + 1}</td>
                  <td className="py-3 font-medium">{spec.name}</td>
                  <td className="py-3 text-center">
                    <input type="checkbox" className="h-4 w-4 rounded border-gray-300" />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <textarea 
            placeholder="Ghi chú chỉ định thêm..." 
            className="w-full border rounded-md px-3 py-2 text-sm mb-4 min-h-[80px] focus:outline-blue-500"
          ></textarea>
          <button className="bg-black text-white px-4 py-2 rounded text-sm font-medium hover:bg-gray-800">
            XÁC NHẬN CHỈ ĐỊNH
          </button>
        </div>

        {/* Đơn thuốc */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-bold">Đơn thuốc</h2>
            <button className="border border-black px-4 py-1.5 rounded text-sm font-medium hover:bg-gray-50">
              TẠO ĐƠN THUỐC
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <input type="text" placeholder="Tên thuốc..." className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
            <input type="text" placeholder="Ghi chú sử dụng..." className="border rounded-md px-3 py-2 text-sm focus:outline-blue-500" />
          </div>
        </div>

        {/* Kết luận & dặn dò */}
        <div className="bg-white border rounded-lg p-6 shadow-sm">
          <h2 className="text-lg font-bold mb-4">Kết luận & dặn dò</h2>
          <textarea 
            placeholder="Chẩn đoán cuối cùng và lời dặn..." 
            className="w-full border rounded-md px-3 py-2 text-sm min-h-[120px] focus:outline-blue-500"
          ></textarea>
        </div>
      </main>

      {/* Floating Action Bar */}
      <div className="fixed bottom-0 left-0 right-0 border-t bg-white p-4 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)] z-20">
        <div className="mx-auto max-w-7xl flex justify-end gap-3 px-4 sm:px-6 lg:px-8">
          <button className="border rounded px-6 py-2 text-sm font-medium hover:bg-gray-50 bg-white">LƯU TẠM</button>
          <button className="border rounded px-6 py-2 text-sm font-medium hover:bg-gray-50 bg-gray-200">LƯU</button>
          <button className="bg-gray-600 text-white rounded px-6 py-2 text-sm font-medium hover:bg-gray-700">HOÀN THÀNH KHÁM</button>
          <button className="bg-black text-white rounded px-6 py-2 text-sm font-medium hover:bg-gray-800">BÁC SĨ XÁC NHẬN</button>
        </div>
      </div>
    </div>
  );
}
