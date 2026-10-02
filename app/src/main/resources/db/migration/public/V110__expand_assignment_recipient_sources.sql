-- ============================================================================
-- V110: expand assignment recipient sources
--
-- Purpose:
--   Record the class or direct-student target that makes a recipient eligible
--   to submit. Removing a source target removes only that provenance row; the
--   recipient roster and past submissions remain intact.
--
-- Deploy order:
--   1. Apply V109 before this migration.
--   2. Deploy code that records an EXACT source for each new recipient and
--      uses an existing source row to decide new-submit eligibility.
--   3. Keep assignment_recipients and legacy targeting columns available
--      until all old pods and readers have been retired.
--   4. This migration revokes live sources on student deletion. If a later
--      migration makes assignment_recipients.student_id nullable, keep that
--      ordering: remove sources before clearing the historical student ID.
--      The composite recipient FK retains student_id to prove source identity.
--
-- Historical backfill:
--   A direct target is evidence only when it currently targets the recipient.
--   A class target is evidence only when the recipient is currently enrolled
--   in that target class. Neither proves the original snapshot membership, so
--   every pre-existing recipient and backfilled source remains UNVERIFIED.
--   No recipient, roster, or submission is removed for missing evidence.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Old pods can still insert recipients without source records. Keep those rows
-- explicitly UNVERIFIED until a source-aware release writes exact provenance.
alter table if exists assignment_recipients
    add column if not exists source_provenance text not null default 'UNVERIFIED';

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_recipients_source_provenance_check'
          and conrelid = 'assignment_recipients'::regclass
    ) then
        alter table assignment_recipients
            add constraint assignment_recipients_source_provenance_check
            check (source_provenance in ('EXACT', 'UNVERIFIED'));
    end if;
end $$;

-- These composite keys let every source assert the same assignment, student,
-- and tenant as the recipient and its typed target.
create unique index if not exists idx_assignment_recipients_id_assignment_student_tenant
    on assignment_recipients (id, assignment_id, student_id, tenant_id);

create unique index if not exists idx_assignment_class_targets_id_assignment_tenant
    on assignment_class_targets (id, assignment_id, tenant_id);

create unique index if not exists idx_assignment_student_targets_id_assignment_student_tenant
    on assignment_student_targets (id, assignment_id, student_id, tenant_id);

create table if not exists assignment_recipient_sources (
    id                      bigserial    not null,
    assignment_recipient_id bigint       not null,
    assignment_id           bigint       not null,
    student_id              bigint       not null,
    class_target_id         bigint,
    student_target_id       bigint,
    tenant_id               bigint       not null,
    source_provenance       text         not null default 'EXACT',
    version                 bigint       not null default 0,
    created_at              timestamptz  not null default now(),
    updated_at              timestamptz  not null default now(),
    constraint assignment_recipient_sources_pkey primary key (id),
    constraint assignment_recipient_sources_one_target_check
        check (num_nonnulls(class_target_id, student_target_id) = 1),
    constraint assignment_recipient_sources_provenance_check
        check (source_provenance in ('EXACT', 'UNVERIFIED'))
);

create unique index if not exists idx_assignment_recipient_sources_recipient_class_target
    on assignment_recipient_sources (assignment_recipient_id, class_target_id)
    where class_target_id is not null;

create unique index if not exists idx_assignment_recipient_sources_recipient_student_target
    on assignment_recipient_sources (assignment_recipient_id, student_target_id)
    where student_target_id is not null;

create index if not exists idx_assignment_recipient_sources_tenant_assignment_student
    on assignment_recipient_sources (tenant_id, assignment_id, student_id);

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_recipient_sources_recipient_tenant_fkey'
          and conrelid = 'assignment_recipient_sources'::regclass
    ) then
        alter table assignment_recipient_sources
            add constraint assignment_recipient_sources_recipient_tenant_fkey
            foreign key (assignment_recipient_id, assignment_id, student_id, tenant_id)
            references assignment_recipients (id, assignment_id, student_id, tenant_id)
            on delete cascade;
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_recipient_sources_class_target_tenant_fkey'
          and conrelid = 'assignment_recipient_sources'::regclass
    ) then
        alter table assignment_recipient_sources
            add constraint assignment_recipient_sources_class_target_tenant_fkey
            foreign key (class_target_id, assignment_id, tenant_id)
            references assignment_class_targets (id, assignment_id, tenant_id)
            on delete cascade;
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_recipient_sources_student_target_tenant_fkey'
          and conrelid = 'assignment_recipient_sources'::regclass
    ) then
        alter table assignment_recipient_sources
            add constraint assignment_recipient_sources_student_target_tenant_fkey
            foreign key (student_target_id, assignment_id, student_id, tenant_id)
            references assignment_student_targets (id, assignment_id, student_id, tenant_id)
            on delete cascade;
    end if;
end $$;

