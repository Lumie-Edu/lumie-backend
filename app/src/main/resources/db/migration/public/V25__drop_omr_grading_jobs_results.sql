-- D.8/A.7 follow-up: drop the dead OmrGradingJob.results jsonb column.
-- Verified: no Java code ever wrote to it; OMR per-question results live
-- in ExamResult entities (modules/exam) via ResultCommandService.
ALTER TABLE omr_grading_jobs DROP COLUMN IF EXISTS results;
