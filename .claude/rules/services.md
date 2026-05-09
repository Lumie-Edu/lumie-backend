---
paths: ["lumie-backend/**/application/service/**/*.java"]
---

# Application Services (path-scoped rules)

Auto-loaded when editing files in `**/application/service/**`.

## NEVER

- ❌ `@Transactional` wrapping `restTemplate.exchange` / `httpClient.send` / external HTTP. Connection pool will exhaust.
- ❌ `@Transactional` wrapping `RabbitTemplate.send` for synchronous flow. Publish AFTER commit (`afterCommit` callback) or via Outbox pattern.
- ❌ Direct adapter import (`adapter.out.persistence.SomeAdapter`). Use port interface.
- ❌ Throw raw `RuntimeException`. Use module's `BusinessException(ErrorCode)`.
- ❌ Field injection (`@Autowired` on field). Use `@RequiredArgsConstructor`.

## ALWAYS

- ✅ `@RequiredArgsConstructor` for constructor injection.
- ✅ `@Transactional(readOnly = true)` on Query services / methods that don't write.
- ✅ Tenant context awareness: assume `TenantContextHolder.getRequiredTenant()` exists; failing-fast on missing tenant is correct.
- ✅ Use `internal-api` interface for cross-module calls; never reach into other module's services directly.
- ✅ Idempotency check on retry-capable handlers (e.g., `/internal/*/callback` consumers).

## References

- `.claude/knowledge/BACKEND_RULES.md`
- CONCURRENCY_AUDIT §B (transactional + external HTTP issues)
