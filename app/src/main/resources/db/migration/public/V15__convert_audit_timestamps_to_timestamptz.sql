-- =====================================================================
-- Convert public-schema audit timestamp columns from
-- `timestamp without time zone` to `timestamptz`.
--
-- See companion tenant migration V34 for the full rationale. Same lossless
-- round-trip via `AT TIME ZONE 'UTC'` because the JVM is locked to UTC.
--
-- `ALTER TABLE IF EXISTS` accommodates environments where billing tables
-- (subscriptions, invoices, plans, etc.) may not yet exist — billing is a
-- recent module and some envs still seed it via JPA ddl-update.
-- =====================================================================

-- Core platform tables ------------------------------------------------

-- NOTE: custom_domain_activated_at / custom_domain_expires_at were added
-- in V10 and dropped in V11 (workflow simplified to manual mode), so V15
-- only converts the audit pair that survived. Older envs that ran V10
-- but somehow skipped V11 are not a real case in this codebase's history.
ALTER TABLE IF EXISTS tenants
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS tenant_settings
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS users
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS federated_identities
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS custom_domain_requests
    ALTER COLUMN created_at        TYPE TIMESTAMPTZ USING created_at        AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at        TYPE TIMESTAMPTZ USING updated_at        AT TIME ZONE 'UTC',
    ALTER COLUMN requested_at      TYPE TIMESTAMPTZ USING requested_at      AT TIME ZONE 'UTC',
    ALTER COLUMN reviewed_at       TYPE TIMESTAMPTZ USING reviewed_at       AT TIME ZONE 'UTC',
    ALTER COLUMN token_expires_at  TYPE TIMESTAMPTZ USING token_expires_at  AT TIME ZONE 'UTC';

-- Billing tables ------------------------------------------------------

ALTER TABLE IF EXISTS plans
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS subscriptions
    ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC',
    ALTER COLUMN started_at   TYPE TIMESTAMPTZ USING started_at   AT TIME ZONE 'UTC',
    ALTER COLUMN expires_at   TYPE TIMESTAMPTZ USING expires_at   AT TIME ZONE 'UTC',
    ALTER COLUMN cancelled_at TYPE TIMESTAMPTZ USING cancelled_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS invoices
    ALTER COLUMN created_at           TYPE TIMESTAMPTZ USING created_at           AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at           TYPE TIMESTAMPTZ USING updated_at           AT TIME ZONE 'UTC',
    ALTER COLUMN billing_period_start TYPE TIMESTAMPTZ USING billing_period_start AT TIME ZONE 'UTC',
    ALTER COLUMN billing_period_end   TYPE TIMESTAMPTZ USING billing_period_end   AT TIME ZONE 'UTC',
    ALTER COLUMN due_date             TYPE TIMESTAMPTZ USING due_date             AT TIME ZONE 'UTC',
    ALTER COLUMN paid_at              TYPE TIMESTAMPTZ USING paid_at              AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS usage_logs
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS paddle_webhook_events
    ALTER COLUMN processed_at TYPE TIMESTAMPTZ USING processed_at AT TIME ZONE 'UTC';
