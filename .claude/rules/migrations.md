---
paths: ["lumie-backend/**/db/migration/**/*.sql"]
---

# Flyway Migrations (path-scoped rules)

## NEVER

- ❌ Edit a migration that has been applied to any environment. Create a new V file instead.
- ❌ Use `TIMESTAMP WITHOUT TIME ZONE`. Use `TIMESTAMP WITH TIME ZONE` for every time field.
- ❌ Drop a column without checking dependencies in `lumie-backend` and FE.
- ❌ Create a tenant-scoped table without a `tenant_id BIGINT NOT NULL` column + RLS policy. Forgetting either is a cross-tenant leak.
- ❌ Enable RLS on a platform table (`tenants`, `plans`, `paddle_webhook_events`). Platform tables are intentionally cross-tenant.
- ❌ DDL without `IF EXISTS` / `IF NOT EXISTS` guards (idempotency).

## ALWAYS

- ✅ Use `migration` agent (`/add-migration` skill) to scaffold a new migration.
- ✅ Naming: `V<n>__<snake_case_description>.sql`.
- ✅ `created_at TIMESTAMPTZ DEFAULT now()` and `updated_at TIMESTAMPTZ DEFAULT now()`.
- ✅ Foreign keys explicit, `ON DELETE` policy declared.
- ✅ For backfills: document irreversibility with a comment in the migration — forward-only is canonical, no rollback companion required.
- ✅ Test against TestContainers before considering "ready".

## Expand / contract for destructive changes

Any rename, type-change, or DROP on a table with rows in ANY environment MUST use the three-phase sequence below. Single-shot destructive DDL is forbidden.

| Phase | What | Migration |
|---|---|---|
| **Expand** | Add new column `NOT NULL DEFAULT` or NULLable; app dual-writes old + new. | `V<n>__add_<col>.sql` |
| **Backfill** | Populate new column for existing rows — idempotent (`WHERE new_col IS NULL`). | `V<n+1>__backfill_<col>.sql` |
| **Contract** | Drop old column, only after the deploy that stops reading it is live everywhere. | `V<n+2>__drop_<col>.sql` |

- ❌ NEVER merge Expand + Contract into a single migration (partial deploy leaves unreadable data).
- ❌ NEVER run Contract before confirming no code path reads the old column (grep + deploy order).
- ✅ ALWAYS mark the Backfill migration idempotent (`WHERE old IS NULL` or `ON CONFLICT DO NOTHING`).
- ✅ ALWAYS document the expected deploy order in the migration's comment block.

## RLS policy template

Every `CREATE TABLE` for a tenant-scoped table MUST be paired with this three-statement block in the same migration:

```sql
ALTER TABLE foo ENABLE ROW LEVEL SECURITY;
ALTER TABLE foo FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON foo
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);
```

- ✅ ALWAYS include `FORCE ROW LEVEL SECURITY` — without it the table owner bypasses the policy.
- ✅ ALWAYS use the `NULLIF(..., '')::bigint` cast form. `NULLIF` guards against the empty-string value present when no tenant context is set; without it the cast fails and non-tenant queries error instead of returning empty results.
