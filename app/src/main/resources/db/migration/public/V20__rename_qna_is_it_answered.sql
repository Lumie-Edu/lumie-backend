-- Rename is_it_answered → is_answered (Java field was already renamed; DB catches up).
-- RENAME COLUMN has no IF EXISTS guard in Postgres, so wrap in exception handler.
DO $$
BEGIN
    ALTER TABLE qna_boards RENAME COLUMN is_it_answered TO is_answered;
EXCEPTION WHEN undefined_column THEN
    NULL;
END $$;

-- Add optimistic locking version columns to user-mutable entities missed in V17
ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE attendance_sessions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE qna_boards
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
