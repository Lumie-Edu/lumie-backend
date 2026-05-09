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

## ALWAYS

- ✅ Constructor injection via `@RequiredArgsConstructor`.
- ✅ `@Version` on user-mutable entities for optimistic locking.
- ✅ `@JoinFetch` or `@EntityGraph` on list queries with associations.
- ✅ `BaseEntity` extension for audit timestamps (createdAt/updatedAt).
- ✅ `@Transactional(readOnly = true)` on Query services.
- ✅ Records (not classes) for DTOs.
- ✅ MapStruct for entity ↔ DTO (or record `from(entity)` factory; do not hand-roll).
- ✅ JUnit 5 + Mockito + MockMvc for unit/slice; TestContainers Postgres for integration.

## Module structure (hexagonal)

```
services/{tier}/{module}-svc/   # ⚠️ DO NOT add -svc to NEW modules; existing are legacy
└── src/main/java/com/lumie/{module}/
    ├── domain/{entity,vo,repository,exception}/
    ├── application/{service,port/in,port/out,dto/{request,response}}/
    └── adapter/{in/web,in/messaging,out/persistence,out/external,internal}/
```

Tier `core` for domain modules; `platform` for cross-tenant; `support` for utilities.

## References

- Cross-cutting rules: `../../../.claude/knowledge/HARD_RULES_CROSS.md`
- Architecture intent: `../../../.claude/knowledge/ARCHITECTURE_INTENT.md`
- Audits: `../../../.claude/audits/README.md`
- Path-scoped rules: `.claude/rules/{controllers,entities,services,migrations}.md`
