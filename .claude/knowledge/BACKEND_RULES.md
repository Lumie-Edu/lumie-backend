# Backend Hard Rules (Java / Spring Boot)

Behavioral invariants. Java-specific. Read with `.claude/knowledge/HARD_RULES_CROSS.md` (cross-cutting).

## NEVER

- ❌ Use `LocalDateTime` in DTOs. Use `Instant` (UTC).
- ❌ Throw raw `RuntimeException`. Extend module's `BusinessException(ErrorCode)`.
- ❌ `@Transactional` wrapping `restTemplate.exchange` / `httpClient.send` / external HTTP. Refactor to commit before external call.
- ❌ `@Scheduled` without `@SchedulerLock` (ShedLock). Multi-pod deployment will double-execute.
- ❌ Field injection (`@Autowired` field). Use `@RequiredArgsConstructor` (Lombok).
- ❌ `@Setter` on JPA entities. Use builder + domain methods.
- ❌ `Map<String, Object>` as request/response body in controllers. Use typed records.
- ❌ Spring annotations in `domain/` layer. Domain stays pure.
- ❌ Controllers returning `Page<T>` directly. Wrap in `PageResponse<T>`.
- ❌ Cross-module `*.domain.entity` import. Use `libs/internal-api`.
- ❌ Create an `adapter/internal/` sibling package. Inter-module synchronous incoming calls (libs/internal-api impls) go in `adapter/in/internal/`, not a top-level `adapter/internal/`.
- ❌ Create an `infrastructure/` package in any module. No such layer exists in this hexagonal layout — see module diagram below for canonical homes.
  - Framework `@Configuration`/`@Bean` → `adapter/out/config/`
  - Spring Security filters and web utilities → `adapter/in/security/` or `adapter/in/web/`
  - Domain error codes → `domain/exception/`
- ❌ Create `application/port/in/*UseCase.java` interfaces. Controllers depend on application service classes directly (see ALWAYS list). All 16 modules follow this pattern.

## ALWAYS

- ✅ Constructor injection via `@RequiredArgsConstructor`.
- ✅ `@Version` on user-mutable entities for optimistic locking.
- ✅ `@JoinFetch` or `@EntityGraph` on list queries with associations.
- ✅ `BaseEntity` extension for audit timestamps (createdAt/updatedAt).
- ✅ `@Transactional(readOnly = true)` on Query services.
- ✅ Records (not classes) for DTOs.
- ✅ MapStruct for entity ↔ DTO (or record `from(entity)` factory; do not hand-roll).
- ✅ JUnit 5 + Mockito + MockMvc for unit/slice; TestContainers Postgres for integration.
- ✅ Controllers inject application services directly. No port/in indirection.
- ✅ `Pageable`-bound list endpoints declare `@AllowedSortFields({...})` (from `libs/common`) listing the property names valid for `?sort=`. Unlisted properties → 400.

## Module structure (hexagonal)

```
modules/{module}/
└── src/main/java/com/lumie/{module}/
    ├── domain/{entity,vo,repository,exception}/
    ├── application/{service,port/out,dto/{request,response}}/
    └── adapter/{in/web,in/messaging,in/internal,out/persistence,out/external}/
```

## References

- Cross-cutting rules: `../../../.claude/knowledge/HARD_RULES_CROSS.md`
- Architecture intent: `../../../.claude/knowledge/ARCHITECTURE_INTENT.md`
- Path-scoped rules: `.claude/rules/{controllers,entities,services,migrations}.md`
