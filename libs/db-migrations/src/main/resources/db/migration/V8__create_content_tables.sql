-- V8: Content tables already exist in tenant schemas from initial migration
-- This migration is a no-op as tables are created by CREATE_TENANT_SCHEMA function
-- Tables: announcements, qna_boards, qna_comments, textbooks, counseling_schedules, counseling_reservations

-- Just log that this migration ran
DO $$
BEGIN
    RAISE NOTICE 'V8 migration: Content tables already exist in tenant schemas';
END $$;
