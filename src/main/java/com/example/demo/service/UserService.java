package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import framework.annotations.Inject;
import framework.annotations.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Business/application layer — orchestrates use cases using repositories.
 *
 * <p><b>Spring equivalent:</b> {@code @Service} class. In a REST app, a {@code @RestController}
 * would call methods here — the service never touches HTTP directly.
 *
 * <p><b>Layer rule:</b>
 * <pre>
 *   Controller  →  Service  →  Repository  →  Database
 *   (HTTP)         (logic)     (SQL)
 * </pre>
 *
 * <p><b>Learn:</b> {@code UserService} doesn't know about JDBC, SQL, or H2 — it only
 * knows {@code UserRepository}. That's dependency inversion: depend on abstractions,
 * not concrete database details. Swap H2 for PostgreSQL by changing config, not this class.
 */
@Service
public class UserService {

    @Inject
    private UserRepository userRepository;

    /**
     * Demo use case: insert two users, list all, look one up by email.
     * Replace this with real business methods as you extend the project.
     */
    public void runDemo() throws SQLException {
        System.out.println();
        System.out.println("=== User Demo ===");

        userRepository.save(new User(1, "Ada Lovelace", "ada@example.com"));
        userRepository.save(new User(2, "Grace Hopper", "grace@example.com"));

        List<User> users = userRepository.findAll();
        System.out.println("All users:");
        users.forEach(u -> System.out.println("  • " + u));

        User found = userRepository.findByEmail("ada@example.com");
        System.out.println();
        System.out.println("Lookup by email: " + found);
    }
}
