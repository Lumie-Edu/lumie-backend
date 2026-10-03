-- ============================================================================
-- V114: enforce exam-job tenant integrity
--
-- Purpose:
--   Make the persisted OMR grading and report-generation job states match the
--   JobStatus enum, and prevent a job or OMR submission from referencing an
--   exam owned by a different tenant. OMR submissions already have separate
--   tenant-safe job and exam foreign keys; the additional triple foreign key
--   closes the same-tenant/different-exam gap.
--
-- Forward-only:
--   Existing data is checked before constraints are added. A mismatch fails
--   closed instead of being rewritten, because an OMR source record must not
--   be silently reassigned to another exam.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
begin
    if exists (
        select 1
        from omr_grading_jobs job
        left join exams exam
          on exam.id = job.exam_id
         and exam.tenant_id = job.tenant_id
        where exam.id is null
    ) then
        raise exception
            'cannot enforce OMR job tenant integrity: an OMR grading job has no same-tenant exam';
    end if;

    if exists (
        select 1
        from report_generation_jobs job
        left join exams exam
          on exam.id = job.exam_id
         and exam.tenant_id = job.tenant_id
        where exam.id is null
    ) then
        raise exception
            'cannot enforce report job tenant integrity: a report generation job has no same-tenant exam';
    end if;

    if exists (
        select 1
        from omr_submissions submission
        left join omr_grading_jobs job
          on job.id = submission.job_id
         and job.exam_id = submission.exam_id
         and job.tenant_id = submission.tenant_id
        where job.id is null
    ) then
        raise exception
            'cannot enforce OMR submission integrity: a submission does not match its grading job exam and tenant';
    end if;

    if exists (
        select 1
        from omr_grading_jobs
        where status not in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    ) then
        raise exception
            'cannot enforce OMR job status: unsupported status exists';
    end if;

    if exists (
        select 1
        from report_generation_jobs
        where status not in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    ) then
        raise exception
            'cannot enforce report job status: unsupported status exists';
    end if;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'omr_grading_jobs_exam_tenant_fkey'
          and conrelid = 'omr_grading_jobs'::regclass
    ) then
        alter table omr_grading_jobs
            add constraint omr_grading_jobs_exam_tenant_fkey
            foreign key (exam_id, tenant_id)
            references exams (id, tenant_id)
            on delete restrict;
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'report_generation_jobs_exam_tenant_fkey'
          and conrelid = 'report_generation_jobs'::regclass
    ) then
        alter table report_generation_jobs
            add constraint report_generation_jobs_exam_tenant_fkey
            foreign key (exam_id, tenant_id)
            references exams (id, tenant_id)
            on delete cascade;
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'omr_grading_jobs_status_check'
          and conrelid = 'omr_grading_jobs'::regclass
    ) then
        alter table omr_grading_jobs
            add constraint omr_grading_jobs_status_check
            check (status in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'report_generation_jobs_status_check'
          and conrelid = 'report_generation_jobs'::regclass
    ) then
        alter table report_generation_jobs
            add constraint report_generation_jobs_status_check
            check (status in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'omr_grading_jobs_id_exam_tenant_key'
          and conrelid = 'omr_grading_jobs'::regclass
    ) then
        alter table omr_grading_jobs
            add constraint omr_grading_jobs_id_exam_tenant_key
            unique (id, exam_id, tenant_id);
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'omr_submissions_job_exam_tenant_fkey'
          and conrelid = 'omr_submissions'::regclass
    ) then
        alter table omr_submissions
            add constraint omr_submissions_job_exam_tenant_fkey
            foreign key (job_id, exam_id, tenant_id)
            references omr_grading_jobs (id, exam_id, tenant_id)
            on delete cascade;
    end if;
end $$;
