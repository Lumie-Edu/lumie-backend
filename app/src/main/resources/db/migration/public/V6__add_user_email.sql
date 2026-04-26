-- Add email column to public.users so OWNER/MANAGER/INSTRUCTOR can edit
-- their email address from /admin/settings/general.
--
-- Nullable: most users (especially STUDENT) have no email yet, and we don't
-- want to invent one. The legacy `admins.email` column in tenant schemas
-- continues to exist as duplicate state and will be reconciled in a follow-up
-- when admin-svc switches to reading from public.users.

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
