-- Role is stored as VARCHAR so no schema change needed, just update comment for documentation
COMMENT ON COLUMN users.role IS 'OWNER, DEVELOPER, ADMIN, STUDENT';
