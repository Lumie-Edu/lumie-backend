-- V25: Backfill admin row for OWNER users
--
-- OWNERs were historically created in public.users only, never in the tenant
-- admins table. From this release onward, registerOwner inserts both. For
-- existing tenants we backfill: for each OWNER user belonging to this tenant
-- (matched via the tenant slug -> schema name derivation), insert a matching
-- admins row if missing.
--
-- Idempotent: NOT EXISTS guards against re-running. Tenant migrations run per
-- schema, so current_schema() identifies which tenant we are in.

INSERT INTO admins (
    user_id,
    user_login_id,
    name,
    phone,
    email,
    is_active,
    employment_status,
    created_at,
    updated_at
)
SELECT
    u.id,
    u.user_login_id,
    u.name,
    u.phone,
    NULL,
    TRUE,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM public.users u
JOIN public.tenants t ON t.id = u.tenant_id
WHERE u.role = 'OWNER'
  AND ('tenant_' || REPLACE(t.slug, '-', '_')) = current_schema()
  AND NOT EXISTS (
      SELECT 1 FROM admins a WHERE a.user_id = u.id
  );
