/**
 * Role-based redirect utilities
 * Maps user roles to their respective dashboard routes
 */

export const ROLES = {
  SYSTEM_ADMIN: 'ROLE_SYSTEM_ADMIN',
  CLINIC_MANAGER: 'ROLE_CLINIC_MANAGER',
  MANAGER: 'ROLE_MANAGER',
  DOCTOR: 'ROLE_DOCTOR',
  CLINICAL_DOCTOR: 'ROLE_CLINICALDOCTOR',
  SPECIALIST_DOCTOR: 'ROLE_SPECIALISTDOCTOR',
  NURSE: 'ROLE_NURSE',
  RECEPTIONIST: 'ROLE_RECEPTIONIST',
  CASHIER: 'ROLE_CASHIER',
  PATIENT: 'ROLE_PATIENT',
} as const;

const ROLE_ALIASES: Record<string, string> = {
  ROLE_ADMIN: ROLES.SYSTEM_ADMIN,
  ADMIN: ROLES.SYSTEM_ADMIN,
  SYSTEM_ADMIN: ROLES.SYSTEM_ADMIN,
  CLINIC_MANAGER: ROLES.CLINIC_MANAGER,
  MANAGER: ROLES.MANAGER,
  DOCTOR: ROLES.DOCTOR,
  ROLE_CLINICAL_DOCTOR: ROLES.CLINICAL_DOCTOR,
  CLINICAL_DOCTOR: ROLES.CLINICAL_DOCTOR,
  CLINICALDOCTOR: ROLES.CLINICAL_DOCTOR,
  ROLE_SPECIALIST_DOCTOR: ROLES.SPECIALIST_DOCTOR,
  SPECIALIST_DOCTOR: ROLES.SPECIALIST_DOCTOR,
  SPECIALISTDOCTOR: ROLES.SPECIALIST_DOCTOR,
  NURSE: ROLES.NURSE,
  RECEPTIONIST: ROLES.RECEPTIONIST,
  CASHIER: ROLES.CASHIER,
  PATIENT: ROLES.PATIENT,
};

export const ROLE_ROUTES: Record<string, string> = {
  [ROLES.SYSTEM_ADMIN]: '/dashboard/admin',
  [ROLES.CLINIC_MANAGER]: '/dashboard/manager',
  [ROLES.MANAGER]: '/dashboard/manager',
  [ROLES.DOCTOR]: '/dashboard/doctor/patients',
  [ROLES.CLINICAL_DOCTOR]: '/dashboard/doctor/patients',
  [ROLES.SPECIALIST_DOCTOR]: '/dashboard/specialist-doctor/patients',
  [ROLES.NURSE]: '/dashboard/nurse',
  [ROLES.RECEPTIONIST]: '/dashboard/receptionist',
  [ROLES.CASHIER]: '/dashboard/cashier',
  [ROLES.PATIENT]: '/dashboard/patient',
};

export function normalizeRole(role: string): string {
  return ROLE_ALIASES[role] ?? role;
}

export function normalizeRoles(roles: string[] = []): string[] {
  return Array.from(new Set(roles.map(normalizeRole)));
}

export function hasRole(roles: string[] = [], role: string): boolean {
  return normalizeRoles(roles).includes(normalizeRole(role));
}

export function hasAnyRole(roles: string[] = [], requiredRoles: string[] = []): boolean {
  const normalizedRoles = normalizeRoles(roles);
  return requiredRoles.some((role) => normalizedRoles.includes(normalizeRole(role)));
}

export function getRolesFromAccessToken(accessToken?: string | null): string[] {
  if (!accessToken) return [];

  const [, payload] = accessToken.split('.');
  if (!payload) return [];

  try {
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const paddedBase64 = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const decodedPayload = JSON.parse(window.atob(paddedBase64));
    const roles = decodedPayload?.roles;

    if (Array.isArray(roles)) {
      return roles.map(String).filter(Boolean);
    }

    if (typeof roles === 'string') {
      return roles
        .split(',')
        .map((role) => role.trim())
        .filter(Boolean);
    }
  } catch {
    return [];
  }

  return [];
}

/**
 * Get dashboard route based on user roles
 * Returns the route for the highest priority role
 */
export function getDashboardRoute(roles: string[]): string {
  const normalizedRoles = normalizeRoles(roles);
  // Priority order (highest to lowest)
  const rolePriority = [
    ROLES.SYSTEM_ADMIN,
    ROLES.CLINIC_MANAGER,
    ROLES.MANAGER,
    ROLES.CLINICAL_DOCTOR,
    ROLES.SPECIALIST_DOCTOR,
    ROLES.DOCTOR,
    ROLES.NURSE,
    ROLES.RECEPTIONIST,
    ROLES.CASHIER,
    ROLES.PATIENT,
  ];

  for (const role of rolePriority) {
    if (normalizedRoles.includes(role)) {
      return ROLE_ROUTES[role];
    }
  }

  // Default fallback
  return '/dashboard';
}

/**
 * Get role display name in Vietnamese
 */
export function getRoleDisplayName(role: string): string {
  const normalizedRole = normalizeRole(role);
  const roleNames: Record<string, string> = {
    [ROLES.SYSTEM_ADMIN]: 'Quản trị hệ thống',
    [ROLES.CLINIC_MANAGER]: 'Quản lý phòng khám',
    [ROLES.MANAGER]: 'Manager',
    [ROLES.DOCTOR]: 'Bác sĩ',
    [ROLES.CLINICAL_DOCTOR]: 'Bác sĩ lâm sàng',
    [ROLES.SPECIALIST_DOCTOR]: 'Bác sĩ chuyên khoa',
    [ROLES.NURSE]: 'Y tá',
    [ROLES.RECEPTIONIST]: 'Lễ tân',
    [ROLES.CASHIER]: 'Thu ngân',
    [ROLES.PATIENT]: 'Bệnh nhân',
  };

  return roleNames[normalizedRole] || role;
}
