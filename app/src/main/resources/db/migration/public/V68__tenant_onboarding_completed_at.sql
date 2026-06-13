-- Add server-owned Tenant onboarding completion state.
-- Backfill is forward-only: existing tenants predate the enforced onboarding gate
-- and are treated as already completed to avoid locking current users out.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE IF EXISTS tenants
    ADD COLUMN IF NOT EXISTS onboarding_completed_at TIMESTAMPTZ;

UPDATE tenants
SET onboarding_completed_at = COALESCE(updated_at, created_at, now())
WHERE onboarding_completed_at IS NULL;
