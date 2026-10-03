-- ============================================================================
-- V115: expand exam-template grading configuration
--
-- Purpose:
--   Add the grading configuration required when a new GRADED exam template is
--   created. These columns intentionally remain nullable for existing
--   templates: no reliable historical source establishes their intended
--   grading policy, so this migration does not guess or backfill values.
--
-- Forward-only:
--   Application validation is responsible for requiring the appropriate
--   values on new GRADED templates. Existing template rows retain NULL until a
--   user explicitly supplies a policy.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

alter table if exists exam_templates
    add column if not exists grading_type varchar(20),
    add column if not exists grade_scale varchar(20);

comment on column exam_templates.grading_type is
    'Template grading policy. Nullable for legacy templates whose historical policy is unknown; new GRADED templates must provide an application-validated value.';

comment on column exam_templates.grade_scale is
    'Template relative-grade scale. Nullable for legacy templates and non-relative policies; new GRADED templates must provide an application-validated value when required.';
