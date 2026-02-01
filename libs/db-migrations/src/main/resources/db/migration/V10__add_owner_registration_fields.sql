-- Add fields for owner registration
ALTER TABLE tenants ADD COLUMN institute_name VARCHAR(200);
ALTER TABLE tenants ADD COLUMN business_registration_number VARCHAR(12);

-- Index for business registration number lookup
CREATE INDEX idx_tenants_business_number ON tenants(business_registration_number);
