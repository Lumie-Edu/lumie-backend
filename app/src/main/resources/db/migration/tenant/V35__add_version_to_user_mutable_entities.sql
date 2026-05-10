-- =====================================================================
-- Add `version BIGINT NOT NULL DEFAULT 0` to user-mutable entities for
-- JPA optimistic locking (`@Version`).
--
-- Scope: entities whose state is mutated by concurrent user actions
-- (Student profile updates, Admin permission changes, ExamResult
-- corrections, ClassEntity/ClassEnrollment edits, Lecture metadata,
-- Assignment edits). Insert-only / append-only entities (activity_logs,
-- omr_grading_jobs, report_generation_jobs, sms_messages, paddle_webhook
-- events, idempotency_keys) are intentionally excluded — there is no
-- concurrent mutation contention to protect against.
--
-- `IF EXISTS` guards each ALTER so this runs cleanly on tenant schemas
-- where deprecated tables have been dropped (matching the V34 pattern).
-- The default 0 backfills existing rows; new rows continue to start at 0
-- because `@Version` initialization seeds the field from the column.
-- =====================================================================

ALTER TABLE IF EXISTS students
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS admins
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS admin_permissions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS permissions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS exams
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS exam_results
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS classes
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS class_enrollments
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS lectures
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS assignments
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS assignment_submissions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS homepage_config
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS subscriptions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS plans
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
