export const APPOINTMENT_STATUS_LABELS: Record<string, string> = {
  CHECKED_IN: 'Đã check-in - chờ thanh toán',
  REQUESTED: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  WAITING: 'Chờ khám',
  IN_PROGRESS: 'Đang khám',
  WAITING_RESULTS: 'Lưu tạm - chờ cận lâm sàng',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Không đến khám',
};

export const SERVICE_ORDER_STATUS_LABELS: Record<string, string> = {
  PAID: 'Chờ gọi',
  PENDING: 'Chờ trả kết quả',
  COMPLETED: 'Đã có kết quả',
  CANCELLED: 'Đã hủy',
};

export const PAYMENT_STATUS_LABELS: Record<string, string> = {
  PAID: 'Đã thanh toán',
  SUCCESS: 'Đã thanh toán',
  PENDING: 'Chờ thanh toán',
  PROCESSING: 'Đang xử lý',
  UNPAID: 'Chưa thanh toán',
  FAILED: 'Thanh toán lỗi',
  CANCELLED: 'Đã hủy',
};

export const RECEIPT_STATUS_LABELS: Record<string, string> = {
  NONE: 'Chưa phát hành',
  PROCESSING: 'Đang phát hành',
  ISSUED: 'Đã phát hành',
  FAILED: 'Phát hành lỗi',
  CANCELLED: 'Đã hủy',
};

export const VISIT_TYPE_LABELS: Record<string, string> = {
  INITIAL: 'Khám mới',
  FOLLOW_UP: 'Tái khám',
};

export const ORDER_TYPE_LABELS: Record<string, string> = {
  INITIAL: 'Dịch vụ ban đầu',
  SUPPLEMENT: 'Dịch vụ bổ sung',
};

export function formatVietnameseLabel(
  value: string | null | undefined,
  labels: Record<string, string>,
  fallback = 'Trạng thái khác'
) {
  const normalized = value?.trim().toUpperCase();
  if (!normalized) return 'Không xác định';
  return labels[normalized] ?? fallback;
}
