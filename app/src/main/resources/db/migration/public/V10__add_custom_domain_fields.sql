-- Custom domain support for PRO/MAX tenants.
-- Domain registration is provisioned via Cloudflare Registrar API; the registration
-- request is gated by an email magic link to the operator.

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS custom_domain VARCHAR(253),
    ADD COLUMN IF NOT EXISTS custom_domain_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS custom_domain_activated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS custom_domain_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cf_zone_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS cf_registration_id VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS idx_tenants_custom_domain
    ON tenants(custom_domain)
    WHERE custom_domain IS NOT NULL;

CREATE TABLE IF NOT EXISTS custom_domain_requests (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    domain_name VARCHAR(253) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    requester_user_id BIGINT NOT NULL,
    approval_token_hash VARCHAR(64),
    token_expires_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    rejection_reason TEXT,
    cf_response JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_custom_domain_requests_tenant
    ON custom_domain_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_custom_domain_requests_status
    ON custom_domain_requests(status);
CREATE INDEX IF NOT EXISTS idx_custom_domain_requests_token_hash
    ON custom_domain_requests(approval_token_hash)
    WHERE approval_token_hash IS NOT NULL;
