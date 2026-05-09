-- Replace time_slot_id with start_time / end_time on counseling_schedules

-- 1. Add new columns as nullable first
ALTER TABLE counseling_schedules
    ADD COLUMN start_time TIME,
    ADD COLUMN end_time   TIME;

-- 2. Backfill from existing time_slot_id values
UPDATE counseling_schedules SET
    start_time = CASE time_slot_id
        WHEN 1  THEN '09:00' WHEN 2  THEN '09:30'
        WHEN 3  THEN '10:00' WHEN 4  THEN '10:30'
        WHEN 5  THEN '11:00' WHEN 6  THEN '11:30'
        WHEN 7  THEN '12:00' WHEN 8  THEN '12:30'
        WHEN 9  THEN '13:00' WHEN 10 THEN '13:30'
        WHEN 11 THEN '14:00' WHEN 12 THEN '14:30'
        WHEN 13 THEN '15:00' WHEN 14 THEN '15:30'
        WHEN 15 THEN '16:00' WHEN 16 THEN '16:30'
        WHEN 17 THEN '17:00' WHEN 18 THEN '17:30'
        WHEN 19 THEN '18:00' WHEN 20 THEN '18:30'
        WHEN 21 THEN '19:00' WHEN 22 THEN '19:30'
        WHEN 23 THEN '20:00' WHEN 24 THEN '20:30'
        WHEN 25 THEN '21:00' WHEN 26 THEN '21:30'
        WHEN 27 THEN '22:00' WHEN 28 THEN '22:30'
    END,
    end_time = CASE time_slot_id
        WHEN 1  THEN '09:30' WHEN 2  THEN '10:00'
        WHEN 3  THEN '10:30' WHEN 4  THEN '11:00'
        WHEN 5  THEN '11:30' WHEN 6  THEN '12:00'
        WHEN 7  THEN '12:30' WHEN 8  THEN '13:00'
        WHEN 9  THEN '13:30' WHEN 10 THEN '14:00'
        WHEN 11 THEN '14:30' WHEN 12 THEN '15:00'
        WHEN 13 THEN '15:30' WHEN 14 THEN '16:00'
        WHEN 15 THEN '16:30' WHEN 16 THEN '17:00'
        WHEN 17 THEN '17:30' WHEN 18 THEN '18:00'
        WHEN 19 THEN '18:30' WHEN 20 THEN '19:00'
        WHEN 21 THEN '19:30' WHEN 22 THEN '20:00'
        WHEN 23 THEN '20:30' WHEN 24 THEN '21:00'
        WHEN 25 THEN '21:30' WHEN 26 THEN '22:00'
        WHEN 27 THEN '22:30' WHEN 28 THEN '23:00'
    END;

-- 3. Enforce NOT NULL after backfill
ALTER TABLE counseling_schedules
    ALTER COLUMN start_time SET NOT NULL,
    ALTER COLUMN end_time   SET NOT NULL;

-- 4. Drop old unique constraint and column
ALTER TABLE counseling_schedules
    DROP CONSTRAINT counseling_schedules_admin_id_date_time_slot_id_key,
    DROP COLUMN time_slot_id;

-- 5. New unique constraint: one admin can't have two schedules starting at the same time on the same day
ALTER TABLE counseling_schedules
    ADD CONSTRAINT uq_schedules_admin_date_start UNIQUE (admin_id, date, start_time);

-- 6. Index for time-based queries
CREATE INDEX idx_schedules_start_time ON counseling_schedules (start_time);
