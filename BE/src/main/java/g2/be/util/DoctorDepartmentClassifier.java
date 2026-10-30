package g2.be.util;

import g2.be.entity.Department;
import g2.be.entity.Doctor;

public final class DoctorDepartmentClassifier {

    private DoctorDepartmentClassifier() {
    }

    public static Department.DeptType resolveDeptType(Doctor doctor) {
        if (doctor == null || doctor.getDepartment() == null) {
            return null;
        }

        return doctor.getDepartment().getDeptType();
    }

    public static boolean matchesDeptType(Doctor doctor, Department.DeptType deptType) {
        return deptType != null && deptType == resolveDeptType(doctor);
    }

    public static boolean isParaclinical(Doctor doctor) {
        return matchesDeptType(doctor, Department.DeptType.PARACLINICAL);
    }

    public static boolean isClinical(Doctor doctor) {
        return matchesDeptType(doctor, Department.DeptType.CLINICAL);
    }
}