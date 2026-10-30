import ProtectedRoute from '@/components/layout/ProtectedRoute';
import { ROLES } from '@/lib/roleRedirect';

export default function ClinicalDoctorLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedRoute requiredRoles={[ROLES.CLINICAL_DOCTOR, ROLES.DOCTOR]}>{children}</ProtectedRoute>;
}
