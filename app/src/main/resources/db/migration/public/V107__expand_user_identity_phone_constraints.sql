-- ============================================================================
-- V107: Phase-A User identity constraint expansion
--
-- Deploy order:
--   1. Apply this migration while the previous release may still be running.
--   2. Deploy the release that reads identity from users and stops writing the
--      duplicated students/staff identity columns.
--   3. Only after every previous-release pod is retired and no code reads or
--      writes the old columns, use a separate contract migration to remove the
--      old columns and their indexes.
--
-- The existing student and staff phone indexes are intentionally retained in
-- this phase. They protect old pods during a rolling deployment. The new
-- partial indexes preserve their current semantics: phone values are unique
-- among students and unique among staff roles, while a student and staff
-- member may share a phone number.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- The next release no longer persists duplicate names on students or staff.
-- Relax these legacy storage requirements before that release is deployed so
-- inserts succeed while previous-release pods can still read the columns.
do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'students'
          and column_name = 'name'
          and is_nullable = 'NO'
    ) then
        alter table public.students alter column name drop not null;
    end if;

    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'staff'
          and column_name = 'name'
          and is_nullable = 'NO'
    ) then
        alter table public.staff alter column name drop not null;
    end if;
end
$$;

-- Fail before DDL if User-owned values cannot support the legacy uniqueness
-- boundary. The exception intentionally omits phone values from migration logs.
do $$
begin
    if exists (
        select 1
        from public.users
        where phone is not null
        group by phone, (role = 'STUDENT')
        having count(*) > 1
    ) then
        raise exception using
            errcode = 'unique_violation',
            message = 'Cannot add User phone uniqueness constraints: duplicate User phone values exist within the same identity role boundary',
            hint = 'Resolve duplicate User phone values before retrying this migration.';
    end if;
end
$$;

create unique index if not exists uq_users_student_phone
    on public.users (phone)
    where phone is not null
      and role = 'STUDENT';

create unique index if not exists uq_users_staff_phone
    on public.users (phone)
    where phone is not null
      -- Includes any legacy non-STUDENT role so an unknown stored role cannot
      -- bypass the staff-side uniqueness boundary during rollout.
      and role <> 'STUDENT';
