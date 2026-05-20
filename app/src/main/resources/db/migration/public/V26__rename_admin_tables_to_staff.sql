-- Migration: rename admin entity tables to staff.
-- Renames: admins → staff, admin_permissions → staff_permissions
-- Renames all associated indexes, constraints, sequences, and FK references.
-- Idempotent: wrapped in DO block — safe to re-run if partially applied.

DO $$
BEGIN

  -- ── admins → staff ──────────────────────────────────────────────────────────

  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'admins') THEN

    -- rename table
    ALTER TABLE admins RENAME TO staff;

    -- rename sequence (IDENTITY column auto-creates a sequence)
    IF EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'admins_id_seq') THEN
      ALTER SEQUENCE admins_id_seq RENAME TO staff_id_seq;
    END IF;

    -- rename primary key constraint
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'admins_pkey') THEN
      ALTER TABLE staff RENAME CONSTRAINT admins_pkey TO staff_pkey;
    END IF;

    -- rename unique constraint on user_id
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'admins_user_id_key') THEN
      ALTER TABLE staff RENAME CONSTRAINT admins_user_id_key TO staff_user_id_key;
    END IF;

    -- rename FK to users
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'admins_user_id_fkey') THEN
      ALTER TABLE staff RENAME CONSTRAINT admins_user_id_fkey TO staff_user_id_fkey;
    END IF;

    -- rename indexes
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_admins_email') THEN
      ALTER INDEX idx_admins_email RENAME TO idx_staff_email;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_admins_employment_status') THEN
      ALTER INDEX idx_admins_employment_status RENAME TO idx_staff_employment_status;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_admins_phone') THEN
      ALTER INDEX idx_admins_phone RENAME TO idx_staff_phone;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_admins_user_id') THEN
      ALTER INDEX idx_admins_user_id RENAME TO idx_staff_user_id;
    END IF;

    -- rename column admin_memo → staff_memo
    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name = 'staff' AND column_name = 'admin_memo'
    ) THEN
      ALTER TABLE staff RENAME COLUMN admin_memo TO staff_memo;
    END IF;

  END IF;

  -- ── admin_permissions → staff_permissions ────────────────────────────────────

  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'admin_permissions') THEN

    -- rename table
    ALTER TABLE admin_permissions RENAME TO staff_permissions;

    -- rename primary key constraint
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'admin_permissions_pkey') THEN
      ALTER TABLE staff_permissions RENAME CONSTRAINT admin_permissions_pkey TO staff_permissions_pkey;
    END IF;

    -- rename column admin_id → staff_id
    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name = 'staff_permissions' AND column_name = 'admin_id'
    ) THEN
      ALTER TABLE staff_permissions RENAME COLUMN admin_id TO staff_id;
    END IF;

    -- rename FK to staff table
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'admin_permissions_admin_id_fkey') THEN
      ALTER TABLE staff_permissions RENAME CONSTRAINT admin_permissions_admin_id_fkey TO staff_permissions_staff_id_fkey;
    END IF;

    -- rename index
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_admin_permissions_admin_id') THEN
      ALTER INDEX idx_admin_permissions_admin_id RENAME TO idx_staff_permissions_staff_id;
    END IF;

  END IF;

  -- ── Update FK references in other tables pointing at admins ──────────────────
  -- classes.teacher_id → staff(id)  (FK was: classes_teacher_id_fkey)
  -- lectures.teacher_id → staff(id) (FK was: fk_lectures_teacher)
  -- sms_messages.sender_id → staff(id) (FK was: sms_messages_sender_id_fkey)
  -- These FKs reference the table by name. After the table rename, Postgres
  -- automatically updates the FK referencing the renamed table, so no action
  -- needed for the FK objects themselves. The constraint names just remain
  -- as-is (they reference the old name but that is cosmetic only).

END
$$;
