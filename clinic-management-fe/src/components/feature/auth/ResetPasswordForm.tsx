'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { authService } from '@/services/auth.service';
import { extractErrorMessage, extractSuccessMessage } from '@/lib/errorHandler';
import { isValidPassword, PASSWORD_RULE_MESSAGE } from '@/lib/password-policy';
import type { ResetPasswordRequest } from '@/types/auth.types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Eye, EyeOff } from 'lucide-react';
import Link from 'next/link';

type ResetStep = 'email' | 'password';

interface ResetPasswordFormState extends ResetPasswordRequest {
  confirmPassword: string;
}

export default function ResetPasswordForm() {
  const router = useRouter();
  const [currentStep, setCurrentStep] = useState<ResetStep>('email');
  const [formData, setFormData] = useState<ResetPasswordFormState>({
    email: '',
    otpCode: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [successMessage, setSuccessMessage] = useState<string>('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError('');
  };

  const handleGoToPasswordStep = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    if (!formData.email) {
      setError('Vui lòng nhập email');
      setIsLoading(false);
      return;
    }

    try {
      const response = await authService.sendOtpForReset({
        email: formData.email,
        type: 'RESET_PASSWORD',
      });

      // ALWAYS use backend success message
      setSuccessMessage(extractSuccessMessage(response));
      setCurrentStep('password');
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      // ALWAYS use backend error message
      setError(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    // Validation
    if (!formData.otpCode || formData.otpCode.length !== 6) {
      setError('Vui lòng nhập mã OTP gồm 6 chữ số');
      setIsLoading(false);
      return;
    }

    if (!isValidPassword(formData.newPassword)) {
      setError(PASSWORD_RULE_MESSAGE);
      setIsLoading(false);
      return;
    }

    if (formData.newPassword !== formData.confirmPassword) {
      setError('Mật khẩu xác nhận không khớp');
      setIsLoading(false);
      return;
    }

    try {
      const response = await authService.resetPassword({
        email: formData.email,
        otpCode: formData.otpCode,
        newPassword: formData.newPassword,
      });

      // ALWAYS use backend success message
      setSuccessMessage(extractSuccessMessage(response));
      setTimeout(() => {
        router.push('/login');
      }, 2000);
    } catch (err: unknown) {
      // ALWAYS use backend error message
      setError(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-blue-100 flex items-center justify-center px-4 py-8">
      {/* Background Decoration */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-40 h-80 w-80 bg-blue-200 rounded-full mix-blend-multiply filter blur-3xl opacity-20" />
        <div className="absolute -bottom-40 -left-40 h-80 w-80 bg-blue-300 rounded-full mix-blend-multiply filter blur-3xl opacity-20" />
      </div>

      <Card className="w-full max-w-md shadow-2xl border border-gray-200 relative">
        <CardHeader className="space-y-2 text-center pb-6">
          <div className="flex justify-center mb-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-xl bg-gradient-to-br from-blue-100 to-blue-200 shadow-lg">
              <svg
                className="h-8 w-8 text-blue-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"
                />
              </svg>
            </div>
          </div>
          <CardTitle className="text-3xl font-bold bg-gradient-to-r from-blue-600 to-blue-800 bg-clip-text text-transparent">
            Đặt lại mật khẩu
          </CardTitle>
          <CardDescription className="text-base text-gray-600">
            {currentStep === 'email' && 'Nhập email và mã OTP của bạn'}
            {currentStep === 'password' && 'Tạo mật khẩu mới'}
          </CardDescription>
        </CardHeader>

        <CardContent>
          {/* Progress Indicator */}
          <div className="mb-6 flex gap-2">
            <div className={`h-1 flex-1 rounded-full transition-colors ${currentStep !== 'email' ? 'bg-blue-600' : 'bg-blue-600'}`} />
            <div className={`h-1 flex-1 rounded-full transition-colors ${currentStep === 'password' ? 'bg-blue-600' : 'bg-gray-300'}`} />
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
              {error}
            </div>
          )}

          {successMessage && (
            <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-700">
              {successMessage}
            </div>
          )}

          {/* Step 1: Email and OTP */}
          {currentStep === 'email' && (
            <form onSubmit={handleGoToPasswordStep} className="space-y-5">
              <div className="space-y-2.5">
                <Label htmlFor="email" className="text-sm font-medium text-gray-700">
                  Email <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="email"
                  name="email"
                  type="email"
                  placeholder="your@email.com"
                  value={formData.email}
                  onChange={handleChange}
                  required
                  disabled={isLoading}
                  className="h-10 rounded-lg border border-gray-300 px-3 py-2"
                />
              </div>
              
              <Button
                type="submit"
                disabled={isLoading || !formData.email}
                className="w-full h-10 bg-gradient-to-r from-blue-600 to-blue-800 hover:from-blue-700 hover:to-blue-900 text-white font-medium rounded-lg transition-all duration-200"
              >
                {isLoading ? 'Đang xử lý...' : 'Tiếp tục'}
              </Button>
            </form>
          )}

          {/* Step 2: New Password */}
          {currentStep === 'password' && (
            <form onSubmit={handleResetPassword} className="space-y-5">
              <div className="space-y-2.5">
                <Label htmlFor="newPassword" className="text-sm font-medium text-gray-700">
                  Mật khẩu mới <span className="text-red-500">*</span>
                </Label>
                <div className="relative">
                  <Input
                    id="newPassword"
                    name="newPassword"
                    type={showNewPassword ? 'text' : 'password'}
                    placeholder="Chữ hoa, chữ thường, số, ký tự đặc biệt"
                    value={formData.newPassword}
                    onChange={handleChange}
                    required
                    disabled={isLoading}
                    maxLength={100}
                    className="h-10 rounded-lg border border-gray-300 px-3 py-2 pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword((value) => !value)}
                    disabled={isLoading}
                    className="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-gray-500 hover:text-gray-700 disabled:opacity-60"
                    aria-label={showNewPassword ? 'An mat khau moi' : 'Hien mat khau moi'}
                  >
                    {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <p className="text-xs text-gray-500">{PASSWORD_RULE_MESSAGE}</p>
              </div>

              <div className="space-y-2.5">
                <Label htmlFor="confirmPassword" className="text-sm font-medium text-gray-700">
                  Xác nhận mật khẩu <span className="text-red-500">*</span>
                </Label>
                <div className="relative">
                  <Input
                    id="confirmPassword"
                    name="confirmPassword"
                    type={showConfirmPassword ? 'text' : 'password'}
                    placeholder="Xác nhận mật khẩu"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    required
                    disabled={isLoading}
                    maxLength={100}
                    className="h-10 rounded-lg border border-gray-300 px-3 py-2 pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword((value) => !value)}
                    disabled={isLoading}
                    className="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-gray-500 hover:text-gray-700 disabled:opacity-60"
                    aria-label={showConfirmPassword ? 'An mat khau xac nhan' : 'Hien mat khau xac nhan'}
                  >
                    {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div className="space-y-2.5">
                <Label htmlFor="otpCode" className="text-sm font-medium text-gray-700">
                  Mã OTP <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="otpCode"
                  name="otpCode"
                  type="text"
                  placeholder="000000"
                  value={formData.otpCode}
                  onChange={handleChange}
                  maxLength={6}
                  required
                  className="h-10 rounded-lg border border-gray-300 px-3 py-2 text-center text-2xl tracking-widest"
                />
              </div>

              <Button
                type="submit"
                disabled={isLoading || !formData.newPassword || !formData.confirmPassword}
                className="w-full h-10 bg-gradient-to-r from-blue-600 to-blue-800 hover:from-blue-700 hover:to-blue-900 text-white font-medium rounded-lg transition-all duration-200"
              >
                {isLoading ? 'Đang xử lý...' : 'Đặt lại mật khẩu'}
              </Button>

              <button
                type="button"
                onClick={() => setCurrentStep('email')}
                className="w-full text-sm text-blue-600 hover:text-blue-700 font-medium"
              >
                Quay lại
              </button>
            </form>
          )}

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600">
              Quay lại trang{' '}
              <Link href="/login" className="text-blue-600 hover:text-blue-700 font-medium">
                đăng nhập
              </Link>
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
