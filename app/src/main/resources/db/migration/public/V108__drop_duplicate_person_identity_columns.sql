-- ============================================================================
-- V108: Phase-B User identity contract
--
-- Irreversible: this migration removes duplicated person identity storage.
--
-- Expected deploy order:
--   1. V107 must already be applied.
--   2. Deploy the release that reads identity only from public.users and stops
--      writing students.name/students.phone/staff.name/staff.phone/staff.email.
--   3. Confirm every previous-release pod is retired and no deployed code,
--      database object, or operational script reads or writes these columns.
--   4. Only then apply this contract migration.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- These were legacy indexes on columns removed below. Remove them explicitly
-- so reruns against partially changed local/dev databases remain safe.
drop index if exists public.idx_students_phone;
drop index if exists public.idx_staff_phone;
drop index if exists public.idx_staff_email;

alter table if exists public.students
    drop column if exists name,
    drop column if exists phone;

alter table if exists public.staff
    drop column if exists name,
    drop column if exists phone,
    drop column if exists email;
