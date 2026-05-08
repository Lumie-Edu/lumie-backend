-- V32: Backfill the per-tenant users table from public.users.
--
-- Copies every row of public.users that belongs to the current tenant into the
-- tenant-local users table. IDs are preserved so existing FK references
-- (admins.user_id, students.user_id, counseling_*.user_id, announcements.author_id,
-- qna_comments.user_id) keep pointing to the same numeric values during cutover.
--
-- After this migration:
--   - Both public.users and tenant_*.users contain the same rows for this
--     tenant (dual-write window).
--   - Application code reading via tenant context will hit tenant_*.users.
--   - Application code reading via public.users still works (legacy path).
--   - Cross-schema FKs still point to public.users(id) — rewritten in V33.
--
-- Note: public.users no longer has an `oauth_provider` column (it was dropped
-- post-V4 once federated_identities replaced it). The tenant.users table has
-- the column for forward compatibility but it stays NULL on backfill.
--
-- Idempotent: ON CONFLICT DO NOTHING handles re-runs safely.

INSERT INTO users (
    id,
    user_login_id,
    password_hash,
    name,
    phone,
    email,
    role,
    status,
    avatar_seed,
    created_at,
    updated_at
)
SELECT
    u.id,
    u.user_login_id,
    u.password_hash,
    u.name,
    u.phone,
    u.email,
    u.role,
    u.status,
    u.avatar_seed,
    u.created_at,
    u.updated_at
FROM public.users u
WHERE u.tenant_id = (
    SELECT t.id
    FROM public.tenants t
    WHERE t.schema_name = current_schema()
)
ON CONFLICT (id) DO NOTHING;

-- Advance the BIGSERIAL sequence past the highest backfilled id so that future
-- inserts (new students/instructors) don't collide with imported rows.
SELECT setval(
    pg_get_serial_sequence('users', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 0) FROM users), 1)
);
