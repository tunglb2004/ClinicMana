import ProtectedRoute from '@/components/layout/ProtectedRoute';
import { ROLES } from '@/lib/roleRedirect';

export default function ReceptionistLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute requiredRoles={[ROLES.RECEPTIONIST, ROLES.CASHIER]}>{children}</ProtectedRoute>;
}
