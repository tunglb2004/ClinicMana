import ProtectedRoute from '@/components/layout/ProtectedRoute';
import { ROLES } from '@/lib/roleRedirect';

export default function CashierLayout({ children }: { children: React.ReactNode }) {
    return <ProtectedRoute requiredRoles={[ROLES.CASHIER]}>{children}</ProtectedRoute>;
}
