'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth.store';
import { hasAnyRole } from '@/lib/roleRedirect';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRoles?: string[];
  redirectTo?: string;
}

/**
 * Higher-Order Component to protect routes
 * Checks authentication and optionally role-based access
 * Waits for Zustand hydration before checking auth
 */
export default function ProtectedRoute({
  children,
  requiredRoles = [],
  redirectTo = '/login',
}: ProtectedRouteProps) {
  const router = useRouter();
  const { isAuthenticated, user, isLoading, accessToken, _hasHydrated } = useAuthStore();
  const [isMounted, setIsMounted] = useState(false);

  // Ensure component only renders on client after mount
  // This is intentional to prevent hydration mismatch
  useEffect(() => {
    setIsMounted(true);
  }, []);

  // Check authentication using accessToken (more reliable than isAuthenticated flag)
  const hasValidAuth = _hasHydrated && !!accessToken && isAuthenticated;

  useEffect(() => {
    // Wait for Zustand to hydrate from localStorage
    if (!_hasHydrated || !isMounted) {
      return;
    }

    // Don't redirect while loading
    if (isLoading) {
      return;
    }

    // Redirect if not authenticated
    if (!accessToken || !isAuthenticated) {
      router.push(redirectTo);
      return;
    }

    // Check role-based access if required
    if (requiredRoles.length > 0 && user) {
      const hasRequiredRole = hasAnyRole(user.roles, requiredRoles);
      if (!hasRequiredRole) {
        router.push('/unauthorized');
      }
    }
  }, [_hasHydrated, isMounted, isAuthenticated, accessToken, user, isLoading, router, requiredRoles, redirectTo]);

  // Prevent hydration mismatch by not rendering until mounted on client
  if (!isMounted || !_hasHydrated || isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center" suppressHydrationWarning>
        <div className="text-center" suppressHydrationWarning>
          <div className="mb-4 h-12 w-12 animate-spin rounded-full border-4 border-gray-200 border-t-blue-600" suppressHydrationWarning></div>
          <p className="text-gray-600" suppressHydrationWarning>Đang tải...</p>
        </div>
      </div>
    );
  }

  // Show redirecting message if not authenticated
  if (!hasValidAuth) {
    return (
      <div className="flex min-h-screen items-center justify-center" suppressHydrationWarning>
        <div className="text-center" suppressHydrationWarning>
          <p className="text-gray-600" suppressHydrationWarning>Đang chuyển hướng...</p>
        </div>
      </div>
    );
  }

  // Check role authorization
  if (requiredRoles.length > 0 && user) {
    const hasRequiredRole = hasAnyRole(user.roles, requiredRoles);
    if (!hasRequiredRole) {
      return null;
    }
  }

  return <>{children}</>;
}
