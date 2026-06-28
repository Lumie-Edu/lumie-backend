-- =====================================================================
-- V75: contract cleanup after OAuth runtime removal
--
-- Forward-only and irreversible: runtime OAuth/SSO support has already been
-- removed, so this migration drops the last legacy public-schema database
-- artifacts left behind by the historical rollout. `if exists` guards keep
-- the contract cleanup idempotent on environments where one or both artifacts
-- were already removed or never created.
-- =====================================================================

drop table if exists public.federated_identities;

alter table if exists public.users
    drop column if exists oauth_provider;
