ALTER TABLE exams
    ADD COLUMN source varchar(30) NOT NULL DEFAULT 'EXAM_MANAGEMENT';

CREATE INDEX idx_exams_source ON exams (source);
