UPDATE tenants SET plan_id = 'MAX' WHERE plan_id = 'ENTERPRISE';

ALTER TABLE tenants ADD COLUMN IF NOT EXISTS custom_id VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenants_custom_id ON tenants(custom_id) WHERE custom_id IS NOT NULL;
