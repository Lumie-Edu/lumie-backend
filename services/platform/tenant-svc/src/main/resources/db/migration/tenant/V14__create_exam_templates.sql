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
