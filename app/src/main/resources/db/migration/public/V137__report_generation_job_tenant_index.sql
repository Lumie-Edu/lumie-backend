-- Support a tenant-safe composite FK from report_generation_items.
-- Keep this concurrent index separate because the parent table may be populated.
set lock_timeout = '5s';
set statement_timeout = '60s';

drop index concurrently if exists idx_report_generation_jobs_id_exam_tenant;
create unique index concurrently idx_report_generation_jobs_id_exam_tenant
    on report_generation_jobs (id, exam_id, tenant_id);
