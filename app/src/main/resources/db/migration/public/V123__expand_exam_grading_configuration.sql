-- ============================================================================
-- V123: expand exam grading configuration
--
-- Purpose:
--   Move defaulting responsibility for exam grading configuration from the
--   database to the application. PASS_FAIL exams do not have a grading type
--   or grade scale, so grading_type must be nullable. Compatible application
--   code supplies ABSOLUTE/NINE_GRADE for omitted GRADED values.
--
-- Deploy order:
--   1. Release this migration together with application code that reads both
--      the pre-expand and expanded shapes, accepts NULL grading_type for
--      PASS_FAIL exams, and applies GRADED defaults in the application layer.
--      Flyway applies this migration before that new pod starts serving.
--   2. Complete the rolling deployment and retire every pod running the old
--      application code. Old pods can still write the legacy defaults while
--      they receive traffic, so do not normalize history during this release.
--   3. In a later, separate release, introduce V124 to normalize the audited
--      historical PASS_FAIL rows. Flyway applies every migration present in
--      one image before the first new pod starts; V124 must therefore not be
--      shipped in the same image as this expand migration.
--
-- Forward-only:
--   No exam, result, answer, question score, or pass score is modified here.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

alter table if exists exams
    alter column grading_type drop not null,
    alter column grading_type drop default,
    alter column grade_scale drop default;

comment on column exams.grading_type is
    'GRADED evaluation method. NULL for PASS_FAIL exams; omitted GRADED values default in the application layer.';

comment on column exams.grade_scale is
    'GRADED grade scale. NULL for PASS_FAIL exams; omitted GRADED values default in the application layer.';
