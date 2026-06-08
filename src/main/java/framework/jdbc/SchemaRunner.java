package framework.jdbc;

import framework.annotations.Component;
import framework.annotations.Inject;
import framework.annotations.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Applies {@code schema.sql} to the database at startup — creates tables before any INSERT runs.
 *
 * <p><b>Spring equivalent:</b> Flyway / Liquibase migrations, or {@code schema.sql} in
 * Spring Boot ({@code spring.sql.init.mode=always}).
 *
 * <p><b>Why @PostConstruct?</b> Runs after {@link JdbcTemplate} is injected, so we can
 * execute SQL. Runs after {@link DataSourceManager} can connect (lazy init in getConnection).
 *
 * <p><b>Learn:</b> Database schema (DDL) is separate from application code. Teams version
 * schema changes in migration files — same idea as {@code schema.sql} here.
 */
@Component
public class SchemaRunner {

    @Inject
    private JdbcTemplate jdbc;

    @PostConstruct
    void runSchema() throws IOException, SQLException {
        // Load schema.sql from classpath root (copied there by build.sh)
        try (InputStream in = SchemaRunner.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql not found on classpath — did you run ./scripts/build.sh?");
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // Simple splitter — production tools (Flyway) handle complex SQL more robustly
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    jdbc.update(trimmed);
                }
            }
        }
        System.out.println("  ✓ Schema applied from schema.sql");
    }
}
