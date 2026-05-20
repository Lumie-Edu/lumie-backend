-- Audit item D.8: add optimistic-locking `version` column to the remaining
-- user-mutable DOMAIN entities. Distinct from V17, which covered only the
-- platform-level tables (tenants, users).
-- ADD COLUMN NOT NULL DEFAULT <constant> is a metadata-only fast-path in
-- Postgres 11+ (no table rewrite).

ALTER TABLE announcements             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE exam_templates            ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE counseling_reservations   ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE reviews                   ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE counseling_schedules      ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sms_templates             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE toggles                   ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE textbooks                 ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE textbook_folders          ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE file_metadata             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
