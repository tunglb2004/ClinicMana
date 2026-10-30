'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ChevronDown, KeyRound, LogOut, UserRound } from 'lucide-react';
import { useState } from 'react';
import { getRoleDisplayName, hasRole, normalizeRoles, ROLES } from '@/lib/roleRedirect';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/store/auth.store';

const ROLE_PRIORITY = [
  ROLES.SYSTEM_ADMIN,
  ROLES.CLINIC_MANAGER,
  ROLES.MANAGER,
  ROLES.DOCTOR,
  ROLES.CLINICAL_DOCTOR,
  ROLES.SPECIALIST_DOCTOR,
  ROLES.NURSE,
  ROLES.RECEPTIONIST,
  ROLES.CASHIER,
  ROLES.PATIENT,
];

export function UserProfileDropdown() {
  const [isOpen, setIsOpen] = useState(false);
  const router = useRouter();
  const { user, logout } = useAuthStore();

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.warn('Logout API failed, but clearing local state:', error);
    } finally {
      setIsOpen(false);
      router.push('/login');
    }
  };

  if (!user) return null;

  const hasValidAvatar =
    user.avatar &&
    user.avatar !== 'string' &&
    (user.avatar.startsWith('http://') || user.avatar.startsWith('https://') || user.avatar.startsWith('/'));
  const normalizedRoles = normalizeRoles(user.roles);
  const primaryRole = ROLE_PRIORITY.find((role) => normalizedRoles.includes(role)) || normalizedRoles[0] || user.roles[0];
  const primaryRoleLabel = primaryRole ? getRoleDisplayName(primaryRole) : '';
  const isAdminUser = hasRole(user.roles, ROLES.SYSTEM_ADMIN);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setIsOpen((value) => !value)}
        className="flex items-center gap-2 rounded-xl px-3 py-2 transition-colors hover:bg-blue-50"
      >
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-blue-600 to-teal-600 text-sm font-semibold text-white shadow-md shadow-blue-600/20">
          {hasValidAvatar ? (
            <img src={user.avatar!} alt={user.fullName} className="h-10 w-10 rounded-full object-cover" />
          ) : (
            user.fullName.charAt(0).toUpperCase()
          )}
        </div>
        <div className="hidden text-left sm:block">
          <p className="text-sm font-semibold text-slate-950">{user.fullName}</p>
          <p className="text-xs text-slate-500">{primaryRoleLabel}</p>
        </div>
        <ChevronDown className={cn('h-4 w-4 text-slate-500 transition-transform duration-200', isOpen && 'rotate-180')} />
      </button>

      {isOpen && (
        <>
          <div className="fixed inset-0 z-30" onClick={() => setIsOpen(false)} />
          <div className="absolute right-0 z-40 mt-2 w-56 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl shadow-slate-900/10 animate-in fade-in slide-in-from-top-2">
            <div className="border-b border-blue-100 bg-gradient-to-br from-blue-50 to-teal-50 px-4 py-3">
              <p className="text-sm font-semibold text-slate-950">{user.fullName}</p>
              <p className="mt-1 text-xs font-medium text-blue-700">
                {primaryRoleLabel}
              </p>
            </div>

            <div className="py-2">
              {!isAdminUser && (
                <Link
                  href="/dashboard/profile"
                  onClick={() => setIsOpen(false)}
                  className="block px-4 py-2 text-sm text-slate-700 transition-colors hover:bg-blue-50 hover:text-blue-700"
                >
                  <span className="flex items-center gap-2">
                    <UserRound className="h-4 w-4" />
                    Hồ sơ cá nhân
                  </span>
                </Link>
              )}

              <Link
                href="/dashboard/change-password"
                onClick={() => setIsOpen(false)}
                className="block px-4 py-2 text-sm text-slate-700 transition-colors hover:bg-blue-50 hover:text-blue-700"
              >
                <span className="flex items-center gap-2">
                  <KeyRound className="h-4 w-4" />
                  Đổi mật khẩu
                </span>
              </Link>

              <div className="my-2 border-t border-slate-100" />

              <button
                type="button"
                onClick={handleLogout}
                className="w-full px-4 py-2 text-left text-sm text-red-600 transition-colors hover:bg-red-50"
              >
                <span className="flex items-center gap-2">
                  <LogOut className="h-4 w-4" />
                  Đăng xuất
                </span>
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

