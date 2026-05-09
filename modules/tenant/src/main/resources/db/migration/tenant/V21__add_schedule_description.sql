-- Add optional description field to counseling_schedules
ALTER TABLE counseling_schedules
    ADD COLUMN description TEXT;
