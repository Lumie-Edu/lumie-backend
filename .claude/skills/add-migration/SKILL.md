---
name: add-migration
description: Create a new Flyway migration with idempotency guards. Uses `migration` agent.
user-invocable: true
argument-hint: <description-snake-case> [scope=public|tenant]
---

Create a Flyway migration safely.

## Steps

1. If $ARGUMENTS empty, ask user for description + scope (public/tenant).
2. Find next V-number: `ls app/src/main/resources/db/migration/{public,tenant}/ | sort | tail -1`.
3. Invoke `migration` agent:
   - Generate `V<N>__<description>.sql` in correct schema dir.
   - Use `IF EXISTS` / `IF NOT EXISTS` guards (idempotency).
   - For destructive ops (DROP/ALTER): document irreversibility with a comment (forward-only migrations are canonical — no `_rollback.sql` companion required).
   - Use `TIMESTAMPTZ` for timestamps (NOT `TIMESTAMP WITHOUT TIME ZONE`).
4. If cross-schema reference (public ↔ tenant), warn about V12/V32 ordering risk; recommend backfill via separate migration.
5. Cross-check naming: snake_case columns, foreign keys explicit with `ON DELETE`.
6. Recommend test: TestContainers integration run before deploy.

## When to use

- New table / column / index.
- Data backfill (idempotent SQL).
- Schema cleanup.

## When NOT to use

- Editing an applied migration (NEVER — create new V file).
- Cross-cutting refactor (use `/refactor`).

## Constraints

- NEVER edit an already-applied V file.
- ALWAYS use `IF EXISTS` / `IF NOT EXISTS`.
- NEVER mix public + tenant schema operations in one migration.
