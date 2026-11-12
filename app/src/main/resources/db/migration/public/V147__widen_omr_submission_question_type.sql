SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE omr_submission_questions
    ALTER COLUMN question_type TYPE VARCHAR(50);
