-- Drop fields that were added for an automated CF Registrar workflow.
-- Custom domain is now operated manually: a single mapping column on tenants
-- is enough for host-based tenant lookup at the edge.

DROP TABLE IF EXISTS custom_domain_requests;

ALTER TABLE tenants
    DROP COLUMN IF EXISTS custom_domain_status,
    DROP COLUMN IF EXISTS custom_domain_activated_at,
    DROP COLUMN IF EXISTS custom_domain_expires_at,
    DROP COLUMN IF EXISTS cf_zone_id,
    DROP COLUMN IF EXISTS cf_registration_id;
