-- =====================================================================
-- V59: drop lecture status
--
-- Purpose:
--   Contract the lecture status field after the application stopped
--   reading, writing, filtering, sorting, and returning lectures.status.
--
-- Destructive change:
--   This migration permanently removes lectures.status and its legacy
--   status index. It must run only with the application version that no
--   longer depends on lecture status semantics.
--
-- Deploy order:
--   V57/V58 expand lecture visibility to lecture_class_targets, then the
--   status-free application version is deployed, then this contract step
--   removes the unused column.
-- =====================================================================

set lock_timeout = '5s';
set statement_timeout = '30s';

drop index if exists idx_lectures_status;

alter table if exists lectures
    drop column if exists status;
