CREATE TABLE homepage_config (
    id BIGSERIAL PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL,
    template_version INTEGER NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    theme JSONB NOT NULL,
    sections JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Enforce at most one row per tenant schema.
CREATE UNIQUE INDEX homepage_config_singleton ON homepage_config ((true));
