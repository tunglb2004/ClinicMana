import ResetPasswordForm from '@/components/feature/auth/ResetPasswordForm';

export const metadata = {
  title: 'Đặt lại mật khẩu - Clinic Management',
  description: 'Đặt lại mật khẩu của bạn bằng mã OTP',
};

export default function ResetPasswordPage() {
  return <ResetPasswordForm />;
}
