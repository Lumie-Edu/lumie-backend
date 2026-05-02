-- V24: Add cascade behavior for teacher (admin/user) deletion
--
-- Before this migration, several tables held teacher references with no FK at
-- all, leaving dangling rows after a teacher was hard-deleted (e.g. the
-- "User not found with id" error on the schedule tab). This migration:
--   1. Cleans up existing orphans so the new constraints can be created.
--   2. Adds proper FK constraints with cascade semantics:
--        - counseling_schedules / counseling_reservations -> ON DELETE CASCADE
--          (a personal counseling slot has no meaning without its teacher)
--        - announcements / qna_comments -> ON DELETE SET NULL
--          (records stay; the application falls back to "deleted user" for the author)
--   3. Drops NOT NULL on announcements.author_id so SET NULL is legal.
--
-- All affected columns store public.users.id (confirmed by V20 rename comment
-- and by frontend payloads passing user.id as authorId), so FKs target
-- public.users(id) directly.

-- ---------------------------------------------------------------------------
-- counseling_schedules.user_id (NOT NULL) -> public.users(id) CASCADE
-- ---------------------------------------------------------------------------

DELETE FROM counseling_schedules
WHERE user_id NOT IN (SELECT id FROM public.users);

ALTER TABLE counseling_schedules
    ADD CONSTRAINT counseling_schedules_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- counseling_reservations.user_id (NOT NULL) -> public.users(id) CASCADE
-- ---------------------------------------------------------------------------

DELETE FROM counseling_reservations
WHERE user_id NOT IN (SELECT id FROM public.users);

ALTER TABLE counseling_reservations
    ADD CONSTRAINT counseling_reservations_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- announcements.author_id (was NOT NULL) -> public.users(id) SET NULL
-- ---------------------------------------------------------------------------

ALTER TABLE announcements
    ALTER COLUMN author_id DROP NOT NULL;

UPDATE announcements
SET author_id = NULL
WHERE author_id IS NOT NULL
  AND author_id NOT IN (SELECT id FROM public.users);

ALTER TABLE announcements
    ADD CONSTRAINT announcements_author_id_fkey
    FOREIGN KEY (author_id) REFERENCES public.users(id) ON DELETE SET NULL;

-- ---------------------------------------------------------------------------
-- qna_comments.user_id (nullable) -> public.users(id) SET NULL
-- ---------------------------------------------------------------------------

UPDATE qna_comments
SET user_id = NULL
WHERE user_id IS NOT NULL
  AND user_id NOT IN (SELECT id FROM public.users);

ALTER TABLE qna_comments
    ADD CONSTRAINT qna_comments_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;
