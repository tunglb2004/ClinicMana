package g2.be.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceSchemaMigrationRunnerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @Test
    void runShouldAddColumnWhenMissingOnMySql() throws Exception {
        ServiceSchemaMigrationRunner runner = new ServiceSchemaMigrationRunner(dataSource, jdbcTemplate);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");
        when(jdbcTemplate.queryForObject(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.eq("services"),
                org.mockito.ArgumentMatchers.eq("is_deleted")
        )).thenReturn(0);

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.contains("ADD COLUMN is_deleted"));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("UPDATE services"));
    }

    @Test
    void runShouldSkipAlterWhenColumnAlreadyExists() throws Exception {
        ServiceSchemaMigrationRunner runner = new ServiceSchemaMigrationRunner(dataSource, jdbcTemplate);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");
        when(jdbcTemplate.queryForObject(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.eq("services"),
                org.mockito.ArgumentMatchers.eq("is_deleted")
        )).thenReturn(1);

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.contains("ADD COLUMN is_deleted"));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("UPDATE services"));
    }

    @Test
    void runShouldSkipMigrationForNonMySqlDatabase() throws Exception {
        ServiceSchemaMigrationRunner runner = new ServiceSchemaMigrationRunner(dataSource, jdbcTemplate);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("H2");

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate, never()).queryForObject(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
        verify(jdbcTemplate, never()).update(org.mockito.ArgumentMatchers.anyString());
    }
}
