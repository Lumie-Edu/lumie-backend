-- V8: Create content tables for content-svc
-- These tables are created in tenant schemas via the CREATE_TENANT_SCHEMA procedure

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

        -- Create announcements table
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.announcements (
                id BIGSERIAL PRIMARY KEY,
                title VARCHAR(200) NOT NULL,
                content TEXT NOT NULL,
                author_id BIGINT NOT NULL,
                author_name VARCHAR(100) NOT NULL,
                is_important BOOLEAN NOT NULL DEFAULT FALSE,
                view_count INTEGER NOT NULL DEFAULT 0,
                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
            )', schema_name);

        -- Create qna_boards table
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.qna_boards (
                id BIGSERIAL PRIMARY KEY,
                title VARCHAR(200) NOT NULL,
                content TEXT NOT NULL,
                student_id BIGINT NOT NULL,
                student_name VARCHAR(100) NOT NULL,
                is_answered BOOLEAN NOT NULL DEFAULT FALSE,
                view_count INTEGER NOT NULL DEFAULT 0,
                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
            )', schema_name);

        -- Create qna_comments table
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.qna_comments (
                id BIGSERIAL PRIMARY KEY,
                qna_board_id BIGINT NOT NULL REFERENCES %I.qna_boards(id) ON DELETE CASCADE,
                content TEXT NOT NULL,
                author_id BIGINT NOT NULL,
                author_name VARCHAR(100) NOT NULL,
                author_type VARCHAR(20) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
            )', schema_name, schema_name);

        -- Create textbooks table
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.textbooks (
                id BIGSERIAL PRIMARY KEY,
                title VARCHAR(200) NOT NULL,
                description TEXT,
                category VARCHAR(20) NOT NULL,
                file_id BIGINT,
                file_name VARCHAR(255),
                file_url VARCHAR(500),
                file_size BIGINT,
                author_id BIGINT NOT NULL,
                author_name VARCHAR(100) NOT NULL,
                is_important BOOLEAN NOT NULL DEFAULT FALSE,
                download_count INTEGER NOT NULL DEFAULT 0,
                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
            )', schema_name);

        -- Create schedules table
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.schedules (
                id BIGSERIAL PRIMARY KEY,
                admin_id BIGINT NOT NULL,
                admin_name VARCHAR(100) NOT NULL,
                schedule_date DATE NOT NULL,
                start_time TIME NOT NULL,
                end_time TIME NOT NULL,
                max_reservations INTEGER NOT NULL,
                description VARCHAR(500),
                is_available BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
            )', schema_name);

        -- Create reservations table
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.reservations (
                id BIGSERIAL PRIMARY KEY,
                schedule_id BIGINT NOT NULL REFERENCES %I.schedules(id) ON DELETE CASCADE,
                student_id BIGINT NOT NULL,
                student_name VARCHAR(100) NOT NULL,
                student_phone VARCHAR(20),
                status VARCHAR(20) NOT NULL DEFAULT ''PENDING'',
                memo VARCHAR(500),
                cancel_reason VARCHAR(500),
                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
            )', schema_name, schema_name);

        -- Create indexes for content tables
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_announcements_is_important ON %I.announcements(is_important)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_announcements_created_at ON %I.announcements(created_at DESC)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_qna_boards_student_id ON %I.qna_boards(student_id)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_qna_boards_is_answered ON %I.qna_boards(is_answered)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_qna_comments_qna_board_id ON %I.qna_comments(qna_board_id)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_textbooks_category ON %I.textbooks(category)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_schedules_date ON %I.schedules(schedule_date)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_schedules_admin_id ON %I.schedules(admin_id)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_reservations_schedule_id ON %I.reservations(schedule_id)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_reservations_student_id ON %I.reservations(student_id)', replace(schema_name, 'tenant_', ''), schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_reservations_status ON %I.reservations(status)', replace(schema_name, 'tenant_', ''), schema_name);

        RAISE NOTICE 'Created content tables in schema: %', schema_name;
    END LOOP;
