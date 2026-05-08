# lumie-backend

Spring Boot **monolith**. `app/` is the single `@SpringBootApplication`; `services/*` are Gradle multi-module domain modules (logical, not network). Java 21 + Spring Boot 3.4. Hexagonal architecture per module.

> Cross-repo policies live in workspace root `CLAUDE.md`. This file owns rules **specific to lumie-backend**.

## Module layout (`settings.gradle.kts`)

- `services/core/*` — domain modules (`student-svc`, `exam-svc`, `class-svc`, `lecture-svc`, `assignment-svc`, `attendance-svc`, `content-svc`, `ai-svc`, `notification-svc`, `admin-svc`, `activity-log-svc`).
- `services/platform/*` — cross-tenant (`tenant-svc`, `auth-svc`, `billing-svc`, `homepage-svc`).
- `services/support/*` — `file-svc` (single module).

> `services/core/journal-svc/` exists on disk but is **not** registered in `settings.gradle.kts` — treat as WIP / not part of the build.
- `libs/common` — `TenantContextHolder`, `UserContextHolder`, `AuthorizationGuard`, `BaseEntity`, `BusinessException`, `DomainEvent`, CSV utils.
- `libs/internal-api` — **interface-only** module. Sealed records / interfaces that modules expose to each other.
- `libs/messaging` — RabbitMQ constants.

The `*-svc` naming is leftover from prior MSA. They are **not** deployables.

## Hexagonal layout (per module)

```
domain/        entity, vo, repository (port), exception
application/   service (use-cases), port/out, dto
adapter/in/    web/* (REST controllers)
adapter/out/   persistence/* (JPA), internal/* (calls into other modules), external/* (worker HTTP clients)
```
Imports flow strictly: `adapter → application → domain`. Never the reverse.

## Inter-module rule

- Modules talk **only via `libs/internal-api` interfaces** (e.g., `LectureInternalService`). Each module implements its own contract under `adapter/internal/`.
- Consumers inject the interface as a Spring bean and call it from `adapter/out/internal/`.
- **No HTTP, no gRPC inside the monolith.** Never import another module's `domain.entity` directly.

## External calls (lumie-worker)

Pattern: `adapter/out/external/<Name>Client.java` (e.g., `ReportServiceClient`, `OmrServiceClient`). `RestTemplate` + `@Value("${lumie.services.<svc>.url}")`. Always forward `X-Tenant-Slug`. Wrap failures in module's `BusinessException` subtype.

## Multi-tenancy

- **Schema-per-tenant** (Flyway). Migrations in `app/src/main/resources/db/migration/{public,tenant_*}`. Use `migration` agent for new migrations.
- `TenantContextHolder` (ThreadLocal) populated by `RequestContextFilter` from `X-Tenant-Slug`. Use `TenantContextHolder.withinContext(slug, action)` when crossing tenants in internal adapters.

## Conventions

- REST routes versioned: `@RequestMapping("/v1/...")`.
- Each module has its own `ErrorCode` enum extending `BusinessException`.
- MapStruct + Lombok — annotation-processor order matters in `build.gradle.kts`.
- Tests: JUnit 5, Mockito, MockMvc; integration via TestContainers (Postgres). Mirror source package in `src/test/java`.

## Anti-patterns

- ❌ Importing `*.domain.entity` from another module → use `libs/internal-api` interface.
- ❌ Calling another module via HTTP/gRPC → direct bean injection only.
- ❌ Outbound work without tenant context populated.
- ❌ Adding logic in `adapter/in/web/*` controllers (controllers thin → service).
- ❌ New deployable module / standalone Spring Boot app — this repo is a monolith.

## Commands

```
./gradlew build          # compile + test
./gradlew test           # all tests
./gradlew :services:core:student-svc:test
./gradlew bootRun        # run app/
```

## Agents

`backend-scaffolder` · `backend-reviewer` · `backend-test-runner` · `migration` (Flyway).
