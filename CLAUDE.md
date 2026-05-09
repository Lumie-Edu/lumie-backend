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
    └── adapter/{in/web,in/messaging,out/persistence,out/external,internal}/
```

## Multi-tenancy

Schema-per-tenant via Hibernate `MultiTenantConnectionProvider`. Tenant slug from `X-Tenant-Slug` header → `RequestContextFilter` → `TenantContextHolder` ThreadLocal. Use `TenantContextHolder.withinContext(slug, action)` when crossing tenants in adapters.

## Inter-module / external

- Internal: only via `libs/internal-api` interfaces. Implementations in `adapter/internal/`.
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
