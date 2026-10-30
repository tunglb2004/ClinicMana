'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { authService } from '@/services/auth.service';
import type { RegisterRequest } from '@/types/auth.types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  User,
  Mail,
  Phone,
  ShieldCheck,
  Lock,
  Eye,
  EyeOff,
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Loader2
} from 'lucide-react';
import Link from 'next/link';
import { isValidPassword, PASSWORD_RULE_MESSAGE } from '@/lib/password-policy';
import {
  CCCD_MESSAGE,
  CCCD_REGEX,
  EMAIL_MESSAGE,
  EMAIL_REGEX,
  FULL_NAME_MESSAGE,
  FULL_NAME_REGEX,
  PHONE_MESSAGE,
  PHONE_REGEX,
} from '@/lib/validation-rules';

const OTP_REGEX = /^[0-9]{6}$/;

const resolveErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosLikeError = error as { response?: { data?: { message?: string } } };
    return axiosLikeError.response?.data?.message || fallbackMessage;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallbackMessage;
};

export default function RegisterForm() {
  const router = useRouter();

  // Form data state
  const [formData, setFormData] = useState<RegisterRequest>({
    fullName: '',
    email: '',
    phone: '',
    cccd: '',
    password: '',
    gender: 'MALE',
    otpCode: '',
  });

  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // OTP logic
  const [showOtpForm, setShowOtpForm] = useState(false);
  const [otpCountdown, setOtpCountdown] = useState(0);

  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (otpCountdown > 0) {
      timer = setTimeout(() => setOtpCountdown(otpCountdown - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [otpCountdown]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    let normalizedValue = value;

    if (name === 'phone' || name === 'cccd' || name === 'otpCode') {
      normalizedValue = value.replace(/\D/g, '');
    }

    setFormData((prev) => ({ ...prev, [name]: normalizedValue }));
    setError('');
  };

  const validateStep1 = (): boolean => {
    const fullName = formData.fullName.trim();
    const email = formData.email.trim();
    const phone = formData.phone.trim();
    const password = formData.password;

    if (!fullName) {
      setError('Họ và tên là bắt buộc');
      return false;
    }

    if (fullName.length > 100) {
      setError('Họ và tên không được vượt quá 100 ký tự');
      return false;
    }

    if (!FULL_NAME_REGEX.test(fullName)) {
      setError(FULL_NAME_MESSAGE);
      return false;
    }

    if (email && !EMAIL_REGEX.test(email)) {
      setError(EMAIL_MESSAGE);
      return false;
    }

    if (!email || !EMAIL_REGEX.test(email)) {
      setError('Email không hợp lệ');
      return false;
    }

    if (phone && !PHONE_REGEX.test(phone)) {
      setError(PHONE_MESSAGE);
      return false;
    }

    if (!phone || !PHONE_REGEX.test(phone)) {
      setError('Số điện thoại không hợp lệ');
      return false;
    }

    if (!CCCD_REGEX.test(formData.cccd.trim())) {
      setError(CCCD_MESSAGE);
      return false;
    }

    if (!isValidPassword(password)) {
      setError(PASSWORD_RULE_MESSAGE);
      return false;
    }

    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp');
      return false;
    }

    return true;
  };

  const handleSendOtp = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!validateStep1()) return;

    const normalizedFormData = {
      ...formData,
      fullName: formData.fullName.trim(),
      email: formData.email.trim(),
      phone: formData.phone.trim(),
      cccd: formData.cccd.trim(),
    };

    setIsLoading(true);
    setError('');

    try {
      setFormData(normalizedFormData);
      await authService.sendOtp({ email: normalizedFormData.email });
      setSuccess('Mã OTP đã được gửi đến email của bạn');
      setShowOtpForm(true);
      setOtpCountdown(300); // 5 mins
    } catch (error: unknown) {
      setError(resolveErrorMessage(error, 'Không thể gửi OTP. Vui lòng thử lại.'));
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!OTP_REGEX.test(formData.otpCode)) {
      setError('Vui lòng nhập mã OTP gồm 6 chữ số');
      return;
    }

    if (!validateStep1()) {
      return;
    }

    const normalizedFormData = {
      ...formData,
      fullName: formData.fullName.trim(),
      email: formData.email.trim(),
      phone: formData.phone.trim(),
      cccd: formData.cccd.trim(),
    };

    setIsLoading(true);
    setError('');

    try {
      setFormData(normalizedFormData);
      await authService.register(normalizedFormData);
      setSuccess('Đăng ký thành công! Đang chuyển hướng...');
      setTimeout(() => router.push('/login'), 2000);
    } catch (error: unknown) {
      setError(resolveErrorMessage(error, 'Đăng ký thất bại. Vui lòng thử lại.'));
    } finally {
      setIsLoading(false);
    }
  };

  const isStep1Ready =
    formData.fullName.trim().length > 0 &&
    formData.fullName.trim().length <= 100 &&
    FULL_NAME_REGEX.test(formData.fullName.trim()) &&
    EMAIL_REGEX.test(formData.email.trim()) &&
    PHONE_REGEX.test(formData.phone.trim()) &&
    CCCD_REGEX.test(formData.cccd.trim()) &&
    isValidPassword(formData.password) &&
    formData.password === confirmPassword;

  const isOtpReady = OTP_REGEX.test(formData.otpCode);
  const isStep1SubmitDisabled = isLoading || !isStep1Ready;
  const isOtpSubmitDisabled = isLoading || !isStep1Ready || !isOtpReady;
  const disabledSubmitClasses =
    'disabled:cursor-not-allowed disabled:from-gray-300 disabled:to-gray-300 disabled:text-gray-500 disabled:shadow-none disabled:hover:opacity-100 disabled:active:scale-100';

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-blue-100 flex items-center justify-center p-4 py-12 relative overflow-hidden">
      {/* Background Shapes */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-blue-100/50 rounded-full blur-3xl animate-pulse" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-200/50 rounded-full blur-3xl animate-pulse" />

      <Card className="w-full max-w-[550px] shadow-2xl border-[var(--border)] relative z-10 backdrop-blur-sm bg-white/90">
        <CardHeader className="space-y-3 text-center pb-8 border-b border-gray-100">
          <div className="flex justify-center mb-2">
            <div className="h-16 w-16 bg-gradient-to-br from-[var(--primary)] to-[var(--secondary)] rounded-2xl flex items-center justify-center shadow-lg transform -rotate-3 hover:rotate-0 transition-transform duration-300">
              <ShieldCheck className="w-9 h-9 text-white" />
            </div>
          </div>
          <CardTitle className="text-3xl font-bold tracking-tight text-[var(--foreground)]">
            {showOtpForm ? 'Xác thực tài khoản' : 'Đăng ký tham gia'}
          </CardTitle>
          <CardDescription className="text-gray-500 font-medium max-w-[80%] mx-auto">
            {showOtpForm
              ? 'Vui lòng kiểm tra email và nhập mã OTP 6 chữ số để hoàn tất.'
              : 'Hãy điền thông tin bên dưới để bắt đầu quản lý khám chữa bệnh hiệu quả.'}
          </CardDescription>
        </CardHeader>

        <CardContent className="pt-8">
          {!showOtpForm ? (
            <form onSubmit={handleSendOtp} className="space-y-5">
              {/* Row 1: Full Name */}
              <div className="space-y-2">
                <Label className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-2">
                  <User className="w-3.5 h-3.5 text-[var(--primary)]" />
                  Họ và tên *
                </Label>
                <Input
                  name="fullName"
                  placeholder="Nguyễn Văn A"
                  value={formData.fullName}
                  onChange={handleChange}
                  className="rounded-xl border-gray-200 h-11 focus:ring-[var(--primary)] focus:border-[var(--primary)] transition-all"
                  required
                />
              </div>

              {/* Row 2: Email & Phone */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-2">
                    <Mail className="w-3.5 h-3.5 text-[var(--primary)]" />
                    Email *
                  </Label>
                  <Input
                    name="email"
                    type="email"
                    placeholder="example@mail.com"
                    value={formData.email}
                    onChange={handleChange}
                    className="rounded-xl border-gray-200 h-11 focus:ring-[var(--primary)]"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-2">
                    <Phone className="w-3.5 h-3.5 text-[var(--primary)]" />
                    Số điện thoại *
                  </Label>
                  <Input
                    name="phone"
                    placeholder="09xxx"
                    value={formData.phone}
                    onChange={handleChange}
                    className="rounded-xl border-gray-200 h-11 focus:ring-[var(--primary)]"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-2">
                    <ShieldCheck className="w-3.5 h-3.5 text-[var(--primary)]" />
                    CCCD *
                  </Label>
                  <Input
                    name="cccd"
                    inputMode="numeric"
                    maxLength={12}
                    placeholder="12 chữ số"
                    value={formData.cccd}
                    onChange={handleChange}
                    className="rounded-xl border-gray-200 h-11 focus:ring-[var(--primary)]"
                    required
                  />
                </div>
              </div>

              {/* Row 3: Passwords */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-2">
                    <Lock className="w-3.5 h-3.5 text-[var(--primary)]" />
                    Mật khẩu *
                  </Label>
                  <div className="relative">
                    <Input
                      name="password"
                      type={showPassword ? 'text' : 'password'}
                      placeholder="Chữ hoa, chữ thường, số, ký tự đặc biệt"
                      minLength={8}
                      maxLength={100}
                      value={formData.password}
                      onChange={handleChange}
                      className="h-11 rounded-xl border-gray-200 pr-11 focus:ring-[var(--primary)]"
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((prev) => !prev)}
                      aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                      className="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-gray-500 transition-colors hover:text-gray-700"
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label className="text-xs font-bold uppercase tracking-wider text-gray-500 flex items-center gap-2">
                    <Lock className="w-3.5 h-3.5 text-[var(--primary)]" />
                    Xác nhận *
                  </Label>
                  <div className="relative">
                    <Input
                      type={showConfirmPassword ? 'text' : 'password'}
                      placeholder="Nhập lại mật khẩu"
                      minLength={8}
                      maxLength={100}
                      value={confirmPassword}
                      onChange={(e) => {
                        setConfirmPassword(e.target.value);
                        setError('');
                      }}
                      className="h-11 rounded-xl border-gray-200 pr-11 focus:ring-[var(--primary)]"
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword((prev) => !prev)}
                      aria-label={showConfirmPassword ? 'Ẩn mật khẩu xác nhận' : 'Hiện mật khẩu xác nhận'}
                      className="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-gray-500 transition-colors hover:text-gray-700"
                    >
                      {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>
              </div>
              <p className="text-xs text-gray-500">{PASSWORD_RULE_MESSAGE}</p>

              {/* Messages */}
              {error && (
                <div className="flex items-center gap-2 text-red-600 bg-red-50 p-3 rounded-xl border border-red-100 animate-in fade-in zoom-in duration-200">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span className="text-sm font-semibold">{error}</span>
                </div>
              )}

              <Button
                type="submit"
                disabled={isStep1SubmitDisabled}
                aria-disabled={isStep1SubmitDisabled}
                className={`w-full h-12 bg-gradient-to-r from-[var(--primary)] to-[var(--primary-dark)] hover:opacity-90 text-white font-bold rounded-xl shadow-lg transition-all active:scale-[0.98] flex items-center justify-center gap-2 mt-4 ${disabledSubmitClasses}`}
              >
                {isLoading ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <>
                    Tiếp tục <ArrowRight className="w-5 h-5" />
                  </>
                )}
              </Button>

              <div className="text-center pt-4 border-t border-gray-50">
                <p className="text-sm text-gray-500 font-medium">
                  Đã có tài khoản tham gia?{' '}
                  <Link href="/login" className="text-[var(--primary)] font-bold hover:underline transition-all">
                    Đăng nhập ngay
                  </Link>
                </p>
              </div>
            </form>
          ) : (
            <form onSubmit={handleRegister} className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-300">
              <div className="bg-blue-50/50 p-6 rounded-2xl border border-blue-100 text-center space-y-2">
                <p className="text-sm text-gray-500 font-medium">Mã OTP đã được gửi đến:</p>
                <div className="flex items-center justify-center gap-2">
                  <Mail className="w-4 h-4 text-[var(--primary)]" />
                  <span className="text-lg font-bold text-[var(--primary)]">{formData.email}</span>
                </div>
              </div>

              <div className="space-y-4">
                <Label className="text-xs font-bold uppercase tracking-wider text-gray-500 text-center block">
                  Nhập mã xác thực 6 chữ số <span className="text-red-500">*</span>
                </Label>
                <div className="flex justify-center">
                  <Input
                    name="otpCode"
                    value={formData.otpCode}
                    onChange={handleChange}
                    placeholder="000 000"
                    inputMode="numeric"
                    pattern="[0-9]{6}"
                    maxLength={6}
                    className="max-w-[200px] h-14 text-center text-3xl font-black tracking-[0.2em] rounded-2xl border-2 border-gray-200 focus:border-[var(--primary)] focus:ring-[var(--primary)]"
                    required
                  />
                </div>
              </div>

              <div className="text-center">
                {otpCountdown > 0 ? (
                  <div className="flex items-center justify-center gap-2 text-sm text-gray-500 font-medium">
                    <div className="w-2 h-2 rounded-full bg-[var(--primary)] animate-pulse" />
                    Gửi lại mã sau: <span className="font-bold text-[var(--foreground)]">{Math.floor(otpCountdown / 60)}:{(otpCountdown % 60).toString().padStart(2, '0')}</span>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={handleSendOtp}
                    className="text-sm font-bold text-[var(--primary)] hover:underline flex items-center justify-center gap-2 mx-auto"
                  >
                    Chưa nhận được mã? Gửi lại ngay
                  </button>
                )}
              </div>

              {error && (
                <div className="flex items-center gap-2 text-red-600 bg-red-50 p-3 rounded-xl border border-red-100 justify-center">
                  <AlertCircle className="w-4 h-4" />
                  <span className="text-sm font-semibold">{error}</span>
                </div>
              )}
              {success && (
                <div className="flex items-center gap-2 text-[var(--primary)] bg-blue-50 p-3 rounded-xl border border-blue-100 justify-center">
                  <CheckCircle2 className="w-4 h-4" />
                  <span className="text-sm font-semibold">{success}</span>
                </div>
              )}

              <div className="flex flex-col gap-3">
                <Button
                  type="submit"
                  disabled={isOtpSubmitDisabled}
                  aria-disabled={isOtpSubmitDisabled}
                  className={`w-full h-12 bg-gradient-to-r from-[var(--secondary)] to-[var(--secondary-dark)] hover:opacity-90 text-white font-bold rounded-xl shadow-lg transition-all ${disabledSubmitClasses}`}
                >
                  {isLoading ? <Loader2 className="w-5 h-5 animate-spin mx-auto" /> : 'Hoàn tất đăng ký'}
                </Button>
                <button
                  type="button"
                  onClick={() => setShowOtpForm(false)}
                  className="flex items-center justify-center gap-2 text-sm font-bold text-gray-500 hover:text-[var(--foreground)] transition-colors"
                >
                  <ArrowLeft className="w-4 h-4" /> Quay lại sửa thông tin
                </button>
              </div>
            </form>
          )}
        </CardContent>
      </Card>

      {/* Footer Decoration */}
      <div className="absolute bottom-8 text-sm text-gray-400 font-medium">
        © 2026 ClinicPro Management System. Bảo mật & An toàn.
      </div>
    </div>
  );
}
