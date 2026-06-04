-- =====================================================================
-- V52: Unique constraint — one attendance session per class per day
--
-- Background: the daily pre-creation scheduler was replaced with an
-- on-demand get-or-create endpoint.  The application guards duplicates
-- via existsByClassIdAndSessionDate, but concurrent requests issued
-- before any session exists can race through that check and insert
-- two sessions for the same (class_id, session_date).  This migration
-- adds a database-level uniqueness guarantee so concurrent inserts are
-- serialised by Postgres rather than the application layer.
--
-- Design decisions:
--   • Partial index on class_id IS NOT NULL — ad-hoc all-student
--     sessions (class_id NULL) are not subject to this constraint.
--     Postgres treats NULLs as distinct in unique indexes anyway, but
--     the WHERE clause makes the intent explicit and keeps the index
--     smaller.
--   • (class_id, session_date) without tenant_id — class_id values are
--     drawn from a single global bigserial sequence on classes(id), so
--     a given class_id belongs to exactly one tenant.  tenant_id is
--     therefore redundant in the index key.
--   • Deduplication before index creation — manual createSession calls
--     that raced with the old scheduler may have left duplicate rows.
--     For each conflicting group the row with the smallest id is kept;
--     all others are deleted.  Their attendance_records cascade-delete
--     automatically (FK attendance_records.session_id ON DELETE CASCADE).
--
-- Idempotency: the DELETE is a no-op when no duplicates exist; the
-- index creation uses IF NOT EXISTS and is therefore also a no-op on
-- replay.
--
-- Forward-only.  No rollback is provided.
-- =====================================================================

-- Prevent lock waits from hanging Flyway boot.
set local lock_timeout = '5s';
set local statement_timeout = '30s';

-- -----------------------------------------------------------------------
-- 1. Deduplicate existing data
--
--    For every (class_id, session_date) group that has more than one
--    row (and class_id IS NOT NULL), delete all rows except the one
--    with the smallest id.  attendance_records rows for the deleted
--    sessions cascade-delete automatically.
--
--    The CTE is evaluated once and is idempotent: if no duplicates
--    exist the DELETE matches zero rows and is a no-op.
-- -----------------------------------------------------------------------
delete from attendance_sessions
where id in (
    select id
    from (
        select
            id,
            row_number() over (
                partition by class_id, session_date
                order by id asc
            ) as rn
        from attendance_sessions
        where class_id is not null
    ) ranked
    where rn > 1
);

-- -----------------------------------------------------------------------
-- 2. Create the partial unique index
--
--    One session per real class per calendar day.  Ad-hoc sessions
--    (class_id IS NULL) are excluded from the uniqueness guarantee.
-- -----------------------------------------------------------------------
create unique index if not exists uq_att_sessions_class_date
    on attendance_sessions (class_id, session_date)
    where class_id is not null;
