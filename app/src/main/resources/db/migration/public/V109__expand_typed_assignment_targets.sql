-- ============================================================================
-- V109: expand typed assignment targets
--
-- Purpose:
--   Replace the polymorphic assignment_targets relationship with typed,
--   tenant-safe class and student target tables while keeping the legacy table
--   and assignments.class_id available during the application rollout.
--
-- Deploy order:
--   1. Apply this migration while releases that write assignment_targets or
--      assignments.class_id can still run.
--   2. Deploy the release that reads the typed tables and dual-writes legacy
--      assignment_targets until every previous-release pod is retired.
--   3. After confirming no runtime reader or writer uses the legacy shapes,
--      remove them in a later contract migration.
--
-- Backfill:
--   Existing legacy targets and compatibility class_id values are copied
--   idempotently. The migration fails before backfill if a legacy target is
--   orphaned or belongs to another tenant; it never deletes ambiguous rows.
--
-- Forward-only:
--   This is an expand migration. assignment_targets and assignments.class_id
--   intentionally remain in place.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Composite foreign keys require a unique parent key. ids are already primary
-- keys, and these indexes provide the exact tenant-safe reference shape.
create unique index if not exists idx_classes_id_tenant_id
    on classes (id, tenant_id);

create unique index if not exists idx_students_id_tenant_id
    on students (id, tenant_id);

create table if not exists assignment_class_targets (
    id            bigserial    not null,
    assignment_id bigint       not null,
    class_id      bigint       not null,
    tenant_id     bigint       not null,
    version       bigint       not null default 0,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),
    constraint assignment_class_targets_pkey primary key (id)
);

create table if not exists assignment_student_targets (
    id            bigserial    not null,
    assignment_id bigint       not null,
    student_id    bigint       not null,
    tenant_id     bigint       not null,
    version       bigint       not null default 0,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),
    constraint assignment_student_targets_pkey primary key (id)
);

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_class_targets_assignment_class_key'
          and conrelid = 'assignment_class_targets'::regclass
    ) then
        alter table assignment_class_targets
            add constraint assignment_class_targets_assignment_class_key
            unique (assignment_id, class_id);
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_student_targets_assignment_student_key'
          and conrelid = 'assignment_student_targets'::regclass
    ) then
        alter table assignment_student_targets
            add constraint assignment_student_targets_assignment_student_key
            unique (assignment_id, student_id);
    end if;
end $$;

create index if not exists idx_assignment_class_targets_tenant_assignment_id
    on assignment_class_targets (tenant_id, assignment_id);

create index if not exists idx_assignment_class_targets_tenant_class_id
    on assignment_class_targets (tenant_id, class_id);

create index if not exists idx_assignment_student_targets_tenant_assignment_id
    on assignment_student_targets (tenant_id, assignment_id);

create index if not exists idx_assignment_student_targets_tenant_student_id
    on assignment_student_targets (tenant_id, student_id);

-- Fail closed rather than silently losing a legacy audience during the
-- backfill. Existing V82 assignment FKs should make the first check empty,
-- but it also protects partially migrated databases.
do $$
begin
    if exists (
        select 1
        from assignment_targets target
        left join assignments assignment
          on assignment.id = target.assignment_id
         and assignment.tenant_id = target.tenant_id
        where assignment.id is null
    ) then
        raise exception
            'cannot expand assignment targets: legacy target has no same-tenant assignment';
    end if;

    if exists (
        select 1
        from assignment_targets
        where target_type not in ('CLASS', 'STUDENT')
    ) then
        raise exception
            'cannot expand assignment targets: legacy target has an unsupported target type';
    end if;

    if exists (
        select 1
        from assignment_targets target
        left join classes class_target
          on class_target.id = target.target_id
         and class_target.tenant_id = target.tenant_id
        where target.target_type = 'CLASS'
          and class_target.id is null
    ) then
        raise exception
            'cannot expand assignment targets: class target is missing or belongs to another tenant';
    end if;

    if exists (
        select 1
        from assignment_targets target
        left join students student_target
          on student_target.id = target.target_id
         and student_target.tenant_id = target.tenant_id
        where target.target_type = 'STUDENT'
          and student_target.id is null
    ) then
        raise exception
            'cannot expand assignment targets: student target is missing or belongs to another tenant';
    end if;

    if exists (
        select 1
        from assignments assignment
        left join classes class_target
          on class_target.id = assignment.class_id
         and class_target.tenant_id = assignment.tenant_id
        where assignment.class_id is not null
          and class_target.id is null
    ) then
        raise exception
            'cannot expand assignment targets: compatibility class_id is missing or belongs to another tenant';
    end if;
end $$;

-- Backfill is idempotent. class_id is included because releases before V81
-- treated it as the only assignment audience and some partial environments may
-- have that value without a matching legacy target row.
insert into assignment_class_targets (assignment_id, class_id, tenant_id)
select target.assignment_id, target.target_id, target.tenant_id
from assignment_targets target
where target.target_type = 'CLASS'
on conflict (assignment_id, class_id) do nothing;

