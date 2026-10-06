-- ============================================================================
-- V128: enforce exam, content, and file tenant integrity
--
-- Purpose:
--   Replace the remaining ID-only foreign keys in exam history, authored
--   content, and textbook folders with same-tenant composite foreign keys.
--   Existing CASCADE, SET NULL, and RESTRICT semantics remain unchanged.
--
-- Forward-only:
--   This migration does not alter rows. It fails closed on an orphan or
--   cross-tenant reference, validates each replacement before contracting the
--   corresponding ID-only key, and leaves tenant_id intact for every SET NULL.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
declare
    parent_table text;
    index_name text;
    parent_key_exists boolean;
begin
    for parent_table, index_name in
        select * from (values
            ('exams', 'idx_exams_id_tenant_id'),
            ('omr_grading_jobs', 'idx_omr_grading_jobs_id_tenant_id'),
            ('qna_boards', 'idx_qna_boards_id_tenant_id'),
            ('students', 'idx_students_id_tenant_id'),
            ('textbook_folders', 'idx_textbook_folders_id_tenant_id'),
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

do $$
declare
    child_table text;
    parent_table text;
    reference_column text;
    mismatch_exists boolean;
begin
    for child_table, parent_table, reference_column in
        select * from (values
            ('exam_results', 'exams', 'exam_id'),
            ('exam_results', 'omr_grading_jobs', 'omr_grading_job_id'),
            ('exam_results', 'students', 'student_id'),
            ('announcements', 'users', 'author_id'),
            ('qna_comments', 'qna_boards', 'qna_id'),
            ('file_metadata', 'textbook_folders', 'folder_id'),
            ('textbook_folders', 'textbook_folders', 'parent_id')
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
            ('exam_results', 'exams', 'exam_id', 'exam_results_exam_tenant_fkey', 'cascade'),
            ('exam_results', 'omr_grading_jobs', 'omr_grading_job_id', 'exam_results_omr_grading_job_tenant_fkey', 'set null (omr_grading_job_id)'),
            ('exam_results', 'students', 'student_id', 'exam_results_student_tenant_fkey', 'set null (student_id)'),
            ('announcements', 'users', 'author_id', 'announcements_author_tenant_fkey', 'set null (author_id)'),
            ('qna_comments', 'qna_boards', 'qna_id', 'qna_comments_qna_tenant_fkey', 'cascade'),
            ('file_metadata', 'textbook_folders', 'folder_id', 'file_metadata_folder_tenant_fkey', 'restrict'),
            ('textbook_folders', 'textbook_folders', 'parent_id', 'textbook_folders_parent_tenant_fkey', 'restrict')
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
            ('exam_results', 'exams', 'exam_id', 'exam_results_exam_tenant_fkey', 'c'::"char"),
            ('exam_results', 'omr_grading_jobs', 'omr_grading_job_id', 'exam_results_omr_grading_job_tenant_fkey', 'n'::"char"),
            ('exam_results', 'students', 'student_id', 'exam_results_student_tenant_fkey', 'n'::"char"),
            ('announcements', 'users', 'author_id', 'announcements_author_tenant_fkey', 'n'::"char"),
            ('qna_comments', 'qna_boards', 'qna_id', 'qna_comments_qna_tenant_fkey', 'c'::"char"),
            ('file_metadata', 'textbook_folders', 'folder_id', 'file_metadata_folder_tenant_fkey', 'r'::"char"),
            ('textbook_folders', 'textbook_folders', 'parent_id', 'textbook_folders_parent_tenant_fkey', 'r'::"char")
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

alter table if exists exam_results
    drop constraint if exists exam_results_exam_id_fkey,
    drop constraint if exists exam_results_omr_grading_job_id_fkey,
    drop constraint if exists exam_results_student_id_fkey;

alter table if exists announcements
    drop constraint if exists announcements_author_id_fkey;

alter table if exists qna_comments
    drop constraint if exists qna_comments_qna_id_fkey;

alter table if exists file_metadata
    drop constraint if exists fk_file_metadata_folder;

alter table if exists textbook_folders
    drop constraint if exists fk_textbook_folders_parent;
