--
-- Fix default status: ACTIVE → PENDING (matches domain entity default)
--

ALTER TABLE ONLY classes ALTER COLUMN status SET DEFAULT 'PENDING'::character varying;
