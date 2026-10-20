-- Keep expiry nullable for existing jobs. Legacy ZIPs without an authoritative expiry
-- are treated as unavailable by the application and removed by the tmp/ MinIO ILM.
ALTER TABLE report_generation_jobs
    ADD COLUMN IF NOT EXISTS zip_expires_at TIMESTAMPTZ;
