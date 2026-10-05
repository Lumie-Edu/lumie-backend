-- ============================================================================
-- V120: retire assignment submission result copies
--
-- Deploy order:
--   1. Deploy the result-projection runtime that reads pass/fail and grade
--      only from the same-tenant official exam result.
--   2. Confirm no application reader or writer maps assignment_submissions
--      passed or grade_value.
--   3. Apply this contract migration.
--
-- Forward-only and destructive:
--   assignment_submissions.passed and grade_value were denormalized copies of
--   the official exam result. Answers, score, exam_result_id, recipients, and
--   submission history are intentionally preserved. The preflight fails if a
--   non-null legacy value has no same-tenant official result; it does not
--   compare scores, because score reconciliation is outside this contract.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Use to_jsonb so this guard remains safe if a manually repaired partial run
-- has already removed one of the two legacy columns. A missing key yields
-- NULL; a present non-null value still requires an official, same-tenant
-- exam result before it may be discarded.
do $$
begin
    if to_regclass('public.assignment_submissions') is not null
       and exists (
            select 1
            from assignment_submissions submission
            where (
                    to_jsonb(submission) ->> 'passed' is not null
                    or to_jsonb(submission) ->> 'grade_value' is not null
                  )
              and not exists (
                    select 1
                    from exam_results result
                    where result.id = submission.exam_result_id
                      and result.tenant_id = submission.tenant_id
              )
       ) then
        raise exception
            'cannot retire assignment submission result copies: a non-null legacy pass/grade has no same-tenant official exam result';
    end if;
end $$;

alter table if exists assignment_submissions
    drop column if exists passed,
    drop column if exists grade_value;
