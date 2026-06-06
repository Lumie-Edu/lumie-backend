-- =====================================================================
-- V55: create announcement_class_targets
--
-- Purpose:
--   Add optional class audience targeting for announcements/resources.
--   No backfill is performed: announcements with no target rows are
--   interpreted by application code as academy-wide.
--
-- Scope:
--   TENANT-SCOPED table with canonical RLS policy for lumie_app.
--
-- Cross-module FK note:
--   class_id references classes.id semantically, but no database-level FK is
--   declared. Content validates targets through the classroom internal API.
-- =====================================================================

create table if not exists announcement_class_targets (
    id                 bigserial    not null,
    tenant_id          bigint       not null,
    announcement_id    bigint       not null,
    class_id           bigint       not null,
    version            bigint       not null default 0,
    created_at         timestamptz  not null default now(),
    updated_at         timestamptz  not null default now(),
    constraint announcement_class_targets_pkey primary key (id),
    constraint announcement_class_targets_announcement_fkey foreign key (announcement_id)
        references announcements(id) on delete cascade,
    constraint announcement_class_targets_announcement_class_key unique (announcement_id, class_id)
);

create index if not exists idx_announcement_class_targets_announcement_id
    on announcement_class_targets (announcement_id);

create index if not exists idx_announcement_class_targets_class_id
    on announcement_class_targets (class_id);

create index if not exists idx_announcement_class_targets_tenant_class
    on announcement_class_targets (tenant_id, class_id);

comment on table announcement_class_targets is 'Optional class audience targets for announcements/resources. No rows means academy-wide.';
comment on column announcement_class_targets.class_id is 'References classes.id semantically; no DB FK because content and classroom are separate module boundaries.';

alter table announcement_class_targets enable row level security;
alter table announcement_class_targets force  row level security;
drop policy if exists tenant_isolation on announcement_class_targets;
create policy tenant_isolation on announcement_class_targets
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on announcement_class_targets to lumie_app;
grant usage, select on sequence announcement_class_targets_id_seq to lumie_app;
