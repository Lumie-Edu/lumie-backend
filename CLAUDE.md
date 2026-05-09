# lumie-backend

Spring Boot **monolith** (Gradle multi-module, hexagonal). Java 21 + Spring Boot 3.4. `app/` is the single `@SpringBootApplication`; `services/*` are domain modules (logical, not deployables).

> Workspace rules apply (`/Users/bluemayne/Projects/Lumie/.claude/CLAUDE.md`).

## NEVER (Backend)

- ❌ `LocalDateTime` in DTOs. Use `Instant` (UTC).
- ❌ Raw `RuntimeException`. Throw `BusinessException(ErrorCode)`.
- ❌ `@Transactional` wrapping external HTTP calls.
- ❌ `@Scheduled` without `@SchedulerLock` (ShedLock).
- ❌ `Map<String, Object>` request/response in controllers. Use records.
- ❌ `@Setter` on JPA entities.
- ❌ Field injection (`@Autowired` field). Use `@RequiredArgsConstructor`.
- ❌ Cross-module `*.domain.entity` import. Use `libs/internal-api`.
- ❌ HTTP/gRPC between monolith modules. Direct bean injection only.
- ❌ New deployable module. This repo is monolith-first.

## ALWAYS (Backend)

- ✅ `@Version` on user-mutable entities.
- ✅ `@JoinFetch` / `@EntityGraph` on list queries with associations.
- ✅ `BaseEntity` extension for audit timestamps.
- ✅ Controllers wrap pagination as `PageResponse<T>` (not raw `Page<T>`).
- ✅ `@RequestMapping("/v1/...")` versioning.
- ✅ Forward `X-Tenant-Slug` to worker HTTP calls.

## Module layout

```
services/{tier}/{module}-svc/   ⚠️ legacy -svc; do NOT add to NEW modules
└── src/main/java/com/lumie/{module}/
    ├── domain/{entity,vo,repository,exception}/
    ├── application/{service,port/in,port/out,dto/{request,response}}/
    └── adapter/{in/web,in/messaging,out/persistence,out/external,internal}/
```

Tiers: `core` (domain), `platform` (cross-tenant), `support` (utilities). `journal-svc` exists but unregistered — dead code.

## Multi-tenancy

Schema-per-tenant via Hibernate `MultiTenantConnectionProvider`. Tenant slug from `X-Tenant-Slug` header → `RequestContextFilter` → `TenantContextHolder` ThreadLocal. Use `TenantContextHolder.withinContext(slug, action)` when crossing tenants in adapters.

## Inter-module / external

- Internal: only via `libs/internal-api` interfaces. Implementations in `adapter/internal/`.
- External (worker): `adapter/out/external/<Name>Client.java`. `RestTemplate` (migration to `RestClient` planned). Always forward `X-Tenant-Slug`.

## Commands

```
./gradlew build          # compile + test
./gradlew test
./gradlew :services:core:student-svc:test
./gradlew bootRun
```

## References

- Full backend rules: `.claude/knowledge/BACKEND_RULES.md`
- Workspace: `/Users/bluemayne/Projects/Lumie/.claude/knowledge/HARD_RULES_CROSS.md`
- Architecture intent: `/Users/bluemayne/Projects/Lumie/.claude/knowledge/ARCHITECTURE_INTENT.md`
- Path-scoped rules: `.claude/rules/{controllers,entities,services,migrations}.md`
- Audits: `/Users/bluemayne/Projects/Lumie/.claude/audits/README.md`

## Agents

`backend-scaffolder` · `backend-reviewer` · `backend-test-runner` · `migration` (Flyway).
