-- V27: Drop attendance_sessions.created_by
--
-- The column was always written as NULL in code and is not part of any
-- product feature. Removed to match the same decision applied to
-- assignments.created_by in V26.

ALTER TABLE attendance_sessions
    DROP COLUMN IF EXISTS created_by;
