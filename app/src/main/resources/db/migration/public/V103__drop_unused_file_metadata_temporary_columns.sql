-- Contract migration: deploy only after every application instance no longer reads
-- the V101 temporary-upload metadata columns. This forward-only change is irreversible;
-- it preserves all file_metadata rows and their referenced MinIO objects.

drop index if exists idx_file_metadata_lifecycle_status_expires_at;

alter table if exists file_metadata
    drop column if exists temporary_object_key,
    drop column if exists lifecycle_status,
    drop column if exists uploaded_by_user_id,
    drop column if exists expires_at,
    drop column if exists completed_etag;
