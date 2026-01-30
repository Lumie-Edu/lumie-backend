-- Usage logs table
CREATE TABLE IF NOT EXISTS usage_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_slug VARCHAR(30) NOT NULL,
    metric_type VARCHAR(30) NOT NULL,
    value BIGINT NOT NULL DEFAULT 0,
    recorded_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for usage logs
CREATE INDEX IF NOT EXISTS idx_usage_logs_tenant_date ON usage_logs(tenant_id, recorded_date);
CREATE INDEX IF NOT EXISTS idx_usage_logs_tenant_metric ON usage_logs(tenant_id, metric_type);
CREATE UNIQUE INDEX IF NOT EXISTS idx_usage_logs_tenant_metric_date ON usage_logs(tenant_id, metric_type, recorded_date);
