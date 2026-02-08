-- V11: Create work schedule and attendance tables

CREATE TABLE work_schedules (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_start_time TIME,
    break_end_time TIME,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_day_off BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (admin_id, day_of_week, effective_from)
);

CREATE TABLE attendances (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    clock_in_time TIMESTAMP,
    clock_out_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    late_minutes INTEGER DEFAULT 0,
    early_leave_minutes INTEGER DEFAULT 0,
    overtime_minutes INTEGER DEFAULT 0,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (admin_id, work_date)
);

-- Indexes
CREATE INDEX idx_work_schedules_admin_id ON work_schedules(admin_id);
CREATE INDEX idx_work_schedules_effective ON work_schedules(effective_from, effective_to);
CREATE INDEX idx_attendances_admin_id ON attendances(admin_id);
CREATE INDEX idx_attendances_work_date ON attendances(work_date);
CREATE INDEX idx_attendances_status ON attendances(status);
