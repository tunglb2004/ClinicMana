// Authentication Types - Generated from OpenAPI Spec

export interface SendOtpRequest {
  email: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  phone: string;
  cccd: string;
  password: string;
  gender: string;
  otpCode: string;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface MessageResponse {
  message: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface UserInfo {
  id: number;
  fullName: string;
  roles: string[];
  avatar?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserInfo;
}

export interface ForgotPasswordRequest {
  email: string;
  type?: string;
}

export interface ResetPasswordRequest {
  email: string;
  otpCode: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmationPassword: string;
}

export interface UserInfoResponse {
  id: number;
  username: string;
  email?: string;
  fullName: string;
  cccd?: string;
  gender?: string;
  phoneNumber?: string;
  avatarUrl?: string;
  roles: string[];
  profile?: Record<string, unknown>;
}
