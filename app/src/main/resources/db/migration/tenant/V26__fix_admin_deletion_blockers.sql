-- V26: Remove blockers that prevented deleting an INSTRUCTOR/MANAGER
--
-- Two columns had ON DELETE SET NULL FK constraints but were declared NOT NULL,
-- so deleting the referenced admin would always raise:
--   "null value in column ... violates not-null constraint"
--
-- - assignments.created_by: domain decision is that author tracking is not
--   meaningful for assignments, so the column (and its FK + index) is dropped.
-- - lectures.teacher_id: teacher matters for a lecture but the record should
--   survive teacher deletion, so we keep SET NULL and drop NOT NULL.

ALTER TABLE assignments
    DROP CONSTRAINT IF EXISTS assignments_created_by_fkey;

DROP INDEX IF EXISTS idx_assignments_created_by;

ALTER TABLE assignments
    DROP COLUMN IF EXISTS created_by;

ALTER TABLE lectures
    ALTER COLUMN teacher_id DROP NOT NULL;
