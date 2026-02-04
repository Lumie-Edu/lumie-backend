-- V4: Add grade_scale column to exams table
-- Supports NINE_GRADE (9등급제) and FIVE_GRADE (5등급제) for relative grading

ALTER TABLE exams
ADD COLUMN IF NOT EXISTS grade_scale VARCHAR(20) DEFAULT 'NINE_GRADE';

-- Add index for grade_scale
CREATE INDEX IF NOT EXISTS idx_exams_grade_scale ON exams(grade_scale);
