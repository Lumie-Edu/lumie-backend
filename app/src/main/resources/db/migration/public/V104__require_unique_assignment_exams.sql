-- =====================================================================
-- V104: require a dedicated exam for every assignment
--
-- Purpose:
--   Every assignment distributes exactly one exam. Historical assignments
--   without an exam belong to the removed manual-assignment workflow and are
--   deleted before the relationship is made mandatory. The composite unique
--   constraint prevents a tenant from using the same exam for more than one
--   assignment while preserving the existing tenant-safe composite FK.
--
-- Forward-only:
--   Rows without linked_exam_id and their cascading submissions are removed.
--   Existing duplicate links still fail before schema changes because there
--   is no safe automatic choice of which assignment to retain.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
begin
    if to_regclass('public.assignments') is null then
        return;
    end if;

    if exists (
        select 1
        from assignments
        where linked_exam_id is not null
        group by tenant_id, linked_exam_id
        having count(*) > 1
    ) then
        raise exception
            'cannot require unique assignment exams: duplicate tenant_id, linked_exam_id pairs exist';
    end if;
end $$;

delete from assignments
where linked_exam_id is null;

alter table if exists assignments
    alter column linked_exam_id set not null;

do $$
begin
    if to_regclass('public.assignments') is null then
        return;
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignments_tenant_linked_exam_key'
          and conrelid = 'assignments'::regclass
    ) then
        alter table assignments
            add constraint assignments_tenant_linked_exam_key
            unique (tenant_id, linked_exam_id);
    end if;
end $$;
