-- =====================================================================
-- Stripe-style idempotency-key table for replay-safe POST endpoints.
--
-- Scope: cross-tenant single table (lives in `public`) — keys are scoped
-- by (tenant_slug, endpoint_path) so the same key can be reused safely
-- across tenants/endpoints.
--
-- Lifecycle: rows expire after 24h via a scheduled cleanup job; the
-- created_at index supports the cleanup scan.
-- =====================================================================

CREATE TABLE idempotency_keys (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_slug         VARCHAR(80)  NOT NULL,
    endpoint_path       VARCHAR(255) NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(64)  NOT NULL,  -- sha256 hex of canonical request body
    response_status     INT          NOT NULL,
    response_body       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotency_keys UNIQUE (tenant_slug, endpoint_path, idempotency_key)
);

CREATE INDEX idx_idempotency_keys_created_at ON idempotency_keys (created_at);