END $$;

-- Update the create_tenant_schema function to include content tables for new tenants
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

    -- Create announcements table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.announcements (
            id BIGSERIAL PRIMARY KEY,
            title VARCHAR(200) NOT NULL,
            content TEXT NOT NULL,
            author_id BIGINT NOT NULL,
            author_name VARCHAR(100) NOT NULL,
            is_important BOOLEAN NOT NULL DEFAULT FALSE,
            view_count INTEGER NOT NULL DEFAULT 0,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    -- Create qna_boards table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.qna_boards (
            id BIGSERIAL PRIMARY KEY,
            title VARCHAR(200) NOT NULL,
            content TEXT NOT NULL,
            student_id BIGINT NOT NULL,
            student_name VARCHAR(100) NOT NULL,
            is_answered BOOLEAN NOT NULL DEFAULT FALSE,
            view_count INTEGER NOT NULL DEFAULT 0,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    -- Create qna_comments table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.qna_comments (
            id BIGSERIAL PRIMARY KEY,
            qna_board_id BIGINT NOT NULL REFERENCES %I.qna_boards(id) ON DELETE CASCADE,
            content TEXT NOT NULL,
            author_id BIGINT NOT NULL,
            author_name VARCHAR(100) NOT NULL,
            author_type VARCHAR(20) NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name, schema_name);

    -- Create textbooks table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.textbooks (
            id BIGSERIAL PRIMARY KEY,
            title VARCHAR(200) NOT NULL,
            description TEXT,
            category VARCHAR(20) NOT NULL,
            file_id BIGINT,
            file_name VARCHAR(255),
            file_url VARCHAR(500),
            file_size BIGINT,
            author_id BIGINT NOT NULL,
            author_name VARCHAR(100) NOT NULL,
            is_important BOOLEAN NOT NULL DEFAULT FALSE,
            download_count INTEGER NOT NULL DEFAULT 0,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    -- Create schedules table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.schedules (
            id BIGSERIAL PRIMARY KEY,
            admin_id BIGINT NOT NULL,
            admin_name VARCHAR(100) NOT NULL,
            schedule_date DATE NOT NULL,
            start_time TIME NOT NULL,
            end_time TIME NOT NULL,
            max_reservations INTEGER NOT NULL,
            description VARCHAR(500),
            is_available BOOLEAN NOT NULL DEFAULT TRUE,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    -- Create reservations table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.reservations (
            id BIGSERIAL PRIMARY KEY,
            schedule_id BIGINT NOT NULL REFERENCES %I.schedules(id) ON DELETE CASCADE,
            student_id BIGINT NOT NULL,
            student_name VARCHAR(100) NOT NULL,
            student_phone VARCHAR(20),
            status VARCHAR(20) NOT NULL DEFAULT ''PENDING'',
            memo VARCHAR(500),
            cancel_reason VARCHAR(500),
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name, schema_name);

    -- Create indexes for exam tables
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_exam_results_exam_id ON %I.exam_results(exam_id)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_exam_results_student_id ON %I.exam_results(student_id)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_question_results_exam_result_id ON %I.question_results(exam_result_id)', schema_name);

    -- Create indexes for content tables
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_announcements_is_important ON %I.announcements(is_important)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_announcements_created_at ON %I.announcements(created_at DESC)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_qna_boards_student_id ON %I.qna_boards(student_id)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_qna_boards_is_answered ON %I.qna_boards(is_answered)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_qna_comments_qna_board_id ON %I.qna_comments(qna_board_id)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_textbooks_category ON %I.textbooks(category)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_schedules_date ON %I.schedules(schedule_date)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_schedules_admin_id ON %I.schedules(admin_id)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_reservations_schedule_id ON %I.reservations(schedule_id)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_reservations_student_id ON %I.reservations(student_id)', schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_reservations_status ON %I.reservations(status)', schema_name);

    RAISE NOTICE 'Created tenant schema with content tables: %', schema_name;
END;
$$ LANGUAGE plpgsql;
