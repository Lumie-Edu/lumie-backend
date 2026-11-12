-- Reason is no longer collected for answer-key corrections.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE exam_answer_key_revisions
    DROP CONSTRAINT ck_exam_answer_key_revisions_reason,
    DROP COLUMN reason;
