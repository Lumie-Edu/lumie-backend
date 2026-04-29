ALTER TABLE tenants ADD COLUMN IF NOT EXISTS logo_object_key VARCHAR(255);

DROP TABLE IF EXISTS tenant_settings;
