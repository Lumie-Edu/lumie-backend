-- ============================================================================
-- V127: enforce people, class, and assignment tenant integrity
--
-- Purpose:
--   Replace the remaining ID-only foreign keys in the people, class, and
--   assignment boundary with same-tenant composite foreign keys. Existing
--   deletion semantics are preserved exactly: CASCADE remains CASCADE and
--   SET NULL clears only the reference column, never tenant_id.
--
-- Forward-only:
--   This migration never rewrites or deletes application rows. It fails
--   closed on an orphan or cross-tenant reference, adds NOT VALID composite
--   foreign keys, validates each exact key/action, then contracts the legacy
--   ID-only constraints.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Ensure each parent has an exact unique (id, tenant_id) key. Do not create a
-- duplicate when an equivalent key exists under a different historical name.
do $$
declare
    parent_table text;
    index_name text;
    parent_key_exists boolean;
begin
    for parent_table, index_name in
        select * from (values
            ('assignments', 'idx_assignments_id_tenant_id'),
            ('classes', 'idx_classes_id_tenant_id'),
            ('staff', 'idx_staff_id_tenant_id'),
            ('students', 'idx_students_id_tenant_id'),
            ('users', 'idx_users_id_tenant_id')
        ) as parent_keys(parent_table, index_name)
    loop
        select exists (
            select 1
              from pg_index index_definition
             where index_definition.indrelid = ('public.' || parent_table)::regclass
               and index_definition.indisunique
               and index_definition.indisvalid
               and index_definition.indpred is null
               and index_definition.indnkeyatts = 2
               and index_definition.indkey[0] = (
                   select attnum from pg_attribute
                    where attrelid = ('public.' || parent_table)::regclass
                      and attname = 'id' and not attisdropped
               )
               and index_definition.indkey[1] = (
                   select attnum from pg_attribute
                    where attrelid = ('public.' || parent_table)::regclass
                      and attname = 'tenant_id' and not attisdropped
               )
        ) into parent_key_exists;

        if not parent_key_exists then
            if to_regclass('public.' || index_name) is not null then
                raise exception
                    'cannot enforce tenant integrity: index % exists but is not an exact unique (id, tenant_id) key on %',
                    index_name, parent_table;
            end if;
            execute format(
                'create unique index if not exists %I on %I (id, tenant_id)',
                index_name,
                parent_table
            );
        end if;
    end loop;
end $$;

-- A same numeric parent ID in another tenant must never satisfy these links.
do $$
declare
    child_table text;
    parent_table text;
    reference_column text;
    mismatch_exists boolean;
begin
    for child_table, parent_table, reference_column in
        select * from (values
            ('assignment_submissions', 'assignments', 'assignment_id'),
            ('assignment_submissions', 'students', 'student_id'),
            ('class_enrollments', 'classes', 'class_id'),
            ('class_enrollments', 'students', 'student_id'),
            ('classes', 'staff', 'teacher_id'),
            ('lectures', 'staff', 'teacher_id'),
            ('staff_permissions', 'staff', 'staff_id'),
            ('staff', 'users', 'user_id'),
            ('students', 'users', 'user_id')
        ) as relationships(child_table, parent_table, reference_column)
    loop
        execute format(
            'select exists (
                select 1
                  from %I child_row
                  left join %I parent_row
                    on parent_row.id = child_row.%I
                   and parent_row.tenant_id = child_row.tenant_id
                 where child_row.%I is not null
                   and parent_row.id is null
            )',
            child_table,
            parent_table,
            reference_column,
            reference_column
        ) into mismatch_exists;

        if mismatch_exists then
            raise exception
                'cannot enforce tenant integrity: %.% has an orphan or cross-tenant reference to %',
                child_table, reference_column, parent_table;
        end if;
    end loop;
end $$;

-- Expand with NOT VALID constraints first. The delete clauses intentionally
-- match the pre-existing ID-only constraints.
do $$
declare
    child_table text;
    parent_table text;
    reference_column text;
    constraint_name text;
    delete_clause text;
begin
    for child_table, parent_table, reference_column, constraint_name, delete_clause in
        select * from (values
            ('assignment_submissions', 'assignments', 'assignment_id', 'assignment_submissions_assignment_tenant_fkey', 'cascade'),
            ('assignment_submissions', 'students', 'student_id', 'assignment_submissions_student_tenant_fkey', 'set null (student_id)'),
            ('class_enrollments', 'classes', 'class_id', 'class_enrollments_class_tenant_fkey', 'cascade'),
            ('class_enrollments', 'students', 'student_id', 'class_enrollments_student_tenant_fkey', 'set null (student_id)'),
            ('classes', 'staff', 'teacher_id', 'classes_teacher_tenant_fkey', 'set null (teacher_id)'),
            ('lectures', 'staff', 'teacher_id', 'lectures_teacher_tenant_fkey', 'set null (teacher_id)'),
            ('staff_permissions', 'staff', 'staff_id', 'staff_permissions_staff_tenant_fkey', 'cascade'),
            ('staff', 'users', 'user_id', 'staff_user_tenant_fkey', 'cascade'),
            ('students', 'users', 'user_id', 'students_user_tenant_fkey', 'cascade')
        ) as relationships(child_table, parent_table, reference_column, constraint_name, delete_clause)
    loop
        if not exists (
            select 1 from pg_constraint
             where conname = constraint_name
               and conrelid = ('public.' || child_table)::regclass
        ) then
            execute format(
                'alter table %I add constraint %I foreign key (%I, tenant_id) references %I (id, tenant_id) on delete %s not valid',
                child_table,
                constraint_name,
                reference_column,
                parent_table,
                delete_clause
            );
        end if;
    end loop;
