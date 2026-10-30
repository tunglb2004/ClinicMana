package g2.be.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import g2.be.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r WHERE r.doctor.doctorId = :doctorId AND r.isDeleted = false ORDER BY r.createdAt DESC")
    Page<Review> findByDoctorId(@Param("doctorId") String doctorId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.patient.patientId = :patientId AND r.isDeleted = false ORDER BY r.createdAt DESC")
    Page<Review> findByPatientId(@Param("patientId") String patientId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.isDeleted = false ORDER BY r.createdAt DESC")
    Page<Review> findAllActive(Pageable pageable);

    Optional<Review> findByReviewIdAndIsDeletedFalse(Long reviewId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.doctor.doctorId = :doctorId AND r.isDeleted = false")
    Double getAverageRatingByDoctorId(@Param("doctorId") String doctorId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.doctor.doctorId = :doctorId AND r.isDeleted = false")
    Long countByDoctorId(@Param("doctorId") String doctorId);

        @Query("SELECT AVG(r.rating) FROM Review r WHERE r.isDeleted = false")
        Double getAverageRating();

        @Query("SELECT COUNT(r) FROM Review r WHERE r.isDeleted = false AND (r.replyContent IS NULL OR r.replyContent = '')")
        Long countPendingReplies();

        @Query(value = """
                        SELECT
                                COALESCE(AVG(r.rating), 0) AS average_rating,
                                COALESCE(SUM(CASE WHEN r.reply_content IS NULL OR r.reply_content = '' THEN 1 ELSE 0 END), 0) AS pending_replies
                        FROM reviews r
                        WHERE r.is_deleted = false
                        """, nativeQuery = true)
        Object[] getManagerReviewStats();

        @Query("""
                        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
                        FROM Review r
                        WHERE r.appointment.appointmentId = :appointmentId
                            AND r.patient.patientId = :patientId
                            AND r.isDeleted = false
                            AND ((:doctorId IS NULL AND r.doctor IS NULL) OR r.doctor.doctorId = :doctorId)
                            AND ((:serviceId IS NULL AND r.service IS NULL) OR r.service.serviceId = :serviceId)
                        """)
        boolean existsActiveReview(@Param("appointmentId") String appointmentId,
                                                             @Param("patientId") String patientId,
                                                             @Param("doctorId") String doctorId,
                                                             @Param("serviceId") String serviceId);

        @Query("SELECT r FROM Review r WHERE r.appointment.appointmentId = :appointmentId AND r.patient.patientId = :patientId AND r.isDeleted = false ORDER BY r.createdAt DESC")
        List<Review> findActiveByAppointmentIdAndPatientId(@Param("appointmentId") String appointmentId,
                                                                                                             @Param("patientId") String patientId);
}
