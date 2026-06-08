# Code Walkthrough — Read the Code in This Order

Follow this path file-by-file. Each section explains **what** the file does, **why** it exists, and **how it maps to Spring**.

---

## Phase 0: Build & config (before Java runs)

| File | Read for |
|------|----------|
| [pom.json](../pom.json) | Project coordinates + H2 dependency declaration |
| [scripts/resolve-deps.py](../scripts/resolve-deps.py) | How JARs download from Maven Central |
| [scripts/build.sh](../scripts/build.sh) | Compile + copy resources to classpath |
| [src/main/resources/application.properties](../src/main/resources/application.properties) | DB connection settings |
| [src/main/resources/schema.sql](../src/main/resources/schema.sql) | CREATE TABLE DDL |

**Checkpoint:** Run `./scripts/build.sh && ./scripts/run.sh`. You should see beans created, schema applied, two users inserted.

---

## Phase 1: Annotations (metadata the container reads)

All in `src/main/java/framework/annotations/`:

| File | Purpose | Spring twin |
|------|---------|-------------|
| [Component.java](../src/main/java/framework/annotations/Component.java) | "Create and manage this class" | `@Component` |
| [Inject.java](../src/main/java/framework/annotations/Inject.java) | "Fill this field from the registry" | `@Autowired` |
| [Repository.java](../src/main/java/framework/annotations/Repository.java) | Data layer stereotype | `@Repository` |
| [Service.java](../src/main/java/framework/annotations/Service.java) | Business layer stereotype | `@Service` |
| [PostConstruct.java](../src/main/java/framework/annotations/PostConstruct.java) | Run after injection | `@PostConstruct` |
| [PreDestroy.java](../src/main/java/framework/annotations/PreDestroy.java) | Run on shutdown | `@PreDestroy` |

**Key concept:** `@Retention(RUNTIME)` — without it, annotations vanish after compile and reflection can't see them.

**Try:** Open `Container.isBeanCandidate()` and trace how `@Repository` is detected via meta-annotation.

---

## Phase 2: The container (IoC core)

| File | Purpose |
|------|---------|
| [Container.java](../src/main/java/framework/core/Container.java) | Scan → register → inject → lifecycle |

### Read these methods in order:

```
init()
  ├── scanPackage()        ← classpath walking (file + JAR)
  ├── registerBean()       ← reflection: newInstance()
  ├── injectDependencies() ← reflection: field.set()
  └── invokeLifecycle()    ← @PostConstruct
```

**Questions to answer as you read:**
1. Why scan before instantiate?
2. Why inject before `@PostConstruct`?
3. What happens if a `@Inject` field has no matching bean?

---

## Phase 3: Configuration & database

| File | Purpose | Spring twin |
|------|---------|-------------|
| [AppConfig.java](../src/main/java/framework/config/AppConfig.java) | Load properties | `Environment` |
| [DataSourceManager.java](../src/main/java/framework/jdbc/DataSourceManager.java) | JDBC connections | `DataSource` (HikariCP) |
| [JdbcTemplate.java](../src/main/java/framework/jdbc/JdbcTemplate.java) | SQL helper | `JdbcTemplate` |
| [RowMapper.java](../src/main/java/framework/jdbc/RowMapper.java) | Row → object mapping | `RowMapper<T>` |
| [SchemaRunner.java](../src/main/java/framework/jdbc/SchemaRunner.java) | Run schema.sql | Flyway / `schema.sql` |

### Dependency chain (follow `@Inject` arrows):

```
AppConfig
    └── DataSourceManager  (@PostConstruct: verifyConnection)
            └── JdbcTemplate
                    ├── SchemaRunner  (@PostConstruct: run schema.sql)
                    └── UserRepository
                            └── UserService
```

---

## Phase 4: Demo application (your code)

| File | Layer | Purpose |
|------|-------|---------|
| [Application.java](../src/main/java/com/example/demo/Application.java) | Bootstrap | `main()` — start container |
| [User.java](../src/main/java/com/example/demo/model/User.java) | Domain | Data holder (entity) |
| [UserRepository.java](../src/main/java/com/example/demo/repository/UserRepository.java) | Repository | SQL: INSERT, SELECT |
| [UserService.java](../src/main/java/com/example/demo/service/UserService.java) | Service | Business use case |

### Trace one INSERT end-to-end:

```
UserService.runDemo()
  → userRepository.save(user)
    → jdbc.update("INSERT ...", id, name, email)
      → dataSource.getConnection()
        → DriverManager.getConnection(jdbcUrl from application.properties)
          → H2 driver (from lib/h2-*.jar) writes row to memory
```

---

## Phase 5: Compare to Spring Boot

| This project | Spring Boot equivalent |
|--------------|------------------------|
| `Container.init("framework", "com.example.demo")` | `@SpringBootApplication` component scan |
| `application.properties` | `application.yml` |
| `schema.sql` + `SchemaRunner` | `spring.sql.init.mode=always` |
| `JdbcTemplate` | Same class name in Spring |
| `getBean(UserService.class)` | `@Autowired UserService` |
| `pom.json` + scripts | `pom.xml` + Maven |

---

## Glossary (quick reference)

| Term | Meaning |
|------|---------|
| **Bean** | Object created and managed by the container |
| **IoC** | Container creates objects; your code doesn't `new` dependencies |
| **DI** | Dependencies passed in via `@Inject`, not looked up manually |
| **Stereotype** | Specialized `@Component` (`@Service`, `@Repository`) |
| **Classpath** | All locations JVM searches for `.class` and resource files |
| **JDBC** | Java API for talking to relational databases |
| **DDL** | Data Definition Language — CREATE TABLE, ALTER, etc. |
| **DML** | Data Manipulation Language — INSERT, SELECT, UPDATE, DELETE |

---

Next: [Hands-on Exercises](./EXERCISES.md) | [Database Guide](./DATABASE-GUIDE.md)
