-- V1: Initial tenant schema structure
-- This migration is applied to each tenant schema (tenant_xxx)

-- Academies table
CREATE TABLE IF NOT EXISTS academies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    address VARCHAR(500),
    phone VARCHAR(20) UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Students table (references public.users, with denormalized user fields)
CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES public.users(id) ON DELETE CASCADE,
    user_login_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    academy_id BIGINT NOT NULL REFERENCES academies(id) ON DELETE CASCADE,
    student_highschool VARCHAR(100),
    student_birth_year INTEGER,
    student_memo TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Admins table (references public.users, with denormalized user fields)
CREATE TABLE IF NOT EXISTS admins (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES public.users(id) ON DELETE CASCADE,
    user_login_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT true,
    admin_position VARCHAR(50) DEFAULT '조교',
    admin_memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Admin-Academy join table
CREATE TABLE IF NOT EXISTS admin_academies (
    admin_id BIGINT NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    academy_id BIGINT NOT NULL REFERENCES academies(id) ON DELETE CASCADE,
    PRIMARY KEY (admin_id, academy_id)
);

-- QnA Boards table
CREATE TABLE IF NOT EXISTS qna_boards (
    id BIGSERIAL PRIMARY KEY,
    qna_user_id BIGINT NOT NULL,
    qna_title VARCHAR(200) NOT NULL,
    qna_content TEXT NOT NULL,
    is_it_answered BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- QnA Comments table
CREATE TABLE IF NOT EXISTS qna_comments (
    id BIGSERIAL PRIMARY KEY,
    qna_id BIGINT NOT NULL REFERENCES qna_boards(id) ON DELETE CASCADE,
    student_id BIGINT,
    admin_id BIGINT,
    comment_content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Counseling Schedules table
CREATE TABLE IF NOT EXISTS counseling_schedules (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    date DATE NOT NULL,
    time_slot_id INTEGER NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (admin_id, date, time_slot_id)
);

-- Counseling Reservations table
CREATE TABLE IF NOT EXISTS counseling_reservations (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL REFERENCES counseling_schedules(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    consultation_content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Announcements table
CREATE TABLE IF NOT EXISTS announcements (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    announcement_title VARCHAR(200) NOT NULL,
    announcement_content TEXT NOT NULL,
    is_it_asset_announcement BOOLEAN NOT NULL DEFAULT false,
    is_it_important_announcement BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Announcement-Academy join table
CREATE TABLE IF NOT EXISTS announcement_academies (
    announcement_id BIGINT NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    academy_id BIGINT NOT NULL,
    PRIMARY KEY (announcement_id, academy_id)
);

-- Textbooks table
CREATE TABLE IF NOT EXISTS textbooks (
    id BIGSERIAL PRIMARY KEY,
    academy_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    author VARCHAR(100),
    publisher VARCHAR(100),
    isbn VARCHAR(20),
    subject VARCHAR(100),
    grade_level VARCHAR(50),
    price DECIMAL(10, 2),
    cover_image_path VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Reviews table
CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    reviewer_name VARCHAR(255) NOT NULL,
    review_title VARCHAR(255) NOT NULL DEFAULT '',
    review_content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Toggles table (review popup settings)
CREATE TABLE IF NOT EXISTS toggles (
    id BIGINT PRIMARY KEY DEFAULT 1,
    is_review_popup_on BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_students_academy_id ON students(academy_id);
CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id);
CREATE INDEX IF NOT EXISTS idx_admins_user_id ON admins(user_id);
CREATE INDEX IF NOT EXISTS idx_qna_boards_qna_user_id ON qna_boards(qna_user_id);
CREATE INDEX IF NOT EXISTS idx_qna_comments_qna_id ON qna_comments(qna_id);
CREATE INDEX IF NOT EXISTS idx_schedules_admin_id ON counseling_schedules(admin_id);
CREATE INDEX IF NOT EXISTS idx_schedules_date ON counseling_schedules(date);
CREATE INDEX IF NOT EXISTS idx_reservations_schedule_id ON counseling_reservations(schedule_id);
CREATE INDEX IF NOT EXISTS idx_reservations_student_id ON counseling_reservations(student_id);
CREATE INDEX IF NOT EXISTS idx_announcements_author_id ON announcements(author_id);
CREATE INDEX IF NOT EXISTS idx_textbooks_academy_id ON textbooks(academy_id);

-- Default data
INSERT INTO toggles (id, is_review_popup_on) VALUES (1, TRUE) ON CONFLICT (id) DO NOTHING;
