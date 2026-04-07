CREATE TABLE report_generation_jobs (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    tenant_slug VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_reports INTEGER NOT NULL DEFAULT 0,
    processed_reports INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    fail_count INTEGER NOT NULL DEFAULT 0,
    student_ids JSONB,
    zip_file_key VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_report_jobs_exam_id ON report_generation_jobs(exam_id);
CREATE INDEX idx_report_jobs_status ON report_generation_jobs(status);
