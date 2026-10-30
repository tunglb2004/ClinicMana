import ProtectedRoute from '@/components/layout/ProtectedRoute';
import { ROLES } from '@/lib/roleRedirect';

export default function NurseLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute requiredRoles={[ROLES.NURSE]}>{children}</ProtectedRoute>;
}
