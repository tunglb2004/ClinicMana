/**
 * Example: Register Component with Field Validation Errors
 * Demonstrates field-level error handling from backend
 */

'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { authService } from '@/services/auth.service';
import {
  extractErrorMessage,
  extractFieldErrors,
  hasFieldErrors,
  isErrorCodeMatch,
} from '@/lib/errorHandler';
import { ErrorCode } from '@/types/api.types';
import { toast } from '@/hooks/use-toast';

interface FormErrors {
  email?: string;
  phone?: string;
  password?: string;
  fullName?: string;
  otpCode?: string;
}

export default function RegisterExample() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    gender: 'MALE',
    otpCode: '',
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState(1); // 1: Send OTP, 2: Register with OTP

  const handleSendOtp = async () => {
    setLoading(true);
    setErrors({});

    try {
      // ✅ Service returns unwrapped data
      const response = await authService.sendOtp({ email: formData.email });

      // ✅ Display backend success message
      toast({
        description: response.message || 'Đã gửi mã OTP đến email của bạn',
      });

      setStep(2);
    } catch (error) {
      // ✅ Check for field-level validation errors
      if (hasFieldErrors(error)) {
        const fieldErrors = extractFieldErrors(error);
        setErrors(fieldErrors);
      } else {
        // ✅ Display general error message
        toast({
          variant: 'destructive',
          description: extractErrorMessage(error),
        });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async () => {
    setLoading(true);
    setErrors({});

    try {
      // ✅ Service returns unwrapped data
      const response = await authService.register(formData);

      // ✅ Display backend success message
      toast({
        description: response.message || 'Đăng ký thành công',
      });

      // Redirect to login
      router.push('/login?registered=true');
    } catch (error) {
      // ✅ Handle specific error codes
      if (isErrorCodeMatch(error, ErrorCode.REGISTER_EMAIL_EXISTS)) {
        setErrors({ email: 'Email này đã được sử dụng' });
      } else if (isErrorCodeMatch(error, ErrorCode.REGISTER_PHONE_EXISTS)) {
        setErrors({ phone: 'Số điện thoại này đã được sử dụng' });
      } else if (isErrorCodeMatch(error, ErrorCode.FORGOT_PASSWORD_INVALID_OTP)) {
        setErrors({ otpCode: 'Mã OTP không hợp lệ hoặc đã hết hạn' });
      } else if (hasFieldErrors(error)) {
        // ✅ Handle field-level validation errors
        const fieldErrors = extractFieldErrors(error);
        setErrors(fieldErrors);
      }

      // ✅ Display general error message
      toast({
        variant: 'destructive',
        description: extractErrorMessage(error),
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {step === 1 && (
        <div>
          <h2>Bước 1: Nhập Email</h2>
          <input
            type="email"
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            placeholder="Email"
          />
          {errors.email && <span className="error">{errors.email}</span>}
          <button onClick={handleSendOtp} disabled={loading}>
            Gửi mã OTP
          </button>
        </div>
      )}

      {step === 2 && (
        <div>
          <h2>Bước 2: Hoàn tất đăng ký</h2>
          <input
            type="text"
            value={formData.fullName}
            onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
            placeholder="Họ và tên"
          />
          {errors.fullName && <span className="error">{errors.fullName}</span>}

          <input
            type="text"
            value={formData.phone}
            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
            placeholder="Số điện thoại"
          />
          {errors.phone && <span className="error">{errors.phone}</span>}

          <input
            type="password"
            value={formData.password}
            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
            placeholder="Mật khẩu"
          />
          {errors.password && <span className="error">{errors.password}</span>}

          <input
            type="text"
            value={formData.otpCode}
            onChange={(e) => setFormData({ ...formData, otpCode: e.target.value })}
            placeholder="Mã OTP"
          />
          {errors.otpCode && <span className="error">{errors.otpCode}</span>}

          <button onClick={handleRegister} disabled={loading}>
            Hoàn tất đăng ký
          </button>
        </div>
      )}
    </div>
  );
}
