-- =====================================================================
-- V57: lecture multi-class student visibility targets
--
-- Purpose:
--   Introduce a tenant-scoped join table that maps lectures to the
--   classes allowed to see them. This is the expand step for multi-class
--   lecture visibility; lectures.class_id remains in place because the
--   current application code still reads and writes that scalar column.
--
-- Backfill:
--   Existing lectures with a non-null class_id are copied into the new
--   join table when the referenced class still exists. The backfill is
--   idempotent via on conflict do nothing.
--
-- Forward-only:
--   No rollback is provided. Dropping lectures.class_id must happen only
--   in a later contract migration after all code paths stop depending on
--   it.
--
-- Cross-module FK note:
--   class_id references classes.id semantically, but no database-level FK is
--   declared. Lecture validates targets through the classroom internal API.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '30s';

create table if not exists lecture_class_targets (
    id           bigserial     not null,
    lecture_id   bigint        not null,
    class_id     bigint        not null,
    tenant_id    bigint        not null,
    version      bigint        not null default 0,
    created_at   timestamptz   not null default now(),
    updated_at   timestamptz   not null default now(),
    constraint lecture_class_targets_pkey primary key (id),
    constraint lecture_class_targets_lecture_class_key unique (lecture_id, class_id),
    constraint lecture_class_targets_lecture_fkey foreign key (lecture_id)
        references lectures (id) on delete cascade
);

create index if not exists idx_lecture_class_targets_lecture_id
    on lecture_class_targets (lecture_id);

create index if not exists idx_lecture_class_targets_class_id
    on lecture_class_targets (class_id);

create index if not exists idx_lecture_class_targets_tenant_id
    on lecture_class_targets (tenant_id);

create index if not exists idx_lecture_class_targets_tenant_class_id
    on lecture_class_targets (tenant_id, class_id);

comment on table lecture_class_targets is 'Tenant-scoped lecture-to-class mappings that define which classes can see a lecture as a student-visible target.';
comment on column lecture_class_targets.lecture_id is 'Lecture that owns the student visibility target.';
comment on column lecture_class_targets.class_id is 'References classes.id semantically; no DB FK because lecture and classroom are separate module boundaries.';

insert into lecture_class_targets (
    lecture_id,
    class_id,
    tenant_id
)
select
    l.id,
    l.class_id,
    l.tenant_id
from lectures l
join classes c
    on c.id = l.class_id
   and c.tenant_id = l.tenant_id
where l.class_id is not null
on conflict (lecture_id, class_id) do nothing;

alter table lecture_class_targets enable row level security;
alter table lecture_class_targets force  row level security;
drop policy if exists tenant_isolation on lecture_class_targets;
create policy tenant_isolation on lecture_class_targets
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on lecture_class_targets to lumie_app;
grant usage, select on sequence lecture_class_targets_id_seq to lumie_app;
