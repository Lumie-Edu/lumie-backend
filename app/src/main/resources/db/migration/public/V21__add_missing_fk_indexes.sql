-- @flyway:nonTransactional
-- CREATE INDEX CONCURRENTLY cannot run inside a transaction (Postgres restriction).
-- This migration establishes the CONCURRENTLY convention for future index additions.

-- omr_grading_jobs.exam_id: FK (omr_grading_jobs_exam_id_fkey) has no supporting index.
-- Used on job-status polls and cascade deletes triggered by exam deletion.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_omr_grading_jobs_exam_id
    ON omr_grading_jobs (exam_id);

-- qna_comments.user_id: FK (qna_comments_user_id_fkey -> users.id) has no index.
-- Queried when loading comments with author info.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_qna_comments_user_id
    ON qna_comments (user_id);

-- counseling_reservations.user_id: FK (counseling_reservations_user_id_fkey -> users.id).
-- Queried per user on counseling schedule lookups.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_counseling_reservations_user_id
    ON counseling_reservations (user_id);
