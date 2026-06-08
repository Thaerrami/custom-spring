# Custom Spring

An educational IoC container that mirrors Spring's core ideas — component scanning, dependency injection, lifecycle hooks, and JDBC database access — built **without** Maven or Gradle. Dependencies are declared in **`pom.json`** (instead of `pom.xml`) and resolved from Maven Central.

## Quick start

Requires **Java 17+** and **Python 3** (for dependency resolution).

```bash
chmod +x scripts/*.sh
./scripts/build.sh    # resolve pom.json deps → compile → copy resources
./scripts/run.sh      # run the demo
```

Expected output:

```
⚡ CustomSpring Container starting...
  + Bean: AppConfig
  + Bean: DataSourceManager
  ...
  ✓ Database connected: jdbc:h2:mem:customspring
  ✓ Schema applied from schema.sql
=== User Demo ===
All users:
  • User{id=1, name='Ada Lovelace', email='ada@example.com'}
  ...
```

## Project layout

```
custom-spring/
├── pom.json                    # Project manifest (replaces pom.xml)
├── pom.schema.json             # JSON schema documenting pom.json fields
├── scripts/
│   ├── resolve-deps.py         # Downloads JARs from Maven Central
│   ├── build.sh                # Compile + package resources
│   └── run.sh                  # Run main class
├── src/main/java/
│   ├── framework/              # The mini-framework
│   │   ├── annotations/        # @Component, @Inject, @Repository, ...
│   │   ├── core/               # Container (IoC)
│   │   ├── config/             # application.properties loader
│   │   └── jdbc/               # DataSource, JdbcTemplate, schema runner
│   └── com/example/demo/       # Runnable demo with H2 database
└── src/main/resources/
    ├── application.properties  # DB connection settings
    └── schema.sql              # CREATE TABLE statements
```

## How pom.json replaces pom.xml

| pom.xml (Maven) | pom.json (this project) |
|-----------------|-------------------------|
| `<groupId>` | `"groupId"` |
| `<artifactId>` | `"artifactId"` |
| `<version>` | `"version"` |
| `<dependencies>` | `"dependencies"` array |
| `<repositories>` | `"repositories"` array |
| `mvn compile` | `./scripts/build.sh` |
| `mvn exec:java` | `./scripts/run.sh` |

See [docs/build-system.md](docs/build-system.md) for the full explanation of how dependencies are resolved and connected to the classpath.

## Architecture

```
Application
    └── Container.init("framework", "com.example.demo")
            ├── scan classpath for @Component / @Service / @Repository
            ├── instantiate beans
            ├── @Inject field wiring
            └── @PostConstruct (connect DB, run schema.sql)

UserService  ──@Inject──►  UserRepository  ──@Inject──►  JdbcTemplate
                                                              │
                                                    DataSourceManager
                                                              │
                                                    application.properties
                                                              │
                                                         H2 Database
```

## Learning docs

| Document | Topic |
|----------|-------|
| [docs/README.md](docs/README.md) | **Learning guide index** — start here |
| [docs/CODE-WALKTHROUGH.md](docs/CODE-WALKTHROUGH.md) | Read every source file in order |
| [docs/JAVA-INTERVIEW-GUIDE.md](docs/JAVA-INTERVIEW-GUIDE.md) | **Java interviews** — loops, collections, threading, top 30 Q&A |
| [docs/DATABASE-GUIDE.md](docs/DATABASE-GUIDE.md) | JDBC, schema, H2, PostgreSQL |
| [docs/architecture.md](docs/architecture.md) | Framework architecture |
| [docs/build-system.md](docs/build-system.md) | pom.json and dependency resolution |
| [docs/evolution-of-web-development.md](docs/evolution-of-web-development.md) | History of Java web stack |
| [docs/java-internals.md](docs/java-internals.md) | JVM internals for frameworks |
| [docs/spring-6-internals.md](docs/spring-6-internals.md) | Spring 6 comparison |

Every Java source file includes **learning comments** (JavaDoc + inline `LEARN` notes) explaining purpose and Spring equivalents.

## Adding a dependency

Edit `pom.json`:

```json
{
  "groupId": "org.postgresql",
  "artifactId": "postgresql",
  "version": "42.7.3",
  "scope": "compile"
}
```

Then rebuild:

```bash
./scripts/build.sh
```

The resolver downloads the JAR to `lib/` and `build.sh` adds `lib/*` to the compile and runtime classpath.

## Switching to PostgreSQL

1. Add the PostgreSQL driver to `pom.json` dependencies.
2. Update `src/main/resources/application.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/mydb
db.username=postgres
db.password=secret
```

3. Run PostgreSQL locally (Docker example):

```bash
docker run --name pg -e POSTGRES_PASSWORD=secret -e POSTGRES_DB=mydb -p 5432:5432 -d postgres:16
```

4. Rebuild and run.

## License

MIT — use freely for learning and teaching.
