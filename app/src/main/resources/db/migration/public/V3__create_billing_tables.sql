-- V1: Billing tables (plans, subscriptions, invoices, usage_logs)

-- Plans table
CREATE TABLE IF NOT EXISTS plans (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    monthly_price DECIMAL(15,0) NOT NULL,
    yearly_price DECIMAL(15,0) NOT NULL,
    max_students INTEGER NOT NULL,
    max_academies INTEGER NOT NULL,
    max_admins INTEGER NOT NULL,
    omr_monthly_quota INTEGER NOT NULL,
    custom_domains BOOLEAN NOT NULL DEFAULT false,
    advanced_analytics BOOLEAN NOT NULL DEFAULT false,
    priority_support BOOLEAN NOT NULL DEFAULT false,
    api_access BOOLEAN NOT NULL DEFAULT false,
    white_labeling BOOLEAN NOT NULL DEFAULT false,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Subscriptions table
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_slug VARCHAR(30) NOT NULL,
    plan_id VARCHAR(20) NOT NULL REFERENCES plans(id),
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    billing_cycle_day INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Invoices table
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id),
    tenant_slug VARCHAR(30) NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    billing_period_start TIMESTAMP,
    billing_period_end TIMESTAMP,
    due_date TIMESTAMP,
    paid_at TIMESTAMP,
    payment_key VARCHAR(200),
    order_id VARCHAR(100),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Usage logs table
CREATE TABLE IF NOT EXISTS usage_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_slug VARCHAR(30) NOT NULL,
    metric_type VARCHAR(30) NOT NULL,
    value BIGINT NOT NULL,
    recorded_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant_id ON subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant_slug ON subscriptions(tenant_slug);
CREATE INDEX IF NOT EXISTS idx_invoices_subscription_id ON invoices(subscription_id);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_slug ON invoices(tenant_slug);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_usage_logs_tenant_date ON usage_logs(tenant_id, recorded_date);
CREATE INDEX IF NOT EXISTS idx_usage_logs_tenant_metric ON usage_logs(tenant_id, metric_type);

-- Seed plans
INSERT INTO plans (id, name, description, monthly_price, yearly_price, max_students, max_academies, max_admins, omr_monthly_quota, custom_domains, advanced_analytics, priority_support, api_access, white_labeling, display_order)
VALUES
    ('FREE', 'Free', '무료 체험판', 0, 0, 30, 1, 2, 50, false, false, false, false, false, 1),
    ('BASIC', 'Basic', '소규모 학원용', 49000, 490000, 100, 3, 5, 200, false, false, false, false, false, 2),
    ('PRO', 'Pro', '중규모 학원용', 149000, 1490000, 500, 10, 20, 1000, true, true, false, true, false, 3),
    ('ENTERPRISE', 'Enterprise', '대형 학원/프랜차이즈', 499000, 4990000, 10000, 100, 100, 10000, true, true, true, true, true, 4)
ON CONFLICT (id) DO NOTHING;
