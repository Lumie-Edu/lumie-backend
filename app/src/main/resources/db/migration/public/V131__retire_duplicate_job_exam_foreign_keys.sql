-- ============================================================================
-- V131: retire duplicate job-to-exam ID-only foreign keys
--
-- V114 already expanded both job relations with validated same-tenant FKs.
-- This later contract step removes only the V18 ID-only duplicates. The
-- composite OMR FK retains RESTRICT and the composite report FK retains
-- CASCADE; job rows and exam rows are not changed by this migration.
-- Forward-only: re-adding the redundant ID-only constraints is unnecessary.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
begin
    if not exists (
        select 1
          from pg_constraint
         where conname = 'omr_grading_jobs_exam_tenant_fkey'
           and conrelid = 'omr_grading_jobs'::regclass
           and confrelid = 'exams'::regclass
           and contype = 'f'
           and convalidated
           and confdeltype = 'r'
           and pg_get_constraintdef(oid) =
               'FOREIGN KEY (exam_id, tenant_id) REFERENCES exams(id, tenant_id) ON DELETE RESTRICT'
    ) then
        raise exception 'validated same-tenant OMR job exam FK with RESTRICT is required';
    end if;

    if not exists (
        select 1
          from pg_constraint
         where conname = 'report_generation_jobs_exam_tenant_fkey'
           and conrelid = 'report_generation_jobs'::regclass
           and confrelid = 'exams'::regclass
           and contype = 'f'
           and convalidated
           and confdeltype = 'c'
           and pg_get_constraintdef(oid) =
               'FOREIGN KEY (exam_id, tenant_id) REFERENCES exams(id, tenant_id) ON DELETE CASCADE'
    ) then
        raise exception 'validated same-tenant report job exam FK with CASCADE is required';
    end if;

    if exists (
        select 1
          from pg_constraint
         where conname = 'omr_grading_jobs_exam_id_fkey'
           and conrelid = 'omr_grading_jobs'::regclass
           and (contype <> 'f'
                or confrelid <> 'exams'::regclass
                or pg_get_constraintdef(oid) <>
                   'FOREIGN KEY (exam_id) REFERENCES exams(id)')
    ) then
        raise exception 'unexpected OMR job exam constraint shape';
    end if;

    if exists (
        select 1
          from pg_constraint
         where conname = 'report_generation_jobs_exam_id_fkey'
           and conrelid = 'report_generation_jobs'::regclass
           and (contype <> 'f'
                or confrelid <> 'exams'::regclass
                or pg_get_constraintdef(oid) <>
                   'FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE')
    ) then
        raise exception 'unexpected report job exam constraint shape';
    end if;
end $$;

alter table omr_grading_jobs
    drop constraint if exists omr_grading_jobs_exam_id_fkey;

alter table report_generation_jobs
    drop constraint if exists report_generation_jobs_exam_id_fkey;
