-- V33: Rewrite cross-schema FK constraints from public.users(id) to the
-- tenant-local users(id) table created in V31 and backfilled in V32.
--
-- Background: 6 tables in this tenant schema previously held cross-schema FK
-- constraints into public.users. The user-storage split moves all non-OWNER
-- identity (and the OWNER's full record) into tenant_*.users, leaving
-- public.users as a slim OWNER-only directory. After this migration:
--   - All user references stay within the tenant schema (no cross-schema FK).
--   - Existing FK target ids remain valid because V32 preserved id values.
--   - New writes via the application layer go to tenant_*.users only.
--
-- Cleanup of leftover non-OWNER rows in public.users and slimming the public
-- users columns is deferred to a separate migration.

-- ---------------------------------------------------------------------------
-- admins.user_id  (NOT NULL, ON DELETE CASCADE)
-- ---------------------------------------------------------------------------
ALTER TABLE admins DROP CONSTRAINT IF EXISTS admins_user_id_fkey;
ALTER TABLE admins
    ADD CONSTRAINT admins_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- students.user_id  (NOT NULL, ON DELETE CASCADE)
-- ---------------------------------------------------------------------------
ALTER TABLE students DROP CONSTRAINT IF EXISTS students_user_id_fkey;
ALTER TABLE students
    ADD CONSTRAINT students_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- counseling_schedules.user_id  (NOT NULL, ON DELETE CASCADE)
-- ---------------------------------------------------------------------------
ALTER TABLE counseling_schedules DROP CONSTRAINT IF EXISTS counseling_schedules_user_id_fkey;
ALTER TABLE counseling_schedules
    ADD CONSTRAINT counseling_schedules_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- counseling_reservations.user_id  (NOT NULL, ON DELETE CASCADE)
-- ---------------------------------------------------------------------------
ALTER TABLE counseling_reservations DROP CONSTRAINT IF EXISTS counseling_reservations_user_id_fkey;
ALTER TABLE counseling_reservations
    ADD CONSTRAINT counseling_reservations_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- announcements.author_id  (nullable, ON DELETE SET NULL)
-- ---------------------------------------------------------------------------
ALTER TABLE announcements DROP CONSTRAINT IF EXISTS announcements_author_id_fkey;
ALTER TABLE announcements
    ADD CONSTRAINT announcements_author_id_fkey
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;

-- ---------------------------------------------------------------------------
-- qna_comments.user_id  (nullable, ON DELETE SET NULL)
-- ---------------------------------------------------------------------------
ALTER TABLE qna_comments DROP CONSTRAINT IF EXISTS qna_comments_user_id_fkey;
ALTER TABLE qna_comments
    ADD CONSTRAINT qna_comments_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
