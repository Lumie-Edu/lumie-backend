-- Add unique constraint on exam_results (exam_id, student_id) for registered students
-- Prevents duplicate results when parallel grading processes the same student concurrently
CREATE UNIQUE INDEX uq_exam_result_student ON exam_results (exam_id, student_id)
    WHERE student_id IS NOT NULL;
