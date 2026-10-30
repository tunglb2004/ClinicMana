import ProtectedRoute from '@/components/layout/ProtectedRoute';
import { ROLES } from '@/lib/roleRedirect';

export default function ManagerLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute requiredRoles={[ROLES.CLINIC_MANAGER, ROLES.MANAGER, ROLES.SYSTEM_ADMIN]}>
      {children}
    </ProtectedRoute>
  );
}
