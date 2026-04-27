-- Add unique constraint on classes (name, academy_id)
-- Prevents duplicate class names within the same academy and protects against
-- race conditions in ClassCommandService.createClass when concurrent requests
-- both pass the existsByNameAndAcademyId check before either has inserted.
--
-- NOTE: If existing data contains duplicate (name, academy_id) rows, this
-- migration will FAIL. Run the diagnostic query below before applying:
--
--   SELECT name, academy_id, COUNT(*) AS cnt
--   FROM classes
--   GROUP BY name, academy_id
--   HAVING COUNT(*) > 1;
--
-- Resolve duplicates manually (rename or merge) before re-running migration.
ALTER TABLE classes
    ADD CONSTRAINT classes_name_academy_id_key UNIQUE (name, academy_id);
