-- =====================================================================
-- V86: prevent deleting exams that back active assignments
--
-- Purpose:
--   Active assignments must stay linked to an exam. V82 originally used
--   ON DELETE SET NULL for the linked exam FK, but V85 made active rows
--   require linked_exam_id. The FK action is tightened to RESTRICT so exam
--   deletion must explicitly close or remove dependent assignments first.
--
-- Forward-only:
--   No previous Flyway migration is modified.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

alter table if exists assignments
    drop constraint if exists assignments_linked_exam_tenant_fkey;

alter table if exists assignments
    add constraint assignments_linked_exam_tenant_fkey
    foreign key (linked_exam_id, tenant_id)
    references exams(id, tenant_id)
    on delete restrict;
