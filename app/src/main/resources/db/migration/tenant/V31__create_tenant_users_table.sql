-- V31: Create per-tenant users table for the user-storage split refactor.
--
-- Background: all users currently live in `public.users`, with cross-schema FKs
-- pointing back to it. We are migrating non-OWNER identity into per-tenant
-- `users` tables so that:
--   - tenant data (students, instructors, managers) lives entirely inside its
--     tenant schema.
--   - cross-schema FKs are eliminated (rewritten to same-schema in V33).
--   - student `user_login_id` namespaces become independent across academies.
--
-- This migration creates the empty tenant.users table. V32 backfills data from
-- public.users (preserving id values so existing FK targets stay valid through
-- the cutover). FK rewrites and public.users slimming come in later migrations.

CREATE TABLE IF NOT EXISTS users (
    id                  BIGSERIAL PRIMARY KEY,
    user_login_id       VARCHAR(50) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    name                VARCHAR(100) NOT NULL,
    phone               VARCHAR(20),
    email               VARCHAR(255),
    role                VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    avatar_seed         VARCHAR(50),
    oauth_provider      VARCHAR(50),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_user_login_id_unique UNIQUE (user_login_id)
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
