-- Preserve the legacy boolean as a projection of the explicit lifecycle.
-- PostgreSQL's fast default avoids rewriting the hot attachment table. Only
-- legacy rows that were already incomplete need an explicit backfill.
ALTER TABLE file_metadata ADD COLUMN IF NOT EXISTS upload_status VARCHAR(16) NOT NULL DEFAULT 'READY';
UPDATE file_metadata
SET upload_status = 'FAILED'
WHERE upload_completed = false AND upload_status IS DISTINCT FROM 'FAILED';
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'file_metadata_upload_status_check'
          AND conrelid = 'file_metadata'::regclass
    ) THEN
        ALTER TABLE file_metadata ADD CONSTRAINT file_metadata_upload_status_check
            CHECK (upload_status IN ('PENDING', 'READY', 'FAILED', 'DELETING')) NOT VALID;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS file_storage_operation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    file_id UUID NOT NULL,
    operation_type VARCHAR(16) NOT NULL CHECK (operation_type IN ('PROMOTE', 'DELETE')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY', 'COMPLETED', 'FAILED')),
    source_object_key VARCHAR(500),
    target_object_key VARCHAR(500) NOT NULL,
    source_etag VARCHAR(255),
    expected_size BIGINT,
    expected_content_type VARCHAR(100),
    source_created_at TIMESTAMPTZ,
    source_expires_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deadline_at TIMESTAMPTZ NOT NULL,
    lease_owner VARCHAR(120),
    lease_until TIMESTAMPTZ,
    last_error_code VARCHAR(80),
    last_error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_file_storage_operation_due
    ON file_storage_operation (tenant_id, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'RETRY');
CREATE INDEX IF NOT EXISTS idx_file_storage_operation_lease
    ON file_storage_operation (tenant_id, lease_until)
    WHERE status = 'PROCESSING';
CREATE INDEX IF NOT EXISTS idx_file_storage_operation_file_status
    ON file_storage_operation (tenant_id, file_id, status);

ALTER TABLE file_storage_operation ENABLE ROW LEVEL SECURITY;
ALTER TABLE file_storage_operation FORCE ROW LEVEL SECURITY;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = current_schema()
          AND tablename = 'file_storage_operation'
          AND policyname = 'tenant_isolation'
    ) THEN
        CREATE POLICY tenant_isolation ON file_storage_operation
            USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
            WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);
    END IF;
END $$;

GRANT SELECT, INSERT, UPDATE, DELETE ON file_storage_operation TO lumie_app;
