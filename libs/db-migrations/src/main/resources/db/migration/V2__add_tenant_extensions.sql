-- Add plan_id column to tenants table
ALTER TABLE tenants ADD COLUMN plan_id VARCHAR(20) NOT NULL DEFAULT 'FREE';
CREATE INDEX idx_tenants_plan_id ON tenants(plan_id);
COMMENT ON COLUMN tenants.plan_id IS 'Subscription plan: FREE, BASIC, PRO, ENTERPRISE';

-- Tenant settings table (logo, theme, etc.)
CREATE TABLE tenant_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    logo_url VARCHAR(500),
    theme JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenant_settings_tenant_id ON tenant_settings(tenant_id);
COMMENT ON TABLE tenant_settings IS 'Customizable settings per tenant';
COMMENT ON COLUMN tenant_settings.theme IS 'JSON theme configuration (colors, fonts, etc.)';

-- Tenant custom domains table (for PRO/ENTERPRISE plans)
CREATE TABLE tenant_domains (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    domain VARCHAR(255) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenant_domains_tenant_id ON tenant_domains(tenant_id);
CREATE INDEX idx_tenant_domains_domain ON tenant_domains(domain);
CREATE INDEX idx_tenant_domains_status ON tenant_domains(status);
COMMENT ON TABLE tenant_domains IS 'Custom domains for tenants (PRO/ENTERPRISE)';
COMMENT ON COLUMN tenant_domains.status IS 'Domain status: PENDING, VERIFYING, VERIFIED, ACTIVE, FAILED';

-- Schema versions tracking table (for Flyway per-tenant migrations)
CREATE TABLE schema_versions (
    id BIGSERIAL PRIMARY KEY,
    tenant_slug VARCHAR(30) NOT NULL,
    version VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    applied_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_slug, version)
);

CREATE INDEX idx_schema_versions_tenant_slug ON schema_versions(tenant_slug);
COMMENT ON TABLE schema_versions IS 'Track schema migration versions per tenant';
