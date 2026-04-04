-- Add phone_number column for unregistered students
ALTER TABLE exam_results ADD COLUMN phone_number VARCHAR(20);

-- Make student_id nullable
ALTER TABLE exam_results ALTER COLUMN student_id DROP NOT NULL;

-- Replace single unique constraint with partial indexes
ALTER TABLE exam_results DROP CONSTRAINT IF EXISTS uq_exam_results_exam_id_student_id;

CREATE UNIQUE INDEX uq_exam_result_registered
    ON exam_results(exam_id, student_id) WHERE student_id IS NOT NULL;

CREATE UNIQUE INDEX uq_exam_result_unregistered
    ON exam_results(exam_id, phone_number) WHERE student_id IS NULL AND phone_number IS NOT NULL;
