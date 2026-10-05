-- ============================================================================
-- V121: preserve Q&A author history
--
-- Deploy order:
--   1. Deploy a Q&A reader that safely handles a deleted board author
--      (qna_user_id = null) before an identity deletion can exercise the
--      new ON DELETE SET NULL behavior.
--   2. Roll out the staff-only comment API fully. This migration fails if a
--      concurrent legacy writer creates a student-authored comment.
--   3. Apply this migration to preserve board and staff-comment history when
--      their user identities are deleted, while enforcing tenant-safe links.
--
-- Forward-only:
--   This migration preserves every Q&A board and comment. It clears only the
--   three audited development board-author references that cannot be resolved
--   to a same-tenant user; content, timestamps, comments, and answer state are
--   retained. It intentionally does not drop student_id or add an author-role
--   column.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Composite foreign keys require an exact unique parent key. The users primary
-- key guarantees uniqueness of the pair; this index supplies PostgreSQL's
-- required reference shape.
create unique index if not exists idx_users_id_tenant_id
    on users (id, tenant_id);

-- Q&A boards created by users that no longer exist must remain readable as
-- historical records. The audited development rows are exactly
-- (id, tenant_id, qna_user_id) = (134,97,559), (136,97,559), (137,97,559).
-- Do not silently repair any other missing or cross-tenant author.
alter table if exists qna_boards
    alter column qna_user_id drop not null;

do $$
begin
    if exists (
        select 1
        from qna_boards board
        where board.qna_user_id is not null
          and not exists (
              select 1
              from users author
              where author.id = board.qna_user_id
                and author.tenant_id = board.tenant_id
          )
          and (board.id, board.tenant_id, board.qna_user_id) not in (
              (134::bigint, 97::bigint, 559::bigint),
              (136::bigint, 97::bigint, 559::bigint),
              (137::bigint, 97::bigint, 559::bigint)
          )
    ) then
        raise exception
            'cannot preserve Q&A board history: found an unexpected missing or cross-tenant board author';
    end if;

    if exists (
        select 1
        from qna_comments comment
        where comment.student_id is not null
    ) then
        raise exception
            'cannot preserve Q&A comment history: student-authored comments remain after the staff-only API rollout';
    end if;
end $$;

update qna_boards board
   set qna_user_id = null
 where (board.id, board.tenant_id, board.qna_user_id) in (
           (134::bigint, 97::bigint, 559::bigint),
           (136::bigint, 97::bigint, 559::bigint),
           (137::bigint, 97::bigint, 559::bigint)
       )
   and not exists (
       select 1
       from users author
       where author.id = board.qna_user_id
         and author.tenant_id = board.tenant_id
   );

-- Fail closed rather than adding a tenant-safe FK to any remaining invalid
-- reference. The repair above is limited to the audited historical tuples;
-- any other inconsistent state must be investigated before this contract runs.
do $$
begin
    if exists (
        select 1
        from qna_boards board
        where board.qna_user_id is not null
          and not exists (
              select 1
              from users author
              where author.id = board.qna_user_id
                and author.tenant_id = board.tenant_id
          )
    ) then
        raise exception
            'cannot preserve Q&A board history: an author is missing or belongs to another tenant';
    end if;

    if exists (
        select 1
        from qna_comments comment
        where comment.user_id is not null
          and not exists (
              select 1
              from users author
              where author.id = comment.user_id
                and author.tenant_id = comment.tenant_id
          )
    ) then
        raise exception
            'cannot replace Q&A comment author FK: an author is missing or belongs to another tenant';
    end if;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'qna_boards_qna_user_tenant_fkey'
          and conrelid = 'qna_boards'::regclass
    ) then
        alter table qna_boards
            add constraint qna_boards_qna_user_tenant_fkey
            foreign key (qna_user_id, tenant_id)
            references users (id, tenant_id)
            on delete set null (qna_user_id);
    end if;
end $$;

-- Replace the ID-only legacy FK only after the cross-tenant preflight above.
-- Deleting a staff user keeps the comment and clears only its former author.
alter table if exists qna_comments
    drop constraint if exists qna_comments_user_id_fkey;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'qna_comments_user_tenant_fkey'
          and conrelid = 'qna_comments'::regclass
    ) then
        alter table qna_comments
            add constraint qna_comments_user_tenant_fkey
            foreign key (user_id, tenant_id)
            references users (id, tenant_id)
            on delete set null (user_id);
    end if;
end $$;

-- Do not add CHECK (student_id is null) or drop student_id here. Apply this
-- migration only after the staff-only API is fully rolled out; the preflight
-- above detects any legacy or concurrent student-comment write without
-- changing the preserved schema shape.
