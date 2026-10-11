# lumie-backend

Spring Boot monolith for the Lumie LMS platform — handles all business logic (auth, billing, class management, student records, AI features, and more).

## Stack

- **Java**: 21 (Gradle toolchain-managed, auto-downloads via Foojay)
- **Spring Boot**: 3.4.13
- **Build**: Gradle 8 multi-module, Kotlin DSL, version catalog (`gradle/libs.versions.toml`)
- **Persistence**: Spring Data JPA + Postgres (RLS-based multi-tenancy)
- **Messaging**: Spring AMQP / RabbitMQ (worker callbacks)
- **Key libs**: Spring AI 1.0.0, Spring Modulith 1.4.4, JJWT 0.12.5, ShedLock 5.16.0, Bucket4j 8.10.1, springdoc-openapi 2.7.0, MinIO 8.5.14, Apache POI 5.2.5, Lombok 1.18.36
- **Format**: Spotless (hygiene mode — unused imports + whitespace; `spotlessApply` to fix)
- **Static analysis**: ErrorProne (warning mode while baseline violations are cleared)
- **Tests**: JUnit 5 + Mockito + MockMvc; TestContainers Postgres for integration

## Architecture

Modular monolith with pragmatic hexagonal boundaries per module. Each module under `modules/` owns a `domain/` layer (JPA aggregate entities with domain behavior), an `application/` layer (services, outbound ports, DTOs), and an `adapter/` layer (web controllers, persistence adapters, AMQP listeners, external HTTP clients). Cross-module calls are only permitted through interfaces declared in `libs/internal-api`; no direct entity imports across modules. Multi-tenancy is enforced via Postgres RLS: every tenant-scoped table carries `tenant_id`, set from JWT (HTTP) or message payload (RabbitListener) into `TenantContextHolder`, and bound to the active transaction by `RlsTenantContextAspect` in `libs/common`. Runtime app connects as `lumie_app` (NOSUPERUSER, NOBYPASSRLS); Flyway migrates as `postgres`.

## Prerequisites

- JDK 21 (or let Gradle toolchain download it)
- Docker (for TestContainers integration tests and Tilt)
- SSH access to the dev k3s cluster (host alias `k3s`)
- Access to GitHub (`github.com/Lumie-Edu/lumie-backend`)

## Setup

The backend runs on the dev k3s cluster (not locally). Stateful services (Postgres, RabbitMQ, Redis, MinIO) are deployed via Tilt. Run from the workspace root:

```bash
bash lumie-devops/tilt.sh up   # opens SSH tunnel to k3s API, then starts Tilt
```

The frontend runs locally at `http://localhost:3000` and proxies `/api/v1/*` to `https://dev.lumie-infra.com`.

For local tests only (no cluster needed — TestContainers handles Postgres):

```bash
./gradlew test
```

## Common commands

```bash
./gradlew build                           # compile + spotlessCheck + test + checkLicense
./gradlew test                            # all tests (TestContainers spins up Postgres)
./gradlew :modules:<module>:test          # single module, e.g. :modules:student:test
./gradlew spotlessApply                   # fix import/whitespace issues
./gradlew compileJava                     # surface ErrorProne warnings
./gradlew :app:bootJar                    # production JAR
./gradlew jacocoTestCoverageVerification  # explicit coverage gate (30% minimum, not in check yet)
./gradlew checkLicense                    # verify dependency licenses against allowlist
```

Configuration cache is enabled (`org.gradle.configuration-cache=true`) — incremental builds are faster.

## Module map

| Module | Purpose |
|---|---|
| `admin` | Admin-facing management operations (tenant provisioning, system config) |
| `ai` | AI feature integrations (Spring AI; tutoring, content generation) |
| `assignment` | Assignment creation, submission, and grading workflows |
| `auth` | Authentication and authorization — JWT issuance, Spring Security config |
| `billing` | Subscription, payment, and invoice management |
| `class` | Class and cohort lifecycle management |
| `content` | Learning content (materials, videos, attachments) |
| `exam` | Exam creation, delivery, and scoring |
| `file` | File upload/download via MinIO object storage |
| `homepage` | Tenant-branded homepage and marketing page data |
| `lecture` | Lecture scheduling and session records |
| `notification` | In-app and push notification dispatch |
| `student` | Student profile, enrollment, and progress tracking |
| `tenant` | Tenant onboarding and per-tenant configuration |

## Libraries

| Library | Purpose |
|---|---|
| `libs/common` | Shared utilities — `BaseEntity`, `TenantContextHolder`, `RlsTenantContextAspect`, `AllowedSortFields`, `PageResponse` |
| `libs/internal-api` | Cross-module service interfaces (the only permitted inter-module call boundary) |
| `libs/messaging` | AMQP wiring and shared message schemas |

## Where to look next

- `AGENTS.md` — Codex conventions and command references (not for human contributors)
- `modules/<module>/` — each module is self-contained; no per-module READMEs exist yet
- `../.codex/.charter/fragments/lumie/architecture.md` — Lumie architecture principles
- Workspace `../AGENTS.md` — cross-repo rules and full repo map
