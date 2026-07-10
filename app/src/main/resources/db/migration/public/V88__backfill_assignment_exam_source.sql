UPDATE exams
   SET source = 'ASSIGNMENT'
 WHERE id IN (
       SELECT linked_exam_id
         FROM assignments
        WHERE linked_exam_id IS NOT NULL
 );
