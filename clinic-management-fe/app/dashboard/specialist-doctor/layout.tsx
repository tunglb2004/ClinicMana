import ProtectedRoute from '@/components/layout/ProtectedRoute';
import { ROLES } from '@/lib/roleRedirect';

export default function SpecialistDoctorLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute requiredRoles={[ROLES.SPECIALIST_DOCTOR]}>{children}</ProtectedRoute>;
}
