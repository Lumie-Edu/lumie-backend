-- V1: Initial public schema structure
-- This migration is applied to the public schema

-- Users table (global, stores all users across all tenants)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_login_id VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    oauth_provider VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_public_users_login_id ON users(user_login_id);
CREATE INDEX IF NOT EXISTS idx_public_users_tenant_id ON users(tenant_id);
