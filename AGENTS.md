# lumie-backend

Spring Boot **monolith** (Gradle multi-module, hexagonal). Java 21 + Spring Boot 3.5.16. `app/` is the single `@SpringBootApplication`; `modules/*` are domain modules (logical, not deployables).

> Workspace rules apply (`../AGENTS.md`).

## NEVER

- ❌ Use `LocalDateTime` in DTOs. Use `Instant` (UTC).
- ❌ Throw raw `RuntimeException`. Extend the module's `BusinessException(ErrorCode)` unless the error is startup or infrastructure fail-fast outside request handling.
- ❌ Wrap external HTTP calls (`restTemplate.exchange`, `httpClient.send`, WebClient calls, or worker clients) in an open `@Transactional` boundary. Commit before external calls.
- ❌ Use `@Scheduled` without `@SchedulerLock`; multi-pod deployment can double-execute.
- ❌ Use field injection (`@Autowired` field). Use constructor injection via `@RequiredArgsConstructor`.
- ❌ Put `@Setter` on JPA entities. Use builders plus domain methods.
- ❌ Use `Map<String, Object>` request or response bodies in controllers. Use typed records.
- ❌ Put Spring stereotypes, DI, web, transaction, messaging, or config annotations in `domain/`. JPA mapping annotations on aggregate entities are allowed.
- ❌ Return `Page<T>` directly from controllers. Wrap list responses in `PageResponse<T>`.
- ❌ Import another module's `*.domain.entity`. Use `libs/internal-api`.
- ❌ Create an `adapter/internal/` sibling package or any `infrastructure/` package. Use the canonical `adapter/in/*` and `adapter/out/*` layout.
- ❌ Create `application/port/in/*UseCase.java` interfaces. Controllers depend on application service classes directly.

## ALWAYS

- ✅ Use constructor injection via `@RequiredArgsConstructor`.
- ✅ Use records for DTOs.
- ✅ Use MapStruct or a record `from(entity)` factory instead of hand-rolled mapping.
- ✅ Extend `BaseEntity` for audit timestamps where applicable.
- ✅ Add `@Version` on user-mutable entities for optimistic locking.
- ✅ Use `@JoinFetch` or `@EntityGraph` on list queries that traverse associations.
- ✅ Mark query services `@Transactional(readOnly = true)`.
- ✅ Use `@Async` or an async boundary for long-running work that should not block request threads.
- ✅ Add `@AllowedSortFields({...})` to pageable list endpoints.
- ✅ Use JUnit 5 + Mockito + MockMvc for unit or slice tests and TestContainers Postgres for integration tests.

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

When adding a new module, register it in `settings.gradle.kts` and match the build configuration of the closest existing module.

## Persistence boundary

- `domain/entity/**` may use `jakarta.persistence` and Hibernate mapping annotations.
- New or refactored persistence access uses `application/port/out/*PersistencePort` plus `adapter/out/persistence/*JpaRepository` and `*PersistenceAdapter`.
- Existing `domain/repository/** extends JpaRepository` interfaces are legacy tolerated. Migrate opportunistically when touching the module.
- Keep domain behavior on the aggregate entity through explicit methods.
- Do not do a big-bang repository migration solely for layout purity.
- Revisit the boundary only when persistence mapping blocks domain invariants, a module is being extracted, or compliance demands separation.

## Multi-tenancy

Postgres RLS-based isolation: every tenant-scoped table has `tenant_id BIGINT NOT NULL`, enforced by `FORCE ROW LEVEL SECURITY` against the `lumie_app` runtime role. When crossing tenants in adapters, use `TenantContextHolder.withinContext(slug, tenantId, action)` — `setTenant` alone leaves rows invisible. See `../.codex/knowledge/ARCHITECTURE_INTENT.md` "Multi-tenancy" for the full mechanism (aspect ordering, RabbitListener path, known costs).

## Review focus

- Trace tenant context through changed HTTP, async, messaging, callback, and cross-tenant adapter boundaries; inspect the actual transaction and RLS path.
- Compare changed controllers, DTOs, events, and consumers for contract drift. Check that controllers delegate to application services and use typed responses.
- Check that RabbitMQ publication cannot happen before commit and that external HTTP does not hold an open transaction.
- Inspect association-heavy list queries and changed foreign keys for concrete N+1 or indexing risks; read the query and migration before claiming an index is missing.
- Check query-service read-only transactions, scheduler locking, and async boundaries for long request work on affected paths.
- Route changed Flyway SQL to `backend-migration-reviewer` in addition to general code review.

## Inter-module / external

- Internal: only via `libs/internal-api` interfaces. Implementations in `adapter/in/internal/` (inter-module synchronous incoming calls).
- External (worker): `adapter/out/external/<Name>Client.java`. Always forward `X-Tenant-Slug`.

## Commands

```
./gradlew build          # compile + test
./gradlew check          # Lumie lint + license/spotless checks + test
./gradlew test
./gradlew :modules:student:test
./gradlew bootRun
```

## References

- Cross-cutting rules: `../.codex/knowledge/HARD_RULES_CROSS.md`
- Architecture intent: `../.codex/knowledge/ARCHITECTURE_INTENT.md`
- Domain glossary: `../.codex/knowledge/DOMAIN_GLOSSARY.md`
- Path-scoped rules: `../.codex/rules/backend/{controllers,entities,services,migrations}.md`

## Agents

The main agent implements backend changes and runs affected Gradle checks only during explicit Verify or an active Loop's Verify phase. `code-reviewer` covers code and semantic performance risks; `backend-migration-reviewer` separately reviews Flyway SQL when Review is requested or reached.
