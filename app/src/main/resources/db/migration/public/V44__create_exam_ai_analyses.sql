-- =====================================================================
-- V44: exam_ai_analyses — AI-generated overall exam analysis (tenant-scoped)
--
-- Purpose: store the AI-generated overall analysis blob for an exam
-- (difficulty summary, topic distribution, key points).  One row per
-- exam.  An admin may override the content (is_edited = true); once
-- edited the AI regeneration is suppressed.
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
--   Migrated from Prisma table `public.exam_ai_analysis` in JoossamDB.
--   Column names and semantics are preserved; naming is normalised to
--   Lumie snake_case plurals convention (exam_ai_analyses).
--
-- Cross-module FK note:
--   exam_id references exams(id) and is INTRA-MODULE — a DB-level FK is
--   declared (ON DELETE CASCADE).  No cross-module boundaries are crossed
--   by this table.
--
-- Rollback: NOT safely reversible after apply.
--   Drop order: exam_ai_analyses (standalone; no dependents).
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. exam_ai_analyses — per-exam AI analysis cache (1:1 with exams)
-- -----------------------------------------------------------------------
create table if not exists exam_ai_analyses (
    id              bigserial       not null,
    tenant_id       bigint          not null,
    exam_id         bigint          not null references exams(id) on delete cascade,
    content         text            not null,
    is_edited       boolean         not null default false,
    generated_at    timestamptz     not null default now(),
    edited_at       timestamptz,
    version         bigint          not null default 0,
    created_at      timestamptz     not null default now(),
    updated_at      timestamptz     not null default now(),
    constraint exam_ai_analyses_pkey          primary key (id),
    constraint exam_ai_analyses_exam_id_key   unique (exam_id)
);

comment on table  exam_ai_analyses             is 'AI-generated overall analysis for an exam (difficulty, topic distribution, key points). One row per exam.';
comment on column exam_ai_analyses.content     is 'AI-generated analysis text blob. Replaced by admin edit when is_edited = true.';
comment on column exam_ai_analyses.is_edited   is 'true = content was manually overridden by an admin; AI regeneration is suppressed.';
comment on column exam_ai_analyses.generated_at is 'Timestamp of the last AI generation run.';
comment on column exam_ai_analyses.edited_at   is 'Timestamp of the last admin edit. NULL until first edit.';

-- idx_exam_ai_analyses_exam_id is covered by the UNIQUE constraint above.
create index if not exists idx_exam_ai_analyses_tenant_id
    on exam_ai_analyses (tenant_id);

alter table exam_ai_analyses enable row level security;
alter table exam_ai_analyses force  row level security;
drop policy if exists tenant_isolation on exam_ai_analyses;
create policy tenant_isolation on exam_ai_analyses
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

-- -----------------------------------------------------------------------
-- 2. Grants — runtime role (lumie_app) full DML on exam_ai_analyses
--    RLS policy above restricts rows; grants give column-level access.
-- -----------------------------------------------------------------------
grant select, insert, update, delete on exam_ai_analyses             to lumie_app;
grant usage,  select                  on sequence exam_ai_analyses_id_seq to lumie_app;
