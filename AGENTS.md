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
- ✅ Accept an `Idempotency-Key` on POST endpoints that create billing records, long-running jobs, or bulk imports.
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
Use a flat module directory name; do not add a new `*-svc` suffix.

## Persistence boundary

- `domain/entity/**` may use `jakarta.persistence` and Hibernate mapping annotations.
- New or refactored persistence access uses `application/port/out/*PersistencePort` plus `adapter/out/persistence/*JpaRepository` and `*PersistenceAdapter`.
- Existing `domain/repository/** extends JpaRepository` interfaces are legacy tolerated. Migrate opportunistically when touching the module.
- Keep domain behavior on the aggregate entity through explicit methods.
- Do not do a big-bang repository migration solely for layout purity.
- Revisit the boundary only when persistence mapping blocks domain invariants, a module is being extracted, or compliance demands separation.

## Scoped implementation rules

- Controllers delegate business logic to application services, return DTO records rather than JPA entities, and let `BusinessException` reach the global handler. Use one `/v1/...` prefix per controller, `@Valid` on request DTOs, and an explicit response status. A long-running job returns `202 Accepted` with a `Location` header; `RequestContextFilter` owns incoming tenant propagation.
- Application services use output ports instead of importing persistence adapters. Publish RabbitMQ messages after commit or through the Outbox; retry-capable callbacks require an idempotency check. Missing tenant context must fail fast.
- JPA aggregates use explicit domain mutators rather than Lombok `@Data` or public mutable fields. Base identity-based equality on the entity ID. Declare bidirectional associations and `@ManyToOne` mappings with explicit `LAZY` fetching; do not rely on the default `EAGER` behavior.

## Flyway migrations

- Create a new `V<N>__<snake_case_description>.sql` in the loaded `app/src/main/resources/db/migration/public/` tree. Never edit an applied migration or substitute direct DDL for one. Check the loaded chain for version collisions.
- Use guarded `IF EXISTS` or `IF NOT EXISTS` DDL and `TIMESTAMPTZ` (`TIMESTAMP WITH TIME ZONE`) for every timestamp column, not just audit fields. Use `created_at TIMESTAMPTZ DEFAULT now()` and `updated_at TIMESTAMPTZ DEFAULT now()` for audit fields, and declare foreign-key delete behavior explicitly. Explain irreversible operations in SQL comments. Check backend and frontend dependencies before dropping a column.
- Every new tenant-scoped table needs `tenant_id BIGINT NOT NULL`, `ENABLE ROW LEVEL SECURITY`, `FORCE ROW LEVEL SECURITY`, and a tenant policy in the same migration. Its predicate uses `tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint` for both `USING` and `WITH CHECK`. Do not add RLS to platform tables such as `tenants`, `plans`, or `paddle_webhook_events`.
- For a destructive change on populated or hot tables, split Expand, idempotent Backfill, and Contract across migrations. Apply Contract only after all deployed readers have stopped using the old shape; document deploy order and lock or timeout assumptions.
- Pair each tenant-scoped table with this RLS shape in the same migration, replacing `foo` with the real table:

```sql
ALTER TABLE foo ENABLE ROW LEVEL SECURITY;
ALTER TABLE foo FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON foo
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);
```

- For a concurrent index, follow the existing non-transactional Flyway convention and keep its deployment and lock behavior explicit.
- When Verify is requested for a migration, run the affected TestContainers Flyway/RLS integration check.

## Multi-tenancy

Postgres RLS-based isolation: every tenant-scoped table has `tenant_id BIGINT NOT NULL`, enforced by `FORCE ROW LEVEL SECURITY` against the `lumie_app` runtime role. When crossing tenants in adapters, use `TenantContextHolder.withinContext(slug, tenantId, action)` — `setTenant` alone leaves rows invisible. See `../.codex/.charter/fragments/lumie/architecture.md` and `../.codex/.charter/fragments/lumie/cross-repo-invariants.md` for the product-level rules; this file owns backend implementation details.

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

- Workspace hard rules: `../AGENTS.md`
- Architecture principles: `../.codex/.charter/fragments/lumie/architecture.md`
- Cross-repository invariants: `../.codex/.charter/fragments/lumie/cross-repo-invariants.md`
- Domain glossary: `../.codex/.charter/fragments/lumie/domain-language.md`

## Agents

The main agent implements backend changes and runs affected Gradle checks only during explicit Verify. `code-reviewer` covers code and semantic performance risks; `backend-migration-reviewer` separately reviews Flyway SQL when Review is requested.
