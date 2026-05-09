---
paths: ["lumie-backend/**/domain/entity/**/*.java"]
---

# JPA Entities (path-scoped rules)

Auto-loaded when editing files in `**/domain/entity/**`.

## NEVER

- ❌ `@Setter` on entities. Use builder + domain methods.
- ❌ Spring annotations (`@Service`, `@Component`, etc.). Domain stays pure.
- ❌ `LocalDateTime` for timestamps. Use `Instant`.
- ❌ Lombok `@Data` (generates `equals`/`hashCode` that break with JPA proxies).
- ❌ Public mutable fields.
- ❌ Cross-module entity import (`com.lumie.OTHER_MODULE.domain.entity.*`). Use `libs/internal-api`.

## ALWAYS

- ✅ Extend `BaseEntity` for audit timestamps (`createdAt`, `updatedAt`).
- ✅ `@Version Long version` on user-mutable entities (optimistic locking).
- ✅ `@Getter` only (Lombok). Mutators are explicit domain methods.
- ✅ `@Builder` for entities with >4 constructor args.
- ✅ `equals`/`hashCode` by ID for managed entities (not by content).
- ✅ Bidirectional associations declared `LAZY` explicitly (Hibernate default for `@ManyToOne` is `EAGER` — footgun).

## References

- `.claude/knowledge/BACKEND_RULES.md`
- CODE_QUALITY_AUDIT §D (database patterns)
