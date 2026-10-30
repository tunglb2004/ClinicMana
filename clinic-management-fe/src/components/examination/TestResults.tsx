'use client';

import { getResultFileName, triggerResultFileDownload } from '@/lib/result-file';
import { FileText, Calendar, Download, ExternalLink, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

interface TestResultsProps {
  labTests: { testName: string; testDate: string }[];
  testFileUrls: { testName: string; fileUrl: string }[];
  testResultSummaries: { testName: string; summary: string }[];
}

export function TestResults({
  labTests,
  testFileUrls,
  testResultSummaries,
}: TestResultsProps) {
  const handleDownloadFile = (testName: string, fileUrl?: string) => {
    if (!fileUrl) {
      toast.error('Không có file kết quả để tải xuống.');
      return;
    }

    try {
      triggerResultFileDownload(fileUrl, getResultFileName(fileUrl, `Ket_qua_${testName}`, 0));
      toast.success('Tải xuống thành công!');
    } catch (error) {
      console.error('Failed to download file:', error);
      toast.error('Không thể tải file kết quả.');
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  if (labTests.length === 0 && testResultSummaries.length === 0) {
    return null;
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 mb-6">
      <h3 className="text-base font-bold text-gray-800 mb-4 flex items-center gap-2">
        <FileText className="w-5 h-5 text-[#8B5CF6]" />
        Kết quả Cận lâm sàng
      </h3>

      <div className="space-y-4">
        {labTests.map((test, index) => (
          <div key={index} className="border-b border-gray-200 pb-4 last:pb-0 last:border-b-0">
            <div className="flex items-start gap-3 mb-2">
              <div className="flex-shrink-0 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-full p-2">
                <FileText className="w-4 h-4 text-blue-600" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <p className="text-sm font-semibold text-gray-900">{test.testName}</p>
                  <div className="flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-gray-500" />
                    <p className="text-xs text-gray-600">{formatDate(test.testDate)}</p>
                  </div>
                </div>

                {/* Download Button */}
                {testFileUrls[index]?.fileUrl ? (
                  <button
                    onClick={() => handleDownloadFile(test.testName, testFileUrls[index].fileUrl)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-[#8B5CF6] hover:text-[#7C3AED] border border-[#8B5CF6] hover:border-[#7C3AED] rounded-lg transition-colors"
                  >
                    <Download className="w-3.5 h-3.5" />
                    [📄 Xem file kết quả]
                  </button>
                ) : (
                  <button
                    disabled
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-gray-400 border border-gray-300 rounded-lg cursor-not-allowed"
                    title="File không có sẵn"
                  >
                    <AlertCircle className="w-3.5 h-3.5" />
                    [📄 Xem file kết quả]
                  </button>
                )}
              </div>
            </div>

            {/* Chi tiết kết quả */}
            {testResultSummaries[index]?.summary && (
              <div className="bg-gray-50 rounded-lg p-4 border border-gray-100">
                <p className="text-sm text-gray-700 leading-relaxed">
                  {testResultSummaries[index].summary}
                </p>
              </div>
            )}
          </div>
        ))}

        {/* External Link */}
        {labTests.length > 0 && (
          <div className="flex items-start gap-2 mt-4 pt-4 border-t border-gray-200">
            <ExternalLink className="w-4 h-4 text-gray-500 flex-shrink-0" />
            <p className="text-sm text-gray-600">
              Xem thêm kết quả xét nghiệm{' '}
              <a
                href="/dashboard/patient/test-results"
                className="text-[#8B5CF6] hover:text-[#7C3AED] font-semibold"
              >
                tại đây
              </a>
            </p>
          </div>
        )}
      </div>
    </div>
  );
}