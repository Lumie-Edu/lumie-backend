-- @flyway:nonTransactional
-- CREATE INDEX CONCURRENTLY cannot run inside a transaction (Postgres restriction).

-- Supports the FK lookup added in V41 (e.g. GET /v1/exams/{id}/omr-jobs/{id}/results).
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_exam_results_omr_grading_job_id
    ON exam_results (omr_grading_job_id);
