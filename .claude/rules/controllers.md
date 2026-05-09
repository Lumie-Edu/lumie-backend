---
paths: ["lumie-backend/**/adapter/in/web/**/*.java"]
---

# Controllers (path-scoped rules)

Auto-loaded when editing files in `**/adapter/in/web/**`. See `.claude/knowledge/BACKEND_RULES.md` for blanket rules (Page wrapping, Map<String,Object>, etc.).

## NEVER (controller-specific)

- ❌ Business logic in controllers. Delegate to application service.
- ❌ Return JPA entities directly. Use DTO records.
- ❌ Catch exceptions in controllers. Let `BusinessException` bubble to global handler.
- ❌ Mix versions in one controller. One controller = one `/v1/...` prefix.

## ALWAYS (controller-specific)

- ✅ `@RestController` + `@RequestMapping("/v1/...")`.
- ✅ Validation via `@Valid` on `@RequestBody` DTO.
- ✅ `ResponseEntity<...>` with explicit status (`200 OK` / `201 Created` / `204 No Content`).
- ✅ Long-running operations → `202 Accepted` + `Location: /jobs/{id}` header.
- ✅ `X-Tenant-Slug` propagation handled by `RequestContextFilter`; do not override.

## References

- `.claude/knowledge/BACKEND_RULES.md`
- `.claude/audits/README.md` (API_DESIGN_AUDIT)
