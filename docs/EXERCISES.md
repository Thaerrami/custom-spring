# Hands-on Exercises

Work through these in order. Each exercise teaches one concept that directly maps to Spring.

---

## Level 1: Observe & break things

### Exercise 1.1 — Watch the startup log

Run `./scripts/run.sh` and label each log line with the container phase:

```
⚡ CustomSpring Container starting...     → ?
  + Bean: AppConfig                       → ?
  → Injected AppConfig → DataSourceManager → ?
  ✓ Schema applied from schema.sql        → ?
  ✓ Database connected: jdbc:h2:...       → ?
✅ Container initialized                  → ?
```

<details>
<summary>Answer</summary>

1. Bootstrap begins
2. Phase 2: Registration
3. Phase 3: Injection
4. Phase 4: @PostConstruct (SchemaRunner)
5. Phase 4: @PostConstruct (DataSourceManager)
6. All phases complete
</details>

### Exercise 1.2 — Missing dependency

1. Comment out `@Inject` on `UserRepository.jdbc`
2. Rebuild and run
3. What happens? Why no compile error?

<details>
<summary>Answer</summary>

`NullPointerException` at runtime when `save()` calls `jdbc.update()`. DI is runtime wiring — the compiler doesn't know about `@Inject`.
</details>

### Exercise 1.3 — Wrong package scan

In `Application.java`, change init to only scan `"com.example.demo"`.

What breaks? Why?

<details>
<summary>Answer</summary>

Framework beans (`AppConfig`, `JdbcTemplate`, etc.) won't be registered. You'll get "Missing dependency" for `AppConfig` or similar. The framework package must be scanned too.
</details>

---

## Level 2: Extend the framework

### Exercise 2.1 — Add a `@Service`

Create `com.example.demo.service.GreetingService`:

```java
@Service
public class GreetingService {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
```

Inject it into `UserService` and call it from `runDemo()`. Rebuild — no container changes needed.

**Learn:** Component scanning finds new beans automatically (convention over configuration).

### Exercise 2.2 — Add a new entity

1. Add `CREATE TABLE products (...)` to `schema.sql`
2. Create `Product` model, `ProductRepository`, `ProductService`
3. Wire and demo CRUD

**Learn:** Full vertical slice — schema → model → repository → service.

### Exercise 2.3 — Add `@PreDestroy` logging

Add a `@PreDestroy` method to `UserService` that prints "UserService shutting down".

Verify it runs when `container.shutdown()` is called.

---

## Level 3: Database deep dive

### Exercise 3.1 — Query from H2 console

1. Change `db.url` to `jdbc:h2:mem:customspring;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
2. Add `AUTO_SERVER=TRUE` and connect with H2 console while app runs

Or: log the JDBC URL and use an file-based H2:

```properties
db.url=jdbc:h2:file:./data/customspring
```

**Learn:** In-memory vs file-based databases.

### Exercise 3.2 — Duplicate email constraint

Try inserting two users with the same email in `runDemo()`.

Read the `SQLException` message — that's the DB enforcing the `UNIQUE` constraint from `schema.sql`.

### Exercise 3.3 — Switch to PostgreSQL

Follow [DATABASE-GUIDE.md](./DATABASE-GUIDE.md#switching-to-postgresql).

---

## Level 4: Framework internals

### Exercise 4.1 — Constructor injection

Modify `Container.registerBean()` to support constructor injection:

```java
@Inject
public UserService(UserRepository repo) { ... }
```

Hint: find constructor parameters annotated with `@Inject`, resolve from registry, call `getDeclaredConstructor(types).newInstance(deps)`.

**Learn:** Why Spring prefers constructor injection (immutable, testable, explicit).

### Exercise 4.2 — Log the classloader

In `scanPackage()`, print `classLoader.getClass().getName()`.

Run from IDE vs `./scripts/run.sh` — same or different?

### Exercise 4.3 — Read Spring source side-by-side

Open Spring's `AutowiredAnnotationBeanPostProcessor` on GitHub.

Compare to `Container.injectDependencies()` — list 5 differences.

---

## Level 5: Publish-ready

### Exercise 5.1 — Add a dependency via pom.json

Add SLF4J for logging:

```json
{
  "groupId": "org.slf4j",
  "artifactId": "slf4j-simple",
  "version": "2.0.13",
  "scope": "compile"
}
```

Rebuild. Replace one `System.out.println` with SLF4J.

**Learn:** How pom.json → lib/ → classpath → import works.

### Exercise 5.2 — Write a one-paragraph architecture doc

Explain IoC to someone who knows Java but not Spring, using only concepts from this repo.

### Exercise 5.3 — Code review checklist

Before publishing, verify:

- [ ] `./scripts/build.sh` succeeds on a clean clone
- [ ] `./scripts/run.sh` prints user demo output
- [ ] No secrets in `application.properties`
- [ ] `.gitignore` excludes `out/` and `lib/`
- [ ] README links to docs/

---

## Challenge projects

| Project | Skills practiced |
|---------|------------------|
| Add `@Controller` + embedded HTTP server (Javalin or raw HttpServer) | Web layer on top of IoC |
| Implement `@Qualifier` for multiple beans of same type | Disambiguation |
| Add connection pooling (HikariCP via pom.json) | Production DataSource |
| Write unit tests without container (mock UserRepository) | Testing layered apps |
| Migrate schema.sql to Flyway-style versioned files | Production migrations |

---

Back to [Learning Guide](./README.md)
