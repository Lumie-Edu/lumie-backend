-- V5: Create file_metadata table for file-svc
-- This migration is applied to each tenant schema

CREATE TABLE IF NOT EXISTS file_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(20) NOT NULL,
    entity_id BIGINT,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    upload_completed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_file_metadata_entity_type_id ON file_metadata(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_file_metadata_upload_completed ON file_metadata(upload_completed);
