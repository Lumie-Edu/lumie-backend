-- =====================================================================
-- V32: Add optimistic-lock version column to plans
--
-- N5: plans.version (BIGINT NOT NULL DEFAULT 0)
--   Plan has deactivate()/activate() domain methods and can be
--   concurrently modified by Lumie ops. @Version ensures last-write-wins
--   is replaced with optimistic locking per HARD_RULES.
-- =====================================================================

ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN plans.version IS
    'Hibernate optimistic-lock version. Incremented on every UPDATE.';
