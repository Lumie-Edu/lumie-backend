---
paths: ["lumie-backend/**/application/service/**/*.java"]
---

# Application Services (path-scoped rules)

Auto-loaded when editing files in `**/application/service/**`. See `.claude/knowledge/BACKEND_RULES.md` for blanket rules (`@Transactional`+external HTTP, raw `RuntimeException`, `@Autowired` field).

## NEVER (service-specific)

- ❌ `@Transactional` wrapping `RabbitTemplate.send` for synchronous flow. Publish AFTER commit (`afterCommit` callback) or via Outbox pattern.
- ❌ Direct adapter import (`adapter.out.persistence.SomeAdapter`). Use port interface.

## ALWAYS (service-specific)

- ✅ Tenant context awareness: assume `TenantContextHolder.getRequiredTenant()` exists; failing-fast on missing tenant is correct.
- ✅ Use `internal-api` interface for cross-module calls; never reach into other module's services directly.
- ✅ Idempotency check on retry-capable handlers (e.g., `/internal/*/callback` consumers).

## References

- `.claude/knowledge/BACKEND_RULES.md`
- CONCURRENCY_AUDIT §B (transactional + external HTTP issues)
