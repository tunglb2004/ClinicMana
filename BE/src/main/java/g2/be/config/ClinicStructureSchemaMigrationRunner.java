package g2.be.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class ClinicStructureSchemaMigrationRunner implements ApplicationRunner {

    private static final String COLUMN_EXISTS_SQL = """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND COLUMN_NAME = ?
            """;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySqlDatabase()) {
            return;
        }

        ensureDepartmentColumns();
        ensureRoomColumns();
        ensureInvoiceColumns();
        ensureAppointmentColumns();
        ensureMedicalRecordColumns();
        ensureServiceOrderColumns();
        ensureReviewColumns();
    }

    private void ensureDepartmentColumns() {
        addColumnIfMissing("departments", "is_active",
                "ALTER TABLE departments ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER description");
        addColumnIfMissing("departments", "is_deleted",
                "ALTER TABLE departments ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER is_active");

        jdbcTemplate.update("""
                UPDATE departments
                SET is_active = 1
                WHERE is_active IS NULL
                """);
        jdbcTemplate.update("""
                UPDATE departments
                SET is_deleted = 0
                WHERE is_deleted IS NULL
                """);
    }

    private void ensureRoomColumns() {
        addColumnIfMissing("rooms", "is_deleted",
                "ALTER TABLE rooms ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER status");

        jdbcTemplate.update("""
                UPDATE rooms
                SET is_deleted = 0
                WHERE is_deleted IS NULL
                """);
    }

    private void ensureInvoiceColumns() {
        addColumnIfMissing("invoices", "receipt_processing_started_at",
                "ALTER TABLE invoices ADD COLUMN receipt_processing_started_at DATETIME NULL AFTER receipt_status");
        addColumnIfMissing("invoices", "receipt_error_message",
                "ALTER TABLE invoices ADD COLUMN receipt_error_message VARCHAR(2000) NULL AFTER receipt_processing_started_at");

        jdbcTemplate.update("""
                UPDATE invoices
                SET receipt_status = 'NONE'
                WHERE receipt_status IS NULL OR TRIM(receipt_status) = ''
                """);
    }

    private void ensureAppointmentColumns() {
        addColumnIfMissing("appointments", "paraclinical_doctor_id",
                "ALTER TABLE appointments ADD COLUMN paraclinical_doctor_id VARCHAR(20) NULL AFTER doctor_id");
        addColumnIfMissing("appointments", "paraclinical_room_id",
                "ALTER TABLE appointments ADD COLUMN paraclinical_room_id VARCHAR(20) NULL AFTER room_id");
        addColumnIfMissing("appointments", "visit_type",
                "ALTER TABLE appointments ADD COLUMN visit_type VARCHAR(20) NOT NULL DEFAULT 'INITIAL' AFTER status");
        addColumnIfMissing("appointments", "follow_up_source_record_id",
                "ALTER TABLE appointments ADD COLUMN follow_up_source_record_id VARCHAR(20) NULL AFTER visit_type");

        jdbcTemplate.update("""
                UPDATE appointments
                SET paraclinical_doctor_id = doctor_id
                WHERE paraclinical_doctor_id IS NULL
                  AND doctor_id IS NOT NULL
                """);

        jdbcTemplate.update("""
                UPDATE appointments a
                LEFT JOIN doctors d ON d.doctor_id = a.doctor_id
                SET a.paraclinical_room_id = COALESCE(a.room_id, d.room_id)
                WHERE a.paraclinical_room_id IS NULL
                """);

        jdbcTemplate.update("""
                UPDATE appointments
                SET visit_type = 'INITIAL'
                WHERE visit_type IS NULL OR TRIM(visit_type) = ''
                """);
    }

    private void ensureMedicalRecordColumns() {
        addColumnIfMissing("medical_records", "visit_type",
                "ALTER TABLE medical_records ADD COLUMN visit_type VARCHAR(20) NOT NULL DEFAULT 'INITIAL' AFTER status");
        addColumnIfMissing("medical_records", "parent_record_id",
                "ALTER TABLE medical_records ADD COLUMN parent_record_id VARCHAR(20) NULL AFTER visit_type");
        addColumnIfMissing("medical_records", "root_record_id",
                "ALTER TABLE medical_records ADD COLUMN root_record_id VARCHAR(20) NULL AFTER parent_record_id");

        jdbcTemplate.update("""
                UPDATE medical_records
                SET visit_type = 'INITIAL'
                WHERE visit_type IS NULL OR TRIM(visit_type) = ''
                """);
    }

    private void ensureServiceOrderColumns() {
        addColumnIfMissing("service_orders", "assigned_room_id",
                "ALTER TABLE service_orders ADD COLUMN assigned_room_id VARCHAR(20) NULL AFTER technician_id");

        jdbcTemplate.update("""
                UPDATE service_orders so
                LEFT JOIN doctors technician ON technician.doctor_id = so.technician_id
                LEFT JOIN service_room technician_sr
                  ON technician_sr.room_id = technician.room_id
                 AND technician_sr.service_id = so.service_id
                LEFT JOIN medical_records mr ON mr.record_id = so.record_id
                LEFT JOIN appointments a ON a.appointment_id = mr.appointment_id
                LEFT JOIN service_room paraclinical_sr
                  ON paraclinical_sr.room_id = a.paraclinical_room_id
                 AND paraclinical_sr.service_id = so.service_id
                LEFT JOIN service_room appointment_sr
                  ON appointment_sr.room_id = a.room_id
                 AND appointment_sr.service_id = so.service_id
                SET so.assigned_room_id = COALESCE(
                    CASE WHEN technician_sr.room_id IS NOT NULL THEN technician.room_id END,
                    CASE WHEN paraclinical_sr.room_id IS NOT NULL THEN a.paraclinical_room_id END,
                    CASE WHEN appointment_sr.room_id IS NOT NULL THEN a.room_id END
                )
                WHERE so.assigned_room_id IS NULL
                """);
    }

    private void ensureReviewColumns() {
        addColumnIfMissing("reviews", "service_id",
                "ALTER TABLE reviews ADD COLUMN service_id VARCHAR(20) NULL AFTER doctor_id");
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        if (columnExists(tableName, columnName)) {
            return;
        }

        log.info("Adding {}.{} column for backward-compatible startup migration", tableName, columnName);
        jdbcTemplate.execute(ddl);
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                COLUMN_EXISTS_SQL,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean isMySqlDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("mysql");
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot inspect database type before running clinic structure migration", exception);
        }
    }
}