-- Current direct targeting is evidence that the recipient has this source,
-- but not proof that it was present when a historical recipient snapshot was
-- created. Preserve that distinction as UNVERIFIED.
insert into assignment_recipient_sources (
    assignment_recipient_id,
    assignment_id,
    student_id,
    student_target_id,
    tenant_id,
    source_provenance
)
select
    recipient.id,
    recipient.assignment_id,
    recipient.student_id,
    target.id,
    recipient.tenant_id,
    'UNVERIFIED'
from assignment_recipients recipient
join assignment_student_targets target
  on target.assignment_id = recipient.assignment_id
 and target.student_id = recipient.student_id
 and target.tenant_id = recipient.tenant_id
on conflict (assignment_recipient_id, student_target_id)
    where student_target_id is not null do nothing;

-- A class source is backfilled only for a current active enrollment. A past
-- enrollment cannot be reconstructed from this schema, so it is intentionally
-- not inferred.
insert into assignment_recipient_sources (
    assignment_recipient_id,
    assignment_id,
    student_id,
    class_target_id,
    tenant_id,
    source_provenance
)
select
    recipient.id,
    recipient.assignment_id,
    recipient.student_id,
    target.id,
    recipient.tenant_id,
    'UNVERIFIED'
from assignment_recipients recipient
join assignment_class_targets target
  on target.assignment_id = recipient.assignment_id
 and target.tenant_id = recipient.tenant_id
join class_enrollments enrollment
  on enrollment.class_id = target.class_id
 and enrollment.student_id = recipient.student_id
 and enrollment.tenant_id = recipient.tenant_id
 and enrollment.status = 'ENROLLED'
 and enrollment.dropped_at is null
on conflict (assignment_recipient_id, class_target_id)
    where class_target_id is not null do nothing;

-- During a rolling deploy, old pods still create recipient rows without
-- provenance. Capture only facts observable at that insert time and mark them
-- UNVERIFIED. A source-aware release may upsert the same rows as EXACT; this
-- trigger never promotes, updates, or removes source history.
create or replace function capture_unverified_assignment_recipient_sources()
returns trigger
language plpgsql
as $$
begin
    insert into assignment_recipient_sources (
        assignment_recipient_id,
        assignment_id,
        student_id,
        student_target_id,
        tenant_id,
        source_provenance
    )
    select
        new.id,
        new.assignment_id,
        new.student_id,
        target.id,
        new.tenant_id,
        'UNVERIFIED'
    from assignment_student_targets target
    where target.assignment_id = new.assignment_id
      and target.student_id = new.student_id
      and target.tenant_id = new.tenant_id
    on conflict (assignment_recipient_id, student_target_id)
        where student_target_id is not null do nothing;

    insert into assignment_recipient_sources (
        assignment_recipient_id,
        assignment_id,
        student_id,
        class_target_id,
        tenant_id,
        source_provenance
    )
    select
        new.id,
        new.assignment_id,
        new.student_id,
        target.id,
        new.tenant_id,
        'UNVERIFIED'
    from assignment_class_targets target
    join class_enrollments enrollment
      on enrollment.class_id = target.class_id
     and enrollment.student_id = new.student_id
     and enrollment.tenant_id = new.tenant_id
     and enrollment.status = 'ENROLLED'
     and enrollment.dropped_at is null
    where target.assignment_id = new.assignment_id
      and target.tenant_id = new.tenant_id
    on conflict (assignment_recipient_id, class_target_id)
        where class_target_id is not null do nothing;

    return new;
end;
$$;

drop trigger if exists trg_capture_unverified_assignment_recipient_sources
    on assignment_recipients;

create trigger trg_capture_unverified_assignment_recipient_sources
after insert on assignment_recipients
for each row
execute function capture_unverified_assignment_recipient_sources();

-- A deleted student can leave a historical recipient row because recipients
-- intentionally do not have a student FK. Revoke every live submission source
-- before the student disappears, including sources via surviving class targets.
-- The recipient and its submitted work remain available to administrators.
create or replace function revoke_assignment_recipient_sources_before_student_delete()
returns trigger
language plpgsql
as $$
begin
    delete from assignment_recipient_sources
    where student_id = old.id
      and tenant_id = old.tenant_id;
    return old;
end;
$$;

drop trigger if exists trg_revoke_assignment_recipient_sources_before_student_delete
    on students;

create trigger trg_revoke_assignment_recipient_sources_before_student_delete
before delete on students
for each row
execute function revoke_assignment_recipient_sources_before_student_delete();

alter table assignment_recipient_sources enable row level security;
alter table assignment_recipient_sources force row level security;
drop policy if exists tenant_isolation on assignment_recipient_sources;
create policy tenant_isolation on assignment_recipient_sources
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on assignment_recipient_sources to lumie_app;
grant usage, select on sequence assignment_recipient_sources_id_seq to lumie_app;
