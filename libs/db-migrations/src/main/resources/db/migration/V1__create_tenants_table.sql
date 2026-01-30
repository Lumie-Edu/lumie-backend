-- Tenant management table (public schema)
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    schema_name VARCHAR(63) NOT NULL UNIQUE,
    owner_email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_status ON tenants(status);

COMMENT ON TABLE tenants IS 'Multi-tenant registry';
COMMENT ON COLUMN tenants.slug IS 'URL-safe unique identifier';
COMMENT ON COLUMN tenants.schema_name IS 'PostgreSQL schema name for tenant data';
