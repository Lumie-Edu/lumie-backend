-- @flyway:nonTransactional
-- CREATE INDEX CONCURRENTLY cannot run inside a transaction (Postgres restriction).

-- Supports cursor (keyset) pagination of GET /v1/activity-logs, which
-- orders by (created_at DESC, id DESC) and is RLS-filtered per tenant.
-- The (tenant_id, created_at DESC, id DESC) shape lets Postgres do an
-- index-only seek that combines the RLS tenant predicate with the keyset
-- range, instead of the prior offset-pagination scan.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_activity_logs_keyset
    ON activity_logs (tenant_id, created_at DESC, id DESC);
