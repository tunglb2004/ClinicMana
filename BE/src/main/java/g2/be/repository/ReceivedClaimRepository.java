package g2.be.repository;

import g2.be.entity.ReceivedClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho quản lý hồ sơ BHYT nhận được (Mock BHXH Server)
 */
@Repository
public interface ReceivedClaimRepository extends JpaRepository<ReceivedClaim, Long> {

    /**
     * Tìm theo mã submission
     */
    Optional<ReceivedClaim> findBySubmissionCode(String submissionCode);

    /**
     * Tìm theo mã phòng khám
     */
    List<ReceivedClaim> findByClinicCode(String clinicCode);

    /**
     * Tìm theo CCCD
     */
    List<ReceivedClaim> findByCccdNumber(String cccdNumber);

    /**
     * Tìm theo trạng thái
     */
    List<ReceivedClaim> findByStatus(String status);

    /**
     * Tìm hồ sơ chưa xử lý
     */
    List<ReceivedClaim> findByStatusAndProcessedAtIsNull(String status);

    /**
     * Tìm hồ sơ theo khoảng thời gian nhận
     */
    List<ReceivedClaim> findByReceivedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Tổng số tiền BHYT đã duyệt theo phòng khám
     */
    List<ReceivedClaim> findByClinicCodeAndStatus(String clinicCode, String status);
}
