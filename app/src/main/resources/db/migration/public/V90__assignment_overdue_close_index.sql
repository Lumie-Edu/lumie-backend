-- =====================================================================
-- V90: assignment overdue-close index
--
-- Purpose:
--   Support the scheduled overdue assignment close query:
--     status = ACTIVE and due_date <= now()
--   RLS adds tenant scoping at runtime, so tenant_id is the leading key.
-- =====================================================================

set lock_timeout = '5s';
set statement_timeout = '60s';

create index concurrently if not exists idx_assignments_active_due_date_tenant
    on assignments (tenant_id, due_date, id)
    where status = 'ACTIVE';
