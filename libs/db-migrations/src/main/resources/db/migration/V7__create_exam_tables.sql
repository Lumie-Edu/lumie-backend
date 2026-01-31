-- V7: Create exam tables for exam-svc
-- These tables are created in tenant schemas via the CREATE_TENANT_SCHEMA procedure

-- Add exam tables to the create_tenant_schema function
-- Note: This migration adds the DDL that will be executed for each tenant schema

-- For existing tenants, we need to add tables to each schema
-- This is done via a DO block that iterates over existing tenant schemas

DO $$
DECLARE
    tenant_record RECORD;
    schema_name TEXT;
BEGIN
    -- Create tables in each existing tenant schema
    FOR tenant_record IN SELECT slug FROM public.tenants WHERE status = 'ACTIVE'
    LOOP
        schema_name := 'tenant_' || replace(tenant_record.slug, '-', '_');

        -- Create exams table
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.exams (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                category VARCHAR(20) NOT NULL DEFAULT ''GRADED'',
                total_questions INTEGER NOT NULL,
                correct_answers JSONB NOT NULL,
                question_scores JSONB NOT NULL,
                question_types JSONB,
                pass_score INTEGER,
                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
            )', schema_name);

        -- Create exam_results table
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.exam_results (
                id BIGSERIAL PRIMARY KEY,
                exam_id BIGINT NOT NULL REFERENCES %I.exams(id) ON DELETE CASCADE,
                student_id BIGINT NOT NULL,
                total_score INTEGER NOT NULL,
                grade INTEGER,
                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                UNIQUE(exam_id, student_id)
            )', schema_name, schema_name);

        -- Create question_results table
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.question_results (
                id BIGSERIAL PRIMARY KEY,
                exam_result_id BIGINT NOT NULL REFERENCES %I.exam_results(id) ON DELETE CASCADE,
                question_number INTEGER NOT NULL,
                selected_choice VARCHAR(10),
                is_correct BOOLEAN NOT NULL,
                score INTEGER NOT NULL,
                UNIQUE(exam_result_id, question_number)
            )', schema_name, schema_name);

        -- Create indexes
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_exam_results_exam_id ON %I.exam_results(exam_id)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_exam_results_student_id ON %I.exam_results(student_id)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_question_results_exam_result_id ON %I.question_results(exam_result_id)', replace(schema_name, 'tenant_', ''), schema_name);

        RAISE NOTICE 'Created exam tables in schema: %', schema_name;
    END LOOP;
END $$;

-- Update the create_tenant_schema function to include exam tables for new tenants
CREATE OR REPLACE FUNCTION public.create_tenant_schema(tenant_slug TEXT)
RETURNS VOID AS $$
DECLARE
    schema_name TEXT;
BEGIN
    schema_name := 'tenant_' || replace(tenant_slug, '-', '_');

    -- Create schema
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);

    -- Create users table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.users (
            id BIGSERIAL PRIMARY KEY,
            email VARCHAR(255) NOT NULL UNIQUE,
            password_hash VARCHAR(255) NOT NULL,
            name VARCHAR(100) NOT NULL,
            phone VARCHAR(20),
            role VARCHAR(20) NOT NULL,
            status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE'',
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    -- Create academies table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.academies (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            phone VARCHAR(20),
            address VARCHAR(255),
            status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE'',
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    -- Create students table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.students (
            id BIGSERIAL PRIMARY KEY,
            user_id BIGINT NOT NULL REFERENCES %I.users(id) ON DELETE CASCADE,
            academy_id BIGINT NOT NULL REFERENCES %I.academies(id) ON DELETE CASCADE,
            student_number VARCHAR(50),
            grade VARCHAR(20),
            school_name VARCHAR(100),
            parent_name VARCHAR(100),
            parent_phone VARCHAR(20),
            enrollment_date DATE,
            status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE'',
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            UNIQUE(user_id)
        )', schema_name, schema_name, schema_name);

    -- Create admins table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.admins (
            id BIGSERIAL PRIMARY KEY,
            user_id BIGINT NOT NULL REFERENCES %I.users(id) ON DELETE CASCADE,
            academy_id BIGINT NOT NULL REFERENCES %I.academies(id) ON DELETE CASCADE,
            position VARCHAR(50),
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            UNIQUE(user_id)
        )', schema_name, schema_name, schema_name);

    -- Create exams table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.exams (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            category VARCHAR(20) NOT NULL DEFAULT ''GRADED'',
            total_questions INTEGER NOT NULL,
            correct_answers JSONB NOT NULL,
            question_scores JSONB NOT NULL,
            question_types JSONB,
            pass_score INTEGER,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    -- Create exam_results table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.exam_results (
            id BIGSERIAL PRIMARY KEY,
            exam_id BIGINT NOT NULL REFERENCES %I.exams(id) ON DELETE CASCADE,
            student_id BIGINT NOT NULL,
            total_score INTEGER NOT NULL,
            grade INTEGER,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            UNIQUE(exam_id, student_id)
        )', schema_name, schema_name);

    -- Create question_results table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.question_results (
            id BIGSERIAL PRIMARY KEY,
            exam_result_id BIGINT NOT NULL REFERENCES %I.exam_results(id) ON DELETE CASCADE,
            question_number INTEGER NOT NULL,
            selected_choice VARCHAR(10),
            is_correct BOOLEAN NOT NULL,
            score INTEGER NOT NULL,
            UNIQUE(exam_result_id, question_number)
        )', schema_name, schema_name);

    -- Create indexes for exam tables
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_exam_results_exam_id ON %I.exam_results(exam_id)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_exam_results_student_id ON %I.exam_results(student_id)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_question_results_exam_result_id ON %I.question_results(exam_result_id)', schema_name);

    RAISE NOTICE 'Created tenant schema with exam tables: %', schema_name;
END;
$$ LANGUAGE plpgsql;
