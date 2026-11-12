SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE exam_answer_key_revisions
    DROP CONSTRAINT ck_exam_answer_key_revisions_changed_answers,
    ADD COLUMN changed_fields JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD CONSTRAINT ck_exam_answer_key_revisions_has_changes
        CHECK (jsonb_typeof(changed_answers) = 'object'
           AND jsonb_typeof(changed_fields) = 'object'
           AND (changed_answers <> '{}'::jsonb OR changed_fields <> '{}'::jsonb));
