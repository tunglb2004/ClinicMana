package g2.be.repository;

import g2.be.entity.MedicalRecord;
import g2.be.entity.Appointment;
import g2.be.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, String> {
    
    Optional<MedicalRecord> findByAppointment(Appointment appointment);
    
    Optional<MedicalRecord> findByAppointment_AppointmentId(String appointmentId);

    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.patientId = :patientId " +
            "AND m.status IN :statuses " +
            "AND (:keyword IS NULL OR LOWER(m.diagnosis) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(m.doctor.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY m.createdAt DESC")
    Page<MedicalRecord> findHistoryWithFilter(
            @Param("patientId") String patientId,
            @Param("statuses") Collection<String> statuses,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT DISTINCT m FROM MedicalRecord m " +
            "LEFT JOIN m.appointment a " +
            "LEFT JOIN m.patient p " +
            "LEFT JOIN p.user pu " +
            "LEFT JOIN m.doctor d " +
            "LEFT JOIN d.user du " +
            "WHERE (p.patientId = :patientId OR a.bookedByPatientId = :patientId) " +
            "AND m.status IN :statuses " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "OR LOWER(COALESCE(m.diagnosis, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(du.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(p.patientId, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(pu.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY m.createdAt DESC")
    Page<MedicalRecord> findVisibleHistoryWithFilter(
            @Param("patientId") String patientId,
            @Param("statuses") Collection<String> statuses,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT m FROM MedicalRecord m " +
            "LEFT JOIN m.patient p " +
            "LEFT JOIN p.user pu " +
            "LEFT JOIN m.doctor d " +
            "LEFT JOIN d.user du " +
            "WHERE du.userId = :doctorUserId " +
            "AND m.status IN :statuses " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "OR LOWER(m.recordId) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(m.diagnosis, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(p.patientId, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(pu.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:fromDateTime IS NULL OR m.createdAt >= :fromDateTime) " +
            "AND (:toDateTime IS NULL OR m.createdAt < :toDateTime) " +
            "ORDER BY m.createdAt DESC")
    Page<MedicalRecord> findDoctorHistoryWithFilter(
            @Param("doctorUserId") Long doctorUserId,
            @Param("statuses") Collection<String> statuses,
            @Param("keyword") String keyword,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime,
            Pageable pageable);

    @Query("SELECT DISTINCT m FROM MedicalRecord m " +
            "JOIN m.appointment a " +
            "JOIN a.services s " +
            "WHERE m.patient.patientId = :patientId " +
            "AND m.status IN :statuses " +
            "AND s.department.deptType = :deptType " +
            "ORDER BY m.createdAt DESC")
    List<MedicalRecord> findFollowUpOptions(
            @Param("patientId") String patientId,
            @Param("statuses") Collection<String> statuses,
            @Param("deptType") Department.DeptType deptType);

    @Query("SELECT DISTINCT m FROM MedicalRecord m " +
            "JOIN m.appointment a " +
            "JOIN a.services s " +
            "WHERE (m.patient.patientId = :patientId OR a.bookedByPatientId = :patientId) " +
            "AND m.status IN :statuses " +
            "AND s.department.deptType = :deptType " +
            "ORDER BY m.createdAt DESC")
    List<MedicalRecord> findVisibleFollowUpOptions(
            @Param("patientId") String patientId,
            @Param("statuses") Collection<String> statuses,
            @Param("deptType") Department.DeptType deptType);
}

