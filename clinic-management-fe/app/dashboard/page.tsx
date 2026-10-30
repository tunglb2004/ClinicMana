'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getDashboardRoute } from '@/lib/roleRedirect';
import { useAuthStore } from '@/store/auth.store';

export default function DashboardPage() {
  const router = useRouter();
  const { user, accessToken, isAuthenticated, isLoading, refreshUserInfo } = useAuthStore();
  const [requestedUser, setRequestedUser] = useState(false);

  useEffect(() => {
    if (user?.roles?.length) {
      router.replace(getDashboardRoute(user.roles));
      return;
    }

    if (!requestedUser && accessToken && isAuthenticated && !isLoading) {
      setRequestedUser(true);
      void refreshUserInfo();
    }
  }, [accessToken, isAuthenticated, isLoading, refreshUserInfo, requestedUser, router, user]);

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="text-center">
        <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-4 border-gray-200 border-t-blue-600" />
        <p className="text-sm text-slate-600">Dang chuyen den dashboard phu hop...</p>
      </div>
    </div>
  );
}
