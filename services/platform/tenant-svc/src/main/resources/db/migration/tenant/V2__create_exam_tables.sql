-- V2: Exam tables for OMR grading functionality
-- This migration is applied to each tenant schema

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

-- Question results table (individual question scores per student per exam)
CREATE TABLE IF NOT EXISTS question_results (
    id BIGSERIAL PRIMARY KEY,
    exam_result_id BIGINT NOT NULL REFERENCES exam_results(id) ON DELETE CASCADE,
    question_number INTEGER NOT NULL,
    selected_choice VARCHAR(10),
    is_correct BOOLEAN NOT NULL,
    score INTEGER NOT NULL,
    UNIQUE (exam_result_id, question_number)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_exams_category ON exams(category);
CREATE INDEX IF NOT EXISTS idx_exam_results_exam_id ON exam_results(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_results_student_id ON exam_results(student_id);
CREATE INDEX IF NOT EXISTS idx_question_results_exam_result_id ON question_results(exam_result_id);
CREATE INDEX IF NOT EXISTS idx_question_results_question_number ON question_results(question_number);
CREATE INDEX IF NOT EXISTS idx_question_results_is_correct ON question_results(is_correct);
