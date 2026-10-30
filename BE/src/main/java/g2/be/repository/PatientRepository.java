package g2.be.repository;

import g2.be.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    @Query("SELECT p FROM Patient p " +
            "JOIN p.user u " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "u.fullName LIKE %:keyword% OR " +
            "u.email LIKE %:keyword% OR " +
            "u.phoneNumber LIKE %:keyword% OR " +
            "p.patientId LIKE %:keyword%) " +
            "AND u.isDeleted = false")
    Page<Patient> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Patient p " +
            "JOIN p.user u " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "u.fullName LIKE %:keyword% OR " +
            "u.email LIKE %:keyword% OR " +
            "u.phoneNumber LIKE %:keyword% OR " +
            "p.patientId LIKE %:keyword%) " +
            "AND u.isDeleted = false " +
            "AND u.createdAt >= :startDate AND u.createdAt <= :endDate")
    Page<Patient> findByKeywordAndDateRange(@Param("keyword") String keyword, 
                                            @Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate, 
                                            Pageable pageable);

    @Query("SELECT DISTINCT p FROM Patient p WHERE p.user.userId = :userId")
    Optional<Patient> findByUser_UserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM Patient p WHERE p.user.phoneNumber = :phoneNumber")
    Optional<Patient> findByUser_PhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT DISTINCT p FROM Patient p WHERE p.user.cccd = :cccd")
    Optional<Patient> findByUser_Cccd(@Param("cccd") String cccd);

    boolean existsByPatientId(String patientId);
}
