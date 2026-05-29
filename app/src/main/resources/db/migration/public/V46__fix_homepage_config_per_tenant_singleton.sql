-- =====================================================================
-- V46: fix homepage_config_singleton — replace global singleton with
-- per-tenant singleton
--
-- Bug: V18 created the following index on the RLS-shared homepage_config
-- table (migrated from the old schema-per-tenant era):
--
--   CREATE UNIQUE INDEX homepage_config_singleton
--       ON homepage_config USING btree ((true));
--
-- ((true)) is a constant expression, so the unique index enforces AT MOST
-- ONE ROW ACROSS THE ENTIRE TABLE, across all tenants.  RLS only filters
-- SELECT/UPDATE/DELETE; a unique index constraint is physical and global.
-- Result: only the first tenant to insert a homepage config row succeeds;
-- every subsequent tenant's first INSERT hits a duplicate-key violation
-- (COMMON_008, HTTP 409).  This is a legacy artifact from schema-per-tenant
-- where each tenant had its own schema (so ((true)) was safe) that became
-- wrong after consolidating to a single public schema with RLS.
--
-- Fix: replace the global constant-expression index with a unique index on
-- (tenant_id), which enforces exactly one homepage config row per tenant.
--
-- Safety: the old index permitted at most one physical row in the table, so
-- no duplicate tenant_id values can exist to block the new index creation.
--
-- Rollback: drop homepage_config_singleton (new), recreate with ((true)).
--   Only safe to roll back if at most one tenant has a homepage config row.
-- =====================================================================

-- 1. Remove the global singleton index (legacy schema-per-tenant artifact).
drop index if exists homepage_config_singleton;

-- 2. Create a per-tenant singleton index — exactly one homepage config per tenant.
create unique index homepage_config_singleton
    on homepage_config (tenant_id);
