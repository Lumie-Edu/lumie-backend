-- Add federated_identities table for multi-OAuth provider linking.
-- Replaces the single oauth_provider column on users with a 1:N relationship,
-- enabling Connected Accounts (link/unlink Google/Kakao/Naver per user).
--
-- Green-field migration: production users.oauth_provider has no data, safe to drop.

CREATE TABLE IF NOT EXISTS federated_identities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_federated_provider_external UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_federated_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX IF NOT EXISTS idx_federated_identities_user_id ON federated_identities(user_id);

ALTER TABLE users DROP COLUMN IF EXISTS oauth_provider;
