'use client';

import { useSyncExternalStore } from 'react';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserInfo } from '@/types/auth.types';
import { authService } from '@/services/auth.service';
import { AxiosError } from 'axios';

interface AuthState {
  // State
  user: UserInfo | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  _hasHydrated: boolean;

  // Actions
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUser: (user: UserInfo | null) => void;
  logout: () => void;
  refreshUserInfo: () => Promise<void>;
  setHasHydrated: (state: boolean) => void;
}

// Create store with persist
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      // Initial State
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
      _hasHydrated: false,

      // Set tokens and update authentication status
      setTokens: (accessToken: string, refreshToken: string) => {
        set({
          accessToken,
          refreshToken,
          isAuthenticated: true,
        });
      },

      // Set user information
      setUser: (user: UserInfo | null) => {
        set({ user });
      },

      // Logout - clear all state
      logout: async () => {
        const { refreshToken } = get();
        
        // Call logout API if refreshToken exists
        if (refreshToken) {
          try {
            await authService.logout({ refreshToken });
          } catch (error) {
            console.error('Logout API error (continuing with local logout):', error);
            // Don't throw - always clear local state even if API fails
          }
        }

        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        });
      },

      // Refresh user information from server
      refreshUserInfo: async () => {
        const { isAuthenticated } = get();
        
        if (!isAuthenticated) {
          return;
        }

        try {
          set({ isLoading: true });
          const userInfo = await authService.getCurrentUser();
          
          // Map UserInfoResponse to UserInfo
          set({
            user: {
              id: userInfo.id,
              fullName: userInfo.fullName,
              roles: userInfo.roles,
              avatar: userInfo.avatarUrl,
            },
            isLoading: false,
          });
        } catch (error) {
          console.error('Failed to refresh user info:', error);
          set({ isLoading: false });
          // If error is 401, logout
          if (error instanceof AxiosError && error.response?.status === 401) {
            get().logout();
          }
        }
      },

      // Set hydration state
      setHasHydrated: (state: boolean) => {
        set({ _hasHydrated: state });
      },
    }),
    {
      name: 'auth-storage', // Key in localStorage
      partialize: (state) => ({
        // Only persist these fields
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        console.log('🔄 Auth Store - Rehydrating from localStorage...');
        if (state) {
          console.log('✅ Auth Store - Hydration complete:', {
            hasAccessToken: !!state.accessToken,
            hasUser: !!state.user,
            isAuthenticated: state.isAuthenticated,
          });
          state.setHasHydrated(true);
        } else {
          console.log('❌ Auth Store - No state found in localStorage');
          // Still set hydrated even if no state, so components can proceed
          useAuthStore.getState().setHasHydrated(true);
        }
      },
    }
  )
);

// Custom hook to wait for hydration using useSyncExternalStore
export const useHydration = () => {
  return useSyncExternalStore(
    // Subscribe function
    (callback) => useAuthStore.subscribe(callback),
    // Get snapshot (client)
    () => useAuthStore.getState()._hasHydrated,
    // Get server snapshot (SSR) - always false on server
    () => false
  );
};
