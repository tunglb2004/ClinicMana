package g2.be.repository;

import g2.be.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    String WAITING_MINUTES_SQL = "COALESCE(TIMESTAMPDIFF(MINUTE, a.waiting_since, :now), 0)";
    String AGING_BONUS_SQL = "CASE WHEN a.waiting_since IS NULL THEN 0 " +
            "WHEN TIMESTAMPDIFF(MINUTE, a.waiting_since, :now) >= 90 THEN 1200 " +
            "WHEN TIMESTAMPDIFF(MINUTE, a.waiting_since, :now) >= 60 THEN 700 " +
            "WHEN TIMESTAMPDIFF(MINUTE, a.waiting_since, :now) >= 30 THEN 300 " +
            "ELSE 0 END";
    String TOTAL_PRIORITY_SCORE_SQL = "(COALESCE(a.priority_score, 0) + " +
            WAITING_MINUTES_SQL + " + " + AGING_BONUS_SQL + ")";
    String PAID_APPOINTMENT_EXISTS_SQL = "EXISTS (SELECT 1 FROM medical_records mr " +
            "JOIN invoices i ON i.record_id = mr.record_id " +
            "WHERE mr.appointment_id = a.appointment_id AND i.payment_status = 'PAID')";

    boolean existsByDoctor_DoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            String doctorId, LocalDate appointmentDate, LocalTime appointmentTime, String status);

    long countByAppointmentDateAndAppointmentTimeAndStatusNotIn(
            LocalDate appointmentDate, LocalTime appointmentTime, java.util.Collection<String> statuses);

    @Query("SELECT COUNT(DISTINCT a.appointmentId) FROM Appointment a JOIN a.services s " +
           "WHERE a.appointmentDate = :appointmentDate " +
           "AND a.appointmentTime = :appointmentTime " +
           "AND a.status NOT IN :statuses " +
           "AND s.serviceId = :serviceId")
    long countDistinctByAppointmentDateAndAppointmentTimeAndStatusNotInAndServiceId(
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("appointmentTime") LocalTime appointmentTime,
            @Param("statuses") java.util.Collection<String> statuses,
            @Param("serviceId") String serviceId);

    @Query("SELECT COUNT(DISTINCT a.appointmentId) FROM Appointment a " +
            "LEFT JOIN a.room room " +
            "WHERE a.appointmentDate = :appointmentDate " +
            "AND a.appointmentTime = :appointmentTime " +
            "AND a.status NOT IN :statuses " +
            "AND (room IS NULL OR room.roomId IN :roomIds)")
    long countDistinctByAppointmentDateAndAppointmentTimeAndStatusNotInAndRoomPool(
             @Param("appointmentDate") LocalDate appointmentDate,
             @Param("appointmentTime") LocalTime appointmentTime,
             @Param("statuses") java.util.Collection<String> statuses,
             @Param("roomIds") java.util.Collection<String> roomIds);

    @Query("SELECT COUNT(DISTINCT a.appointmentId) FROM Appointment a " +
            "JOIN a.services s " +
            "LEFT JOIN a.room room " +
            "WHERE a.appointmentDate = :appointmentDate " +
            "AND a.appointmentTime = :appointmentTime " +
            "AND a.status NOT IN :statuses " +
            "AND s.serviceId = :serviceId " +
            "AND (room IS NULL OR room.roomId IN :roomIds)")
    long countDistinctByAppointmentDateAndAppointmentTimeAndStatusNotInAndServiceIdAndRoomPool(
             @Param("appointmentDate") LocalDate appointmentDate,
             @Param("appointmentTime") LocalTime appointmentTime,
             @Param("statuses") java.util.Collection<String> statuses,
             @Param("serviceId") String serviceId,
             @Param("roomIds") java.util.Collection<String> roomIds);

    @Query("SELECT COUNT(DISTINCT a.appointmentId) FROM Appointment a " +
            "LEFT JOIN a.room room " +
            "LEFT JOIN a.doctor doctor " +
            "LEFT JOIN doctor.room doctorRoom " +
            "WHERE a.appointmentDate = :appointmentDate " +
            "AND a.appointmentTime = :appointmentTime " +
            "AND a.status NOT IN :statuses " +
            "AND (room.roomId = :roomId OR (room IS NULL AND doctorRoom.roomId = :roomId))")
    long countDistinctByAppointmentDateAndAppointmentTimeAndStatusNotInAndEffectiveRoomId(
             @Param("appointmentDate") LocalDate appointmentDate,
             @Param("appointmentTime") LocalTime appointmentTime,
             @Param("statuses") java.util.Collection<String> statuses,
             @Param("roomId") String roomId);

    @Query("SELECT COUNT(DISTINCT a.appointmentId) FROM Appointment a " +
            "JOIN a.services s " +
            "LEFT JOIN a.room room " +
            "LEFT JOIN a.doctor doctor " +
            "LEFT JOIN doctor.room doctorRoom " +
            "WHERE a.appointmentDate = :appointmentDate " +
            "AND a.appointmentTime = :appointmentTime " +
            "AND a.status NOT IN :statuses " +
            "AND s.serviceId = :serviceId " +
            "AND (room.roomId = :roomId OR (room IS NULL AND doctorRoom.roomId = :roomId))")
    long countDistinctByAppointmentDateAndAppointmentTimeAndStatusNotInAndServiceIdAndEffectiveRoomId(
             @Param("appointmentDate") LocalDate appointmentDate,
             @Param("appointmentTime") LocalTime appointmentTime,
             @Param("statuses") java.util.Collection<String> statuses,
             @Param("serviceId") String serviceId,
             @Param("roomId") String roomId);

    // Visit history: own appointments + ones booked on behalf, newest first
    @Query("SELECT DISTINCT a FROM Appointment a " +
           "WHERE a.patient.patientId = :patientId OR a.bookedByPatientId = :patientId " +
           "ORDER BY a.appointmentDate DESC, a.appointmentTime DESC")
    Page<Appointment> findByPatientOrBooker(@Param("patientId") String patientId, Pageable pageable);

    // Visit history: all appointments of a patient, newest first (kept for internal/admin use)
    Page<Appointment> findByPatient_PatientIdOrderByAppointmentDateDescAppointmentTimeDesc(
            String patientId, Pageable pageable);

    // All appointments of a patient (non-paginated, for simple list)
    List<Appointment> findByPatient_PatientIdOrderByAppointmentDateDescAppointmentTimeDesc(
            String patientId);

    // All appointments with pagination, newest first
    Page<Appointment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Filter appointments by status with pagination, newest first
    Page<Appointment> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByAppointmentDate(LocalDate appointmentDate);

    long countByAppointmentDateAndStatus(LocalDate appointmentDate, String status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.appointmentDate = :appointmentDate AND a.status IN :statuses")
    long countByAppointmentDateAndStatusIn(@Param("appointmentDate") LocalDate appointmentDate,
                                           @Param("statuses") java.util.Collection<String> statuses);

    @Query(value = """
            SELECT
              COUNT(*) AS today_appointments,
              COALESCE(SUM(CASE WHEN status IN ('REQUESTED','CONFIRMED','WAITING','IN_PROGRESS') THEN 1 ELSE 0 END), 0) AS waiting_appointments,
              COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_appointments
            FROM appointments
            WHERE appointment_date = :appointmentDate
            """, nativeQuery = true)
    Object[] getManagerAppointmentStats(@Param("appointmentDate") LocalDate appointmentDate);

    // Doctor view: appointments assigned to this doctor, sorted by priority score (dynamic)
    @Query(value = "SELECT a.* FROM appointments a WHERE a.doctor_id = :doctorId " +
           "AND (:status IS NULL OR a.status = :status) " +
            "AND a.appointment_date = :appointmentDate " +
           "AND a.status NOT IN ('CANCELLED') " +
           "AND " + PAID_APPOINTMENT_EXISTS_SQL + " " +
           "ORDER BY " +
           "CASE WHEN a.status = 'IN_PROGRESS' THEN 0 " +
           "     WHEN a.status = 'WAITING' THEN 1 " +
           "     ELSE 2 END ASC, " +
           TOTAL_PRIORITY_SCORE_SQL + " DESC, " +
           "a.created_at ASC",
           countQuery = "SELECT COUNT(*) FROM appointments a WHERE a.doctor_id = :doctorId " +
           "AND (:status IS NULL OR a.status = :status) " +
            "AND a.appointment_date = :appointmentDate " +
           "AND a.status NOT IN ('CANCELLED') " +
           "AND " + PAID_APPOINTMENT_EXISTS_SQL,
           nativeQuery = true)
    Page<Appointment> findByDoctorId(@Param("doctorId") String doctorId,
                                          @Param("appointmentDate") LocalDate appointmentDate,
                                     @Param("now") LocalDateTime now,
                                     @Param("status") String status,
                                     Pageable pageable);

    @Query(value = "SELECT a.* FROM appointments a " +
           "LEFT JOIN doctors d ON d.doctor_id = a.doctor_id " +
           "WHERE ((:status IS NULL AND a.status IN ('WAITING', 'IN_PROGRESS', 'POSTPONED')) OR (:status IS NOT NULL AND a.status = :status)) " +
           "AND a.appointment_date = :appointmentDate " +
           "AND a.status NOT IN ('CANCELLED') " +
           "AND (a.status IN ('IN_PROGRESS', 'POSTPONED') OR " + PAID_APPOINTMENT_EXISTS_SQL + ") " +
           "AND (a.room_id IN (:roomIds) OR (a.room_id IS NULL AND d.room_id IN (:roomIds))) " +
           "ORDER BY " +
           "CASE WHEN a.status = 'IN_PROGRESS' THEN 0 " +
           "     WHEN a.status = 'WAITING' THEN 1 " +
           "     WHEN a.status = 'POSTPONED' THEN 2 " +
           "     ELSE 2 END ASC, " +
           TOTAL_PRIORITY_SCORE_SQL + " DESC, " +
           "COALESCE(a.queue_number, 999999) ASC, " +
           "a.created_at ASC",
           countQuery = "SELECT COUNT(*) FROM appointments a " +
           "LEFT JOIN doctors d ON d.doctor_id = a.doctor_id " +
           "WHERE ((:status IS NULL AND a.status IN ('WAITING', 'IN_PROGRESS', 'POSTPONED')) OR (:status IS NOT NULL AND a.status = :status)) " +
           "AND a.appointment_date = :appointmentDate " +
           "AND a.status NOT IN ('CANCELLED') " +
           "AND (a.status IN ('IN_PROGRESS', 'POSTPONED') OR " + PAID_APPOINTMENT_EXISTS_SQL + ") " +
           "AND (a.room_id IN (:roomIds) OR (a.room_id IS NULL AND d.room_id IN (:roomIds)))",
           nativeQuery = true)
    Page<Appointment> findByRoomIdsForDoctorQueue(@Param("roomIds") java.util.Collection<String> roomIds,
                                                  @Param("appointmentDate") LocalDate appointmentDate,
                                                  @Param("now") LocalDateTime now,
                                                  @Param("status") String status,
                                                  Pageable pageable);

    // Lấy 1 bệnh nhân WAITING có tổng điểm cao nhất (cho call-next)
    @Query(value = "SELECT a.* FROM appointments a WHERE a.doctor_id = :doctorId " +
           "AND a.status = 'WAITING' AND a.waiting_since IS NOT NULL " +
           "AND " + PAID_APPOINTMENT_EXISTS_SQL + " " +
           "ORDER BY " + TOTAL_PRIORITY_SCORE_SQL + " DESC",
           nativeQuery = true)
    List<Appointment> findWaitingByDoctorIdOrderByPriority(@Param("doctorId") String doctorId,
                                                           @Param("now") LocalDateTime now,
                                                           Pageable pageable);

    // Kiểm tra bác sĩ có đang khám ai không (IN_PROGRESS)
    Optional<Appointment> findByDoctor_DoctorIdAndStatus(String doctorId, String status);

    /**
     * Tìm các appointment có services nhưng chưa có invoice PAID.
     * Tức là appointment chưa được thanh toán.
     */
    @Query("SELECT DISTINCT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.user u " +
            "LEFT JOIN FETCH a.doctor doc " +
            "LEFT JOIN FETCH doc.department dept " +
            "LEFT JOIN FETCH a.services s " +
            "WHERE a.services IS NOT EMPTY " +
            "AND a.status <> 'CANCELLED' " +
            "AND (:appointmentDate IS NULL OR a.appointmentDate = :appointmentDate) " +
            "AND NOT EXISTS (SELECT i FROM Invoice i JOIN i.medicalRecord mr WHERE mr.appointment = a AND i.paymentStatus = 'PAID') " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.patientId, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(a.appointmentId, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findUnpaidAppointments(@Param("keyword") String keyword,
                                             @Param("appointmentDate") LocalDate appointmentDate,
                                             Pageable pageable);

    @Query("SELECT COUNT(DISTINCT a.appointmentId) FROM Appointment a " +
            "LEFT JOIN a.patient p " +
            "LEFT JOIN p.user u " +
            "WHERE a.services IS NOT EMPTY " +
            "AND a.status <> 'CANCELLED' " +
            "AND (:appointmentDate IS NULL OR a.appointmentDate = :appointmentDate) " +
            "AND NOT EXISTS (SELECT i FROM Invoice i JOIN i.medicalRecord mr WHERE mr.appointment = a AND i.paymentStatus = 'PAID') " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.patientId, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(a.appointmentId, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Long countUnpaidAppointments(@Param("keyword") String keyword,
                                 @Param("appointmentDate") LocalDate appointmentDate);

    // Queue display: lấy appointments WAITING + IN_PROGRESS của 1 bác sĩ (sorted by priority)
    @Query(value = "SELECT a.* FROM appointments a " +
           "WHERE a.doctor_id = :doctorId " +
           "AND a.status IN ('WAITING', 'IN_PROGRESS') " +
           "AND a.appointment_date = :appointmentDate " +
           "AND " + PAID_APPOINTMENT_EXISTS_SQL + " " +
           "ORDER BY " +
           "CASE WHEN a.status = 'IN_PROGRESS' THEN 0 ELSE 1 END ASC, " +
           TOTAL_PRIORITY_SCORE_SQL + " DESC",
           nativeQuery = true)
    List<Appointment> findActiveQueueByDoctorId(@Param("doctorId") String doctorId,
                                                @Param("appointmentDate") LocalDate appointmentDate,
                                                @Param("now") LocalDateTime now);

    @Query(value = "SELECT a.* FROM appointments a " +
            "WHERE a.status IN ('WAITING', 'IN_PROGRESS') " +
            "AND a.appointment_date = :appointmentDate " +
            "AND " + PAID_APPOINTMENT_EXISTS_SQL + " " +
            "AND (a.room_id = :roomId OR (a.room_id IS NULL AND EXISTS (" +
            "    SELECT 1 FROM doctors d WHERE d.doctor_id = a.doctor_id AND d.room_id = :roomId" +
            "))) " +
            "ORDER BY " +
            "CASE WHEN a.status = 'IN_PROGRESS' THEN 0 ELSE 1 END ASC, " +
            TOTAL_PRIORITY_SCORE_SQL + " DESC",
            nativeQuery = true)
    List<Appointment> findActiveQueueByRoomId(@Param("roomId") String roomId,
                                              @Param("appointmentDate") LocalDate appointmentDate,
                                              @Param("now") LocalDateTime now);

    @Query(value = "SELECT COALESCE(MAX(a.queue_number), 0) FROM appointments a " +
            "LEFT JOIN doctors d ON d.doctor_id = a.doctor_id " +
            "WHERE a.appointment_date = :appointmentDate " +
            "AND a.status <> 'CANCELLED' " +
            "AND (a.room_id = :roomId OR (a.room_id IS NULL AND d.room_id = :roomId))",
            nativeQuery = true)
    Integer findMaxQueueNumberByRoomIdAndDate(@Param("roomId") String roomId,
                                              @Param("appointmentDate") LocalDate appointmentDate);
}
