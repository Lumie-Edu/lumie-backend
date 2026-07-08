-- Contract migration: tenant owner_email and business_registration_number are
-- no longer collected or stored by the application. Deploy after code that no
-- longer reads or writes these columns.
ALTER TABLE tenants DROP COLUMN IF EXISTS owner_email;
ALTER TABLE tenants DROP COLUMN IF EXISTS business_registration_number;
