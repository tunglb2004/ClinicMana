import ProtectedRoute from '@/components/layout/ProtectedRoute';
import { ROLES } from '@/lib/roleRedirect';

export default function DoctorLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute requiredRoles={[ROLES.DOCTOR, ROLES.CLINICAL_DOCTOR]}>{children}</ProtectedRoute>;
}
