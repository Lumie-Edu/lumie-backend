---
paths: ["lumie-backend/**/domain/entity/**/*.java"]
---

# JPA Entities (path-scoped rules)

Auto-loaded when editing files in `**/domain/entity/**`. See `.claude/knowledge/BACKEND_RULES.md` for blanket rules (`@Setter`, Spring-in-domain, Instant, cross-module entity import, `@Version`, `BaseEntity`).

## NEVER (entity-specific)

- ❌ Lombok `@Data` (generates `equals`/`hashCode` that break with JPA proxies).
- ❌ Public mutable fields.

## ALWAYS (entity-specific)

- ✅ `@Getter` only (Lombok). Mutators are explicit domain methods.
- ✅ `@Builder` for entities with >4 constructor args.
- ✅ `equals`/`hashCode` by ID for managed entities (not by content).
- ✅ Bidirectional associations declared `LAZY` explicitly (Hibernate default for `@ManyToOne` is `EAGER` — footgun).

## References

- `.claude/knowledge/BACKEND_RULES.md`
