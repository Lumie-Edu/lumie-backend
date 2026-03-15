-- V6: Attendance (sessions and records)

-- Attendance sessions table
CREATE TABLE IF NOT EXISTS attendance_sessions (
    id BIGSERIAL PRIMARY KEY,
    academy_id BIGINT NOT NULL REFERENCES academies(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    session_date DATE NOT NULL,
    subject VARCHAR(100),
    attendance_code VARCHAR(6) NOT NULL,
    code_expires_at TIMESTAMP,
    late_threshold_minutes INTEGER NOT NULL DEFAULT 10,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Attendance records table
CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES attendance_sessions(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ABSENT',
    check_method VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    checked_at TIMESTAMP,
    checked_by BIGINT,
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_session_student UNIQUE (session_id, student_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_att_sessions_academy_id ON attendance_sessions(academy_id);
CREATE INDEX IF NOT EXISTS idx_att_sessions_date ON attendance_sessions(session_date);
CREATE INDEX IF NOT EXISTS idx_att_sessions_code ON attendance_sessions(attendance_code);
CREATE INDEX IF NOT EXISTS idx_att_records_session_id ON attendance_records(session_id);
CREATE INDEX IF NOT EXISTS idx_att_records_student_id ON attendance_records(student_id);
