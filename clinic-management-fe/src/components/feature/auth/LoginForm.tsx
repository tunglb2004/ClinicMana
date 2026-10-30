'use client';

import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth.store';
import { authService } from '@/services/auth.service';
import { getDashboardRoute } from '@/lib/roleRedirect';
import type { LoginRequest } from '@/types/auth.types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Eye, EyeOff } from 'lucide-react';
import Link from 'next/link';

export default function LoginForm() {
  const router = useRouter();
  const { setTokens, setUser, logout, isAuthenticated, accessToken, _hasHydrated } = useAuthStore();
  const checkedStoredSession = useRef(false);
  
  const [formData, setFormData] = useState<LoginRequest>({
    identifier: '',
    password: '',
  });
  
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    if (!_hasHydrated || checkedStoredSession.current) {
      return;
    }

    checkedStoredSession.current = true;
    if (isAuthenticated && accessToken) {
      logout();
    }
  }, [_hasHydrated, accessToken, isAuthenticated, logout]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError(''); // Clear error when user types
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const identifier = formData.identifier.trim();
    const password = formData.password;

    if (!identifier) {
      setError('Vui lòng nhập email hoặc số điện thoại.');
      return;
    }

    if (!password.trim()) {
      setError('Vui lòng nhập mật khẩu.');
      return;
    }

    setIsLoading(true);

    try {
      const response = await authService.login({ ...formData, identifier });
      
      console.log('🔑 Login Success - Response:', response);
      
      // Save tokens to Zustand store
      setTokens(response.accessToken, response.refreshToken);
      setUser(response.user);
      
      console.log('✅ Tokens saved to store');

      // Redirect based on user role
      const dashboardRoute = getDashboardRoute(response.user.roles);
      router.push(dashboardRoute);
    } catch (err: unknown) {
      let errorMessage = 'Đăng nhập thất bại. Vui lòng thử lại.';
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosError = err as { response?: { data?: { message?: string } } };
        errorMessage = axiosError.response?.data?.message || errorMessage;
      } else if (err instanceof Error) {
        errorMessage = err.message;
      }
      setError(errorMessage);
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
          <div className="flex justify-center mb-6">
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
            Đăng nhập
          </CardTitle>
          <CardDescription className="text-base text-gray-600">
            Truy cập vào hệ thống quản lý phòng khám
          </CardDescription>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Identifier Field (Email or Phone) */}
            <div className="space-y-2.5">
              <Label htmlFor="identifier" className="text-gray-900 font-semibold text-sm">
                Email hoặc Số điện thoại <span className="text-red-500">*</span>
              </Label>
              <Input
                id="identifier"
                name="identifier"
                type="text"
                required
                value={formData.identifier}
                onChange={handleChange}
                placeholder="Email hoặc số điện thoại"
                disabled={isLoading}
                className="border-gray-300 text-gray-900 placeholder:text-gray-400 h-11 text-base"
              />
            </div>

            {/* Password Field */}
            <div className="space-y-2.5">
              <div className="flex items-center justify-between">
                <Label htmlFor="password" className="text-gray-900 font-semibold text-sm">
                  Mật khẩu <span className="text-red-500">*</span>
                </Label>
                <Link
                  href="/reset-password"
                  className="text-sm text-blue-600 hover:text-blue-700 font-medium hover:underline"
                >
                  Quên mật khẩu?
                </Link>
              </div>
              <div className="relative">
                <Input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Nhập mật khẩu"
                  disabled={isLoading}
                  className="h-11 border-gray-300 pr-11 text-base text-gray-900 placeholder:text-gray-400"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  disabled={isLoading}
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  className="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-gray-500 transition-colors hover:text-gray-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            {/* Error Message */}
            {error && (
              <div className="rounded-lg bg-red-50 p-4 border border-red-200 flex gap-3">
                <svg
                  className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 8v4m0 4v.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
                <p className="text-sm text-red-700 font-medium">{error}</p>
              </div>
            )}

            {/* Submit Button */}
            <Button
              type="submit"
              disabled={isLoading}
              className="w-full h-11 bg-gradient-to-r from-blue-600 to-blue-800 hover:from-blue-700 hover:to-blue-900 text-white font-semibold text-base rounded-lg shadow-md hover:shadow-lg transition-all duration-300 disabled:opacity-70"
            >
              {isLoading ? (
                <div className="flex items-center gap-2">
                  <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Đang đăng nhập...
                </div>
              ) : (
                'Đăng nhập'
              )}
            </Button>

            {/* Divider */}
            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-200" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-white text-gray-500">hoặc</span>
              </div>
            </div>

            {/* Register Link */}
            <div className="text-center space-y-3">
              <p className="text-sm text-gray-600">
                Chưa có tài khoản?
              </p>
              <Link
                href="/register"
                className="inline-block w-full py-2.5 rounded-lg border-2 border-blue-600 text-blue-600 font-semibold hover:bg-blue-50 transition-colors duration-300"
              >
                Đăng ký tài khoản mới
              </Link>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
