-- V4: Optimistic locking + unique constraint on usage_logs
--
-- @Version column: safety net for direct (non-upsert) update paths.
-- Unique constraint on (tenant_id, metric_type, recorded_date): required by
-- the ON CONFLICT clause used in UsageTrackingService.incrementUsage().
--
-- Note: this migration is pending Phase 3 merge into the active Flyway path.
-- For production deployment on a live table, replace ADD CONSTRAINT with:
--   CREATE UNIQUE INDEX CONCURRENTLY uq_usage_logs_tenant_metric_date
--       ON usage_logs (tenant_id, metric_type, recorded_date);
--   ALTER TABLE usage_logs
--       ADD CONSTRAINT uq_usage_logs_tenant_metric_date
--       UNIQUE USING INDEX uq_usage_logs_tenant_metric_date;
-- (two separate migrations — CONCURRENTLY cannot run inside a transaction)

ALTER TABLE usage_logs
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE usage_logs
    DROP CONSTRAINT IF EXISTS uq_usage_logs_tenant_metric_date;

ALTER TABLE usage_logs
    ADD CONSTRAINT uq_usage_logs_tenant_metric_date
        UNIQUE (tenant_id, metric_type, recorded_date);
