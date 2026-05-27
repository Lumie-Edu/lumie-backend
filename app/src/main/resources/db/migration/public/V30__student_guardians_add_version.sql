-- N-2: Add optimistic locking version column to student_guardians
-- Required because StudentGuardian is updated by user actions (designateAsPrimary/demoteFromPrimary).
ALTER TABLE student_guardians
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
