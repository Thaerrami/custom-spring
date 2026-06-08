package com.example.demo.repository;

import com.example.demo.model.User;
import framework.annotations.Inject;
import framework.annotations.Repository;
import framework.jdbc.JdbcTemplate;

import java.sql.SQLException;
import java.util.List;

/**
 * Data-access layer — all SQL for the {@code users} table lives here.
 *
 * <p><b>Spring equivalent:</b> {@code @Repository} class, or a Spring Data
 * {@code JpaRepository<User, Long>} interface (Spring generates the implementation).
 *
 * <p><b>Layer rule:</b> Repositories translate between Java objects and SQL rows.
 * They should NOT contain business rules (e.g. "is this email valid for registration?").
 *
 * <p><b>SQL injection note:</b> We use {@code PreparedStatement} with {@code ?} placeholders
 * — never concatenate user input into SQL strings. See {@code JdbcTemplate.update()}.
 *
 * <p><b>Learn:</b> Compare {@code save()} (INSERT) and {@code findAll()} (SELECT + RowMapper)
 * to see the two fundamental database operations.
 */
@Repository
public class UserRepository {

    @Inject
    private JdbcTemplate jdbc;

    /** INSERT a new user row. The {@code ?} placeholders map to the params in order. */
    public void save(User user) throws SQLException {
        jdbc.update(
                "INSERT INTO users (id, name, email) VALUES (?, ?, ?)",
                user.getId(), user.getName(), user.getEmail()
        );
    }

    /** SELECT all users, mapping each ResultSet row to a User via RowMapper lambda. */
    public List<User> findAll() throws SQLException {
        return jdbc.query(
                "SELECT id, name, email FROM users ORDER BY id",
                row -> new User(row.getLong("id"), row.getString("name"), row.getString("email"))
        );
    }

    /** SELECT one user by email — the {@code email} param binds to the single {@code ?}. */
    public User findByEmail(String email) throws SQLException {
        return jdbc.queryOne(
                "SELECT id, name, email FROM users WHERE email = ?",
                row -> new User(row.getLong("id"), row.getString("name"), row.getString("email")),
                email
        );
    }
}
