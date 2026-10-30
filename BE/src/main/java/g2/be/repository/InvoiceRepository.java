package g2.be.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import g2.be.entity.Invoice;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    boolean existsByMedicalRecord_Appointment_AppointmentIdAndPaymentStatus(String appointmentId, String paymentStatus);

    List<Invoice> findByMedicalRecord_RecordId(String recordId);

        List<Invoice> findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(String appointmentId);

    @Query("SELECT i FROM Invoice i " +
            "LEFT JOIN i.medicalRecord mr " +
            "LEFT JOIN mr.patient p " +
            "LEFT JOIN p.user u " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(COALESCE(i.invoiceId, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.patientId, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:paymentStatus IS NULL OR :paymentStatus = '' OR i.paymentStatus = :paymentStatus) " +
            "AND (:cashierId IS NULL OR :cashierId = '' OR i.cashierId = :cashierId OR i.cashierId = :cashierStaffId) " +
            "AND (:from IS NULL OR COALESCE(i.paidAt, i.createdAt) >= :from) " +
            "AND (:to IS NULL OR COALESCE(i.paidAt, i.createdAt) < :to) " +
            "ORDER BY COALESCE(i.paidAt, i.createdAt) DESC")
    Page<Invoice> findByFilters(@Param("keyword") String keyword,
                                @Param("paymentStatus") String paymentStatus,
                                @Param("cashierId") String cashierId,
                                @Param("cashierStaffId") String cashierStaffId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.totalServiceFee), 0) FROM Invoice i WHERE i.paymentStatus = 'PAID'")
    BigDecimal getTotalRevenue();

    @Query("SELECT COALESCE(SUM(i.insurancePaid), 0) FROM Invoice i WHERE i.paymentStatus = 'PAID'")
    BigDecimal getTotalInsurancePaid();

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.paymentStatus = :status")
    Long countByPaymentStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(COALESCE(i.customerPaid, i.totalServiceFee, 0)), 0) " +
            "FROM Invoice i " +
            "WHERE i.paymentStatus = 'PAID' " +
            "AND COALESCE(i.paidAt, i.createdAt) >= :from " +
            "AND COALESCE(i.paidAt, i.createdAt) < :to")
    BigDecimal sumPaidCustomerAmountBetween(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(i) FROM Invoice i " +
            "WHERE i.paymentStatus = 'PAID' " +
            "AND COALESCE(i.paidAt, i.createdAt) >= :from " +
            "AND COALESCE(i.paidAt, i.createdAt) < :to")
    Long countPaidBetween(@Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(COALESCE(i.customerPaid, i.totalServiceFee, 0)), 0) " +
            "FROM Invoice i " +
            "WHERE i.paymentStatus = 'PAID' " +
            "AND (i.cashierId = :cashierId OR i.cashierId = :cashierStaffId) " +
            "AND COALESCE(i.paidAt, i.createdAt) >= :from " +
            "AND COALESCE(i.paidAt, i.createdAt) < :to")
    BigDecimal sumPaidCustomerAmountByCashierBetween(@Param("cashierId") String cashierId,
                                                     @Param("cashierStaffId") String cashierStaffId,
                                                     @Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(i) FROM Invoice i " +
            "WHERE i.paymentStatus = 'PAID' " +
            "AND (i.cashierId = :cashierId OR i.cashierId = :cashierStaffId) " +
            "AND COALESCE(i.paidAt, i.createdAt) >= :from " +
            "AND COALESCE(i.paidAt, i.createdAt) < :to")
    Long countPaidByCashierBetween(@Param("cashierId") String cashierId,
                                   @Param("cashierStaffId") String cashierStaffId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT
              COALESCE(SUM(COALESCE(i.customer_paid, i.total_service_fee, 0)), 0) AS revenue,
              COUNT(*) AS paid_count
            FROM invoices i
            WHERE i.payment_status = 'PAID'
              AND COALESCE(i.paid_at, i.created_at) >= :from
              AND COALESCE(i.paid_at, i.created_at) < :to
            """, nativeQuery = true)
    Object[] getPaidSummaryBetween(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT DATE(COALESCE(i.paid_at, i.created_at)) AS paid_date,
                   COALESCE(SUM(COALESCE(i.customer_paid, i.total_service_fee, 0)), 0) AS revenue
            FROM invoices i
            WHERE i.payment_status = 'PAID'
              AND COALESCE(i.paid_at, i.created_at) >= :from
              AND COALESCE(i.paid_at, i.created_at) < :to
            GROUP BY DATE(COALESCE(i.paid_at, i.created_at))
            """, nativeQuery = true)
    List<Object[]> sumPaidCustomerAmountByDateBetween(@Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.paymentStatus = 'PAID'
              AND (:cashierId IS NULL OR :cashierId = '' OR i.cashierId = :cashierId OR i.cashierId = :cashierStaffId)
              AND COALESCE(i.paidAt, i.createdAt) >= :from
              AND COALESCE(i.paidAt, i.createdAt) < :to
            ORDER BY COALESCE(i.paidAt, i.createdAt) DESC
            """)
    List<Invoice> findPaidInvoicesForCashierBetween(@Param("cashierId") String cashierId,
                                                    @Param("cashierStaffId") String cashierStaffId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

        Optional<Invoice> findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(String prefix);

                @Query("""
                                                SELECT MIN(i.invoiceId)
                                                FROM Invoice i
                                                JOIN i.medicalRecord mr
                                                JOIN mr.appointment a
                                                WHERE a.status = :appointmentStatus
                                                        AND NOT EXISTS (
                                                                                SELECT 1
                                                                                FROM Invoice issuedInvoice
                                                                                WHERE issuedInvoice.medicalRecord = mr
                                                                                        AND issuedInvoice.receiptStatus = 'ISSUED'
                                                        )
                                                        AND NOT EXISTS (
                                                                                SELECT 1
                                                                                FROM Invoice processingInvoice
                                                                                WHERE processingInvoice.medicalRecord = mr
                                                                                        AND processingInvoice.receiptStatus = :processingStatus
                                                                                        AND processingInvoice.receiptProcessingStartedAt IS NOT NULL
                                                                                        AND processingInvoice.receiptProcessingStartedAt >= :staleBefore
                                                        )
                                                        AND i.paymentStatus = :paymentStatus
                                                        AND (
                                                                                i.receiptStatus IN :claimableStatuses
                                                                                OR (
                                                                                                i.receiptStatus = :processingStatus
                                                                                                AND i.receiptProcessingStartedAt IS NOT NULL
                                                                                                AND i.receiptProcessingStartedAt < :staleBefore
                                                                        )
                                                        )
                                                GROUP BY mr.recordId
                                                ORDER BY MIN(i.createdAt) ASC
                                                """)
                List<String> findInvoiceIdsEligibleForReceiptIssuance(@Param("appointmentStatus") String appointmentStatus,
                                                                                                                                                                                                                                        @Param("paymentStatus") String paymentStatus,
                                                                                                                                                                                                                                        @Param("claimableStatuses") Collection<String> claimableStatuses,
                                                                                                                                                                                                                                        @Param("processingStatus") String processingStatus,
                                                                                                                                                                                                                                        @Param("staleBefore") LocalDateTime staleBefore,
                                                                                                                                                                                                                                        Pageable pageable);

                /**
                 * Find distinct record IDs that have at least one invoice eligible for receipt issuance.
                 * This ensures we issue exactly ONE consolidated e-invoice per medical record (per appointment),
                 * regardless of how many individual invoices (initial + supplement) exist.
                 */
                @Query("""
                                SELECT DISTINCT mr.recordId
                                FROM Invoice i
                                JOIN i.medicalRecord mr
                                JOIN mr.appointment a
                                WHERE a.status = :appointmentStatus
                                        AND NOT EXISTS (
                                                        SELECT 1
                                                        FROM Invoice issuedInvoice
                                                        WHERE issuedInvoice.medicalRecord = mr
                                                                AND issuedInvoice.receiptStatus = 'ISSUED'
                                        )
                                        AND NOT EXISTS (
                                                        SELECT 1
                                                        FROM Invoice processingInvoice
                                                        WHERE processingInvoice.medicalRecord = mr
                                                                AND processingInvoice.receiptStatus = :processingStatus
                                                                AND processingInvoice.receiptProcessingStartedAt IS NOT NULL
                                                                AND processingInvoice.receiptProcessingStartedAt >= :staleBefore
                                        )
                                        AND i.paymentStatus = :paymentStatus
                                        AND (
                                                        i.receiptStatus IN :claimableStatuses
                                                        OR (
                                                                        i.receiptStatus = :processingStatus
                                                                        AND i.receiptProcessingStartedAt IS NOT NULL
                                                                        AND i.receiptProcessingStartedAt < :staleBefore
                                                        )
                                        )
                                ORDER BY mr.recordId ASC
                                """)
                List<String> findRecordIdsEligibleForReceiptIssuance(@Param("appointmentStatus") String appointmentStatus,
                                                                                                                                        @Param("paymentStatus") String paymentStatus,
                                                                                                                                        @Param("claimableStatuses") Collection<String> claimableStatuses,
                                                                                                                                        @Param("processingStatus") String processingStatus,
                                                                                                                                        @Param("staleBefore") LocalDateTime staleBefore,
                                                                                                                                        Pageable pageable);

                @Modifying(clearAutomatically = true, flushAutomatically = true)
                @Query(value = """
                                                UPDATE invoices current_invoice
                                                LEFT JOIN invoices active_invoice
                                                        ON active_invoice.record_id = current_invoice.record_id
                                                        AND active_invoice.receipt_status = :processingStatus
                                                        AND active_invoice.receipt_processing_started_at IS NOT NULL
                                                        AND active_invoice.receipt_processing_started_at >= :staleBefore
                                                SET current_invoice.receipt_status = :processingStatus,
                                                                current_invoice.receipt_processing_started_at = :processingStartedAt,
                                                                current_invoice.receipt_error_message = NULL
                                                WHERE current_invoice.record_id = :recordId
                                                        AND active_invoice.invoice_id IS NULL
                                                        AND current_invoice.payment_status = :paymentStatus
                                                        AND (
                                                                current_invoice.receipt_status = :pendingStatus
                                                                OR current_invoice.receipt_status = :failedStatus
                                                                OR (
                                                                        current_invoice.receipt_status = :processingStatus
                                                                        AND current_invoice.receipt_processing_started_at IS NOT NULL
                                                                        AND current_invoice.receipt_processing_started_at < :staleBefore
                                                                )
                                                        )
                                                """, nativeQuery = true)
                int claimReceiptIssuanceByRecord(@Param("recordId") String recordId,
                                                                                                                                 @Param("paymentStatus") String paymentStatus,
                                                                                                                                 @Param("pendingStatus") String pendingStatus,
                                                                                                                                 @Param("failedStatus") String failedStatus,
                                                                                                                                 @Param("processingStatus") String processingStatus,
                                                                                                                                 @Param("processingStartedAt") LocalDateTime processingStartedAt,
                                                                                                                                 @Param("staleBefore") LocalDateTime staleBefore);

                @Modifying(clearAutomatically = true, flushAutomatically = true)
                @Query("""
                                                UPDATE Invoice i
                                                SET i.receiptStatus = :processingStatus,
                                                                i.receiptProcessingStartedAt = :processingStartedAt,
                                                                i.receiptErrorMessage = NULL
                                                WHERE i.invoiceId = :invoiceId
                                                        AND i.paymentStatus = :paymentStatus
                                                        AND (
                                                                                i.receiptStatus IN :claimableStatuses
                                                                                OR (
                                                                                                i.receiptStatus = :processingStatus
                                                                                                AND i.receiptProcessingStartedAt IS NOT NULL
                                                                                                AND i.receiptProcessingStartedAt < :staleBefore
                                                                                )
                                                        )
                                                """)
                int claimReceiptIssuance(@Param("invoiceId") String invoiceId,
                                                                                                                 @Param("paymentStatus") String paymentStatus,
                                                                                                                 @Param("claimableStatuses") Collection<String> claimableStatuses,
                                                                                                                 @Param("processingStatus") String processingStatus,
                                                                                                                 @Param("processingStartedAt") LocalDateTime processingStartedAt,
                                                                                                                 @Param("staleBefore") LocalDateTime staleBefore);

                @Modifying(clearAutomatically = true, flushAutomatically = true)
                @Query("""
                                                UPDATE Invoice i
                                                SET i.receiptStatus = :failedStatus,
                                                                i.receiptProcessingStartedAt = NULL,
                                                                i.receiptErrorMessage = :errorMessage
                                                WHERE i.invoiceId = :invoiceId
                                                        AND i.receiptStatus <> :issuedStatus
                                                """)
                int markReceiptIssuanceFailed(@Param("invoiceId") String invoiceId,
                                                                                                                                        @Param("failedStatus") String failedStatus,
                                                                                                                                        @Param("issuedStatus") String issuedStatus,
                                                                                                                                        @Param("errorMessage") String errorMessage);

                @Query("""
                                                SELECT DISTINCT a.appointmentId
                                                FROM Invoice i
                                                JOIN i.medicalRecord mr
                                                JOIN mr.appointment a
                                                WHERE a.status = :appointmentStatus
                                                        AND i.paymentStatus = :paymentStatus
                                                        AND i.bhytSyncStatus IN :claimableStatuses
                                                ORDER BY a.appointmentId ASC
                                                """)
                List<String> findAppointmentIdsEligibleForBhytSync(@Param("appointmentStatus") String appointmentStatus,
                                                                                                                                                                                                                         @Param("paymentStatus") String paymentStatus,
                                                                                                                                                                                                                         @Param("claimableStatuses") Collection<String> claimableStatuses,
                                                                                                                                                                                                                         Pageable pageable);

                @Modifying(clearAutomatically = true, flushAutomatically = true)
                @Query(value = """
                                                UPDATE invoices i
                                                JOIN medical_records mr ON mr.record_id = i.record_id
                                                JOIN appointments a ON a.appointment_id = mr.appointment_id
                                                SET i.bhyt_sync_status = :processingStatus,
                                                                i.sync_error_message = NULL
                                                WHERE a.appointment_id = :appointmentId
                                                        AND i.payment_status = :paymentStatus
                                                        AND (i.bhyt_sync_status = :pendingStatus OR i.bhyt_sync_status = :failedStatus)
                                                """, nativeQuery = true)
                int claimBhytSyncProcessingByAppointment(@Param("appointmentId") String appointmentId,
                                                                                                                                                                                 @Param("paymentStatus") String paymentStatus,
                                                                                                                                                                                 @Param("pendingStatus") String pendingStatus,
                                                                                                                                                                                 @Param("failedStatus") String failedStatus,
                                                                                                                                                                                 @Param("processingStatus") String processingStatus);

                @Modifying(clearAutomatically = true, flushAutomatically = true)
                @Query(value = """
                                                UPDATE invoices i
                                                JOIN medical_records mr ON mr.record_id = i.record_id
                                                JOIN appointments a ON a.appointment_id = mr.appointment_id
                                                SET i.bhyt_sync_status = :status,
                                                                i.sync_error_message = :errorMessage
                                                WHERE a.appointment_id = :appointmentId
                                                        AND i.payment_status = :paymentStatus
                                                """, nativeQuery = true)
                int updateBhytSyncStatusByAppointment(@Param("appointmentId") String appointmentId,
                                                                                                                                                                        @Param("paymentStatus") String paymentStatus,
                                                                                                                                                                        @Param("status") String status,
                                                                                                                                                                        @Param("errorMessage") String errorMessage);

    // Customer-facing: find invoices by patient's userId (with optional status filter)
    @Query("SELECT i FROM Invoice i " +
            "LEFT JOIN i.medicalRecord mr " +
            "LEFT JOIN mr.patient p " +
            "LEFT JOIN p.user u " +
            "WHERE u.userId = :userId " +
            "AND (:paymentStatus IS NULL OR :paymentStatus = '' OR i.paymentStatus = :paymentStatus) " +
            "ORDER BY i.createdAt DESC")
    Page<Invoice> findByPatientUserId(@Param("userId") Long userId,
                                      @Param("paymentStatus") String paymentStatus,
                                      Pageable pageable);

    // Check if invoice belongs to a given userId
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Invoice i " +
            "LEFT JOIN i.medicalRecord mr " +
            "LEFT JOIN mr.patient p " +
            "LEFT JOIN p.user u " +
            "WHERE i.invoiceId = :invoiceId AND u.userId = :userId")
    boolean existsByInvoiceIdAndPatientUserId(@Param("invoiceId") String invoiceId,
                                              @Param("userId") Long userId);
}
