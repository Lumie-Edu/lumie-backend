-- Homepage delivery and custom-domain routing are now owned by GitOps edge
-- configuration. Run only after the replacement frontend and edge routes are
-- verified and a database backup is available: this forward-only migration
-- intentionally discards homepage configuration. Homepage-referenced object
-- storage is not inferred or deleted here; its audited cleanup is separate.
-- This migration intentionally preserves all older Flyway migrations and
-- removes only schema no longer read by application code.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '2min';
--
-- The legacy joossameng slug is converted to the immutable inst-* namespace.
-- Object storage remains stable because its key derivation strips inst-.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM tenants WHERE slug = 'joossameng')
       AND EXISTS (SELECT 1 FROM tenants WHERE slug = 'inst-joossameng') THEN
        RAISE EXCEPTION 'cannot migrate joossameng slug: inst-joossameng already exists';
    END IF;

    UPDATE tenants
    SET slug = 'inst-joossameng'
    WHERE slug = 'joossameng';
END $$;

-- Keep denormalized job and subscription data consistent with the tenant
-- registry. Existing JWT and Redis session keys deliberately remain invalid,
-- requiring a fresh login after deployment.
UPDATE subscriptions SET tenant_slug = 'inst-joossameng' WHERE tenant_slug = 'joossameng';
UPDATE ai_scheduled_tasks SET tenant_slug = 'inst-joossameng' WHERE tenant_slug = 'joossameng';
UPDATE omr_grading_jobs SET tenant_slug = 'inst-joossameng' WHERE tenant_slug = 'joossameng';
UPDATE report_generation_jobs SET tenant_slug = 'inst-joossameng' WHERE tenant_slug = 'joossameng';
DELETE FROM idempotency_keys WHERE tenant_slug = 'joossameng';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM subscriptions WHERE tenant_slug = 'joossameng')
       OR EXISTS (SELECT 1 FROM ai_scheduled_tasks WHERE tenant_slug = 'joossameng')
       OR EXISTS (SELECT 1 FROM omr_grading_jobs WHERE tenant_slug = 'joossameng')
       OR EXISTS (SELECT 1 FROM report_generation_jobs WHERE tenant_slug = 'joossameng') THEN
        RAISE EXCEPTION 'legacy joossameng tenant slug remains after migration';
    END IF;
END $$;

DROP TABLE IF EXISTS homepage_config;

DROP INDEX IF EXISTS uq_tenants_custom_id;
DROP INDEX IF EXISTS idx_tenants_custom_domain;

ALTER TABLE IF EXISTS tenants
    DROP COLUMN IF EXISTS custom_id,
    DROP COLUMN IF EXISTS custom_domain;
