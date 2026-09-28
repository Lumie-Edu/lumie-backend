-- Contract migration: run only after every backend pod no longer reads file_links
-- and all legacy shared attachments have been migrated to direct file_metadata rows.
--
-- This is forward-only and irreversible. It removes only the obsolete association
-- table; file_metadata rows and their MinIO objects are intentionally preserved.
-- The table drop removes its indexes, foreign key, grants, and RLS policy together.

drop table if exists file_links;
