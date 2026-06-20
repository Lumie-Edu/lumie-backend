-- V5: Normalize stale MAX plan display names in the legacy billing migration track.
--
-- This module-level Flyway path is not loaded by the main app today, but keeping
-- it aligned avoids stale Enterprise labels in tests or future standalone runs.
--
-- Forward-only: no rollback is provided.
-- Scope: plans is platform-global, not tenant-scoped.

UPDATE plans
SET name = 'Max',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'MAX'
  AND name <> 'Max';
