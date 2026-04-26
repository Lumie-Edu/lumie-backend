-- Add federated_identities table for multi-OAuth provider linking.
-- Replaces the single oauth_provider column on users with a 1:N relationship,
-- enabling Connected Accounts (link/unlink Google/Kakao/Naver per user).
--
-- The legacy users.oauth_provider column is left in place; the JPA entity no
-- longer maps it so Hibernate ignores it. Drop it in a follow-up DBA-owned
-- migration (requires table-owner privilege which the application role lacks).

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
