-- =====================================================================
-- Add `version BIGINT NOT NULL DEFAULT 0` to public-schema user-mutable
-- entities for JPA optimistic locking (`@Version`).
--
-- Scope: tenants (admin renames slug/contact), users (owner directory
-- updates email/password). Append-only public tables (event_publication,
-- shedlock, idempotency_keys, activity_logs, federated_identities,
-- custom_domain_requests, tenant_settings) are intentionally excluded.
-- =====================================================================

ALTER TABLE IF EXISTS tenants
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
