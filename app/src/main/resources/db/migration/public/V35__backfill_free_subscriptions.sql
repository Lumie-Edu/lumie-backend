-- Backfill FREE subscriptions for tenants that were created before the
-- TenantCreatedEvent → SubscriptionCommandService.createFreeSubscription
-- listener existed (or before the billing module shipped at all).
--
-- Restores the "every tenant has a subscription" invariant the BE assumes,
-- so GET /v1/subscriptions/{tenantSlug} stops returning 404 on these
-- legacy tenants and the FE billing page renders cleanly.
--
-- Re-runnable: filters out tenants that already have a subscription row.
-- Depends on V34 (plans seed) so the FREE plan exists.

INSERT INTO subscriptions (
    tenant_id, tenant_slug, plan_id, status,
    suspension_reason, started_at, billing_cycle_day,
    payment_failure_count, version
)
SELECT
    t.id,
    t.slug,
    'FREE',
    'ACTIVE',
    'NONE',
    now(),
    EXTRACT(DAY FROM now())::integer,
    0,
    0
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM subscriptions s WHERE s.tenant_id = t.id
);
