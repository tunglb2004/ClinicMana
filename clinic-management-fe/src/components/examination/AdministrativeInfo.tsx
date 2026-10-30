'use client';

import { User, Calendar, FileText, Phone, MapPin, Hash } from 'lucide-react';

interface AdministrativeInfoProps {
  administrativeInfo: {
    recordId: string;
    birthYear?: number;
    fullName: string;
    phoneNumber?: string;
    gender?: string;
    bloodType?: string;
    allergies?: string;
    address?: string;
    departmentName?: string;
    appointmentDate: string;
    appointmentTime: string;
  };
}

export function AdministrativeInfo({ administrativeInfo }: AdministrativeInfoProps) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  const formatGender = (gender?: string) => {
    if (gender === 'MALE') return 'Nam';
    if (gender === 'FEMALE') return 'Nữ';
    return 'Khác';
  };

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 mb-6">
      <h3 className="text-base font-bold text-gray-800 mb-4 flex items-center gap-2">
        <User className="w-5 h-5 text-[#8B5CF6]" />
        Thông tin bệnh nhân
      </h3>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* Mã phiếu */}
        <div>
          <p className="text-xs text-gray-500 uppercase tracking-wide font-semibold mb-1">Mã phiếu</p>
          <div className="bg-gray-50 rounded-lg px-3 py-2 border border-gray-200">
            <Hash className="w-4 h-4 text-gray-500 mr-2" />
            <p className="font-mono text-sm font-semibold text-gray-900">{administrativeInfo.recordId}</p>
          </div>
        </div>

        {/* Tuổi */}
        {administrativeInfo.birthYear && (
          <div>
            <p className="text-xs text-gray-500 uppercase tracking-wide font-semibold mb-1">Tuổi</p>
            <p className="text-base font-semibold text-gray-900">
              {new Date().getFullYear() - administrativeInfo.birthYear} tuổi
            </p>
          </div>
        )}

        {/* Họ tên */}
        <div>
          <p className="text-xs text-gray-500 uppercase tracking-wide font-semibold mb-1">Họ tên</p>
          <p className="text-base font-semibold text-gray-900">{administrativeInfo.fullName}</p>
        </div>

        {/* SĐT */}
        {administrativeInfo.phoneNumber && (
          <div>
            <p className="text-xs text-gray-500 uppercase tracking-wide font-semibold mb-1">SĐT</p>
            <div className="flex items-center gap-2">
              <Phone className="w-4 h-4 text-gray-600" />
              <p className="text-base font-semibold text-gray-900 font-mono">{administrativeInfo.phoneNumber}</p>
            </div>
          </div>
        )}

        {/* Giới tính */}
        <div>
          <p className="text-xs text-gray-500 uppercase tracking-wide font-semibold mb-1">Giới tính</p>
          <div className="flex items-center gap-2">
            <FileText className="w-4 h-4 text-gray-600" />
            <p className="text-base font-semibold text-gray-900">
              {administrativeInfo.gender ? formatGender(administrativeInfo.gender) : '--'}
            </p>
          </div>
        </div>

        {/* Khoa */}
        {administrativeInfo.departmentName && (
          <div>
            <p className="text-xs text-gray-500 uppercase tracking-wide font-semibold mb-1">Khoa</p>
            <div className="flex items-center gap-2">
              <MapPin className="w-4 h-4 text-gray-600" />
              <p className="text-base font-semibold text-gray-900">{administrativeInfo.departmentName}</p>
            </div>
          </div>
        )}

        {/* Ngày khám */}
        {administrativeInfo.appointmentDate && (
          <div>
            <p className="text-xs text-gray-500 uppercase tracking-wide font-semibold mb-1">Ngày khám</p>
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4 text-gray-600" />
              <p className="text-base font-semibold text-gray-900">
                {formatDate(administrativeInfo.appointmentDate)}
                {administrativeInfo.appointmentTime && (
                  <span className="ml-2 text-sm text-gray-600">
                    {administrativeInfo.appointmentTime}
                  </span>
                )}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
