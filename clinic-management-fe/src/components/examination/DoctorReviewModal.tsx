'use client';

import { useState } from 'react';
import { Star, X, MessageSquare } from 'lucide-react';
import toast from 'react-hot-toast';

interface DoctorReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { rating: number; reviewText: string }) => Promise<void>;
  doctorInfo: {
    doctorId: string;
    doctorName: string;
    doctorAvatarUrl?: string;
    departmentName: string;
  };
  hasReviewed?: boolean;
}

export function DoctorReviewModal({
  isOpen,
  onClose,
  onSubmit,
  doctorInfo,
}: DoctorReviewModalProps) {
  const [rating, setRating] = useState(5);
  const [reviewText, setReviewText] = useState('');

  const handleSubmit = async () => {
    if (rating < 1) {
      toast.error('Vui lòng chọn ít nhất 1 sao.');
      return;
    }

    try {
      await onSubmit({ rating, reviewText: reviewText.trim() });
      toast.success('Đánh giá đã được gửi thành công!');
      setRating(5);
      setReviewText('');
      onClose();
    } catch (error) {
      console.error('Failed to submit review:', error);
      toast.error('Không thể gửi đánh giá. Vui lòng thử lại.');
    }
  };

  const handleCancel = () => {
    setRating(5);
    setReviewText('');
    onClose();
  };

  const renderStarSelector = (currentRating: number, onChange: (value: number) => void) => {
    return (
      <div className="flex items-center gap-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            onClick={() => onChange(star)}
            className="transition-transform hover:scale-110"
          >
            <Star
              className={`w-6 h-6 ${
                star <= currentRating ? 'fill-yellow-400 text-yellow-400' : 'fill-gray-200 text-gray-200'
              }`}
            />
          </button>
        ))}
        <span className="ml-2 text-sm font-semibold text-gray-700">{currentRating}/5</span>
      </div>
    );
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-gradient-to-r from-[#8B5CF6] to-[#7C3AED] text-white rounded-t-xl px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold">Đánh giá bác sĩ</h2>
              <p className="text-sm text-white/80 mt-1">
                Đánh giá bác sĩ đã khám cho bạn
              </p>
            </div>
            <button
              onClick={handleCancel}
              className="flex items-center justify-center w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        <div className="p-6 space-y-6">
          <div className="flex items-start gap-4 mb-6 pb-6 border-b border-gray-200">
            {doctorInfo.doctorAvatarUrl ? (
              <img
                src={doctorInfo.doctorAvatarUrl}
                alt={doctorInfo.doctorName}
                className="w-16 h-16 rounded-full object-cover border-2 border-gray-200 flex-shrink-0"
              />
            ) : (
              <div className="w-16 h-16 rounded-full bg-gradient-to-br from-[#8B5CF6] to-[#7C3AED] border-2 border-gray-200 flex items-center justify-center flex-shrink-0">
                <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
                  <MessageSquare className="w-4 h-4 text-white" />
                </div>
              </div>
            )}

            <div className="flex-1 min-w-0">
              <p className="text-lg font-bold text-gray-900">{doctorInfo.doctorName}</p>
              <p className="text-sm text-gray-600 mt-1">{doctorInfo.departmentName}</p>
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              Mức độ hài lòng <span className="text-red-500">*</span>
            </label>
            <div className="flex items-center justify-center gap-4">
              {renderStarSelector(rating, setRating)}
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Nhận xét của bạn
            </label>
            <textarea
              value={reviewText}
              onChange={(e) => setReviewText(e.target.value)}
              placeholder="Chia sẻ trải nghiệm của bạn về bác sĩ..."
              rows={5}
              className="w-full px-4 py-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#8B5CF6] focus:border-transparent resize-none"
              maxLength={500}
            />
            <div className="flex justify-between items-center mt-1">
              <p className="text-xs text-gray-500">
                Hãy chia sẻ chi tiết để giúp cải thiện dịch vụ
              </p>
              <p className="text-xs text-gray-400">{reviewText.length}/500</p>
            </div>
          </div>

          <div className="flex gap-3 pt-4 border-t border-gray-200">
            <button
              onClick={handleCancel}
              className="flex-1 px-4 py-3 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-semibold"
            >
              HỦY BỎ
            </button>
            <button
              onClick={handleSubmit}
              disabled={rating < 1}
              className="flex-1 px-4 py-3 bg-gradient-to-r from-[#8B5CF6] to-[#7C3AED] text-white rounded-lg hover:from-[#7C3AED] hover:to-[#6D28D9] transition-all font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              GỬI ĐÁNH GIÁ
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
