-- V3: Exam domain (exams, results, templates, OMR grading)

-- Exams table
CREATE TABLE IF NOT EXISTS exams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL DEFAULT 'PASS_FAIL',
    total_questions INTEGER NOT NULL,
    correct_answers JSONB NOT NULL,
    question_scores JSONB NOT NULL,
    question_types JSONB,
    pass_score INTEGER,
    grading_type VARCHAR(20) NOT NULL DEFAULT 'ABSOLUTE',
    grade_scale VARCHAR(20) DEFAULT 'NINE_GRADE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Exam results table
CREATE TABLE IF NOT EXISTS exam_results (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    total_score INTEGER NOT NULL,
    grade INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Question results table
CREATE TABLE IF NOT EXISTS question_results (
    id BIGSERIAL PRIMARY KEY,
    exam_result_id BIGINT NOT NULL REFERENCES exam_results(id) ON DELETE CASCADE,
    question_number INTEGER NOT NULL,
    selected_choice VARCHAR(10),
    is_correct BOOLEAN NOT NULL,
    score INTEGER NOT NULL,
    UNIQUE (exam_result_id, question_number)
);

-- Exam templates table
CREATE TABLE IF NOT EXISTS exam_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL DEFAULT 'GRADED',
    total_questions INTEGER NOT NULL,
    question_scores JSONB NOT NULL,
    question_types JSONB,
    pass_score INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- OMR grading jobs table
CREATE TABLE IF NOT EXISTS omr_grading_jobs (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id),
    tenant_slug VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_images INT NOT NULL,
    processed_images INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    saved_count INT NOT NULL DEFAULT 0,
    image_keys JSONB,
    results JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_exams_category ON exams(category);
CREATE INDEX IF NOT EXISTS idx_exams_grading_type ON exams(grading_type);
CREATE INDEX IF NOT EXISTS idx_exams_grade_scale ON exams(grade_scale);
CREATE INDEX IF NOT EXISTS idx_exam_results_exam_id ON exam_results(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_results_student_id ON exam_results(student_id);
CREATE INDEX IF NOT EXISTS idx_question_results_exam_result_id ON question_results(exam_result_id);
CREATE INDEX IF NOT EXISTS idx_question_results_question_number ON question_results(question_number);
CREATE INDEX IF NOT EXISTS idx_question_results_is_correct ON question_results(is_correct);
