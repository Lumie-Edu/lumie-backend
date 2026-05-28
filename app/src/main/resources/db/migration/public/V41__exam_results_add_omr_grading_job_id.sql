-- N-41: Link exam_results to the omr_grading_jobs batch that produced them.
-- ON DELETE SET NULL: deleting a job must not cascade-delete canonical result rows.
ALTER TABLE exam_results
    ADD COLUMN IF NOT EXISTS omr_grading_job_id BIGINT;

ALTER TABLE exam_results
    ADD CONSTRAINT exam_results_omr_grading_job_id_fkey
    FOREIGN KEY (omr_grading_job_id)
    REFERENCES omr_grading_jobs (id)
    ON DELETE SET NULL;
