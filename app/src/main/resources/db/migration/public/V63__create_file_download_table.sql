CREATE TABLE IF NOT EXISTS file_download (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    file_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_file_download_file_user UNIQUE (tenant_id, file_id, user_id)
);

CREATE INDEX idx_file_download_file_id ON file_download(file_id);

DROP TABLE IF EXISTS textbook_read CASCADE;
DROP TABLE IF EXISTS textbook CASCADE;
