package g2.be.repository;

import g2.be.dto.response.UnpaidRecordProjection;
import g2.be.dto.response.UnpaidRecordResponse;
import g2.be.entity.ServiceOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, String> {
    String WAITING_START_SQL = "COALESCE(inv.paid_at, inv.created_at, so.created_at)";
    String WAITING_MINUTES_SQL = "COALESCE(TIMESTAMPDIFF(MINUTE, " + WAITING_START_SQL + ", :now), 0)";
    String AGING_BONUS_SQL = "CASE WHEN " + WAITING_START_SQL + " IS NULL THEN 0 " +
            "WHEN TIMESTAMPDIFF(MINUTE, " + WAITING_START_SQL + ", :now) >= 90 THEN 1200 " +
            "WHEN TIMESTAMPDIFF(MINUTE, " + WAITING_START_SQL + ", :now) >= 60 THEN 700 " +
            "WHEN TIMESTAMPDIFF(MINUTE, " + WAITING_START_SQL + ", :now) >= 30 THEN 300 " +
            "ELSE 0 END";
    String TOTAL_PRIORITY_SCORE_SQL = "(COALESCE(a.priority_score, 0) + " +
            WAITING_MINUTES_SQL + " + " + AGING_BONUS_SQL + ")";

    List<ServiceOrder> findByMedicalRecord_RecordId(String recordId);
    List<ServiceOrder> findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(String appointmentId);
    List<ServiceOrder> findByMedicalRecord_RecordIdAndOrderTypeAndStatus(String recordId, String orderType, String status);
    List<ServiceOrder> findByMedicalRecord_Patient_PatientIdAndStatusOrderByCreatedAtDesc(String patientId, String status);
    @Query("SELECT so FROM ServiceOrder so " +
            "JOIN so.medicalRecord mr " +
            "LEFT JOIN mr.appointment a " +
            "LEFT JOIN mr.patient p " +
            "WHERE (p.patientId = :patientId OR a.bookedByPatientId = :patientId) " +
            "AND so.status = :status " +
            "ORDER BY so.createdAt DESC")
    List<ServiceOrder> findVisibleByPatientAndStatusOrderByCreatedAtDesc(
            @Param("patientId") String patientId,
            @Param("status") String status);
    List<ServiceOrder> findByInvoiceId(String invoiceId);
    List<ServiceOrder> findByMedicalRecord_RecordIdAndStatusAndInvoiceIdIsNull(String recordId, String status);
    List<ServiceOrder> findByMedicalRecord_RecordIdAndStatus(String recordId, String status);

    // Paginated: all orders (optional status filter)
    Page<ServiceOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ServiceOrder> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    // Paginated: supplement orders only (optional status filter)
    Page<ServiceOrder> findByOrderTypeOrderByCreatedAtDesc(String orderType, Pageable pageable);
    Page<ServiceOrder> findByOrderTypeAndStatusOrderByCreatedAtDesc(String orderType, String status, Pageable pageable);

    /**
     * Lấy danh sách Medical Records có ServiceOrder chưa thanh toán (invoice_id IS NULL).
     * GROUP BY record → SUM(service.price) để tính tổng tiền nợ.
     * Trả về đủ fields tương thích với FinancialResponse: departmentName, createdAt, totalServiceFee.
     */
    @Query(value = "SELECT new g2.be.dto.response.UnpaidRecordResponse(" +
            "mr.recordId, a.appointmentId, p.patientId, u.fullName, u.phoneNumber, " +
            "p.healthInsuranceNumber, dept.deptName, a.appointmentDate, a.appointmentTime, " +
            "COUNT(so), COALESCE(SUM(s.price), 0), a.createdAt) " +
            "FROM ServiceOrder so " +
            "JOIN so.medicalRecord mr " +
            "JOIN so.service s " +
            "LEFT JOIN mr.appointment a " +
            "LEFT JOIN mr.patient p " +
            "LEFT JOIN p.user u " +
            "LEFT JOIN mr.doctor doc " +
            "LEFT JOIN doc.department dept " +
            "WHERE so.invoiceId IS NULL AND so.orderType = 'INITIAL' " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "u.fullName LIKE %:keyword% OR p.patientId LIKE %:keyword% OR a.appointmentId LIKE %:keyword%) " +
            "GROUP BY mr.recordId, a.appointmentId, p.patientId, u.fullName, " +
            "u.phoneNumber, p.healthInsuranceNumber, dept.deptName, " +
            "a.appointmentDate, a.appointmentTime, a.createdAt " +
            "ORDER BY a.createdAt DESC",
            countQuery = "SELECT COUNT(DISTINCT mr.recordId) " +
            "FROM ServiceOrder so " +
            "JOIN so.medicalRecord mr " +
            "LEFT JOIN mr.appointment a " +
            "LEFT JOIN mr.patient p " +
            "LEFT JOIN p.user u " +
            "WHERE so.invoiceId IS NULL AND so.orderType = 'INITIAL' " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "u.fullName LIKE %:keyword% OR p.patientId LIKE %:keyword% OR a.appointmentId LIKE %:keyword%)")
    Page<UnpaidRecordResponse> findUnpaidRecords(@Param("keyword") String keyword, Pageable pageable);

        /**
         * Lấy danh sách Medical Records có ServiceOrder SUPPLEMENT chưa thanh toán.
         * GROUP BY record → SUM(price), GROUP_CONCAT(service_name) để tính tổng + tóm tắt dịch vụ.
         * Native SQL vì JPQL không hỗ trợ GROUP_CONCAT.
         */
    @Query(value = "SELECT " +
            "mr.record_id AS recordId, " +
            "a.appointment_id AS appointmentId, " +
            "p.patient_id AS patientId, " +
            "u.full_name AS patientName, " +
            "u.phone_number AS phoneNumber, " +
            "p.health_insurance_number AS healthInsuranceNumber, " +
            "dept.dept_name AS departmentName, " +
            "a.appointment_date AS appointmentDate, " +
            "a.appointment_time AS appointmentTime, " +
            "COUNT(*) AS unpaidServiceCount, " +
            "COALESCE(SUM(sv.price), 0) AS totalServiceFee, " +
            "mr.created_at AS createdAt, " +
            "GROUP_CONCAT(DISTINCT sv.service_name ORDER BY sv.service_name SEPARATOR ', ') AS serviceSummary " +
            "FROM service_orders so " +
            "JOIN medical_records mr ON so.record_id = mr.record_id " +
            "JOIN services sv ON so.service_id = sv.service_id " +
            "LEFT JOIN appointments a ON mr.appointment_id = a.appointment_id " +
            "LEFT JOIN patients p ON mr.patient_id = p.patient_id " +
            "LEFT JOIN users u ON p.user_id = u.user_id " +
            "LEFT JOIN doctors doc ON mr.doctor_id = doc.doctor_id " +
            "LEFT JOIN departments dept ON doc.dept_id = dept.dept_id " +
            "WHERE so.status = 'PENDING' " +
            "AND so.order_type = 'SUPPLEMENT' " +
            "AND (a.appointment_id IS NULL OR a.status <> 'CANCELLED') " +
            "AND (:queryDate IS NULL OR " +
            "  (a.appointment_id IS NOT NULL AND a.appointment_date = :queryDate) " +
            "  OR (a.appointment_id IS NULL AND DATE(so.created_at) = :queryDate)) " +
            "AND (so.invoice_id IS NULL OR EXISTS (" +
            "  SELECT 1 FROM invoices inv WHERE inv.invoice_id = so.invoice_id AND inv.payment_status = 'PENDING')) " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "  OR LOWER(COALESCE(u.full_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(COALESCE(p.patient_id, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(COALESCE(mr.record_id, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "GROUP BY mr.record_id, a.appointment_id, p.patient_id, u.full_name, " +
            "u.phone_number, p.health_insurance_number, dept.dept_name, " +
            "a.appointment_date, a.appointment_time, mr.created_at " +
            "ORDER BY mr.created_at DESC",
            countQuery = "SELECT COUNT(DISTINCT mr.record_id) " +
            "FROM service_orders so " +
            "JOIN medical_records mr ON so.record_id = mr.record_id " +
            "LEFT JOIN appointments a ON mr.appointment_id = a.appointment_id " +
            "LEFT JOIN patients p ON mr.patient_id = p.patient_id " +
            "LEFT JOIN users u ON p.user_id = u.user_id " +
            "WHERE so.status = 'PENDING' " +
            "AND so.order_type = 'SUPPLEMENT' " +
            "AND (a.appointment_id IS NULL OR a.status <> 'CANCELLED') " +
            "AND (:queryDate IS NULL OR " +
            "  (a.appointment_id IS NOT NULL AND a.appointment_date = :queryDate) " +
            "  OR (a.appointment_id IS NULL AND DATE(so.created_at) = :queryDate)) " +
            "AND (so.invoice_id IS NULL OR EXISTS (" +
            "  SELECT 1 FROM invoices inv WHERE inv.invoice_id = so.invoice_id AND inv.payment_status = 'PENDING')) " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "  OR LOWER(COALESCE(u.full_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(COALESCE(p.patient_id, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(COALESCE(mr.record_id, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))",
            nativeQuery = true)
    Page<UnpaidRecordProjection> findSupplementPendingRecords(@Param("keyword") String keyword,
                                                              @Param("queryDate") LocalDate queryDate,
                                                              Pageable pageable);

    @Query(value = "SELECT COUNT(DISTINCT mr.record_id) " +
            "FROM service_orders so " +
            "JOIN medical_records mr ON so.record_id = mr.record_id " +
            "LEFT JOIN appointments a ON mr.appointment_id = a.appointment_id " +
            "LEFT JOIN patients p ON mr.patient_id = p.patient_id " +
            "LEFT JOIN users u ON p.user_id = u.user_id " +
            "WHERE so.status = 'PENDING' " +
            "AND so.order_type = 'SUPPLEMENT' " +
            "AND (a.appointment_id IS NULL OR a.status <> 'CANCELLED') " +
            "AND (:queryDate IS NULL OR " +
            "  (a.appointment_id IS NOT NULL AND a.appointment_date = :queryDate) " +
            "  OR (a.appointment_id IS NULL AND DATE(so.created_at) = :queryDate)) " +
            "AND (so.invoice_id IS NULL OR EXISTS (" +
            "  SELECT 1 FROM invoices inv WHERE inv.invoice_id = so.invoice_id AND inv.payment_status = 'PENDING')) " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "  OR LOWER(COALESCE(u.full_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(COALESCE(p.patient_id, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(COALESCE(mr.record_id, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))",
            nativeQuery = true)
    Long countSupplementPendingRecords(@Param("keyword") String keyword,
                                       @Param("queryDate") LocalDate queryDate);

    /**
     * Tìm service orders theo danh sách phòng (dùng cho bác sĩ cận lâm sàng).
     * Join service_orders → services → service_room để lọc theo roomId.
     * Lọc theo ngày tạo = hôm nay, và status nếu có.
     */
    @Query(value = "SELECT so.* FROM service_orders so " +
            "LEFT JOIN medical_records mr ON so.record_id = mr.record_id " +
            "LEFT JOIN appointments a ON mr.appointment_id = a.appointment_id " +
            "LEFT JOIN invoices inv ON inv.invoice_id = so.invoice_id " +
            "JOIN services sv ON so.service_id = sv.service_id " +
            "JOIN departments dept ON sv.dept_id = dept.dept_id " +
            "WHERE so.assigned_room_id IN (:roomIds) " +
            "AND ((a.appointment_id IS NOT NULL AND a.appointment_date = :queryDate) " +
            "  OR (a.appointment_id IS NULL AND DATE(so.created_at) = :queryDate)) " +
            "AND (:status IS NULL OR so.status = :status) " +
            "AND so.status <> 'CANCELLED' " +
            "AND dept.dept_type = 'PARACLINICAL' " +
            "ORDER BY " +
            "CASE WHEN so.status IN ('PAID', 'PENDING') THEN 0 ELSE 1 END ASC, " +
            "CASE WHEN a.status = 'IN_PROGRESS' THEN 0 " +
            "     WHEN a.status = 'WAITING' THEN 1 " +
            "     ELSE 2 END ASC, " +
            TOTAL_PRIORITY_SCORE_SQL + " DESC, " +
            "COALESCE(a.queue_number, 999999) ASC, " +
            "so.created_at ASC",
            countQuery = "SELECT COUNT(so.order_id) FROM service_orders so " +
            "LEFT JOIN medical_records mr ON so.record_id = mr.record_id " +
            "LEFT JOIN appointments a ON mr.appointment_id = a.appointment_id " +
            "JOIN services sv ON so.service_id = sv.service_id " +
            "JOIN departments dept ON sv.dept_id = dept.dept_id " +
            "WHERE so.assigned_room_id IN (:roomIds) " +
            "AND ((a.appointment_id IS NOT NULL AND a.appointment_date = :queryDate) " +
            "  OR (a.appointment_id IS NULL AND DATE(so.created_at) = :queryDate)) " +
            "AND (:status IS NULL OR so.status = :status) " +
            "AND so.status <> 'CANCELLED' " +
            "AND dept.dept_type = 'PARACLINICAL'",
            nativeQuery = true)
    Page<ServiceOrder> findByRoomIdsAndDate(
            @Param("roomIds") Collection<String> roomIds,
            @Param("queryDate") LocalDate queryDate,
            @Param("now") LocalDateTime now,
            @Param("status") String status,
            Pageable pageable);

    @Query(value = "SELECT DISTINCT so.* FROM service_orders so " +
            "JOIN medical_records mr ON so.record_id = mr.record_id " +
            "JOIN appointments a ON mr.appointment_id = a.appointment_id " +
            "JOIN services sv ON so.service_id = sv.service_id " +
            "JOIN departments dept ON sv.dept_id = dept.dept_id " +
            "LEFT JOIN service_room sr ON sv.service_id = sr.service_id " +
            "WHERE mr.appointment_id = :appointmentId " +
            "AND so.assigned_room_id IN (:roomIds) " +
            "AND a.appointment_date = :queryDate " +
            "AND so.status <> 'CANCELLED' " +
            "AND dept.dept_type = 'PARACLINICAL' " +
            "ORDER BY so.created_at DESC",
            nativeQuery = true)
    List<ServiceOrder> findByRoomIdsAndAppointmentIdAndDate(
            @Param("roomIds") Collection<String> roomIds,
            @Param("appointmentId") String appointmentId,
            @Param("queryDate") LocalDate queryDate);
}
