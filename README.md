# lumie-backend

Spring Boot **monolith** (Gradle multi-module, hexagonal architecture) — single deployable from `app/`, with domain modules in `modules/*` and shared libs in `libs/*`.

## Stack

- Java 21 + Spring Boot 3.4
- Gradle 8 multi-module (root + `app/` + 16 modules + 3 libs)
- Spring Data JPA + Postgres (RLS-based multi-tenancy)
- Spring AMQP (RabbitMQ for worker callbacks)
- **Format**: Spotless (hygiene mode — `removeUnusedImports` + whitespace)
- **Static analysis**: ErrorProne (warning mode while baseline cleaned)
- **Tests**: JUnit 5 + Mockito + MockMvc; TestContainers Postgres for integration

## Architecture

Hexagonal per module:

```
modules/{module}/src/main/java/com/lumie/{module}/
├── domain/{entity,vo,repository,exception}/
├── application/{service,port/in,port/out,dto/{request,response}}/
└── adapter/{in/web,in/messaging,out/persistence,out/external,internal}/
```

Inter-module communication: ONLY via `libs/internal-api` interfaces. No HTTP/gRPC inside the monolith; direct bean injection.

## Multi-tenancy

Postgres RLS — every tenant-scoped table has `tenant_id BIGINT NOT NULL` + an `RLS USING (tenant_id = current_setting('app.tenant_id'))` policy. The tenant id is read from JWT (HTTP) or message payload (RabbitListener), set into `TenantContextHolder`, and bound by `RlsTenantContextAspect`. Runtime app connects as `lumie_app` (NOSUPERUSER, NOBYPASSRLS); Flyway as `postgres`.

## Prerequisites

- JDK 21 (Gradle auto-downloads via toolchain)
- Docker (for TestContainers + Tilt)
- Access to Gitea (`github.lumie-infra.com/Lumie-Edu/lumie-backend`)

## Setup

```bash
./gradlew build           # compile + test
cp .env.example .env      # fill in DB/MQ/MinIO URLs
./gradlew bootRun         # http://localhost:8080
tilt up                   # full k3s dev stack
```

## Common commands

```bash
./gradlew build                      # compile + test + spotlessCheck + checkLicense
./gradlew :modules:student:test      # per-module test
./gradlew spotlessApply              # auto-format
./gradlew compileJava                # ErrorProne warnings surface here
./gradlew :app:bootJar               # production jar
./gradlew :app:dependencies          # resolution graph
```

Configuration-cache is enabled (`org.gradle.configuration-cache=true`) — incremental builds are 20–40% faster.

## References

- Workspace rules: `../.claude/CLAUDE.md`
- Per-repo AI rules: `./CLAUDE.md`
- Hard rules: `.claude/knowledge/BACKEND_RULES.md`
- Architecture intent (workspace): `../.claude/knowledge/ARCHITECTURE_INTENT.md`
- Path-scoped rules: `.claude/rules/{controllers,entities,services,migrations}.md`
