package framework.jdbc;

import java.sql.SQLException;

/**
 * Maps a single row from a SQL {@code ResultSet} to a Java object.
 *
 * <p><b>Spring equivalent:</b> {@code RowMapper<T>} in {@code org.springframework.jdbc.core}.
 *
 * <p><b>Why a separate interface?</b> Lambdas that call {@code row.getString()} throw
 * checked {@link SQLException}. This interface declares {@code throws SQLException} so
 * callers inside {@link JdbcTemplate} can propagate it cleanly.
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * row -> new User(row.getLong("id"), row.getString("name"), row.getString("email"))
 * }</pre>
 *
 * @param <T> the domain type to map each row to (e.g. {@code User})
 */
@FunctionalInterface
public interface RowMapper<T> {
    T map(JdbcTemplate.ResultRow row) throws SQLException;
}
