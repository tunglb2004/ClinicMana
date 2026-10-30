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
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class CashHandoverSchemaMigrationRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySqlDatabase()) {
            return;
        }

        log.info("Ensuring cash_handovers table exists");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS cash_handovers (
                    handover_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    business_date DATE NOT NULL,
                    from_cashier_user_id BIGINT NOT NULL,
                    to_cashier_user_id BIGINT NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    note VARCHAR(1000) NULL,
                    created_at DATETIME NOT NULL,
                    confirmed_at DATETIME NULL,
                    cancelled_at DATETIME NULL,
                    INDEX idx_cash_handovers_business_from (business_date, from_cashier_user_id),
                    INDEX idx_cash_handovers_business_to (business_date, to_cashier_user_id),
                    INDEX idx_cash_handovers_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

    private boolean isMySqlDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("mysql");
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot inspect database type before running cash handover migration", exception);
        }
    }
}
