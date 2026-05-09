-- Remove position_id from admins and drop positions table
ALTER TABLE admins DROP CONSTRAINT IF EXISTS fk_admins_position;
ALTER TABLE admins DROP COLUMN IF EXISTS position_id;

DROP TABLE IF EXISTS positions;
DROP SEQUENCE IF EXISTS positions_id_seq;