insert into assignment_student_targets (assignment_id, student_id, tenant_id)
select target.assignment_id, target.target_id, target.tenant_id
from assignment_targets target
where target.target_type = 'STUDENT'
on conflict (assignment_id, student_id) do nothing;

insert into assignment_class_targets (assignment_id, class_id, tenant_id)
select assignment.id, assignment.class_id, assignment.tenant_id
from assignments assignment
where assignment.class_id is not null
on conflict (assignment_id, class_id) do nothing;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_class_targets_assignment_tenant_fkey'
          and conrelid = 'assignment_class_targets'::regclass
    ) then
        alter table assignment_class_targets
            add constraint assignment_class_targets_assignment_tenant_fkey
            foreign key (assignment_id, tenant_id)
            references assignments (id, tenant_id)
            on delete cascade;
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_class_targets_class_tenant_fkey'
          and conrelid = 'assignment_class_targets'::regclass
    ) then
        alter table assignment_class_targets
            add constraint assignment_class_targets_class_tenant_fkey
            foreign key (class_id, tenant_id)
            references classes (id, tenant_id)
            on delete cascade;
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_student_targets_assignment_tenant_fkey'
          and conrelid = 'assignment_student_targets'::regclass
    ) then
        alter table assignment_student_targets
            add constraint assignment_student_targets_assignment_tenant_fkey
            foreign key (assignment_id, tenant_id)
            references assignments (id, tenant_id)
            on delete cascade;
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_student_targets_student_tenant_fkey'
          and conrelid = 'assignment_student_targets'::regclass
    ) then
        alter table assignment_student_targets
            add constraint assignment_student_targets_student_tenant_fkey
            foreign key (student_id, tenant_id)
            references students (id, tenant_id)
            on delete cascade;
    end if;
end $$;

-- Keep the compatibility scalar nullable and tenant-safe. Deleting a class
-- clears only this legacy pointer; it cannot cascade to assignments or their
-- submissions.
alter table if exists assignments
    alter column class_id drop not null;

alter table if exists assignments
    drop constraint if exists assignments_class_id_fkey;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignments_class_tenant_fkey'
          and conrelid = 'assignments'::regclass
    ) then
        alter table assignments
            add constraint assignments_class_tenant_fkey
            foreign key (class_id, tenant_id)
            references classes (id, tenant_id)
            on delete set null (class_id);
    end if;
end $$;

-- The legacy table remains the writer for old pods. Mirror its mutations into
-- typed targets so a rolling deployment cannot create an audience visible only
-- to the old code path. A typed-table writer still needs to dual-write the
-- legacy table until the contract migration removes legacy readers.
create or replace function sync_legacy_assignment_target_to_typed_targets()
returns trigger
language plpgsql
as $$
begin
    if tg_op = 'DELETE' or tg_op = 'UPDATE' then
        if old.target_type = 'CLASS' then
            delete from assignment_class_targets
            where assignment_id = old.assignment_id
              and class_id = old.target_id
              and tenant_id = old.tenant_id;
        elsif old.target_type = 'STUDENT' then
            delete from assignment_student_targets
            where assignment_id = old.assignment_id
              and student_id = old.target_id
              and tenant_id = old.tenant_id;
        end if;
    end if;

    if tg_op = 'DELETE' then
        return old;
    end if;

    if new.target_type = 'CLASS' then
        insert into assignment_class_targets (assignment_id, class_id, tenant_id)
        values (new.assignment_id, new.target_id, new.tenant_id)
        on conflict (assignment_id, class_id) do nothing;
    elsif new.target_type = 'STUDENT' then
        insert into assignment_student_targets (assignment_id, student_id, tenant_id)
        values (new.assignment_id, new.target_id, new.tenant_id)
        on conflict (assignment_id, student_id) do nothing;
    else
        raise exception 'unsupported legacy assignment target type: %', new.target_type;
    end if;

    return new;
end;
$$;

drop trigger if exists trg_sync_legacy_assignment_target_to_typed_targets
    on assignment_targets;

create trigger trg_sync_legacy_assignment_target_to_typed_targets
after insert or update or delete on assignment_targets
for each row
execute function sync_legacy_assignment_target_to_typed_targets();

-- Old pods can still create an assignment through class_id alone. Add that
-- class to the typed model. An update never removes another typed target: the
-- legacy table remains the target source until the application dual-write
-- release takes over target replacement.
create or replace function sync_legacy_assignment_class_to_typed_target()
returns trigger
language plpgsql
as $$
begin
    if new.class_id is not null then
        insert into assignment_class_targets (assignment_id, class_id, tenant_id)
        values (new.id, new.class_id, new.tenant_id)
        on conflict (assignment_id, class_id) do nothing;
    end if;

    return new;
end;
$$;

drop trigger if exists trg_sync_legacy_assignment_class_to_typed_target
    on assignments;

create trigger trg_sync_legacy_assignment_class_to_typed_target
after insert or update of class_id on assignments
for each row
execute function sync_legacy_assignment_class_to_typed_target();

