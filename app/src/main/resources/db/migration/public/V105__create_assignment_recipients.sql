-- =====================================================================
-- V105: snapshot assignment recipients
--
-- Purpose:
--   Persist the students responsible for an assignment independently of
--   later class enrollment changes. Existing targets are expanded once from
--   direct student targets and currently enrolled class members.
--
-- Forward-only:
--   This migration creates recipient snapshots and backfills them from the
--   current target/enrollment state. It does not remove recipient records
--   when a student subsequently leaves a class.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

create table if not exists assignment_recipients (
    id            bigserial    not null,
    assignment_id bigint       not null,
    student_id    bigint       not null,
    tenant_id     bigint       not null,
    version       bigint       not null default 0,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),
    constraint assignment_recipients_pkey primary key (id),
    constraint assignment_recipients_assignment_student_key unique (assignment_id, student_id),
    constraint assignment_recipients_assignment_tenant_fkey
        foreign key (assignment_id, tenant_id)
        references assignments (id, tenant_id)
        on delete cascade
);

create index if not exists idx_assignment_recipients_tenant_assignment_id
    on assignment_recipients (tenant_id, assignment_id);

create index if not exists idx_assignment_recipients_tenant_student_id
    on assignment_recipients (tenant_id, student_id);

-- The backfill is idempotent. A student directly targeted and enrolled in a
-- targeted class still receives exactly one snapshot row.
insert into assignment_recipients (assignment_id, student_id, tenant_id)
select distinct recipients.assignment_id, recipients.student_id, recipients.tenant_id
from (
    select target.assignment_id, student.id as student_id, target.tenant_id
    from assignment_targets target
    join assignments assignment
        on assignment.id = target.assignment_id
       and assignment.tenant_id = target.tenant_id
    join students student
        on student.id = target.target_id
       and student.tenant_id = target.tenant_id
    where target.target_type = 'STUDENT'

    union

    select target.assignment_id, enrollment.student_id, target.tenant_id
    from assignment_targets target
    join assignments assignment
        on assignment.id = target.assignment_id
       and assignment.tenant_id = target.tenant_id
    join class_enrollments enrollment
        on enrollment.class_id = target.target_id
       and enrollment.tenant_id = target.tenant_id
       and enrollment.status = 'ENROLLED'
       and enrollment.dropped_at is null
    join students student
        on student.id = enrollment.student_id
       and student.tenant_id = target.tenant_id
    where target.target_type = 'CLASS'
) recipients
on conflict (assignment_id, student_id) do nothing;

alter table assignment_recipients enable row level security;
alter table assignment_recipients force row level security;
drop policy if exists tenant_isolation on assignment_recipients;
create policy tenant_isolation on assignment_recipients
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on assignment_recipients to lumie_app;
grant usage, select on sequence assignment_recipients_id_seq to lumie_app;
