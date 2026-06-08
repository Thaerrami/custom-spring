package com.example.demo.model;

/**
 * Domain model (entity) — represents a row in the {@code users} table.
 *
 * <p><b>Spring equivalent:</b> JPA {@code @Entity} class mapped to a database table.
 * We use a plain POJO here to keep the JDBC layer visible for learning.
 *
 * <p><b>Layer rule:</b> Models hold data + simple behavior only. No SQL, no HTTP.
 * Business rules belong in {@code UserService}; persistence belongs in {@code UserRepository}.
 *
 * <p><b>Learn:</b> {@code final} fields make this object immutable after construction —
 * a good practice that prevents accidental mutation as objects pass between layers.
 */
public class User {

    private final long id;
    private final String name;
    private final String email;

    public User(long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