-- Legacy class targets are soft references, so clear them before class
-- deletion for old readers too. If that was the final class or student
-- audience, close only the affected assignment and preserve its recipient and
-- submission history.
create or replace function remove_legacy_class_targets_before_class_delete()
returns trigger
language plpgsql
as $$
declare
    affected_assignment_ids bigint[];
begin
    select array_agg(distinct affected.assignment_id)
      into affected_assignment_ids
      from (
          select assignment_id
          from assignment_class_targets
          where tenant_id = old.tenant_id
            and class_id = old.id

          union

          select assignment_id
          from assignment_targets
          where tenant_id = old.tenant_id
            and target_type = 'CLASS'
            and target_id = old.id

          union

          select id
          from assignments
          where tenant_id = old.tenant_id
            and class_id = old.id
      ) affected;

    -- Do this explicitly before deleting the parent so closure does not rely
    -- on the ordering of FK cascade and user-defined delete triggers.
    delete from assignment_class_targets
    where tenant_id = old.tenant_id
      and class_id = old.id;

    delete from assignment_targets
    where tenant_id = old.tenant_id
      and target_type = 'CLASS'
      and target_id = old.id;

    update assignments assignment
       set status = 'CLOSED',
           updated_at = now(),
           version = assignment.version + 1
     where assignment.tenant_id = old.tenant_id
       and assignment.status = 'ACTIVE'
       and assignment.id = any(coalesce(affected_assignment_ids, '{}'::bigint[]))
       and not exists (
           select 1
           from assignment_targets target
           where target.assignment_id = assignment.id
             and target.tenant_id = assignment.tenant_id
       )
       and not exists (
           select 1
           from assignment_class_targets class_target
           where class_target.assignment_id = assignment.id
             and class_target.tenant_id = assignment.tenant_id
       )
       and not exists (
           select 1
           from assignment_student_targets student_target
           where student_target.assignment_id = assignment.id
             and student_target.tenant_id = assignment.tenant_id
       );

    return old;
end;
$$;

drop trigger if exists trg_remove_legacy_class_targets_after_class_delete
    on classes;

drop trigger if exists trg_remove_legacy_class_targets_before_class_delete
    on classes;

create trigger trg_remove_legacy_class_targets_before_class_delete
before delete on classes
for each row
execute function remove_legacy_class_targets_before_class_delete();

-- Direct student targets need the same compatibility cleanup. The typed FK
-- removes new rows, while the legacy table still carries a soft reference for
-- old readers. Deleting a source never deletes recipient or submission history.
create or replace function remove_legacy_student_targets_before_student_delete()
returns trigger
language plpgsql
as $$
declare
    affected_assignment_ids bigint[];
begin
    select array_agg(distinct affected.assignment_id)
      into affected_assignment_ids
      from (
          select assignment_id
          from assignment_student_targets
          where tenant_id = old.tenant_id
            and student_id = old.id

          union

          select assignment_id
          from assignment_targets
          where tenant_id = old.tenant_id
            and target_type = 'STUDENT'
            and target_id = old.id
      ) affected;

    -- Remove the typed source before the parent deletion. When V110 is
    -- present, its source FK cascades only provenance rows; recipients and
    -- submissions remain untouched.
    delete from assignment_student_targets
    where tenant_id = old.tenant_id
      and student_id = old.id;

    delete from assignment_targets
    where tenant_id = old.tenant_id
      and target_type = 'STUDENT'
      and target_id = old.id;

    update assignments assignment
       set status = 'CLOSED',
           updated_at = now(),
           version = assignment.version + 1
     where assignment.tenant_id = old.tenant_id
       and assignment.status = 'ACTIVE'
       and assignment.id = any(coalesce(affected_assignment_ids, '{}'::bigint[]))
       and not exists (
           select 1
           from assignment_targets target
           where target.assignment_id = assignment.id
             and target.tenant_id = assignment.tenant_id
       )
       and not exists (
           select 1
           from assignment_class_targets class_target
           where class_target.assignment_id = assignment.id
             and class_target.tenant_id = assignment.tenant_id
       )
       and not exists (
           select 1
           from assignment_student_targets student_target
           where student_target.assignment_id = assignment.id
             and student_target.tenant_id = assignment.tenant_id
       );

    return old;
end;
$$;

drop trigger if exists trg_remove_legacy_student_targets_before_student_delete
    on students;

create trigger trg_remove_legacy_student_targets_before_student_delete
before delete on students
for each row
execute function remove_legacy_student_targets_before_student_delete();

alter table assignment_class_targets enable row level security;
alter table assignment_class_targets force row level security;
drop policy if exists tenant_isolation on assignment_class_targets;
create policy tenant_isolation on assignment_class_targets
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

alter table assignment_student_targets enable row level security;
alter table assignment_student_targets force row level security;
drop policy if exists tenant_isolation on assignment_student_targets;
create policy tenant_isolation on assignment_student_targets
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on assignment_class_targets to lumie_app;
grant usage, select on sequence assignment_class_targets_id_seq to lumie_app;
grant select, insert, update, delete on assignment_student_targets to lumie_app;
grant usage, select on sequence assignment_student_targets_id_seq to lumie_app;
