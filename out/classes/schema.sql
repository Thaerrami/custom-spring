-- =============================================================================
-- Database schema (DDL) — applied at startup by framework.jdbc.SchemaRunner
-- Spring Boot equivalent: src/main/resources/schema.sql or Flyway migration V1__
-- =============================================================================

-- IF NOT EXISTS makes re-runs safe during development
CREATE TABLE IF NOT EXISTS users (
    id    BIGINT PRIMARY KEY,          -- unique identifier for each user
    name  VARCHAR(100) NOT NULL,       -- display name, required
    email VARCHAR(150) NOT NULL UNIQUE -- email must be unique across all users
);

-- LEARN: Try adding a column here, rebuild, and re-run — SchemaRunner re-applies this file.
-- For production schema changes, use versioned migrations (Flyway/Liquibase).
