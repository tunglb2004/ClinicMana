'use client';

import { Fragment, useCallback, useEffect, useState } from 'react';
import { resolveResultFileUrl } from '@/lib/result-file';
import { healthRecordService } from '@/services/health-record.service';
import {
  HealthRecordInfo,
  MedicalDocument,
  MedicalHistoryItem,
  HealthRecordResponse,
} from '@/types/health-record.types';
import { serviceOrderService } from '@/services/service-order.service';
import { Loader2, Search, Download, Eye, FileText, Calendar, User, Shield, CalendarPlus } from 'lucide-react';
import { useRouter } from 'next/navigation';
import toast from 'react-hot-toast';

export default function HealthRecordPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [patientInfo, setPatientInfo] = useState<HealthRecordInfo | null>(null);
  const [medicalHistory, setMedicalHistory] = useState<MedicalHistoryItem[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');

  const fetchHealthRecord = useCallback(async () => {
    setLoading(true);
    try {
      const response: HealthRecordResponse = await healthRecordService.getHealthRecord({
        page: currentPage,
        size: pageSize,
        keyword: keyword,
      });

      setPatientInfo(response.patientInfo);
      setMedicalHistory(response.medicalHistory);
      setTotalCount(response.totalCount);
    } catch (error) {
      console.error('Failed to fetch health record:', error);
      toast.error('Không thể tải hồ sơ sức khỏe. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, keyword]);

  useEffect(() => {
    fetchHealthRecord();
  }, [fetchHealthRecord]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    setKeyword(searchInput);
  };

  const openResolvedUrl = (fileUrl: string) => {
    try {
      const resolvedUrl = resolveResultFileUrl(fileUrl);
      if (!resolvedUrl) {
        toast.error('Không tìm thấy file kết quả.');
        return;
      }

      const opened = window.open(resolvedUrl, '_blank', 'noopener,noreferrer');
      if (!opened) {
        const link = document.createElement('a');
        link.href = resolvedUrl;
        link.target = '_blank';
        link.rel = 'noopener noreferrer';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }
    } catch (error) {
      console.error('Failed to open document:', error);
      toast.error('Không thể mở tài liệu.');
    }
  };

  const handleOpenDocument = async (document: MedicalDocument) => {
    if (document.fileType === 'pdf' && document.orderId) {
      const previewWindow = window.open('', '_blank');
      if (previewWindow) {
        previewWindow.opener = null;
      }

      try {
        const pdfBlob = await serviceOrderService.exportMyPdf(document.orderId);
        const blobUrl = URL.createObjectURL(new Blob([pdfBlob], { type: 'application/pdf' }));
        if (previewWindow) {
          previewWindow.location.href = blobUrl;
        } else {
          const link = window.document.createElement('a');
          link.href = blobUrl;
          link.target = '_blank';
          link.rel = 'noopener noreferrer';
          window.document.body.appendChild(link);
          link.click();
          window.document.body.removeChild(link);
        }
        window.setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000);
        return;
      } catch (error) {
        console.error('Failed to export result PDF:', error);
        previewWindow?.close();
        toast.error('Không thể mở file kết quả PDF.');
        return;
      }
    }

    openResolvedUrl(document.fileUrl);
  };

  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize);
    setCurrentPage(0);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('vi-VN');
  };

  const getRecordPatientName = (item: MedicalHistoryItem) => {
    return item.patientName || patientInfo?.fullName || '--';
  };

  const isBookedForRelative = (item: MedicalHistoryItem) => {
    return Boolean(item.patientId && patientInfo?.patientId && item.patientId !== patientInfo.patientId);
  };

  const handleViewDetail = (recordId: string) => {
    router.push(`/dashboard/patient/examination-detail?id=${recordId}`);
  };

  const handleBookFollowUp = (item: MedicalHistoryItem) => {
    const bookForRelative = isBookedForRelative(item);
    const followUpReason = item.diagnosis?.trim()
      ? `Tái khám theo hồ sơ ${item.id}. Chẩn đoán trước: ${item.diagnosis.trim()}`
      : `Tái khám theo hồ sơ ${item.id}`;
    sessionStorage.setItem('appt_draft', JSON.stringify({
      bookFor: bookForRelative ? 'relative' : 'self',
      symptoms: followUpReason,
      relative: bookForRelative ? {
        patientId: item.patientId ?? undefined,
        fullName: getRecordPatientName(item),
      } : undefined,
      followUpSourceRecordId: item.id,
      followUpSourceRecord: {
        recordId: item.id,
        examinationDate: item.visitDate,
        diagnosisSummary: item.diagnosis,
        doctorName: item.doctorName,
        clinicalServiceId: item.clinicalServiceId,
        clinicalServiceName: item.clinicalServiceName,
      },
    }));
    router.push('/dashboard/patient/appointments/new/step2');
  };

  const renderDocuments = (item: MedicalHistoryItem) => (
    item.documents && item.documents.length > 0 ? (
      <div className="flex flex-wrap gap-1">
        {item.documents.slice(0, 2).map((doc) => (
          <button
            key={doc.documentId}
            type="button"
            className="text-[#8B5CF6] hover:text-[#7C3AED] hover:underline text-xs"
            onClick={() => handleOpenDocument(doc)}
          >
            {doc.fileName}
          </button>
        ))}
        {item.documents.length > 2 && (
          <span className="text-xs text-slate-500">
            +{item.documents.length - 2} file
          </span>
        )}
      </div>
    ) : (
      <span className="text-slate-500">Không có tài liệu</span>
    )
  );

  const renderActions = (item: MedicalHistoryItem) => (
    <div className="flex items-center justify-center gap-2">
      {item.documents && item.documents.length > 0 && (
        <button
          type="button"
          onClick={() => handleOpenDocument(item.documents[0])}
          className="flex items-center gap-1 px-3 py-1.5 text-sm text-[#8B5CF6] hover:text-[#7C3AED] border border-[#8B5CF6] hover:border-[#7C3AED] rounded-lg transition-colors"
          title="Mở kết quả đầu tiên"
        >
          <Download className="w-4 h-4" />
          <span className="hidden sm:inline">Mở file</span>
        </button>
      )}
      {item.canFollowUp && item.clinicalServiceId && (
        <button
          type="button"
          onClick={() => handleBookFollowUp(item)}
          className="flex items-center gap-1 px-3 py-1.5 text-sm text-green-700 hover:text-white border border-green-600 hover:bg-green-700 rounded-lg transition-colors"
          title="Đặt lịch tái khám"
        >
          <CalendarPlus className="w-4 h-4" />
          <span className="hidden sm:inline">Tái khám</span>
        </button>
      )}
    </div>
  );

  const renderDetailButton = (item: MedicalHistoryItem) => (
    <button
      type="button"
      onClick={() => handleViewDetail(item.id)}
      className="inline-flex items-center gap-1 px-3 py-1.5 text-sm text-[#1a2b3c] hover:text-white border border-gray-300 hover:border-[#1a2b3c] hover:bg-[#1a2b3c] rounded-lg transition-colors"
    >
      <Eye className="w-4 h-4" />
      <span className="hidden sm:inline">Chi tiết</span>
    </button>
  );

  const renderHistoryRow = (item: MedicalHistoryItem, indexLabel: number | string, isFollowUp = false) => (
    <tr
      key={item.id}
      className={`border-b border-gray-100 transition-colors ${
        isFollowUp ? 'bg-emerald-50/40 hover:bg-emerald-50' : 'hover:bg-gray-50'
      }`}
    >
      <td className="py-4 px-4 text-sm text-gray-600">{indexLabel}</td>
      <td className="py-4 px-4 text-sm text-gray-900 font-medium">
        <div className={isFollowUp ? 'flex flex-col gap-1 border-l-2 border-emerald-400 pl-3' : ''}>
          {isFollowUp && (
            <span className="inline-flex w-fit rounded-full border border-emerald-200 bg-white px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-emerald-700">
              Tái khám
            </span>
          )}
          <span>{formatDate(item.visitDate)}</span>
        </div>
      </td>
      <td className="py-4 px-4 text-sm text-gray-700">
        <div className="font-medium text-gray-900">{getRecordPatientName(item)}</div>
        {item.patientId && <div className="mt-0.5 text-xs text-gray-500 font-mono">{item.patientId}</div>}
        {isBookedForRelative(item) && (
          <span className="mt-1 inline-flex rounded-full bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700">
            Đặt hộ
          </span>
        )}
      </td>
      <td className="py-4 px-4 text-sm text-gray-700">{item.diagnosis}</td>
      <td className="py-4 px-4 text-sm text-gray-700">{item.doctorName}</td>
      <td className="py-4 px-4 text-sm text-gray-600">{renderDocuments(item)}</td>
      <td className="py-4 px-4 text-center">{renderActions(item)}</td>
      <td className="py-4 px-4 text-center">{renderDetailButton(item)}</td>
    </tr>
  );

  const totalPages = Math.ceil(totalCount / pageSize);

  return (
    <div className="min-h-full bg-gray-50 p-6 text-black">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-[#1a2b3c] mb-2">Hồ sơ sức khỏe</h1>
          <p className="text-sm text-gray-500">Xem thông tin hành chính và lịch sử khám bệnh</p>
        </div>

        {loading ? (
          <div className="flex items-center justify-center gap-2 py-32 text-slate-600">
            <Loader2 className="animate-spin" size={22} />
            <span>Đang tải hồ sơ sức khỏe...</span>
          </div>
        ) : (
          <>
            {/* Patient Information Section */}
            {patientInfo && (
              <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 mb-6">
                <h2 className="text-lg font-semibold text-[#1a2b3c] mb-4">Thông tin hành chính</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="flex items-start gap-3">
                    <User className="mt-0.5 h-5 w-5 flex-shrink-0 text-slate-500" />
                    <div>
                      <p className="text-xs text-gray-500 uppercase tracking-wide mb-1">Họ tên</p>
                      <p className="text-sm font-medium text-gray-900">{patientInfo.fullName}</p>
                    </div>
                  </div>

                  <div className="flex items-start gap-3">
                    <FileText className="mt-0.5 h-5 w-5 flex-shrink-0 text-slate-500" />
                    <div>
                      <p className="text-xs text-gray-500 uppercase tracking-wide mb-1">Mã BN</p>
                      <p className="text-sm font-medium text-gray-900 font-mono">{patientInfo.patientId}</p>
                    </div>
                  </div>

                  <div className="flex items-start gap-3">
                    <Calendar className="mt-0.5 h-5 w-5 flex-shrink-0 text-slate-500" />
                    <div>
                      <p className="text-xs text-gray-500 uppercase tracking-wide mb-1">Năm sinh</p>
                      <p className="text-sm font-medium text-gray-900">{patientInfo.birthYear}</p>
                    </div>
                  </div>

                  <div className="flex items-start gap-3">
                    <Shield className="mt-0.5 h-5 w-5 flex-shrink-0 text-slate-500" />
                    <div>
                      <p className="text-xs text-gray-500 uppercase tracking-wide mb-1">BHYT</p>
                      <p className="text-sm font-medium text-gray-900 font-mono">
                        {patientInfo.healthInsuranceNumber || 'Chưa có'}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Medical History Section */}
            <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
              {/* Section Header */}
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
                <h2 className="text-lg font-semibold text-[#1a2b3c]">Lịch sử khám bệnh</h2>

                {/* Search Bar */}
                <form onSubmit={handleSearch} className="flex gap-2 w-full sm:w-auto">
                  <div className="relative flex-1 sm:flex-initial">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
                    <input
                      type="text"
                      placeholder="Tìm kiếm theo chẩn đoán, bác sĩ..."
                      value={searchInput}
                      onChange={(e) => setSearchInput(e.target.value)}
                      className="pl-10 pr-4 py-2 w-full sm:w-80 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#8B5CF6] focus:border-transparent"
                    />
                  </div>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-[#8B5CF6] text-white rounded-lg hover:bg-[#7C3AED] transition-colors text-sm font-medium"
                  >
                    Tìm kiếm
                  </button>
                </form>
              </div>

              {/* Results Count */}
              {totalCount > 0 && (
                <div className="mb-4 text-sm text-gray-500">
                  Hiển thị <span className="font-medium">{Math.min((currentPage + 1) * pageSize, totalCount)}</span> /{' '}
                  <span className="font-medium">{totalCount}</span> kết quả
                </div>
              )}

              {/* Medical History Table */}
              {medicalHistory.length === 0 ? (
                <div className="text-center py-12">
                  <FileText className="mx-auto mb-3 h-12 w-12 text-slate-400" />
                  <p className="text-base text-slate-600">Chưa có lịch sử khám bệnh nào</p>
                  <p className="mt-1 text-sm text-slate-500">
                    {!keyword ? 'Lịch sử khám bệnh sẽ hiển thị ở đây sau khi bạn đi khám.' : 'Không tìm thấy kết quả phù hợp.'}
                  </p>
                </div>
              ) : (
                <>
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-gray-200">
                          <th className="text-left py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            STT
                          </th>
                          <th className="text-left py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            Ngày khám
                          </th>
                          <th className="text-left py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            Bệnh nhân
                          </th>
                          <th className="text-left py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            Chẩn đoán
                          </th>
                          <th className="text-left py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            Bác sĩ
                          </th>
                          <th className="text-left py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            Tài liệu / Kết quả
                          </th>
                          <th className="text-center py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            Thao tác
                          </th>
                          <th className="text-center py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                            Chi tiết
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {medicalHistory.map((item, index) => {
                          const rowNumber = index + 1 + currentPage * pageSize;
                          return (
                            <Fragment key={item.id}>
                          <tr className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                            <td className="py-4 px-4 text-sm text-gray-600">{rowNumber}</td>
                            <td className="py-4 px-4 text-sm text-gray-900 font-medium">
                              {formatDate(item.visitDate)}
                            </td>
                            <td className="py-4 px-4 text-sm text-gray-700">
                              <div className="font-medium text-gray-900">{getRecordPatientName(item)}</div>
                              {item.patientId && <div className="mt-0.5 text-xs text-gray-500 font-mono">{item.patientId}</div>}
                              {isBookedForRelative(item) && (
                                <span className="mt-1 inline-flex rounded-full bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700">
                                  Đặt hộ
                                </span>
                              )}
                            </td>
                            <td className="py-4 px-4 text-sm text-gray-700">{item.diagnosis}</td>
                            <td className="py-4 px-4 text-sm text-gray-700">{item.doctorName}</td>
                            <td className="py-4 px-4 text-sm text-gray-600">
                              {item.documents && item.documents.length > 0 ? (
                                <div className="flex flex-wrap gap-1">
                                  {item.documents.slice(0, 2).map((doc) => (
                                    <button
                                      key={doc.documentId}
                                      type="button"
                                      className="text-[#8B5CF6] hover:text-[#7C3AED] hover:underline text-xs"
                                      onClick={() => handleOpenDocument(doc)}
                                    >
                                      {doc.fileName}
                                    </button>
                                  ))}
                                  {item.documents.length > 2 && (
                                    <span className="text-xs text-slate-500">
                                      +{item.documents.length - 2} file
                                    </span>
                                  )}
                                </div>
                              ) : (
                                <span className="text-slate-500">Không có tài liệu</span>
                              )}
                            </td>
                            <td className="py-4 px-4 text-center">
                              {renderActions(item)}
                            </td>
                            <td className="py-4 px-4 text-center">
                              <button
                                type="button"
                                onClick={() => handleViewDetail(item.id)}
                                className="inline-flex items-center gap-1 px-3 py-1.5 text-sm text-[#1a2b3c] hover:text-white border border-gray-300 hover:border-[#1a2b3c] hover:bg-[#1a2b3c] rounded-lg transition-colors"
                              >
                                <Eye className="w-4 h-4" />
                                <span className="hidden sm:inline">Chi tiết</span>
                              </button>
                            </td>
                          </tr>
                          {item.followUps?.map((followUp, followUpIndex) =>
                            renderHistoryRow(followUp, `${rowNumber}.${followUpIndex + 1}`, true)
                          )}
                            </Fragment>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>

                  {/* Pagination */}
                  {totalPages > 1 && (
                    <div className="mt-6 flex flex-col sm:flex-row justify-between items-center gap-4">
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
      </div>
    </div>
  );
}
