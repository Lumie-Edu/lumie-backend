-- V9: exam_results에 (exam_id, student_id) unique 제약조건 추가
-- 동일 시험에 동일 학생의 중복 결과 방지

ALTER TABLE exam_results
    ADD CONSTRAINT uq_exam_results_exam_id_student_id UNIQUE (exam_id, student_id);
