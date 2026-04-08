-- Rename role values: ADMIN → INSTRUCTOR, DEVELOPER → MANAGER
UPDATE users SET role = 'INSTRUCTOR' WHERE role = 'ADMIN';
UPDATE users SET role = 'MANAGER' WHERE role = 'DEVELOPER';
