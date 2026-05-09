-- Make class_id optional (lectures can exist without a class)
ALTER TABLE lectures ALTER COLUMN class_id DROP NOT NULL;
ALTER TABLE lectures DROP CONSTRAINT IF EXISTS fk_lectures_class;
