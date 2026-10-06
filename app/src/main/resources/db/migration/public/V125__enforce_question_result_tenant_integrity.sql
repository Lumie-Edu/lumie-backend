-- ============================================================================
-- V125: enforce question result tenant integrity
--
-- Purpose:
--   Replace the ID-only question_results -> exam_results foreign key with a
--   composite tenant-safe foreign key. A question result must belong to the
--   same tenant as its parent exam result.
--
-- Forward-only:
--   This migration does not rewrite or delete question results. It fails
--   closed if an orphan or cross-tenant reference exists. The new foreign key
--   is validated before the legacy ID-only foreign key is removed, preserving
--   the existing ON DELETE CASCADE behavior throughout the transition.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Do not let a same numeric exam_result_id in another tenant satisfy the new
-- relationship. Historical rows must be reconciled explicitly before this
-- contract migration can run.
do $$
begin
    if exists (
        select 1
          from question_results question_result
          left join exam_results exam_result
            on exam_result.id = question_result.exam_result_id
           and exam_result.tenant_id = question_result.tenant_id
         where exam_result.id is null
    ) then
        raise exception
            'cannot enforce question result tenant integrity: an orphan or cross-tenant exam result reference remains';
    end if;
end $$;

-- V82 provides idx_exam_results_id_tenant_id, the unique parent key required
-- for this composite foreign key. Add NOT VALID first so validation is an
-- explicit, auditable step before the legacy constraint is contracted.
do $$
begin
    if not exists (
        select 1
          from pg_constraint
         where conname = 'question_results_exam_result_tenant_fkey'
           and conrelid = 'question_results'::regclass
    ) then
        alter table question_results
            add constraint question_results_exam_result_tenant_fkey
            foreign key (exam_result_id, tenant_id)
            references exam_results (id, tenant_id)
            on delete cascade
            not valid;
    end if;
end $$;

-- Validate only the intended composite FK. A manually altered database must
-- fail instead of allowing this migration to remove the legacy protection.
do $$
declare
    constraint_is_valid boolean;
begin
    select convalidated
      into constraint_is_valid
      from pg_constraint
     where conname = 'question_results_exam_result_tenant_fkey'
       and conrelid = 'question_results'::regclass
       and confrelid = 'exam_results'::regclass
       and contype = 'f'
       and conkey = array[
           (select attnum from pg_attribute
             where attrelid = 'question_results'::regclass
               and attname = 'exam_result_id'
               and not attisdropped),
           (select attnum from pg_attribute
             where attrelid = 'question_results'::regclass
               and attname = 'tenant_id'
               and not attisdropped)
       ]::smallint[]
       and confkey = array[
           (select attnum from pg_attribute
             where attrelid = 'exam_results'::regclass
               and attname = 'id'
               and not attisdropped),
           (select attnum from pg_attribute
             where attrelid = 'exam_results'::regclass
               and attname = 'tenant_id'
               and not attisdropped)
       ]::smallint[]
       and confdeltype = 'c';

    if not found then
        raise exception
            'cannot enforce question result tenant integrity: expected tenant-safe foreign key is missing';
    end if;

    if not constraint_is_valid then
        alter table question_results
            validate constraint question_results_exam_result_tenant_fkey;
    end if;
end $$;

-- Contract the legacy ID-only FK only after the tenant-safe relationship has
-- been validated. The delete behavior remains ON DELETE CASCADE.
alter table if exists question_results
    drop constraint if exists question_results_exam_result_id_fkey;
