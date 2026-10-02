-- =====================================================================
-- V112: expand content audience scope and class-target foreign keys
--
-- Deploy order:
--   Apply V109--V111 (ERD-04 assignment expand migrations) before this
--   migration. Deploy the ERD-05 application after V112. The application
--   must treat LEGACY_UNSPECIFIED announcements as staff-only, and must use
--   only ALL or TARGETED for newly-created announcements.
--
-- Audience backfill:
--   A legacy announcement with at least one class target has evidence that
--   it was targeted and is marked TARGETED. A legacy announcement with no
--   target has no reliable evidence that it was intentionally academy-wide,
--   so it is marked LEGACY_UNSPECIFIED rather than being made public. The
--   default keeps an old application pod fail-closed while it is rolling.
--   An old-pod target insert changes the scope to TARGETED, even if that
--   announcement had previously been ALL; explicit target selection must
--   not leave a restricted notice academy-wide during a rolling deploy.
--   Deleting the final target never changes TARGETED to ALL.
--
-- Class target integrity:
--   Class-target rows receive tenant-safe FKs. They cascade only the target
--   link when a class is deleted, never the announcement or lecture. The
--   known historical orphan lecture target is deliberately retained: NOT
--   VALID FKs enforce all new writes and class deletes without deleting
--   content, while a later data-reconciliation migration can classify or
--   repair that orphan before validation.
--
-- Forward-only:
--   lectures.class_id remains as a compatibility column. This migration
--   replaces any class FK on it with ON DELETE SET NULL; it must not be
--   dropped until all readers and writers use lecture_class_targets.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Needed by the tenant-safe class references below. The primary key already
-- makes the pair unique; this index supplies PostgreSQL's composite FK target.
create unique index if not exists idx_classes_id_tenant_id
    on classes (id, tenant_id);

-- This is an expand column. LEGACY_UNSPECIFIED is an intentional migration
-- sentinel, not a product-visible public audience scope.
alter table if exists announcements
    add column if not exists audience_scope varchar(32) not null default 'LEGACY_UNSPECIFIED';

update announcements a
set audience_scope = 'TARGETED'
where a.audience_scope = 'LEGACY_UNSPECIFIED'
  and exists (
      select 1
      from announcement_class_targets target
      where target.announcement_id = a.id
        and target.tenant_id = a.tenant_id
  );

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'announcements_audience_scope_check'
          and conrelid = 'announcements'::regclass
    ) then
        alter table announcements
            add constraint announcements_audience_scope_check
            check (audience_scope in ('ALL', 'TARGETED', 'LEGACY_UNSPECIFIED'));
    end if;
end $$;

comment on column announcements.audience_scope is
    'Explicit student audience: ALL or TARGETED. LEGACY_UNSPECIFIED is a fail-closed migration sentinel pending staff classification.';

comment on table announcement_class_targets is
    'Class audience links for TARGETED announcements. Zero links never imply academy-wide visibility; use announcements.audience_scope.';

comment on column announcement_class_targets.class_id is
    'Tenant-safe class reference; deleting a class removes only this target link.';

-- Old application pods create an announcement first and its target rows
-- second. An inserted target is positive evidence of a restricted audience;
-- this also covers an old pod narrowing a new ALL announcement during rollout.
create or replace function promote_legacy_announcement_audience_scope()
returns trigger
language plpgsql
as $$
begin
    update announcements
       set audience_scope = 'TARGETED'
     where id = new.announcement_id
       and tenant_id = new.tenant_id
       and audience_scope <> 'TARGETED';
    return new;
end;
$$;

drop trigger if exists trg_promote_legacy_announcement_audience_scope
    on announcement_class_targets;

create trigger trg_promote_legacy_announcement_audience_scope
after insert on announcement_class_targets
for each row
execute function promote_legacy_announcement_audience_scope();

-- Retain historical target rows, including any that cannot currently be
-- joined to classes. NOT VALID FKs still enforce every new or changed row.
do $$
declare
    constraint_name text;
begin
    for constraint_name in
        select conname
        from pg_constraint
        where contype = 'f'
          and conrelid = 'announcement_class_targets'::regclass
          and confrelid = 'classes'::regclass
          and conname <> 'announcement_class_targets_class_tenant_fkey'
    loop
        execute format(
            'alter table announcement_class_targets drop constraint if exists %I',
            constraint_name
        );
    end loop;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'announcement_class_targets_class_tenant_fkey'
          and conrelid = 'announcement_class_targets'::regclass
    ) then
        alter table announcement_class_targets
            add constraint announcement_class_targets_class_tenant_fkey
            foreign key (class_id, tenant_id)
            references classes (id, tenant_id)
            on delete cascade
            not valid;
    end if;
end $$;

-- Remove every legacy class FK on the compatibility scalar, including an
-- unknown historical constraint name, before adding the non-destructive one.
do $$
declare
    constraint_name text;
begin
    for constraint_name in
        select conname
        from pg_constraint
        where contype = 'f'
          and conrelid = 'lectures'::regclass
          and confrelid = 'classes'::regclass
          and conname <> 'lectures_class_tenant_fkey'
    loop
        execute format(
            'alter table lectures drop constraint if exists %I',
            constraint_name
        );
    end loop;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'lectures_class_tenant_fkey'
          and conrelid = 'lectures'::regclass
    ) then
        alter table lectures
            add constraint lectures_class_tenant_fkey
            foreign key (class_id, tenant_id)
            references classes (id, tenant_id)
            on delete set null (class_id)
            not valid;
    end if;
end $$;

do $$
declare
    constraint_name text;
begin
    for constraint_name in
        select conname
        from pg_constraint
        where contype = 'f'
          and conrelid = 'lecture_class_targets'::regclass
          and confrelid = 'classes'::regclass
          and conname <> 'lecture_class_targets_class_tenant_fkey'
    loop
        execute format(
            'alter table lecture_class_targets drop constraint if exists %I',
            constraint_name
        );
    end loop;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'lecture_class_targets_class_tenant_fkey'
          and conrelid = 'lecture_class_targets'::regclass
    ) then
        alter table lecture_class_targets
            add constraint lecture_class_targets_class_tenant_fkey
            foreign key (class_id, tenant_id)
            references classes (id, tenant_id)
            on delete cascade
            not valid;
    end if;
end $$;

comment on constraint announcement_class_targets_class_tenant_fkey
    on announcement_class_targets is
    'Tenant-safe class target. Class deletion removes only this target link; constraint is not valid until historical orphan reconciliation.';

comment on constraint lecture_class_targets_class_tenant_fkey
    on lecture_class_targets is
    'Tenant-safe class target. Class deletion removes only this target link; constraint is not valid until the retained historical orphan is reconciled.';

comment on column lecture_class_targets.class_id is
    'Tenant-safe class reference; this target relation is the canonical student audience.';

comment on constraint lectures_class_tenant_fkey
    on lectures is
    'Compatibility scalar relationship. Class deletion sets class_id null; lecture_class_targets is the long-term audience source.';
