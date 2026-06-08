package framework.jdbc;

import framework.annotations.Component;
import framework.annotations.Inject;
import framework.annotations.PostConstruct;
import framework.annotations.PreDestroy;
import framework.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC connections to the database configured in {@code application.properties}.
 *
 * <p><b>Spring equivalent:</b> {@code DataSource} bean (usually HikariCP connection pool).
 * We use plain {@link DriverManager} for simplicity — one connection per operation.
 *
 * <p><b>JDBC URL format:</b> {@code jdbc:h2:mem:name} tells the H2 driver (from {@code lib/h2-*.jar})
 * to open an in-memory database. The driver class is auto-registered via Java SPI when the JAR
 * is on the classpath — no {@code Class.forName("org.h2.Driver")} needed on Java 6+.
 *
 * <p><b>Lifecycle:</b>
 * <pre>
 *   @PostConstruct  → verifyConnection() — fail fast if DB is unreachable
 *   getConnection() → lazy init of URL/credentials from AppConfig
 *   @PreDestroy     → SHUTDOWN for H2 in-memory DB
 * </pre>
 */
@Component
public class DataSourceManager {

    @Inject
    private AppConfig config;

    private String jdbcUrl;
    private String username;
    private String password;

    /** Fail-fast check: if DB config is wrong, we know at startup, not on first user request. */
    @PostConstruct
    void verifyConnection() throws SQLException {
        try (Connection ignored = getConnection()) {
            System.out.println("  ✓ Database connected: " + jdbcUrl);
        }
    }

    /**
     * Opens a new JDBC connection. Lazy-init reads config on first call so @Inject has completed.
     *
     * <p><b>Learn:</b> Each call returns a new Connection. Production apps use a pool (HikariCP)
     * that reuses connections. {@code try-with-resources} in JdbcTemplate closes them after use.
     */
    public Connection getConnection() throws SQLException {
        if (jdbcUrl == null) {
            jdbcUrl = config.require("db.url");
            username = config.get("db.username", "sa");
            password = config.get("db.password", "");
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /** Clean shutdown — important for H2 in-memory DB to release memory. */
    @PreDestroy
    void shutdown() throws SQLException {
        if (jdbcUrl != null && jdbcUrl.contains("jdbc:h2:mem:")) {
            try (Connection conn = getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("SHUTDOWN");
            }
        }
        System.out.println("  ✓ Database connection closed");
    }
}
