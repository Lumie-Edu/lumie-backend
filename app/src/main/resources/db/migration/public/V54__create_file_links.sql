-- =====================================================================
-- V54: create reusable file_links for linking existing textbook files to entities
--
-- Purpose:
--   Introduce a tenant-scoped link table that lets existing files in
--   file_metadata be attached to multiple domain entities such as posts.
--
-- Reusable link semantics:
--   file_links records an association only. Deleting or unlinking a post
--   link must remove the file_links row without deleting the original
--   textbook file in file_metadata. If the underlying file_metadata row is
--   deleted, its link rows are cleaned up via ON DELETE CASCADE.
--
-- Scope:
--   TENANT-SCOPED table with canonical RLS policy for lumie_app.
-- =====================================================================

create unique index if not exists idx_file_metadata_id_tenant_id
    on file_metadata (id, tenant_id);

create table if not exists file_links (
    id                               uuid         not null default gen_random_uuid(),
    tenant_id                        bigint       not null,
    file_id                          uuid         not null,
    entity_type                      varchar(20)  not null,
    entity_id                        bigint       not null,
    version                          bigint       not null default 0,
    created_at                       timestamptz  not null default now(),
    updated_at                       timestamptz  not null default now(),
    constraint file_links_pkey       primary key (id),
    constraint file_links_file_tenant_fkey foreign key (file_id, tenant_id)
        references file_metadata(id, tenant_id) on delete cascade,
    constraint file_links_file_entity_key unique (tenant_id, file_id, entity_type, entity_id)
);

create index if not exists idx_file_links_entity_type_entity_id
    on file_links (entity_type, entity_id);

create index if not exists idx_file_links_tenant_entity_type_entity_id
    on file_links (tenant_id, entity_type, entity_id);

create index if not exists idx_file_links_file_id
    on file_links (file_id);

create index if not exists idx_file_links_tenant_id
    on file_links (tenant_id);

comment on table file_links is 'Reusable links from existing files to domain entities. Removing a link does not delete the original file_metadata row.';
comment on column file_links.file_id is 'Referenced reusable file. Unlinking a post deletes only this link row; the original textbook file remains in file_metadata.';
comment on column file_links.entity_type is 'Linked domain entity type. Initially used for posts, but designed to support other attachable entities.';
comment on column file_links.entity_id is 'Identifier of the linked entity within the entity_type namespace.';

alter table file_links enable row level security;
alter table file_links force  row level security;
drop policy if exists tenant_isolation on file_links;
create policy tenant_isolation on file_links
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on file_links to lumie_app;
