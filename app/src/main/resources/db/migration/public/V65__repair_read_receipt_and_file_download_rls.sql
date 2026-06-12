-- Repair read/download tracking tables introduced after the RLS baseline.
-- Keep V62-V64 immutable; this migration only applies forward fixes.

set lock_timeout = '5s';
set statement_timeout = '60s';

alter table announcement_reads
    add column if not exists tenant_id bigint;

update announcement_reads ar
set tenant_id = a.tenant_id
from announcements a
where ar.announcement_id = a.id
  and ar.tenant_id is null;

-- Rows that still have no tenant after joining announcements are dangling
-- reads for announcements that no longer exist. They cannot be made tenant-safe
-- and would block the NOT NULL + RLS repair, so discard only those orphans.
delete from announcement_reads
where tenant_id is null;

alter table announcement_reads
    alter column tenant_id set not null;

create unique index if not exists idx_announcements_id_tenant_id
    on announcements (id, tenant_id);

alter table announcement_reads
    drop constraint if exists announcement_reads_announcement_id_fkey;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'announcement_reads_announcement_tenant_fkey'
          and conrelid = 'announcement_reads'::regclass
    ) then
        alter table announcement_reads
            add constraint announcement_reads_announcement_tenant_fkey
            foreign key (announcement_id, tenant_id)
            references announcements(id, tenant_id)
            on delete cascade;
    end if;
end $$;

create index if not exists idx_announcement_reads_tenant_announcement
    on announcement_reads (tenant_id, announcement_id, read_at desc);

alter table announcement_reads enable row level security;
alter table announcement_reads force row level security;

do $$
begin
    if not exists (
        select 1
        from pg_policies
        where schemaname = current_schema()
          and tablename = 'announcement_reads'
          and policyname = 'tenant_isolation'
    ) then
        create policy tenant_isolation on announcement_reads
            using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
            with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);
    end if;
end $$;

-- V62 created textbook_reads, but the textbook aggregate/controller was removed
-- and no Java code writes or reads this table. There is no surviving target to
-- migrate these read receipts into, so drop this dead table without CASCADE.
drop table if exists textbook_reads;

-- Remove download rows whose file metadata disappeared or belongs to another
-- tenant before adding the tenant-safe FK. Such rows cannot be served or shown
-- correctly and are unsafe to keep under RLS.
delete from file_download fd
where not exists (
    select 1
    from file_metadata fm
    where fm.id = fd.file_id
      and fm.tenant_id = fd.tenant_id
);

create unique index if not exists idx_file_metadata_id_tenant_id
    on file_metadata (id, tenant_id);

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'file_download_file_metadata_tenant_fkey'
          and conrelid = 'file_download'::regclass
    ) then
        alter table file_download
            add constraint file_download_file_metadata_tenant_fkey
            foreign key (file_id, tenant_id)
            references file_metadata(id, tenant_id)
            on delete cascade;
    end if;
end $$;

create index if not exists idx_file_download_tenant_file_updated
    on file_download (tenant_id, file_id, updated_at desc);

alter table file_download enable row level security;
alter table file_download force row level security;

do $$
begin
    if not exists (
        select 1
        from pg_policies
        where schemaname = current_schema()
          and tablename = 'file_download'
          and policyname = 'tenant_isolation'
    ) then
        create policy tenant_isolation on file_download
            using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
            with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);
    end if;
end $$;
