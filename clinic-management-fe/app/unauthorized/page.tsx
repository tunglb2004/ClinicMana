import Link from 'next/link';

export default function UnauthorizedPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-md text-center">
        <div className="mb-6">
          <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-red-100">
            <svg
              className="h-10 w-10 text-red-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
          </div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900">
            Không có quyền truy cập
          </h1>
          <p className="text-gray-600">
            Bạn không có quyền truy cập vào trang này. Vui lòng liên hệ quản trị
            viên nếu bạn cho rằng đây là lỗi.
          </p>
        </div>

        <div className="space-y-3">
          <Link
            href="/dashboard"
            className="block w-full rounded-md bg-blue-600 px-4 py-3 font-medium text-white hover:bg-blue-700"
          >
            Về trang chủ
          </Link>
          <Link
            href="/login"
            className="block w-full rounded-md border border-gray-300 bg-white px-4 py-3 font-medium text-gray-700 hover:bg-gray-50"
          >
            Đăng nhập với tài khoản khác
          </Link>
        </div>
      </div>
    </div>
  );
}
