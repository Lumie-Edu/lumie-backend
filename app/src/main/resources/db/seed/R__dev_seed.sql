-- Dev seed data. Repeatable migration — re-runs when this file changes.
-- Idempotent; the demo row converges its plan_id on re-run so a stale
-- value can't wedge backend startup. Do NOT run on prod (excluded from
-- prod Flyway locations).

-- Demo tenant. plan_id must be a valid TenantPlan enum constant
-- (FREE | BASIC | PRO | MAX) — it is read back as the entity enum.
INSERT INTO tenants (slug, name, display_name, status, owner_email, plan_id, institute_name)
VALUES ('demo', 'Demo Academy', '데모 학원', 'ACTIVE', 'demo@lumie-edu.com', 'FREE', '데모학원')
ON CONFLICT (slug) DO UPDATE SET plan_id = EXCLUDED.plan_id;
