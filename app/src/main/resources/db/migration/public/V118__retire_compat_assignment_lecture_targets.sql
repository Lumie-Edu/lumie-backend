-- ============================================================================
-- V118: retire compatibility assignment and lecture targets
--
-- Deploy order:
--   1. Deploy the typed-only ERD-04/05 application while V109/V112 legacy
--      shapes still exist, then confirm no legacy reader or writer remains.
--   2. Confirm that every remaining legacy target exists in the typed tables.
--      New assignments written by the typed-only runtime may have no legacy
--      rows, so typed targets need not have a legacy counterpart.
--   3. Confirm the typed-only pod is the sole runtime before applying this
--      migration. Never apply it while an old writer can restart or coexist.
--
-- Forward-only and destructive:
--   The legacy assignment_targets table plus assignments.class_id and
--   lectures.class_id are retired. This migration fails before DDL if any
--   remaining legacy audience is absent from the typed source of truth;
--   it never deletes assignment recipients or submissions.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Refuse to discard a legacy target that is absent from the typed source of
-- truth. Compare tenant-safe logical keys, not surrogate IDs or audit data.
-- The typed-only runtime intentionally creates new targets without legacy
-- counterparts, so reverse parity would reject valid post-cutover writes.
do $$
begin
    if exists (
        select target.assignment_id, target.target_id as class_id, target.tenant_id
        from assignment_targets target
        where target.target_type = 'CLASS'

        except

        select target.assignment_id, target.class_id, target.tenant_id
        from assignment_class_targets target
    ) then
        raise exception
            'cannot retire assignment targets: a legacy class target is absent from the typed table';
    end if;

    if exists (
        select target.assignment_id, target.target_id as student_id, target.tenant_id
        from assignment_targets target
        where target.target_type = 'STUDENT'

        except

        select target.assignment_id, target.student_id, target.tenant_id
        from assignment_student_targets target
    ) then
        raise exception
            'cannot retire assignment targets: a legacy student target is absent from the typed table';
    end if;

    if exists (
        select 1
        from assignment_targets
        where target_type not in ('CLASS', 'STUDENT')
    ) then
        raise exception
            'cannot retire assignment targets: legacy table contains an unsupported target type';
    end if;

    if exists (
        select 1
        from assignments assignment
        where assignment.class_id is not null
          and not exists (
              select 1
              from assignment_class_targets target
              where target.assignment_id = assignment.id
                and target.class_id = assignment.class_id
                and target.tenant_id = assignment.tenant_id
          )
    ) then
        raise exception
            'cannot retire assignments.class_id: compatibility class is absent from typed class targets';
    end if;

    -- lectures.class_id predates lecture_class_targets. Do not lose a
    -- compatibility audience that has not yet been represented canonically.
    if exists (
        select 1
        from lectures lecture
        where lecture.class_id is not null
          and not exists (
              select 1
              from lecture_class_targets target
              where target.lecture_id = lecture.id
                and target.class_id = lecture.class_id
                and target.tenant_id = lecture.tenant_id
          )
    ) then
        raise exception
            'cannot retire lectures.class_id: compatibility class is absent from lecture class targets';
    end if;
end $$;

-- Replace V109's before-delete cleanup with typed-only cleanup before
-- removing its legacy references. Deleting a target cascades only source
-- provenance rows through V110; recipient snapshots and submissions remain.
create or replace function remove_typed_assignment_class_targets_before_class_delete()
returns trigger
language plpgsql
as $$
declare
    affected_assignment_ids bigint[];
begin
    select array_agg(distinct target.assignment_id)
      into affected_assignment_ids
      from assignment_class_targets target
     where target.tenant_id = old.tenant_id
       and target.class_id = old.id;

    -- Explicit removal occurs before the parent delete so closure does not
    -- depend on FK cascade or user-trigger ordering.
    delete from assignment_class_targets
    where tenant_id = old.tenant_id
      and class_id = old.id;

    update assignments assignment
       set status = 'CLOSED',
           updated_at = now(),
           version = assignment.version + 1
     where assignment.tenant_id = old.tenant_id
       and assignment.status = 'ACTIVE'
       and assignment.id = any(coalesce(affected_assignment_ids, '{}'::bigint[]))
       and not exists (
           select 1
           from assignment_class_targets class_target
           where class_target.assignment_id = assignment.id
             and class_target.tenant_id = assignment.tenant_id
       )
       and not exists (
           select 1
           from assignment_student_targets student_target
           where student_target.assignment_id = assignment.id
             and student_target.tenant_id = assignment.tenant_id
       );

    return old;
end;
$$;

create or replace function remove_typed_assignment_student_targets_before_student_delete()
returns trigger
language plpgsql
as $$
declare
    affected_assignment_ids bigint[];
begin
    select array_agg(distinct target.assignment_id)
      into affected_assignment_ids
      from assignment_student_targets target
     where target.tenant_id = old.tenant_id
       and target.student_id = old.id;

    -- V110 separately revokes all live recipient sources for this student.
    -- This delete also cascades only the target-linked provenance, never the
    -- recipient or the submitted assignment work.
    delete from assignment_student_targets
    where tenant_id = old.tenant_id
      and student_id = old.id;

    update assignments assignment
       set status = 'CLOSED',
           updated_at = now(),
           version = assignment.version + 1
     where assignment.tenant_id = old.tenant_id
       and assignment.status = 'ACTIVE'
       and assignment.id = any(coalesce(affected_assignment_ids, '{}'::bigint[]))
       and not exists (
           select 1
           from assignment_class_targets class_target
           where class_target.assignment_id = assignment.id
             and class_target.tenant_id = assignment.tenant_id
       )
       and not exists (
           select 1
           from assignment_student_targets student_target
           where student_target.assignment_id = assignment.id
             and student_target.tenant_id = assignment.tenant_id
       );

    return old;
end;
$$;

drop trigger if exists trg_remove_legacy_class_targets_before_class_delete
    on classes;

drop trigger if exists trg_remove_legacy_class_targets_after_class_delete
    on classes;

drop trigger if exists trg_remove_typed_assignment_class_targets_before_class_delete
    on classes;

create trigger trg_remove_typed_assignment_class_targets_before_class_delete
before delete on classes
for each row
execute function remove_typed_assignment_class_targets_before_class_delete();

drop trigger if exists trg_remove_legacy_student_targets_before_student_delete
    on students;

drop trigger if exists trg_remove_typed_student_targets_before_delete
    on students;

create trigger trg_remove_typed_student_targets_before_delete
before delete on students
for each row
execute function remove_typed_assignment_student_targets_before_student_delete();

-- Remove V109's legacy-write synchronization before dropping the table and
-- scalar. Do not change V110's assignment_recipient source-capture trigger.
drop trigger if exists trg_sync_legacy_assignment_target_to_typed_targets
    on assignment_targets;

drop trigger if exists trg_sync_legacy_assignment_class_to_typed_target
    on assignments;

drop function if exists sync_legacy_assignment_target_to_typed_targets();
drop function if exists sync_legacy_assignment_class_to_typed_target();
drop function if exists remove_legacy_class_targets_before_class_delete();
drop function if exists remove_legacy_student_targets_before_student_delete();

alter table if exists assignments
    drop constraint if exists assignments_class_tenant_fkey;

alter table if exists lectures
    drop constraint if exists lectures_class_tenant_fkey;

alter table if exists assignments
    drop column if exists class_id;

alter table if exists lectures
    drop column if exists class_id;

drop table if exists assignment_targets;
