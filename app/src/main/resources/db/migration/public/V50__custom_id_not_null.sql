-- =====================================================================
-- V50: custom_id NOT NULL — permanence for white-label link stability
--
-- custom_id (added in V9 as VARCHAR(50), nullable, partially-unique indexed)
-- is becoming a permanent, immutable identifier for each tenant's white-label
-- presence.  White-label feature links are rooted at /c/<custom_id>/...; once
-- a tenant has shared those URLs publicly the value must never change or go
-- missing, so nullability is no longer acceptable.
--
-- Step 1: Backfill any remaining NULL rows from the tenant's slug, but only
-- where the slug already satisfies the app's format constraint:
--   ^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$  (3-30 chars, lowercase alphanum + hyphen)
-- In dev and prod there is effectively one tenant (joossameng) whose slug fits
-- this regex, so the UPDATE is near-zero-risk and idempotent.  Tenants whose
-- slug does NOT satisfy the regex are left NULL and must be resolved manually
-- before this migration runs; in practice none exist.
--
-- Step 2: Set NOT NULL on the column so the constraint is enforced at the DB
-- level going forward.
--
-- Forward-only.  The existing partial unique index on custom_id (created in V9)
-- is unaffected — it already handles uniqueness; this migration only tightens
-- nullability.
-- =====================================================================

-- Guard against the ALTER TABLE or backfill stalling open connections.
set local lock_timeout = '2s';
set local statement_timeout = '5s';

-- 1. Backfill NULLs from slug where the slug satisfies the custom_id format.
update tenants
set custom_id = slug
where custom_id is null
  and slug ~ '^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$';

-- 2. Pre-flight: abort with an actionable message if any row is still NULL after
--    the backfill (slug failed the regex and needs manual resolution).
do $$
begin
  if exists (select 1 from tenants where custom_id is null) then
    raise exception 'V50 abort: % tenant(s) still have NULL custom_id after backfill (slug failed the custom_id regex) — resolve manually before re-running',
      (select count(*) from tenants where custom_id is null);
  end if;
end $$;

-- 3. Lock in permanence — custom_id must always be present going forward.
--    NOTE: ALTER COLUMN ... SET NOT NULL is a Postgres no-op when every row
--    already carries a non-NULL value and no constraint needs to be validated
--    from scratch, so replay of this step is safe.
alter table tenants alter column custom_id set not null;
