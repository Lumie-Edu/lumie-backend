-- V9: Rename content columns to match backend standard terminology
-- Changes:
-- - announcements: is_pinned → is_important (important notice marking)
-- - qna_boards: author_id → student_id (Q&A author is a student)
-- - counseling_schedules: counselor_id → admin_id (counselor is an admin)
--
-- Note: Content tables exist in tenant schemas (tenant_*), not public schema

DO $$
DECLARE
    tenant_schema TEXT;
BEGIN
    -- Iterate over all tenant schemas
    FOR tenant_schema IN
        SELECT schema_name
        FROM information_schema.schemata
        WHERE schema_name LIKE 'tenant_%'
    LOOP
        -- 1. Announcement: is_pinned → is_important
        EXECUTE format('ALTER TABLE %I.announcements RENAME COLUMN is_pinned TO is_important', tenant_schema);

        -- 2. QnaBoard: author_id → student_id
        EXECUTE format('ALTER TABLE %I.qna_boards RENAME COLUMN author_id TO student_id', tenant_schema);

        -- 3. Schedule: counselor_id → admin_id
        EXECUTE format('ALTER TABLE %I.counseling_schedules RENAME COLUMN counselor_id TO admin_id', tenant_schema);

        RAISE NOTICE 'Renamed columns in schema: %', tenant_schema;
    END LOOP;
END $$;
