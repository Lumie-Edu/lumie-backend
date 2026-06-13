# lumie-backend

Spring Boot **monolith** (Gradle multi-module, hexagonal). Java 21 + Spring Boot 3.4. `app/` is the single `@SpringBootApplication`; `modules/*` are domain modules (logical, not deployables).

> Workspace rules apply (`../AGENTS.md`).

@../.codex/knowledge/backend/BACKEND_RULES.md

## Module layout

```
modules/{module}/
└── src/main/java/com/lumie/{module}/
    ├── domain/{entity,vo,exception}/
    ├── application/{service,port/out,dto/{request,response}}/
    └── adapter/{in/web,in/messaging,in/internal,out/persistence,out/external}/
```

`domain/entity` contains persistence-backed JPA aggregate entities with
domain behavior. JPA mapping annotations are intentional here. New or
refactored persistence access should use `application/port/out` plus
`adapter/out/persistence`; existing `domain/repository` Spring Data
interfaces are legacy tolerated and should migrate opportunistically.

## Multi-tenancy

Postgres RLS-based isolation: every tenant-scoped table has `tenant_id BIGINT NOT NULL`, enforced by `FORCE ROW LEVEL SECURITY` against the `lumie_app` runtime role. When crossing tenants in adapters, use `TenantContextHolder.withinContext(slug, tenantId, action)` — `setTenant` alone leaves rows invisible. See `../.codex/knowledge/ARCHITECTURE_INTENT.md` "Multi-tenancy" for the full mechanism (aspect ordering, RabbitListener path, known costs).

## Inter-module / external

- Internal: only via `libs/internal-api` interfaces. Implementations in `adapter/in/internal/` (inter-module synchronous incoming calls).
- External (worker): `adapter/out/external/<Name>Client.java`. Always forward `X-Tenant-Slug`.

## Commands

```
./gradlew build          # compile + test
./gradlew test
./gradlew :modules:student:test
./gradlew bootRun
```

## References

- Architecture intent: `../.codex/knowledge/ARCHITECTURE_INTENT.md`
- Path-scoped rules: `../.codex/rules/backend/{controllers,entities,services,migrations}.md`

## Agents

`backend-scaffolder` · `backend-reviewer` · `backend-migration-author` (Flyway scaffold) · `backend-migration-reviewer` (read-only Flyway diff review) · `backend-test-orchestrator` (per-module Gradle test + JaCoCo coverage gate) · `backend-performance-monitor` (static N+1 / tx-scope-HTTP / missing-index analysis).
