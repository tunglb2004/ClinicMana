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
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ServiceSchemaMigrationRunner implements ApplicationRunner {

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

        ensureIsDeletedColumn();
    }

    private void ensureIsDeletedColumn() {
        if (!columnExists("services", "is_deleted")) {
            log.info("Adding services.is_deleted column for backward-compatible startup migration");
            jdbcTemplate.execute("""
                    ALTER TABLE services
                    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER is_active
                    """);
        }

        jdbcTemplate.update("""
                UPDATE services
                SET is_deleted = 0
                WHERE is_deleted IS NULL
                """);
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
            throw new IllegalStateException("Cannot inspect database type before running schema migration", exception);
        }
    }
}
