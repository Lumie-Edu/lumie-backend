-- =====================================================================
-- V85: keep active assignments exam-backed only
--
-- Purpose:
--   The assignment workflow now distributes a freshly created exam sheet
--   with assignment-specific deadline and visibility. Legacy non-exam rows
--   may remain closed for historical reads, but active assignments must be
--   EXAM_MANUAL / EXAM_AUTO and linked to an exam.
--
-- Forward-only:
--   Existing non-exam or unlinked assignment rows are closed before adding
--   the active-row guard. No previous Flyway migration is modified.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

update assignments
set status = 'CLOSED'
where status = 'ACTIVE'
  and (
      submission_mode <> 'EXAM_MANUAL'
      or evaluation_mode <> 'EXAM_AUTO'
      or linked_exam_id is null
      or show_grade is true
  );

alter table if exists assignments
    alter column submission_mode set default 'EXAM_MANUAL',
    alter column evaluation_mode set default 'EXAM_AUTO',
    alter column show_score set default true,
    alter column show_pass_fail set default true,
    alter column show_grade set default false,
    alter column show_question_results set default true,
    alter column show_correct_answers set default false;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'assignments_active_exam_only_check'
          and conrelid = 'assignments'::regclass
    ) then
        alter table assignments
            add constraint assignments_active_exam_only_check
            check (
                status = 'CLOSED'
                or (
                    submission_mode = 'EXAM_MANUAL'
                    and evaluation_mode = 'EXAM_AUTO'
                    and linked_exam_id is not null
                    and show_score is true
                    and show_pass_fail is true
                    and show_grade is false
                    and show_question_results is true
                )
            );
    end if;
end $$;
