-- Dev seed data. Repeatable migration — re-runs when this file changes.
-- All inserts are idempotent (ON CONFLICT DO NOTHING).
-- Do NOT run on prod (excluded from prod Flyway locations).

-- Demo tenant
INSERT INTO tenants (slug, name, display_name, status, owner_email, plan_id, institute_name)
VALUES ('demo', 'Demo Academy', '데모 학원', 'ACTIVE', 'demo@lumie-edu.com', 'starter', '데모학원')
ON CONFLICT (slug) DO NOTHING;

-- Demo tenant settings (logo, theme defaults)
INSERT INTO tenant_settings (tenant_id)
SELECT id FROM tenants WHERE slug = 'demo'
ON CONFLICT (tenant_id) DO NOTHING;
