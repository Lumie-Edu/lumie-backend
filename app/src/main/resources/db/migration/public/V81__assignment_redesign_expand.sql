-- =====================================================================
-- V81: expand assignments for the redesign workflow
--
-- Purpose:
--   Expand assignments and assignment_submissions to support exam-linked
--   assignment flows, richer evaluation modes, result visibility toggles,
--   and polymorphic audience targeting.
--
-- Backfill:
--   Existing assignments with a non-null class_id are copied into the new
--   assignment_targets table as CLASS targets. The backfill is idempotent
--   via the unique constraint and on conflict do nothing.
--
-- Forward-only:
--   No rollback is provided. assignments.class_id remains in place for
--   compatibility while assignment_targets becomes the new targeting source.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

alter table if exists assignments
    add column if not exists submission_mode text,
    add column if not exists evaluation_mode text,
    add column if not exists linked_exam_id bigint,
    add column if not exists show_score boolean,
    add column if not exists show_pass_fail boolean,
    add column if not exists show_grade boolean,
    add column if not exists show_question_results boolean,
    add column if not exists show_correct_answers boolean;

update assignments
set submission_mode = 'TEXT'
where submission_mode is null;

update assignments
set evaluation_mode = 'SCORE'
where evaluation_mode is null;

update assignments
set show_score = true
where show_score is null;

update assignments
set show_pass_fail = true
where show_pass_fail is null;

update assignments
set show_grade = true
where show_grade is null;

update assignments
set show_question_results = false
where show_question_results is null;

update assignments
set show_correct_answers = false
where show_correct_answers is null;

alter table if exists assignments
    alter column submission_mode set default 'TEXT',
    alter column submission_mode set not null,
    alter column evaluation_mode set default 'SCORE',
    alter column evaluation_mode set not null,
    alter column show_score set default true,
    alter column show_score set not null,
    alter column show_pass_fail set default true,
    alter column show_pass_fail set not null,
    alter column show_grade set default true,
    alter column show_grade set not null,
    alter column show_question_results set default false,
    alter column show_question_results set not null,
    alter column show_correct_answers set default false,
    alter column show_correct_answers set not null;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignments_submission_mode_check'
          and conrelid = 'assignments'::regclass
    ) then
        alter table assignments
            add constraint assignments_submission_mode_check
            check (submission_mode in ('TEXT', 'EXAM_MANUAL'));
    end if;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignments_evaluation_mode_check'
          and conrelid = 'assignments'::regclass
    ) then
        alter table assignments
            add constraint assignments_evaluation_mode_check
            check (evaluation_mode in ('PASS_FAIL', 'GRADE', 'SCORE', 'EXAM_AUTO'));
    end if;
end $$;

create table if not exists assignment_targets (
    id          bigserial    not null,
    assignment_id bigint     not null,
    target_type text         not null,
    target_id    bigint      not null,
    tenant_id    bigint      not null,
    version      bigint      not null default 0,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    constraint assignment_targets_pkey primary key (id)
);

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_targets_assignment_target_key'
          and conrelid = 'assignment_targets'::regclass
    ) then
        alter table assignment_targets
            add constraint assignment_targets_assignment_target_key
            unique (assignment_id, target_type, target_id);
    end if;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_targets_target_type_check'
          and conrelid = 'assignment_targets'::regclass
    ) then
        alter table assignment_targets
            add constraint assignment_targets_target_type_check
            check (target_type in ('CLASS', 'STUDENT'));
    end if;
end $$;

create index if not exists idx_assignment_targets_tenant_assignment_id
    on assignment_targets (tenant_id, assignment_id);

create index if not exists idx_assignment_targets_tenant_target_lookup
    on assignment_targets (tenant_id, target_type, target_id);

comment on table assignment_targets is 'Tenant-scoped assignment visibility targets. target_type=CLASS maps target_id to classes.id; target_type=STUDENT maps target_id to students.id.';
comment on column assignment_targets.target_id is 'Polymorphic target identifier. CLASS rows reference classes.id semantically and STUDENT rows reference students.id semantically.';

insert into assignment_targets (
    assignment_id,
    target_type,
    target_id,
    tenant_id
)
select
    a.id,
    'CLASS',
    a.class_id,
    a.tenant_id
from assignments a
join classes c
    on c.id = a.class_id
   and c.tenant_id = a.tenant_id
where a.class_id is not null
on conflict (assignment_id, target_type, target_id) do nothing;

alter table assignment_targets enable row level security;
alter table assignment_targets force row level security;
drop policy if exists tenant_isolation on assignment_targets;
create policy tenant_isolation on assignment_targets
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on assignment_targets to lumie_app;
grant usage, select on sequence assignment_targets_id_seq to lumie_app;

alter table if exists assignment_submissions
    add column if not exists answers jsonb,
    add column if not exists exam_result_id bigint,
    add column if not exists passed boolean,
    add column if not exists grade_value character varying(30);
