-- V18: Drop academy domain
-- Removes the academies table and all academy_id columns/FKs/indexes
-- from child tables. All operations are idempotent (IF EXISTS).

-- -----------------------------------------------------------------------
-- Step 1: Drop join tables that FK into academies
-- -----------------------------------------------------------------------

drop table if exists admin_academies;
drop table if exists announcement_academies;

-- -----------------------------------------------------------------------
-- Step 2: students
-- -----------------------------------------------------------------------

alter table students drop constraint if exists students_academy_id_fkey;
drop index if exists idx_students_academy_id;
alter table students drop column if exists academy_id;

-- -----------------------------------------------------------------------
-- Step 3: attendance_sessions
-- -----------------------------------------------------------------------

alter table attendance_sessions drop constraint if exists attendance_sessions_academy_id_fkey;
drop index if exists idx_att_sessions_academy_id;
alter table attendance_sessions drop column if exists academy_id;

-- -----------------------------------------------------------------------
-- Step 4: textbooks
-- (FK was not given an explicit name in V1 — drop dynamically)
-- -----------------------------------------------------------------------

do $$
declare
    c text;
begin
    for c in
        select conname
        from pg_constraint
        where conrelid = 'textbooks'::regclass
          and contype = 'f'
          and pg_get_constraintdef(oid) like '%academy_id%'
    loop
        execute format('alter table textbooks drop constraint %I', c);
    end loop;
end $$;

drop index if exists idx_textbooks_academy_id;
alter table textbooks drop column if exists academy_id;

-- -----------------------------------------------------------------------
-- Step 5: classes
-- -----------------------------------------------------------------------

alter table classes drop constraint if exists classes_academy_id_fkey;
drop index if exists idx_classes_academy_id;
alter table classes drop column if exists academy_id;

-- -----------------------------------------------------------------------
-- Step 6: assignments
-- -----------------------------------------------------------------------

alter table assignments drop constraint if exists assignments_academy_id_fkey;
drop index if exists idx_assignments_academy_id;
alter table assignments drop column if exists academy_id;

-- -----------------------------------------------------------------------
-- Step 7: sms_templates
-- -----------------------------------------------------------------------

alter table sms_templates drop constraint if exists sms_templates_academy_id_fkey;
drop index if exists idx_sms_templates_academy_id;
alter table sms_templates drop column if exists academy_id;

-- -----------------------------------------------------------------------
-- Step 8: sms_messages
-- -----------------------------------------------------------------------

alter table sms_messages drop constraint if exists sms_messages_academy_id_fkey;
drop index if exists idx_sms_messages_academy_id;
alter table sms_messages drop column if exists academy_id;

-- -----------------------------------------------------------------------
-- Step 9: class_journals (may already be dropped by V12, guard with DO block)
-- -----------------------------------------------------------------------

do $$
begin
    if exists (select 1 from information_schema.tables where table_name = 'class_journals') then
        alter table class_journals drop constraint if exists class_journals_academy_id_fkey;
        drop index if exists idx_class_journals_academy_id;
        alter table class_journals drop column if exists academy_id;
    end if;
end $$;

-- -----------------------------------------------------------------------
-- Step 10: lectures
-- -----------------------------------------------------------------------

alter table lectures drop constraint if exists fk_lectures_academy;
drop index if exists idx_lectures_academy_id;
alter table lectures drop column if exists academy_id;

-- -----------------------------------------------------------------------
-- Step 11: Drop the academies table itself (CASCADE clears any stragglers)
-- -----------------------------------------------------------------------

drop table if exists academies cascade;
