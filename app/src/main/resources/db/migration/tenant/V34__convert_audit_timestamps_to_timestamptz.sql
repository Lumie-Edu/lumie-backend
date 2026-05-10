-- =====================================================================
-- Convert tenant-schema audit timestamp columns from
-- `timestamp without time zone` to `timestamptz`.
--
-- Motivation: backend entity audit fields migrated from LocalDateTime to
-- Instant (DTOs already used Instant since 2026-04). Keeping the columns
-- as `timestamp without time zone` left an implicit "JVM is UTC, so
-- wall-clock == UTC" assumption at the boundary; making the column
-- timezone-aware lets Hibernate map directly to Instant without coercion.
--
-- The JVM is locked to UTC (Dockerfile `ENV TZ=UTC`,
-- `-Duser.timezone=UTC`), so existing wall-clock values are already UTC.
-- `AT TIME ZONE 'UTC'` interprets the naive timestamp as UTC and produces
-- the matching timestamptz — a lossless round-trip.
--
-- `ALTER TABLE IF EXISTS` lets this run on tenant schemas that may not
-- have every table (e.g., legacy schemas where deprecated tables have
-- been dropped, or schemas seeded after a table was removed).
-- =====================================================================

ALTER TABLE IF EXISTS academies
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS activity_logs
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS admins
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS ai_chat_messages
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS ai_conversations
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS ai_scheduled_tasks
    ALTER COLUMN created_at        TYPE TIMESTAMPTZ USING created_at        AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at        TYPE TIMESTAMPTZ USING updated_at        AT TIME ZONE 'UTC',
    ALTER COLUMN next_execution_at TYPE TIMESTAMPTZ USING next_execution_at AT TIME ZONE 'UTC',
    ALTER COLUMN last_executed_at  TYPE TIMESTAMPTZ USING last_executed_at  AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS announcements
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS assignments
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN due_date   TYPE TIMESTAMPTZ USING due_date   AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS assignment_submissions
    ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC',
    ALTER COLUMN submitted_at TYPE TIMESTAMPTZ USING submitted_at AT TIME ZONE 'UTC',
    ALTER COLUMN graded_at    TYPE TIMESTAMPTZ USING graded_at    AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS attendance_records
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN checked_at TYPE TIMESTAMPTZ USING checked_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS attendance_sessions
    ALTER COLUMN created_at      TYPE TIMESTAMPTZ USING created_at      AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at      TYPE TIMESTAMPTZ USING updated_at      AT TIME ZONE 'UTC',
    ALTER COLUMN code_expires_at TYPE TIMESTAMPTZ USING code_expires_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS classes
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS class_enrollments
    ALTER COLUMN created_at  TYPE TIMESTAMPTZ USING created_at  AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at  TYPE TIMESTAMPTZ USING updated_at  AT TIME ZONE 'UTC',
    ALTER COLUMN enrolled_at TYPE TIMESTAMPTZ USING enrolled_at AT TIME ZONE 'UTC',
    ALTER COLUMN dropped_at  TYPE TIMESTAMPTZ USING dropped_at  AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS class_journals
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS counseling_reservations
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS counseling_schedules
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS exams
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS exam_results
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS exam_templates
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS file_metadata
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS lectures
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS omr_grading_jobs
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS permissions
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS positions
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS qna_boards
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS qna_comments
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS reviews
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS sms_messages
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN sent_at    TYPE TIMESTAMPTZ USING sent_at    AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS sms_templates
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS students
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS textbooks
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS textbook_folders
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS toggles
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS users
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
