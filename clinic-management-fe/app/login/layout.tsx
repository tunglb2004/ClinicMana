import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Đăng nhập - Clinic Management',
  description: 'Đăng nhập vào hệ thống quản lý phòng khám',
};

export default function LoginLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
