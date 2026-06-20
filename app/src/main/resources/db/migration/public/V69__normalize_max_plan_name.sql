-- V69: Normalize stale MAX plan display names.
--
-- V51 migrated the plan key from ENTERPRISE to MAX. Environments that copied
-- the old row or received manual edits can still expose the display name as
-- Enterprise/enterprise in subscription responses. Keep the canonical product
-- name Max without editing applied migrations.
--
-- Forward-only: no rollback is provided.
-- Scope: plans is platform-global, not tenant-scoped.

UPDATE plans
SET name = 'Max',
    updated_at = now()
WHERE id = 'MAX'
  AND name <> 'Max';
