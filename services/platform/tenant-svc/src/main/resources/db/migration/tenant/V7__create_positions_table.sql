-- Create positions table
CREATE TABLE IF NOT EXISTS positions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Delete existing admins to avoid migration issues with legacy data
DELETE FROM admin_academies;
DELETE FROM admins;

-- Drop old admin_position column
ALTER TABLE admins DROP COLUMN IF EXISTS admin_position;

-- Add position_id column with FK
ALTER TABLE admins ADD COLUMN position_id BIGINT;

ALTER TABLE admins
    ADD CONSTRAINT fk_admins_position
    FOREIGN KEY (position_id) REFERENCES positions(id);
