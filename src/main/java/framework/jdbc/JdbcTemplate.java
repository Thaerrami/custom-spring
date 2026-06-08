package framework.jdbc;

import framework.annotations.Component;
import framework.annotations.Inject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around JDBC that removes boilerplate: open connection, bind params, close resources.
 *
 * <p><b>Spring equivalent:</b> {@code org.springframework.jdbc.core.JdbcTemplate} — one of
 * Spring's most-used classes. Spring Data JPA sits above this; raw JDBC sits below.
 *
 * <p><b>Key JDBC concepts used here:</b>
 * <ul>
 *   <li>{@code PreparedStatement} — SQL with {@code ?} placeholders (prevents SQL injection)</li>
 *   <li>{@code ResultSet} — cursor over query result rows</li>
 *   <li>{@code try-with-resources} — auto-closes Connection/Statement/ResultSet</li>
 * </ul>
 *
 * <p><b>Learn:</b> Trace {@code UserRepository.save()} → {@code update()} → {@code getConnection()}
 * to follow one INSERT from Java object to SQL row.
 */
@Component
public class JdbcTemplate {

    @Inject
    private DataSourceManager dataSource;

    /**
     * Runs INSERT, UPDATE, or DELETE. Returns the number of affected rows.
     *
     * @param sql    SQL with {@code ?} placeholders
     * @param params values bound to placeholders in order (1-indexed in JDBC)
     */
    public int update(String sql, Object... params) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParams(stmt, params);
            return stmt.executeUpdate();
        }  // LEARN: try-with-resources calls conn.close() here — always close connections!
    }

    /**
     * Runs a SELECT and maps every row to a domain object via {@link RowMapper}.
     */
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParams(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {  // advance cursor row by row
                    results.add(mapper.map(new ResultRow(rs)));
                }
                return results;
            }
        }
    }

    /** Like {@link #query} but returns the first row or {@code null} if empty. */
    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> results = query(sql, mapper, params);
        return results.isEmpty() ? null : results.get(0);
    }

    /** Binds Java values to SQL {@code ?} placeholders — JDBC uses 1-based indexing. */
    private void bindParams(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);  // LEARN: first ? is index 1, not 0
        }
    }

    /**
     * Hides raw {@link ResultSet} behind named column accessors.
     * Avoids leaking JDBC types into repository code.
     */
    public static final class ResultRow {
        private final ResultSet rs;

        ResultRow(ResultSet rs) {
            this.rs = rs;
        }

        public long getLong(String column) throws SQLException {
            return rs.getLong(column);
        }

        public String getString(String column) throws SQLException {
            return rs.getString(column);
        }

        public int getInt(String column) throws SQLException {
            return rs.getInt(column);
        }
    }
}
