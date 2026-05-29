-- =====================================================================
-- V47: exam_dashboard_rollups — derived rollup of exam dashboard stats (tenant-scoped)
--
-- Purpose: store pre-aggregated dashboard statistics for each exam.
-- One row per exam, refreshed by application events (e.g., after an
-- ExamResult is submitted or updated).  Covers result counts, score
-- sums, question-type accuracy maps, wrong-choice distributions, and
-- five/nine-grade bucket histograms.
--
-- NON-AUTHORITATIVE: this is a derived/refreshable rollup table.
--   Source of truth = exam_results + question_results.
--   It is safe to TRUNCATE this table and rebuild all rows via the
--   admin rebuild endpoint (/v1/admin/exam-dashboard-rollups/rebuild).
--   Never treat these aggregates as the primary record of student work.
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
-- Cross-module FK note:
--   exam_id references exams(id) and is INTRA-MODULE — a DB-level FK is
--   declared (ON DELETE CASCADE).  No cross-module boundaries are crossed
--   by this table.
--
-- Rollback: drop table exam_dashboard_rollups (standalone; no dependents).
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. exam_dashboard_rollups — per-exam derived statistics rollup (1:1 with exams)
-- -----------------------------------------------------------------------
create table if not exists exam_dashboard_rollups (
    id                  bigserial       not null,
    tenant_id           bigint          not null,
    exam_id             bigint          not null references exams(id) on delete cascade,
    result_count        bigint          not null default 0,
    score_sum           bigint          not null default 0,
    type_accuracy_map   jsonb           not null default '{}'::jsonb,
    wrong_choice_map    jsonb           not null default '{}'::jsonb,
    five_grade_buckets  jsonb           not null default '{}'::jsonb,
    nine_grade_buckets  jsonb           not null default '{}'::jsonb,
    version             bigint          not null default 0,
    created_at          timestamptz     not null default now(),
    updated_at          timestamptz     not null default now(),
    constraint exam_dashboard_rollups_pkey          primary key (id),
    constraint exam_dashboard_rollups_exam_id_key   unique (exam_id)
);

comment on table  exam_dashboard_rollups                        is 'Derived rollup of exam dashboard statistics — one row per exam. NON-AUTHORITATIVE: source of truth is exam_results + question_results. Safe to TRUNCATE and rebuild via the admin rebuild endpoint.';
comment on column exam_dashboard_rollups.tenant_id              is 'Owning tenant. Enforced by RLS policy tenant_isolation.';
comment on column exam_dashboard_rollups.exam_id                is 'FK to exams(id). Cascades on delete. Unique: at most one rollup row per exam.';
comment on column exam_dashboard_rollups.result_count           is 'Number of ExamResult rows submitted for this exam. Derived from exam_results.';
comment on column exam_dashboard_rollups.score_sum              is 'Sum of ExamResult.total_score across all results. Overall average = score_sum / result_count.';
comment on column exam_dashboard_rollups.type_accuracy_map      is 'Per-question-type accuracy counters. Shape: {questionType: {total: N, correct: N}}. Derived from question_results.';
comment on column exam_dashboard_rollups.wrong_choice_map       is 'Per-question wrong-choice distribution. Shape: {questionNumber: {totalAttempts: N, incorrectCount: N, wrongChoiceCounts: {choice: count}}}. Derived from question_results.';
comment on column exam_dashboard_rollups.five_grade_buckets     is 'Grade histogram for the FIVE_GRADE scale. Shape: {grade: count}. Derived from exam_results.';
comment on column exam_dashboard_rollups.nine_grade_buckets     is 'Grade histogram for the NINE_GRADE scale. Shape: {grade: count}. Derived from exam_results.';
comment on column exam_dashboard_rollups.version                is 'Optimistic-locking counter (@Version). Incremented on every application-side update.';

-- idx_exam_dashboard_rollups_exam_id is covered by the UNIQUE constraint above.
create index if not exists idx_exam_dashboard_rollups_tenant_id
    on exam_dashboard_rollups (tenant_id);

alter table exam_dashboard_rollups enable row level security;
alter table exam_dashboard_rollups force  row level security;
drop policy if exists tenant_isolation on exam_dashboard_rollups;
create policy tenant_isolation on exam_dashboard_rollups
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

-- -----------------------------------------------------------------------
-- 2. Grants — runtime role (lumie_app) full DML on exam_dashboard_rollups
--    RLS policy above restricts rows; grants give column-level access.
-- -----------------------------------------------------------------------
grant select, insert, update, delete on exam_dashboard_rollups                 to lumie_app;
grant usage,  select                  on sequence exam_dashboard_rollups_id_seq to lumie_app;
