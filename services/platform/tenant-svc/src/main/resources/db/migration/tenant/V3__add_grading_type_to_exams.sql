-- V3: Add grading_type column to exams table
-- Supports RELATIVE (상대평가) and ABSOLUTE (절대평가) grading types

ALTER TABLE exams
ADD COLUMN IF NOT EXISTS grading_type VARCHAR(20) NOT NULL DEFAULT 'ABSOLUTE';

-- Add index for grading_type
CREATE INDEX IF NOT EXISTS idx_exams_grading_type ON exams(grading_type);
