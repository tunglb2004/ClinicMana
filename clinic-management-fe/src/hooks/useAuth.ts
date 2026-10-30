'use client';

import { useAuthStore } from '@/store/auth.store';
import { authService } from '@/services/auth.service';
import type { LoginRequest, RegisterRequest } from '@/types/auth.types';
import { hasRole as userHasRole } from '@/lib/roleRedirect';

/**
 * Custom hook for authentication
 * Provides easy access to auth state and methods
 */
export function useAuth() {
  const store = useAuthStore();

  /**
   * Login with username and password
   */
  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authService.login(credentials);
      store.setTokens(response.accessToken, response.refreshToken);
      store.setUser(response.user);
      return { success: true, user: response.user };
    } catch (error: any) {
      const message = error.response?.data?.message || 'Đăng nhập thất bại';
      return { success: false, error: message };
    }
  };

  /**
   * Register new user account
   */
  const register = async (data: RegisterRequest) => {
    try {
      const response = await authService.register(data);
      return { success: true, message: response.message };
    } catch (error: any) {
      const message = error.response?.data?.message || 'Đăng ký thất bại';
      return { success: false, error: message };
    }
  };

  /**
   * Logout and clear auth state
   */
  const logout = () => {
    store.logout();
  };

  /**
   * Refresh current user info
   */
  const refreshUser = async () => {
    try {
      await store.refreshUserInfo();
      return { success: true };
    } catch (error: any) {
      return { success: false, error: error.message };
    }
  };

  /**
   * Check if user has specific role
   */
  const hasRole = (role: string): boolean => {
    return store.user ? userHasRole(store.user.roles, role) : false;
  };

  /**
   * Check if user has any of the specified roles
   */
  const hasAnyRole = (roles: string[]): boolean => {
    return roles.some((role) => hasRole(role));
  };

  /**
   * Check if user has all of the specified roles
   */
  const hasAllRoles = (roles: string[]): boolean => {
    return roles.every((role) => hasRole(role));
  };

  return {
    // State
    user: store.user,
    isAuthenticated: store.isAuthenticated,
    isLoading: store.isLoading,
    accessToken: store.accessToken,

    // Methods
    login,
    register,
    logout,
    refreshUser,
    hasRole,
    hasAnyRole,
    hasAllRoles,
  };
}
