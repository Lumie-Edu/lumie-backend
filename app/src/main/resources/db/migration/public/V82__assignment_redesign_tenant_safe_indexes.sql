-- =====================================================================
-- V82: tenant-safe assignment redesign indexes and FKs
--
-- Purpose:
--   Add concurrent indexes needed by tenant-safe composite FKs introduced by
--   the assignment redesign. This is split from V81 so index creation on
--   existing tables can run outside the Flyway transaction.
-- =====================================================================

set lock_timeout = '5s';
set statement_timeout = '60s';

create unique index concurrently if not exists idx_assignments_id_tenant_id
    on assignments (id, tenant_id);

create unique index concurrently if not exists idx_exams_id_tenant_id
    on exams (id, tenant_id);

create unique index concurrently if not exists idx_exam_results_id_tenant_id
    on exam_results (id, tenant_id);

create index concurrently if not exists idx_assignments_linked_exam_id
    on assignments (linked_exam_id)
    where linked_exam_id is not null;

create index concurrently if not exists idx_assignment_submissions_exam_result_id
    on assignment_submissions (exam_result_id)
    where exam_result_id is not null;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignments_linked_exam_tenant_fkey'
          and conrelid = 'assignments'::regclass
    ) then
        alter table assignments
            add constraint assignments_linked_exam_tenant_fkey
            foreign key (linked_exam_id, tenant_id)
            references exams(id, tenant_id)
            on delete set null;
    end if;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_targets_assignment_tenant_fkey'
          and conrelid = 'assignment_targets'::regclass
    ) then
        alter table assignment_targets
            add constraint assignment_targets_assignment_tenant_fkey
            foreign key (assignment_id, tenant_id)
            references assignments(id, tenant_id)
            on delete cascade;
    end if;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_submissions_exam_result_tenant_fkey'
          and conrelid = 'assignment_submissions'::regclass
    ) then
        alter table assignment_submissions
            add constraint assignment_submissions_exam_result_tenant_fkey
            foreign key (exam_result_id, tenant_id)
            references exam_results(id, tenant_id)
            on delete set null;
    end if;
end $$;
