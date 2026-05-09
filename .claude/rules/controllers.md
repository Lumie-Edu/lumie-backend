---
paths: ["lumie-backend/**/adapter/in/web/**/*.java"]
---

# Controllers (path-scoped rules)

Auto-loaded when editing files in `**/adapter/in/web/**`.

## NEVER

- ❌ Business logic in controllers. Delegate to application service.
- ❌ Return JPA entities directly. Use DTO records.
- ❌ Return raw Spring `Page<T>`. Wrap in `PageResponse<T>`.
- ❌ `Map<String, Object>` request/response. Define typed records.
- ❌ Catch exceptions in controllers. Let `BusinessException` bubble to global handler.
- ❌ Mix versions in one controller. One controller = one `/v1/...` prefix.
- ❌ POST without `Idempotency-Key` header support if endpoint creates resources or kicks long-running jobs.

## ALWAYS

- ✅ `@RestController` + `@RequestMapping("/v1/...")`.
- ✅ Validation via `@Valid` on `@RequestBody` DTO.
- ✅ Forward `X-Tenant-Slug` header propagation (handled by `RequestContextFilter`; do not override).
- ✅ `ResponseEntity<...>` with explicit status (`200 OK` / `201 Created` / `204 No Content`).
- ✅ Long-running operations → `202 Accepted` + `Location: /jobs/{id}` header.

## References

- `.claude/knowledge/BACKEND_RULES.md`
- `/Users/bluemayne/Projects/Lumie/.claude/audits/README.md` (API_DESIGN_AUDIT)
