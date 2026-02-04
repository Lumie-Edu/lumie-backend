package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    List<ExamResult> findByExamId(Long examId);

    List<ExamResult> findByStudentId(Long studentId);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    @Query(nativeQuery = true, value = """
        SELECT
            a.id AS academyId,
            a.name AS academyName,
            COUNT(r.id) AS participantCount,
            COALESCE(AVG(r.total_score), 0) AS average,
            COUNT(CASE WHEN r.grade = 1 THEN 1 END) AS grade1Count
        FROM exam_results r
        JOIN students s ON r.student_id = s.id
        JOIN academies a ON s.academy_id = a.id
        WHERE r.exam_id = :examId
        GROUP BY a.id, a.name
        ORDER BY AVG(r.total_score) DESC
        """)
    List<AcademyComparisonProjection> findAcademyComparisonByExamId(@Param("examId") Long examId);

    @Query(nativeQuery = true, value = """
        SELECT
            a.id AS academyId,
            a.name AS academyName,
            r.student_id AS studentId,
            r.total_score AS totalScore,
            r.grade AS grade
        FROM exam_results r
        JOIN students s ON r.student_id = s.id
        JOIN academies a ON s.academy_id = a.id
        WHERE r.exam_id = :examId
        """)
    List<ResultWithAcademyProjection> findResultsWithAcademyByExamId(@Param("examId") Long examId);
}
