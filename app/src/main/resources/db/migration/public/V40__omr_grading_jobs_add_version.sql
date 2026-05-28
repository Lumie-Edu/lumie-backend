-- N-2: Add optimistic locking version column to omr_grading_jobs
-- Required because OmrGradingJob.version (bde8f62) satisfies @Version but the column was never migrated.
ALTER TABLE omr_grading_jobs
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
