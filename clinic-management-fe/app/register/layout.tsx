import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Đăng ký - Clinic Management',
  description: 'Đăng ký tài khoản mới',
};

export default function RegisterLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
