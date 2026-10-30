'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { authService } from '@/services/auth.service';
import { extractErrorMessage, extractSuccessMessage } from '@/lib/errorHandler';
import { isValidPassword, PASSWORD_RULE_MESSAGE } from '@/lib/password-policy';
import type { ChangePasswordRequest } from '@/types/auth.types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Eye, EyeOff } from 'lucide-react';
import { getDashboardRoute, getRolesFromAccessToken } from '@/lib/roleRedirect';
import { useAuthStore } from '@/store/auth.store';

export default function ChangePasswordForm() {
  const router = useRouter();
  const { user, accessToken, refreshUserInfo } = useAuthStore();
  const [formData, setFormData] = useState<ChangePasswordRequest>({
    currentPassword: '',
    newPassword: '',
    confirmationPassword: '',
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [successMessage, setSuccessMessage] = useState<string>('');
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    new: false,
    confirm: false,
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    // Validation
    if (!formData.currentPassword) {
      setError('Vui lòng nhập mật khẩu hiện tại');
      setIsLoading(false);
      return;
    }

    if (!isValidPassword(formData.newPassword)) {
      setError(PASSWORD_RULE_MESSAGE);
      setIsLoading(false);
      return;
    }

    if (formData.newPassword !== formData.confirmationPassword) {
      setError('Mật khẩu xác nhận không khớp');
      setIsLoading(false);
      return;
    }

    if (formData.currentPassword === formData.newPassword) {
      setError('Mật khẩu mới không được giống mật khẩu hiện tại');
      setIsLoading(false);
      return;
    }

    try {
      const response = await authService.changePassword(formData);
      // ALWAYS use backend success message
      setSuccessMessage(extractSuccessMessage(response));

      // Reset form
      setFormData({
        currentPassword: '',
        newPassword: '',
        confirmationPassword: '',
      });

      // Redirect after success
      let redirectRoles = user?.roles ?? [];
      if (redirectRoles.length === 0) {
        redirectRoles = getRolesFromAccessToken(accessToken);
      }
      if (redirectRoles.length === 0) {
        await refreshUserInfo();
        const latestAuthState = useAuthStore.getState();
        redirectRoles = latestAuthState.user?.roles ?? [];
        if (redirectRoles.length === 0) {
          redirectRoles = getRolesFromAccessToken(latestAuthState.accessToken);
        }
      }
      const redirectRoute = redirectRoles.length > 0 ? getDashboardRoute(redirectRoles) : '/login';
      setTimeout(() => {
        router.replace(redirectRoute);
      }, 2000);
    } catch (err: unknown) {
      // ALWAYS use backend error message
      setError(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  const togglePasswordVisibility = (field: 'current' | 'new' | 'confirm') => {
    setShowPasswords((prev) => ({
      ...prev,
      [field]: !prev[field],
    }));
  };

  return (
    <div className="flex items-center justify-center px-4 py-8">
      <Card className="w-full max-w-md shadow-xl border border-gray-200">
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
                  d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
                />
              </svg>
            </div>
          </div>
          <CardTitle className="text-3xl font-bold bg-gradient-to-r from-blue-600 to-blue-800 bg-clip-text text-transparent">
            Đổi mật khẩu
          </CardTitle>
          <CardDescription className="text-base text-gray-600">
            Cập nhật mật khẩu để bảo vệ tài khoản của bạn
          </CardDescription>
        </CardHeader>

        <CardContent>
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

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Current Password */}
            <div className="space-y-2.5">
              <Label htmlFor="currentPassword" className="text-sm font-medium text-gray-700">
                Mật khẩu hiện tại <span className="text-red-500">*</span>
              </Label>
              <div className="relative">
                <Input
                  id="currentPassword"
                  name="currentPassword"
                  type={showPasswords.current ? 'text' : 'password'}
                  placeholder="Nhập mật khẩu hiện tại"
                  value={formData.currentPassword}
                  onChange={handleChange}
                  required
                  disabled={isLoading}
                  maxLength={100}
                  className="h-10 rounded-lg border border-gray-300 px-3 py-2 pr-10"
                />
                <button
                  type="button"
                  onClick={() => togglePasswordVisibility('current')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                >
                  {showPasswords.current ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </div>

            {/* New Password */}
            <div className="space-y-2.5">
              <Label htmlFor="newPassword" className="text-sm font-medium text-gray-700">
                Mật khẩu mới <span className="text-red-500">*</span>
              </Label>
              <div className="relative">
                <Input
                  id="newPassword"
                  name="newPassword"
                  type={showPasswords.new ? 'text' : 'password'}
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
                  onClick={() => togglePasswordVisibility('new')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                >
                  {showPasswords.new ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
              <p className="text-xs text-gray-500">{PASSWORD_RULE_MESSAGE}</p>
            </div>

            {/* Confirm Password */}
            <div className="space-y-2.5">
              <Label htmlFor="confirmationPassword" className="text-sm font-medium text-gray-700">
                Xác nhận mật khẩu mới <span className="text-red-500">*</span>
              </Label>
              <div className="relative">
                <Input
                  id="confirmationPassword"
                  name="confirmationPassword"
                  type={showPasswords.confirm ? 'text' : 'password'}
                  placeholder="Xác nhận mật khẩu mới"
                  value={formData.confirmationPassword}
                  onChange={handleChange}
                  required
                  disabled={isLoading}
                  className="h-10 rounded-lg border border-gray-300 px-3 py-2 pr-10"
                />
                <button
                  type="button"
                  onClick={() => togglePasswordVisibility('confirm')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                >
                  {showPasswords.confirm ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </div>

            <Button
              type="submit"
              disabled={isLoading || !formData.currentPassword || !formData.newPassword || !formData.confirmationPassword}
              className="w-full h-10 bg-gradient-to-r from-blue-600 to-blue-800 hover:from-blue-700 hover:to-blue-900 text-white font-medium rounded-lg transition-all duration-200"
            >
              {isLoading ? 'Đang xử lý...' : 'Cập nhật mật khẩu'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
