# lumie-backend

Spring Boot **monolith** (Gradle multi-module, hexagonal). Java 21 + Spring Boot 3.4. `app/` is the single `@SpringBootApplication`; `modules/*` are domain modules (logical, not deployables).

> Workspace rules apply (`.claude/CLAUDE.md`).

@.claude/knowledge/BACKEND_RULES.md

## Module layout

```
modules/{module}/
└── src/main/java/com/lumie/{module}/
    ├── domain/{entity,vo,repository,exception}/
    ├── application/{service,port/in,port/out,dto/{request,response}}/
    └── adapter/{in/web,in/messaging,in/internal,out/persistence,out/external}/
```

## Multi-tenancy

Postgres RLS-based isolation: every tenant-scoped table has a `tenant_id BIGINT NOT NULL` column + RLS policy `USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)`. The tenant id is read from JWT (HTTP path) or message payload (RabbitListener path), set into `TenantContextHolder.setTenantId(Long)`, and bound to the active transaction by `RlsTenantContextAspect` (libs/common). When crossing tenants in adapters, use `TenantContextHolder.withinContext(slug, tenantId, action)` — `setTenant` alone leaves rows invisible. See `.claude/knowledge/ARCHITECTURE_INTENT.md` for the full why.

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

- Architecture intent: `.claude/knowledge/ARCHITECTURE_INTENT.md`
- Path-scoped rules: `.claude/rules/{controllers,entities,services,migrations}.md`

## Agents

`backend-scaffolder` · `backend-reviewer` · `migration` (Flyway scaffold) · `migration-reviewer` (read-only Flyway diff review) · `backend-test-orchestrator` (per-module Gradle test + JaCoCo coverage gate) · `backend-performance-monitor` (static N+1 / tx-scope-HTTP / missing-index analysis).
