-- =====================================================================
-- V53: Default new attendance records to PENDING ("미인증") not ABSENT
--
-- Background: sessions used to be pre-created by a daily scheduler with
-- every student defaulted to ABSENT (결석).  With on-demand session
-- creation, defaulting everyone to ABSENT is punitive and wrong — a
-- record should start "not yet decided" until a teacher marks it or the
-- student checks in.  A new AttendanceStatus value PENDING represents
-- that state; the application (AttendanceRecord) now creates records as
-- PENDING.  This migration aligns the column default with the app.
--
-- Scope: changes the column DEFAULT only.  Existing rows are NOT
-- rewritten — historical ABSENT records may be genuine absences and
-- cannot be distinguished from "never marked", so they are left intact.
--
-- Idempotency: ALTER COLUMN ... SET DEFAULT is declarative; re-running
-- sets the same default and is a no-op.  Metadata-only change — no table
-- rewrite, no long lock.
--
-- Forward-only.  No rollback is provided.
-- =====================================================================

set local lock_timeout = '5s';

alter table attendance_records
    alter column status set default 'PENDING';
