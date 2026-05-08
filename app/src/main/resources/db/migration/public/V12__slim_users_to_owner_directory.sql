-- V12: Slim public.users into an OWNER-only routing directory.
--
-- Background: V31-V33 (tenant-side) moved full user identity to per-tenant
-- users tables and rewrote the cross-schema FKs to point there. This public
-- migration completes the split by:
--   1. Removing all non-OWNER rows (their identity is now in tenant_*.users).
--   2. Dropping the columns whose data has moved to tenant_*.users
--      (password_hash, name, phone, status, avatar_seed).
--   3. Adding a CHECK constraint enforcing the OWNER-only invariant.
--
-- Columns kept on the directory: id, user_login_id, tenant_id, role, email,
-- created_at, updated_at. The email column stays for future password-recovery
-- flows that hit the root entry point (lumie-edu.com).

-- ---------------------------------------------------------------------------
-- 1. Remove non-OWNER rows. Their data has been backfilled into
--    tenant_*.users by V32; deleting here is purely a cleanup step.
-- ---------------------------------------------------------------------------
DELETE FROM users WHERE role != 'OWNER';

-- ---------------------------------------------------------------------------
-- 2. Drop columns whose data lives only in tenant_*.users from now on.
--    IF EXISTS guards make the migration idempotent against partial reruns.
-- ---------------------------------------------------------------------------
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
ALTER TABLE users DROP COLUMN IF EXISTS name;
ALTER TABLE users DROP COLUMN IF EXISTS phone;
ALTER TABLE users DROP COLUMN IF EXISTS status;
ALTER TABLE users DROP COLUMN IF EXISTS avatar_seed;

-- ---------------------------------------------------------------------------
-- 3. Enforce the OWNER-only invariant. Without this, a buggy code path or a
--    direct DB write could insert a STUDENT row and silently break root-login
--    routing (the directory adapter filters by role = 'OWNER').
-- ---------------------------------------------------------------------------
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_owner_only;
ALTER TABLE users
    ADD CONSTRAINT chk_users_owner_only CHECK (role = 'OWNER');
