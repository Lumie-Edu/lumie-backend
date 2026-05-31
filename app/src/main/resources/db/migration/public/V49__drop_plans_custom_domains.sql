-- =====================================================================
-- V49: drop plans.custom_domains — custom domains are no longer plan-gated
--
-- Custom domain connection was previously gated to PRO/MAX plans via the
-- plans.custom_domains capability flag (mirrored by a TenantPlan check that
-- threw on FREE/BASIC). That gate is wrong: every plan, including FREE, may
-- connect a custom domain (set up manually by an operator after DNS work).
-- The flag had no live consumer beyond the now-removed gate — no UI read it —
-- so the column and its Java mappings (Plan entity, PlanFeatures VO,
-- PlanFeaturesResponse) are dropped together.
--
-- Forward-only: the column was created by V28 and seeded by V34 on existing
-- environments; this DROP removes it. IF EXISTS makes it a no-op where it
-- never existed. The plans table is platform-global (no tenant_id / RLS), so
-- no row-level-security policy changes are involved.
-- =====================================================================

alter table plans drop column if exists custom_domains;
