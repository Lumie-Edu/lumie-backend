-- =====================================================================
-- V48: drop exam_dashboard_rollups — reverts the V47 rollup experiment
--
-- The per-exam dashboard rollup (V47) was over-engineered for the current
-- data volume: a tenant's exam statistics aggregate over only tens of
-- thousands of question_results rows, which Postgres GROUP BY handles in
-- milliseconds. The dashboard now computes its aggregates directly with
-- COUNT/GROUP BY (heap-bounded, RLS-scoped per tenant) behind a short-TTL
-- cache, so the rollup table, its refresh events, and the backfill machinery
-- are no longer needed.
--
-- V47 was applied to the dev cluster, so this is a forward-only DROP rather
-- than deleting V47. On environments where V47 never ran, the table simply
-- does not exist and the IF EXISTS guard makes this a no-op.
--
-- Rollback: re-create via V47's DDL (and restore the rollup code) — but the
-- direct-aggregate path is the intended state.
-- =====================================================================

drop table if exists exam_dashboard_rollups;
