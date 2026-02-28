-- Remove attendance and position_permissions tables
DROP TABLE IF EXISTS attendances;
DROP TABLE IF EXISTS position_permissions;

-- Remove contract-related columns from admins
ALTER TABLE admins DROP COLUMN IF EXISTS hire_date;
ALTER TABLE admins DROP COLUMN IF EXISTS contract_start_date;
ALTER TABLE admins DROP COLUMN IF EXISTS contract_end_date;
ALTER TABLE admins DROP COLUMN IF EXISTS contract_type;

-- Create admin_permissions table for per-admin permission management
CREATE TABLE admin_permissions (
    admin_id BIGINT NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    permission_code VARCHAR(100) NOT NULL,
    access_level VARCHAR(10) NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (admin_id, permission_code)
);
CREATE INDEX idx_admin_permissions_admin_id ON admin_permissions(admin_id);
