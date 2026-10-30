package g2.be.repository;

import g2.be.entity.NurseDoctorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho bảng nurse_doctor_assignments.
 * Truy vấn quan hệ y tá theo dõi bác sĩ.
 * 
 * @author Nam
 */
@Repository
public interface NurseDoctorAssignmentRepository extends JpaRepository<NurseDoctorAssignment, Long> {

    // Lấy tất cả y tá đang theo dõi 1 bác sĩ (active)
    List<NurseDoctorAssignment> findByDoctor_DoctorIdAndIsActiveTrue(String doctorId);

    // Lấy tất cả bác sĩ mà 1 y tá đang theo dõi (active)
    List<NurseDoctorAssignment> findByNurse_NurseIdAndIsActiveTrue(String nurseId);

    // Kiểm tra trùng gán
    boolean existsByNurse_NurseIdAndDoctor_DoctorIdAndIsActiveTrue(String nurseId, String doctorId);

    // Tìm theo nurse + doctor (active)
    java.util.Optional<NurseDoctorAssignment> findByNurse_NurseIdAndDoctor_DoctorIdAndIsActiveTrue(
            String nurseId, String doctorId);
}
