-- Rollback for V25__drop_omr_grading_jobs_results.sql
-- Restores the column structure; data is unrecoverable (field was always null — never written).
ALTER TABLE omr_grading_jobs ADD COLUMN IF NOT EXISTS results jsonb;
