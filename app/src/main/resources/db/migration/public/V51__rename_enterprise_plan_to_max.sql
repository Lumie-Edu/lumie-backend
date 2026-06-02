-- =====================================================================
-- V51: Rename plan ENTERPRISE → MAX
--
-- Purpose: complete the TenantPlan enum unification. The Java enum
-- TenantPlan is already {FREE, BASIC, PRO, MAX}; V9 already renamed
-- tenants.plan_id ENTERPRISE→MAX. This migration finishes the rename on
-- the plans catalogue and the subscriptions FK side.
--
-- The billing-module migration track (modules/billing/src/main/resources/
-- db/migration/) is NOT loaded by Flyway (spring.flyway.locations points
-- only at classpath:db/migration/public), so billing V3 has never run.
-- This migration replaces billing V3 for the app-level track.
--
-- Idempotency: every step is guarded so this is safe to run regardless of
-- whether ENTERPRISE already exists, MAX already exists, or both states are
-- already clean (billing V3 ran out-of-band, or the row was never inserted).
--
-- Forward-only: no rollback is provided.
-- Scope: plans is platform-global — no RLS, no tenant_id concerns.
-- =====================================================================

-- Fail fast instead of hanging Flyway boot if subscriptions/plans rows are
-- locked by in-flight billing writes.
set local lock_timeout = '5s';
set local statement_timeout = '15s';

-- -----------------------------------------------------------------------
-- 1. Ensure MAX exists — copy from ENTERPRISE if ENTERPRISE is present
--    and MAX is absent; no-op otherwise.
--
--    Only columns that exist in the V28/V32/V49-managed schema are listed.
--    Paddle columns (paddle_product_id etc.) were added only by the
--    unloaded billing-module track and are NOT present on plans here.
-- -----------------------------------------------------------------------
insert into plans (
    id, name, description,
    monthly_price, yearly_price,
    max_students, max_academies, max_admins, omr_monthly_quota,
    advanced_analytics, priority_support, api_access, white_labeling,
    display_order, active, version,
    created_at, updated_at
)
select
    'MAX', name, description,
    monthly_price, yearly_price,
    max_students, max_academies, max_admins, omr_monthly_quota,
    advanced_analytics, priority_support, api_access, white_labeling,
    display_order, active, version,
    created_at, updated_at
from plans
where id = 'ENTERPRISE'
on conflict (id) do nothing;

-- -----------------------------------------------------------------------
-- 2. Re-point any subscriptions still on ENTERPRISE → MAX.
--    Both FK columns reference plans(id) ON DELETE RESTRICT, so BOTH must be
--    cleared before the ENTERPRISE row can be deleted in step 3:
--      - plan_id          (active plan)
--      - pending_plan_id  (scheduled downgrade target, added in V37)
--    Guards: each only runs if matching rows exist (no-op otherwise).
-- -----------------------------------------------------------------------
update subscriptions
set plan_id = 'MAX'
where plan_id = 'ENTERPRISE';

update subscriptions
set pending_plan_id = 'MAX'
where pending_plan_id = 'ENTERPRISE';

-- -----------------------------------------------------------------------
-- 3. Remove the ENTERPRISE row (no-op if already gone).
-- -----------------------------------------------------------------------
delete from plans where id = 'ENTERPRISE';

-- -----------------------------------------------------------------------
-- 4. Normalise the MAX row's display name in case it was copied as
--    'Enterprise' rather than 'Max' (covers both the V34 seed values and
--    any manually set value). No-op if name is already 'Max'.
-- -----------------------------------------------------------------------
update plans
set name = 'Max'
where id = 'MAX'
  and name = 'Enterprise';

-- -----------------------------------------------------------------------
-- 5. Fix the stale column comment in V28 (editing V28 directly would
--    cause a Flyway checksum-mismatch boot failure).
-- -----------------------------------------------------------------------
comment on column plans.id is 'Human-readable plan key: free | basic | pro | max';
