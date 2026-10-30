import ProtectedRoute from '@/components/layout/ProtectedRoute';
import { ROLES } from '@/lib/roleRedirect';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute requiredRoles={[ROLES.SYSTEM_ADMIN]}>
      {children}
    </ProtectedRoute>
  );
}
