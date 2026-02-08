-- V9: Add admin fields and position permissions
-- Extends admins table with employee management fields
-- Creates permissions and position_permissions tables for RBAC

-- admins table extensions
ALTER TABLE admins ADD COLUMN email VARCHAR(255);
ALTER TABLE admins ADD COLUMN hire_date DATE;
ALTER TABLE admins ADD COLUMN contract_start_date DATE;
ALTER TABLE admins ADD COLUMN contract_end_date DATE;
ALTER TABLE admins ADD COLUMN contract_type VARCHAR(30);
ALTER TABLE admins ADD COLUMN employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- permissions table (feature-level access control)
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- position_permissions join table (Position <-> Permission M2M)
CREATE TABLE position_permissions (
    position_id BIGINT NOT NULL REFERENCES positions(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (position_id, permission_id)
);

-- Indexes
CREATE INDEX idx_admins_email ON admins(email);
CREATE INDEX idx_admins_employment_status ON admins(employment_status);
CREATE INDEX idx_permissions_category ON permissions(category);
