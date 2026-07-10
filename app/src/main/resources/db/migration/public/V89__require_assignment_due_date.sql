-- =====================================================================
-- V89: require assignments.due_date
--
-- Purpose:
--   Backfill legacy null assignment due dates, then enforce not null so the
--   database matches the current assignment model contract.
--
-- Irreversible backfill:
--   Rows with null due_date are assigned coalesce(updated_at, created_at, now()).
--   Once written, the original null state cannot be reconstructed.
--
-- Forward-only:
--   No rollback is provided. This migration intentionally tightens column
--   nullability after normalizing existing rows.
-- =====================================================================

set local lock_timeout = '2s';
set local statement_timeout = '10s';

update assignments
set due_date = coalesce(updated_at, created_at, now())
where due_date is null;

alter table if exists assignments
    alter column due_date set not null;
