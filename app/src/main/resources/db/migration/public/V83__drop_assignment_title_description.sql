-- =====================================================================
-- V83: remove assignment-owned display copy
--
-- Purpose:
--   Assignment names are derived from the linked exam. The assignment row
--   now owns distribution, deadline, visibility, and grading workflow only.
--
-- Forward-only:
--   Drops legacy assignments.title and assignments.description.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

alter table if exists assignments
    drop column if exists title,
    drop column if exists description;
