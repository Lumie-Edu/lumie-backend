-- ============================================================================
-- V119: validate content class targets
--
-- Deploy order:
--   1. Apply V118 after the typed-only assignment/lecture consumer cutover.
--   2. Reconcile or remove every historical class-target orphan before this
--      migration. This migration does not repair or delete content rows.
--   3. Apply this migration to validate the existing tenant-safe target FKs.
--
-- Forward-only:
--   V112 introduced these constraints as NOT VALID to preserve historical
--   content. The orphan reconciliation is now complete, so this migration
--   makes the two canonical content audience relationships fully enforced.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Fail closed before validating: a class with the same numeric id in another
-- tenant is still an orphan and must never satisfy a content target.
do $$
begin
    if exists (
        select 1
        from announcement_class_targets target
        left join classes class_target
          on class_target.id = target.class_id
         and class_target.tenant_id = target.tenant_id
        where class_target.id is null
    ) then
        raise exception
            'cannot validate announcement class targets: an orphan or cross-tenant target remains';
    end if;

    if exists (
        select 1
        from lecture_class_targets target
        left join classes class_target
          on class_target.id = target.class_id
         and class_target.tenant_id = target.tenant_id
        where class_target.id is null
    ) then
        raise exception
            'cannot validate lecture class targets: an orphan or cross-tenant target remains';
    end if;
end $$;

-- Validate only the two V112 constraints. Their existence and referenced
-- relation are checked explicitly so a partial or manually altered database
-- fails rather than validating an unintended constraint.
do $$
declare
    constraint_is_valid boolean;
begin
    select convalidated
      into constraint_is_valid
      from pg_constraint
     where conname = 'announcement_class_targets_class_tenant_fkey'
       and conrelid = 'announcement_class_targets'::regclass
       and confrelid = 'classes'::regclass
       and contype = 'f';

    if not found then
        raise exception
            'cannot validate announcement class targets: expected tenant-safe FK is missing';
    end if;

    if not constraint_is_valid then
        alter table announcement_class_targets
            validate constraint announcement_class_targets_class_tenant_fkey;
    end if;

    select convalidated
      into constraint_is_valid
      from pg_constraint
     where conname = 'lecture_class_targets_class_tenant_fkey'
       and conrelid = 'lecture_class_targets'::regclass
       and confrelid = 'classes'::regclass
       and contype = 'f';

    if not found then
        raise exception
            'cannot validate lecture class targets: expected tenant-safe FK is missing';
    end if;

    if not constraint_is_valid then
        alter table lecture_class_targets
            validate constraint lecture_class_targets_class_tenant_fkey;
    end if;
end $$;
