-- Rename admin_id to user_id in counseling and qna tables
-- These columns store users.id (not admins.id), so the name should reflect that

-- 1. counseling_schedules
ALTER TABLE counseling_schedules
    DROP CONSTRAINT uq_schedules_admin_date_start;

DROP INDEX IF EXISTS idx_schedules_admin_id;

ALTER TABLE counseling_schedules
    RENAME COLUMN admin_id TO user_id;

ALTER TABLE counseling_schedules
    ADD CONSTRAINT uq_schedules_user_date_start UNIQUE (user_id, date, start_time);

CREATE INDEX idx_schedules_user_id ON counseling_schedules (user_id);

-- 2. counseling_reservations
ALTER TABLE counseling_reservations
    RENAME COLUMN admin_id TO user_id;

-- 3. qna_comments
ALTER TABLE qna_comments
    RENAME COLUMN admin_id TO user_id;
