package g2.be.repository;

import g2.be.entity.InsuranceSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho quản lý hồ sơ BHYT đã gửi
 */
@Repository
public interface InsuranceSubmissionRepository extends JpaRepository<InsuranceSubmission, Long> {

    /**
     * Tìm theo mã submission
     */
    Optional<InsuranceSubmission> findBySubmissionCode(String submissionCode);

    /**
     * Tìm theo ServiceOrder ID
     */
    Optional<InsuranceSubmission> findByServiceOrderId(Long serviceOrderId);

    /**
     * Tìm theo CCCD
     */
    List<InsuranceSubmission> findByCccdNumber(String cccdNumber);

    /**
     * Tìm theo trạng thái
     */
    List<InsuranceSubmission> findByStatus(String status);

    /**
     * Tìm hồ sơ chưa gửi
     */
    List<InsuranceSubmission> findByStatusAndSubmittedAtIsNull(String status);

    /**
     * Tìm hồ sơ theo khoảng thời gian
     */
    List<InsuranceSubmission> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
