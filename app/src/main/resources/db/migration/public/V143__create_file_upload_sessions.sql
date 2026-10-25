CREATE TABLE IF NOT EXISTS file_upload_sessions (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    initiating_user_id BIGINT NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    object_key VARCHAR(500) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    upload_url_expires_at TIMESTAMPTZ NOT NULL,
    session_expires_at TIMESTAMPTZ NOT NULL,
    uploaded_etag VARCHAR(255),
    claimed_file_id UUID,
    status VARCHAR(24) NOT NULL CHECK (status IN (
        'ISSUED', 'UPLOADED', 'CLAIMED', 'CANCEL_REQUESTED', 'EXPIRED',
        'CLEANUP_PENDING', 'CLEANED', 'CLEANUP_FAILED'
    )),
    last_cleanup_error VARCHAR(1000),
    cleanup_attempts INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_file_upload_sessions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_upload_sessions_claimed_file
        FOREIGN KEY (claimed_file_id) REFERENCES file_metadata(id) ON DELETE SET NULL,
    CONSTRAINT ck_file_upload_sessions_expiry
        CHECK (session_expires_at > upload_url_expires_at)
);

CREATE INDEX IF NOT EXISTS idx_file_upload_sessions_tenant_user
    ON file_upload_sessions (tenant_id, initiating_user_id, id);

ALTER TABLE file_upload_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE file_upload_sessions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON file_upload_sessions
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

COMMENT ON TABLE file_upload_sessions IS
    'Durable per-file upload authorization, claim, and event-driven orphan cleanup lifecycle.';
COMMENT ON COLUMN file_upload_sessions.object_key IS
    'Unique private MinIO object key scoped to one upload session; never accepted from a post-save client request.';
COMMENT ON COLUMN file_upload_sessions.claimed_file_id IS
    'File metadata row that claimed this session; claim occurs in the same transaction as the owning domain save.';
