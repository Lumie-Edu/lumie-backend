-- V12: Create salary and payroll tables

CREATE TABLE salaries (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    base_salary DECIMAL(12, 0) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payroll_records (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    pay_year INTEGER NOT NULL,
    pay_month INTEGER NOT NULL,
    base_salary DECIMAL(12, 0) NOT NULL,
    total_allowances DECIMAL(12, 0) NOT NULL DEFAULT 0,
    total_deductions DECIMAL(12, 0) NOT NULL DEFAULT 0,
    net_pay DECIMAL(12, 0) NOT NULL,
    total_work_days INTEGER,
    actual_work_days INTEGER,
    total_overtime_minutes INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (admin_id, pay_year, pay_month)
);

CREATE TABLE payroll_items (
    id BIGSERIAL PRIMARY KEY,
    payroll_record_id BIGINT NOT NULL REFERENCES payroll_records(id) ON DELETE CASCADE,
    item_type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(12, 0) NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_salaries_admin_id ON salaries(admin_id);
CREATE INDEX idx_salaries_effective ON salaries(effective_from, effective_to);
CREATE INDEX idx_payroll_records_admin_id ON payroll_records(admin_id);
CREATE INDEX idx_payroll_records_period ON payroll_records(pay_year, pay_month);
CREATE INDEX idx_payroll_records_status ON payroll_records(status);
CREATE INDEX idx_payroll_items_record_id ON payroll_items(payroll_record_id);
