/**
 * Example: Login Component with New API Response Handling
 * Demonstrates proper error handling with ErrorCode enum
 */

'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { authService } from '@/services/auth.service';
import { extractErrorMessage, extractErrorCode } from '@/lib/errorHandler';
import { ErrorCode } from '@/types/api.types';
import { toast } from '@/hooks/use-toast';
import { useAuthStore } from '@/store/auth.store';

export default function LoginExample() {
  const router = useRouter();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [attempts, setAttempts] = useState(0);
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      // ✅ Service returns unwrapped data directly
      const response = await authService.login({ identifier, password });

      // ✅ Display backend message (optional for success)
      toast({
        description: 'Đăng nhập thành công', // Or use response message if available
      });

      // Save auth data
      setAuth(response.accessToken, response.refreshToken, response.user);

      // Redirect based on role
      const userRole = response.user.roles[0];
      router.push(`/dashboard/${userRole.toLowerCase()}`);
    } catch (error) {
      // ✅ Extract error code for conditional logic
      const errorCode = extractErrorCode(error);

      // Handle specific error codes
      if (errorCode === ErrorCode.LOGIN_ACCOUNT_LOCKED) {
        // Special handling for locked account
        router.push('/contact-admin?reason=locked');
      } else if (errorCode === ErrorCode.LOGIN_INVALID_CREDENTIALS) {
        // Track failed attempts
        setAttempts((prev) => prev + 1);
        if (attempts >= 4) {
          toast({
            variant: 'destructive',
            title: 'Cảnh báo',
            description: 'Tài khoản sẽ bị khóa sau 5 lần đăng nhập sai.',
          });
        }
      }

      // ✅ Always display backend message to user
      toast({
        variant: 'destructive',
        description: extractErrorMessage(error),
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleLogin}>
      {/* Form fields */}
      <input
        type="text"
        value={identifier}
        onChange={(e) => setIdentifier(e.target.value)}
        placeholder="Email hoặc SĐT"
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Mật khẩu"
      />
      <button type="submit" disabled={loading}>
        {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
      </button>
    </form>
  );
}
