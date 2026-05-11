---
paths: ["lumie-backend/**/db/migration/**/*.sql"]
---

# Flyway Migrations (path-scoped rules)

Auto-loaded when editing files in `**/db/migration/**`. SQL-only rules — Java-side timestamp rules (`Instant` in DTOs) live in `.claude/knowledge/BACKEND_RULES.md`.

## NEVER

- ❌ Edit a migration that has been applied to any environment. Create a new V file instead.
- ❌ Use `LocalDateTime`-equivalent (`TIMESTAMP WITHOUT TIME ZONE`). Use `TIMESTAMP WITH TIME ZONE` for any time field.
- ❌ Drop a column without checking dependencies in `lumie-backend` and FE.
- ❌ Mix public-schema and tenant-schema operations in one migration.
- ❌ Cross-schema reference without verifying both schemas' state at migration time (V12/V32 incident).
- ❌ DDL without `IF EXISTS` / `IF NOT EXISTS` guards (idempotency).

## ALWAYS

- ✅ Use `migration` agent (`/add-migration` skill) to scaffold a new migration.
- ✅ Naming: `V<n>__<snake_case_description>.sql`.
- ✅ `created_at TIMESTAMPTZ DEFAULT now()` and `updated_at TIMESTAMPTZ DEFAULT now()`.
- ✅ Foreign keys explicit, `ON DELETE` policy declared.
- ✅ For backfills: write companion rollback `.sql` or document irreversibility.
- ✅ Test against TestContainers before considering "ready".

## Expand / contract for destructive changes

Any rename, type-change, or DROP on a table that has rows in ANY environment MUST use the
three-phase sequence below. Single-shot destructive DDL is forbidden.

| Phase | What | Migration |
|---|---|---|
| **Expand** | Add new column `NOT NULL DEFAULT` or NULLable; app dual-writes old + new. | `V<n>__add_<col>.sql` |
| **Backfill** | Populate new column for existing rows — idempotent (`WHERE new_col IS NULL`). | `V<n+1>__backfill_<col>.sql` |
| **Contract** | Drop old column, only after the deploy that stops reading it is live everywhere. | `V<n+2>__drop_<col>.sql` |

- ❌ NEVER merge Expand + Contract into a single migration (partial deploy leaves unreadable data).
- ❌ NEVER run Contract before confirming no code path reads the old column (grep + deploy order).
- ✅ ALWAYS mark the Backfill migration idempotent (`WHERE old IS NULL` or `ON CONFLICT DO NOTHING`).
- ✅ ALWAYS document the expected deploy order in the migration's comment block.

## References

- `.claude/knowledge/BACKEND_RULES.md`
