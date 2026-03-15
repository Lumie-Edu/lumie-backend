-- V1: Platform tables (migrated from init-job, UUID -> BIGSERIAL)

-- Plans table (SaaS subscription plans)
CREATE TABLE IF NOT EXISTS plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    max_students INTEGER NOT NULL,
    max_instructors INTEGER NOT NULL,
    max_courses INTEGER NOT NULL,
    max_storage_gb INTEGER NOT NULL,
    features JSONB DEFAULT '{}',
    price_monthly DECIMAL(10,2) NOT NULL,
    price_yearly DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tenants table (organizations/academies)
CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    plan_id BIGINT REFERENCES plans(id),
    schema_name VARCHAR(63) NOT NULL UNIQUE,
    domain VARCHAR(255),
    status VARCHAR(20) DEFAULT 'active',
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tenant settings table
CREATE TABLE IF NOT EXISTS tenant_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    value JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(tenant_id, key)
);

-- Subscriptions table (billing)
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_id BIGINT NOT NULL REFERENCES plans(id),
    status VARCHAR(20) DEFAULT 'active',
    billing_cycle VARCHAR(20) DEFAULT 'monthly',
    current_period_start TIMESTAMPTZ NOT NULL,
    current_period_end TIMESTAMPTZ NOT NULL,
    canceled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Invoices table
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subscription_id BIGINT REFERENCES subscriptions(id),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'KRW',
    status VARCHAR(20) DEFAULT 'pending',
    due_date DATE NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Usage logs table (for metered billing)
CREATE TABLE IF NOT EXISTS usage_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    metric_type VARCHAR(50) NOT NULL,
    value DECIMAL(15,4) NOT NULL,
    recorded_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_tenants_slug ON tenants(slug);
CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant ON subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_usage_logs_tenant_metric ON usage_logs(tenant_id, metric_type, recorded_at);

-- Seed initial plans
INSERT INTO plans (name, display_name, description, max_students, max_instructors, max_courses, max_storage_gb, features, price_monthly, price_yearly)
VALUES
    ('free', 'Free', 'Free tier for small academies', 50, 2, 5, 1, '{"analytics": false, "custom_domain": false, "api_access": false}', 0, 0),
    ('basic', 'Basic', 'Basic plan for growing academies', 200, 5, 20, 10, '{"analytics": true, "custom_domain": false, "api_access": false}', 49000, 490000),
    ('pro', 'Pro', 'Professional plan for established academies', 1000, 20, 100, 50, '{"analytics": true, "custom_domain": true, "api_access": true}', 149000, 1490000),
    ('enterprise', 'Enterprise', 'Enterprise plan for large organizations', 10000, 100, 500, 500, '{"analytics": true, "custom_domain": true, "api_access": true, "sso": true, "dedicated_support": true}', 499000, 4990000)
ON CONFLICT (name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    max_students = EXCLUDED.max_students,
    max_instructors = EXCLUDED.max_instructors,
    max_courses = EXCLUDED.max_courses,
    max_storage_gb = EXCLUDED.max_storage_gb,
    features = EXCLUDED.features,
    price_monthly = EXCLUDED.price_monthly,
    price_yearly = EXCLUDED.price_yearly,
    updated_at = NOW();
