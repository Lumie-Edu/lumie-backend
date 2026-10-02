-- ============================================================================
-- V111: link assignment submissions to recipients
--
-- Purpose:
--   Preserve the submission's recipient identity independently of future
--   enrollment, source-target, or student-deletion changes. The link is
--   additive and nullable so old pods can continue inserting submissions
--   during a rolling deployment.
--
-- Backfill:
--   Non-null student submissions must match exactly one recipient in the same
--   assignment and tenant. The migration fails rather than inventing a link
--   when that evidence is absent. Student-null historical submissions remain
--   unlinked.
--
-- Forward-only:
--   Keep the existing assignment/student uniqueness constraint until all old
--   readers and writers are retired. A later contract migration may require
--   recipient_id after the source-aware release is fully deployed.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

alter table if exists assignment_submissions
    add column if not exists recipient_id bigint;

-- Fail closed before the backfill. The production and development inventory
-- found no unmatched non-null students, but this migration does not rely on
-- that historical observation.
do $$
begin
    if exists (
        select 1
        from assignment_submissions submission
        where submission.student_id is not null
          and not exists (
              select 1
              from assignment_recipients recipient
              where recipient.assignment_id = submission.assignment_id
                and recipient.student_id = submission.student_id
                and recipient.tenant_id = submission.tenant_id
          )
    ) then
        raise exception
            'cannot link assignment submissions: non-null student submission has no same-tenant recipient';
    end if;
end $$;

-- The V110 composite unique index is used here so a linked submission with a
-- non-null student cannot point at a recipient from another assignment,
-- tenant, or student.
update assignment_submissions submission
   set recipient_id = recipient.id
  from assignment_recipients recipient
 where submission.recipient_id is null
   and submission.student_id is not null
   and recipient.assignment_id = submission.assignment_id
   and recipient.student_id = submission.student_id
   and recipient.tenant_id = submission.tenant_id;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_submissions_recipient_assignment_student_tenant_fkey'
          and conrelid = 'assignment_submissions'::regclass
    ) then
        alter table assignment_submissions
            add constraint assignment_submissions_recipient_assignment_student_tenant_fkey
            foreign key (recipient_id, assignment_id, student_id, tenant_id)
            references assignment_recipients (id, assignment_id, student_id, tenant_id)
            on delete set null (recipient_id);
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignment_submissions_assignment_recipient_key'
          and conrelid = 'assignment_submissions'::regclass
    ) then
        alter table assignment_submissions
            add constraint assignment_submissions_assignment_recipient_key
            unique (assignment_id, recipient_id);
    end if;
end $$;

-- Recipient deletion clears only recipient_id. The submission retains its
-- assignment, student, score, and audit history. Assignment deletion continues
-- to use the existing assignment FK cascade for the whole submission record.
-- The four-column FK uses PostgreSQL MATCH SIMPLE semantics: when V66's
-- student-delete FK later nulls student_id, a retained recipient_id remains a
-- valid historical link. Application writes must validate recipient/student
-- identity whenever student_id is non-null.
