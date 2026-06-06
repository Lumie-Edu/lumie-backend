-- =====================================================================
-- V58: make lecture_class_targets FK tenant-safe
--
-- Purpose:
--   Replace the initial lecture_id-only FK with a composite FK that also
--   enforces matching tenant_id. No data backfill is performed.
--
-- Scope:
--   Constraint-only correction for the tenant-scoped lecture_class_targets
--   table, matching the announcement target-table rollout pattern.
-- =====================================================================

set lock_timeout = '5s';
set statement_timeout = '30s';

create unique index concurrently if not exists idx_lectures_id_tenant_id
    on lectures (id, tenant_id);

alter table lecture_class_targets
    drop constraint if exists lecture_class_targets_lecture_fkey;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'lecture_class_targets_lecture_tenant_fkey'
          and conrelid = 'lecture_class_targets'::regclass
    ) then
        alter table lecture_class_targets
            add constraint lecture_class_targets_lecture_tenant_fkey
            foreign key (lecture_id, tenant_id)
            references lectures(id, tenant_id)
            on delete cascade;
    end if;
end $$;
