-- Expand file_metadata for centrally managed temporary uploads.
-- Existing rows remain active; no RLS change is required because file_metadata is already tenant-scoped.

alter table if exists file_metadata
    add column if not exists temporary_object_key varchar(500),
    add column if not exists lifecycle_status varchar(20) not null default 'ACTIVE',
    add column if not exists uploaded_by_user_id bigint,
    add column if not exists expires_at timestamptz;

alter table file_metadata
    add column if not exists completed_etag varchar(128);

create index if not exists idx_file_metadata_lifecycle_status_expires_at
    on file_metadata (lifecycle_status, expires_at);
