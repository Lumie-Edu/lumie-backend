-- =====================================================================
-- V45: student_ai_feedbacks — AI-generated per-student exam feedback (tenant-scoped)
--
-- Purpose: store the AI-generated feedback blob for a single student's
-- performance on a single exam.  One row per exam_result (1:1 with
-- exam_results).  An admin may override the content (is_edited = true);
-- once edited AI regeneration is suppressed.
--
-- Scope / ownership:
--   TENANT-SCOPED table.  Each row belongs to exactly one tenant
--   (academy).  Row-Level Security is applied using the canonical V18
--   pattern:
--     USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
--     WITH CHECK (...)
--   The migrator role (postgres, SUPERUSER) runs Flyway and sees all
--   rows.  The runtime role (lumie_app, NOBYPASSRLS) is filtered by RLS.
--
-- Legacy source:
--   Migrated from Prisma table `public.student_ai_feedback` in JoossamDB.
--   Column names and semantics are preserved; naming is normalised to
--   Lumie snake_case plurals convention (student_ai_feedbacks).
--
-- Cross-module FK note:
--   exam_result_id references exam_results(id) and is INTRA-MODULE — a
--   DB-level FK is declared (ON DELETE CASCADE).  No cross-module
--   boundaries are crossed by this table.  Contrast with V29's
--   cross-module references (e.g. student_id → users.id) where NO DB FK
--   is declared and integrity is enforced at the application layer.
--
-- Rollback: NOT safely reversible after apply.
--   Drop order: student_ai_feedbacks (standalone; no dependents).
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. student_ai_feedbacks — per-exam-result AI feedback cache (1:1 with exam_results)
-- -----------------------------------------------------------------------
create table if not exists student_ai_feedbacks (
    id              bigserial       not null,
    tenant_id       bigint          not null,
    exam_result_id  bigint          not null references exam_results(id) on delete cascade,
    content         text            not null,
    is_edited       boolean         not null default false,
    generated_at    timestamptz     not null default now(),
    edited_at       timestamptz,
    version         bigint          not null default 0,
    created_at      timestamptz     not null default now(),
    updated_at      timestamptz     not null default now(),
    constraint student_ai_feedbacks_pkey                    primary key (id),
    constraint student_ai_feedbacks_exam_result_id_key      unique (exam_result_id)
);

comment on table  student_ai_feedbacks                      is 'AI-generated performance feedback for a student on a single exam. One row per exam_result.';
comment on column student_ai_feedbacks.content              is 'AI-generated feedback text blob. Replaced by admin edit when is_edited = true.';
comment on column student_ai_feedbacks.is_edited            is 'true = content was manually overridden by an admin; AI regeneration is suppressed.';
comment on column student_ai_feedbacks.generated_at         is 'Timestamp of the last AI generation run.';
comment on column student_ai_feedbacks.edited_at            is 'Timestamp of the last admin edit. NULL until first edit.';

-- idx_student_ai_feedbacks_exam_result_id is covered by the UNIQUE constraint above.
create index if not exists idx_student_ai_feedbacks_tenant_id
    on student_ai_feedbacks (tenant_id);

alter table student_ai_feedbacks enable row level security;
alter table student_ai_feedbacks force  row level security;
drop policy if exists tenant_isolation on student_ai_feedbacks;
create policy tenant_isolation on student_ai_feedbacks
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

-- -----------------------------------------------------------------------
-- 2. Grants — runtime role (lumie_app) full DML on student_ai_feedbacks
--    RLS policy above restricts rows; grants give column-level access.
-- -----------------------------------------------------------------------
grant select, insert, update, delete on student_ai_feedbacks                  to lumie_app;
grant usage,  select                  on sequence student_ai_feedbacks_id_seq to lumie_app;
