-- Keep existing exam_results online while creating the unique official-submission pointer.
-- Drop first so a rerun repairs PostgreSQL's invalid-index residue after cancellation.
set lock_timeout = '5s';
set statement_timeout = '5min';

drop index concurrently if exists uq_exam_results_current_submission_id;
create unique index concurrently uq_exam_results_current_submission_id
    on exam_results (tenant_id, current_submission_id)
    where current_submission_id is not null;
