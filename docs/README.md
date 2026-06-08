# Custom Spring — Learning Guide

This documentation teaches **modern web architecture**, **Java internals**, and **Spring internals** by starting from the code in *this* repository and building outward to Spring 6 and production systems.

## Start here

| If you want to… | Read this |
|-----------------|-----------|
| Run the project | [README.md](../README.md) → Quick start |
| Understand every file | [Code Walkthrough](./CODE-WALKTHROUGH.md) |
| **Java interview prep** | **[Java Interview Guide](./JAVA-INTERVIEW-GUIDE.md)** — loops, collections, threading |
| Practice by doing | [Hands-on Exercises](./EXERCISES.md) |
| Learn JDBC & databases | [Database Guide](./DATABASE-GUIDE.md) |
| Understand pom.json | [Build System](./build-system.md) |

## Full curriculum (read in order)

| # | Document | What you will learn |
|---|----------|---------------------|
| 1 | [Code Walkthrough](./CODE-WALKTHROUGH.md) | **Start here** — read every source file in the right order with Spring mappings |
| 2 | [Your Framework Architecture](./architecture.md) | IoC pipeline: scan → register → inject → lifecycle |
| 3 | [Build System: pom.json](./build-system.md) | How dependencies replace `pom.xml` and connect to JDBC |
| 4 | [Database & JDBC Guide](./DATABASE-GUIDE.md) | From pom.json H2 JAR to SQL rows — full data path |
| 5 | [Hands-on Exercises](./EXERCISES.md) | Break things, extend the framework, level up |
| 6 | [Java Interview Guide](./JAVA-INTERVIEW-GUIDE.md) | Tricky Java, loops, data structures, multithreading, top 30 Q&A |
| 7 | [Evolution of Web Development](./evolution-of-web-development.md) | CGI → servlets → Spring → Boot 3 — *why* each layer exists |
| 8 | [Java Internals for Framework Builders](./java-internals.md) | Classloading, reflection, annotations, proxies |
| 9 | [Spring 6 Internals](./spring-6-internals.md) | Production Spring mapped to your mini-framework |

## Source code map (with learning comments)

Every Java file has **class-level JavaDoc** explaining purpose and Spring equivalents, plus **inline LEARN comments** on non-obvious lines.

```
src/main/java/
├── framework/
│   ├── annotations/     ← @Component, @Inject, @Repository, @Service, lifecycle
│   ├── core/
│   │   └── Container.java   ← IoC heart — read this carefully
│   ├── config/
│   │   └── AppConfig.java   ← application.properties loader
│   └── jdbc/
│       ├── DataSourceManager.java  ← JDBC connections
│       ├── JdbcTemplate.java       ← SQL helper (PreparedStatement)
│       ├── RowMapper.java          ← row → object mapping
│       └── SchemaRunner.java       ← runs schema.sql at startup
└── com/example/demo/
    ├── Application.java     ← main() entry point
    ├── model/User.java      ← domain entity
    ├── repository/UserRepository.java  ← SQL layer
    └── service/UserService.java        ← business layer
```

## Mental model

```
HTTP Request (future @Controller)
     │
     ▼
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Controller │ ──► │  Service layer   │ ──► │  Repository /   │
│  (@Rest...) │     │  (@Service)      │     │  Data access    │
└─────────────┘     └──────────────────┘     └─────────────────┘
        │                     │                        │
        └─────────────────────┴────────────────────────┘
                              │
                    All wired by the IoC Container
                    (Container ≈ Spring ApplicationContext)
                              │
                    application.properties → JDBC → H2/PostgreSQL
```

## Quick map: your code ↔ Spring

| Your framework | Spring equivalent | Purpose |
|----------------|-------------------|---------|
| `@Component` | `@Component`, `@Service`, `@Repository` | Mark a managed bean |
| `@Inject` on field | `@Autowired`, `@Inject` (JSR-330) | Declare a dependency |
| `@PostConstruct` | `@PostConstruct` | Run after injection |
| `@PreDestroy` | `@PreDestroy` | Cleanup on shutdown |
| `Container.beanRegistry` | `DefaultSingletonBeanRegistry` | Store live instances |
| `Container.init()` | Context refresh / component scan | Bootstrap |
| `AppConfig` | `Environment` / `@ConfigurationProperties` | External config |
| `JdbcTemplate` | `org.springframework.jdbc.core.JdbcTemplate` | SQL operations |
| `SchemaRunner` | Flyway / `schema.sql` init | Database DDL |
| `pom.json` | `pom.xml` | Dependency declaration |

## Suggested 7-day learning path

| Day | Activity |
|-----|----------|
| 1 | Run project, read [Code Walkthrough](./CODE-WALKTHROUGH.md) Phase 0–2 |
| 2 | Read `Container.java` line by line; do [Exercises 1.1–1.3](./EXERCISES.md) |
| 3 | Read JDBC layer; trace one INSERT in [Database Guide](./DATABASE-GUIDE.md) |
| 4 | Do [Exercises 2.1–2.3](./EXERCISES.md) — add your own service |
| 5 | Read [architecture.md](./architecture.md) + [java-internals.md](./java-internals.md) |
| 6 | Read [evolution-of-web-development.md](./evolution-of-web-development.md) |
| 7 | Read [spring-6-internals.md](./spring-6-internals.md); build a Spring Boot 3 app and compare |

## Repository layout

```
custom-spring/
├── pom.json                           # Dependencies & build config (replaces pom.xml)
├── scripts/                           # resolve-deps.py, build.sh, run.sh
├── src/main/java/
│   ├── framework/                     # IoC container + JDBC layer
│   └── com/example/demo/              # Runnable demo (H2 database)
├── src/main/resources/
│   ├── application.properties         # DB connection config
│   └── schema.sql                     # CREATE TABLE statements
└── docs/                              # You are here
```

---

*Goal: understand frameworks by building one, then recognize every Spring concept as an evolution of what you already wrote.*