end $$;

-- Validate the exact child and parent key order and delete action before any
-- legacy protection is removed.
do $$
declare
    child_table text;
    parent_table text;
    reference_column text;
    constraint_name text;
    expected_delete_action "char";
    constraint_is_valid boolean;
begin
    for child_table, parent_table, reference_column, constraint_name, expected_delete_action in
        select * from (values
            ('assignment_submissions', 'assignments', 'assignment_id', 'assignment_submissions_assignment_tenant_fkey', 'c'::"char"),
            ('assignment_submissions', 'students', 'student_id', 'assignment_submissions_student_tenant_fkey', 'n'::"char"),
            ('class_enrollments', 'classes', 'class_id', 'class_enrollments_class_tenant_fkey', 'c'::"char"),
            ('class_enrollments', 'students', 'student_id', 'class_enrollments_student_tenant_fkey', 'n'::"char"),
            ('classes', 'staff', 'teacher_id', 'classes_teacher_tenant_fkey', 'n'::"char"),
            ('lectures', 'staff', 'teacher_id', 'lectures_teacher_tenant_fkey', 'n'::"char"),
            ('staff_permissions', 'staff', 'staff_id', 'staff_permissions_staff_tenant_fkey', 'c'::"char"),
            ('staff', 'users', 'user_id', 'staff_user_tenant_fkey', 'c'::"char"),
            ('students', 'users', 'user_id', 'students_user_tenant_fkey', 'c'::"char")
        ) as relationships(child_table, parent_table, reference_column, constraint_name, expected_delete_action)
    loop
        execute format(
            'select foreign_key.convalidated
               from pg_constraint foreign_key
              where foreign_key.conname = %L
                and foreign_key.conrelid = %L::regclass
                and foreign_key.confrelid = %L::regclass
                and foreign_key.contype = ''f''
                and foreign_key.confdeltype = %L::"char"
                and foreign_key.conkey = array[
                    (select attnum from pg_attribute where attrelid = %L::regclass and attname = %L and not attisdropped),
                    (select attnum from pg_attribute where attrelid = %L::regclass and attname = ''tenant_id'' and not attisdropped)
                ]::smallint[]
                and foreign_key.confkey = array[
                    (select attnum from pg_attribute where attrelid = %L::regclass and attname = ''id'' and not attisdropped),
                    (select attnum from pg_attribute where attrelid = %L::regclass and attname = ''tenant_id'' and not attisdropped)
                ]::smallint[]
                and (
                    %L::"char" <> ''n''::"char"
                    or position(format(''ON DELETE SET NULL (%%I)'', %L) in pg_get_constraintdef(foreign_key.oid)) > 0
                )',
            constraint_name,
            'public.' || child_table,
            'public.' || parent_table,
            expected_delete_action,
            'public.' || child_table,
            reference_column,
            'public.' || child_table,
            'public.' || parent_table,
            'public.' || parent_table,
            expected_delete_action,
            reference_column
        ) into constraint_is_valid;

        if constraint_is_valid is null then
            raise exception
                'cannot enforce tenant integrity: expected composite foreign key % is missing or has a different definition',
                constraint_name;
        end if;

        if not constraint_is_valid then
            execute format('alter table %I validate constraint %I', child_table, constraint_name);
        end if;
    end loop;
end $$;

-- Contract the legacy ID-only FKs only after every tenant-safe replacement is
-- validated. The relationship-specific delete behavior above remains active.
alter table if exists assignment_submissions
    drop constraint if exists assignment_submissions_assignment_id_fkey,
    drop constraint if exists assignment_submissions_student_id_fkey;

alter table if exists class_enrollments
    drop constraint if exists class_enrollments_class_id_fkey,
    drop constraint if exists class_enrollments_student_id_fkey;

alter table if exists classes
    drop constraint if exists classes_teacher_id_fkey;

alter table if exists lectures
    drop constraint if exists fk_lectures_teacher;

alter table if exists staff_permissions
    drop constraint if exists staff_permissions_staff_id_fkey;

alter table if exists staff
    drop constraint if exists staff_user_id_fkey;

alter table if exists students
    drop constraint if exists students_user_id_fkey;
