-- =====================================================================
-- V56: make announcement_class_targets FK tenant-safe
--
-- Purpose:
--   Replace the initial announcement_id-only FK with a composite FK that
--   also enforces matching tenant_id. No data backfill is performed.
--
-- Scope:
--   Constraint-only correction for the tenant-scoped
--   announcement_class_targets table.
-- =====================================================================

create unique index if not exists idx_announcements_id_tenant_id
    on announcements (id, tenant_id);

alter table announcement_class_targets
    drop constraint if exists announcement_class_targets_announcement_fkey;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'announcement_class_targets_announcement_tenant_fkey'
          and conrelid = 'announcement_class_targets'::regclass
    ) then
        alter table announcement_class_targets
            add constraint announcement_class_targets_announcement_tenant_fkey
            foreign key (announcement_id, tenant_id)
            references announcements(id, tenant_id)
            on delete cascade;
    end if;
end $$;
