-- Homepage delivery and custom-domain routing are now owned by GitOps edge
-- configuration. Run only after the replacement frontend and edge routes are
-- verified and a database backup is available: this forward-only migration
-- intentionally discards homepage configuration. Homepage-referenced object
-- storage is not inferred or deleted here; its audited cleanup is separate.
-- This migration intentionally preserves all older Flyway migrations and
-- removes only schema no longer read by application code.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '2min';

DROP TABLE IF EXISTS homepage_config;

DROP INDEX IF EXISTS uq_tenants_custom_id;
DROP INDEX IF EXISTS idx_tenants_custom_domain;

ALTER TABLE IF EXISTS tenants
    DROP COLUMN IF EXISTS custom_id,
    DROP COLUMN IF EXISTS custom_domain;
