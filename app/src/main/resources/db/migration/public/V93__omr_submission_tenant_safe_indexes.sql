-- =====================================================================
-- V93: online-safe supporting indexes for tenant-safe OMR submission FKs
--
-- The existing exam and exam-result composite indexes were introduced by V82.
-- This migration only adds the missing OMR job parent index required before
-- the transactional OMR submission DDL can add its composite foreign keys.
-- Split out so Flyway can run these CREATE INDEX CONCURRENTLY statements
-- outside the migration transaction.
-- =====================================================================

set lock_timeout = '5s';
set statement_timeout = '60s';

drop index concurrently if exists idx_omr_grading_jobs_id_tenant_id;
create unique index concurrently idx_omr_grading_jobs_id_tenant_id
    on omr_grading_jobs (id, tenant_id);
