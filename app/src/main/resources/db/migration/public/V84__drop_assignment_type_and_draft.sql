-- =====================================================================
-- V84: remove assignment type and draft state
--
-- Purpose:
--   Assignments no longer carry a separate category/type. They are exam-backed
--   distribution records with ACTIVE/CLOSED visibility only.
--
-- Forward-only:
--   Existing DRAFT assignments are promoted to ACTIVE before tightening the
--   allowed status values. Existing assignment type values are discarded.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

update assignments
set status = 'ACTIVE'
where status = 'DRAFT';

alter table if exists assignments
    drop column if exists type;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignments_status_active_closed_check'
          and conrelid = 'assignments'::regclass
    ) then
        alter table assignments
            add constraint assignments_status_active_closed_check
            check (status in ('ACTIVE', 'CLOSED'));
    end if;
end $$;
