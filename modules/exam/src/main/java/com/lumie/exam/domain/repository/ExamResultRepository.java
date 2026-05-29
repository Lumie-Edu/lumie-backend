package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.ExamResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    Optional<ExamResult> findByExamIdAndPhoneNumber(Long examId, String phoneNumber);

    @EntityGraph(attributePaths = {"exam", "questionResults"})
    List<ExamResult> findByExamId(Long examId);

    @EntityGraph(attributePaths = {"exam", "questionResults"})
    List<ExamResult> findByOmrGradingJobId(Long omrGradingJobId);

    /**
     * Batch-fetch all results across multiple exams. Used to avoid N+1 queries
     * when computing per-exam statistics for a student's history.
     */
    List<ExamResult> findByExamIdIn(Collection<Long> examIds);

    List<ExamResult> findByExamIdAndStudentIdIsNotNull(Long examId);

    @EntityGraph(attributePaths = {"exam", "questionResults"})
    List<ExamResult> findByStudentId(Long studentId);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ExamResult r
           SET r.studentId = :studentId
         WHERE r.phoneNumber = :phoneNumber
           AND r.studentId IS NULL
        """)
    int linkUnregisteredByPhone(@Param("phoneNumber") String phoneNumber,
                                @Param("studentId") Long studentId);

    @EntityGraph(attributePaths = {"exam", "questionResults"})
    @Query("SELECT r FROM ExamResult r JOIN r.questionResults qr WHERE qr.id = :questionResultId")
    Optional<ExamResult> findByQuestionResultId(@Param("questionResultId") Long questionResultId);

    /**
     * Top students by average total_score for the current month. Native query —
     * studentName is left joined later in the service via StudentService.
     */
    @Query(value = """
        SELECT r.student_id AS studentId,
               AVG(r.total_score) AS avgScore,
               COUNT(*) AS attemptCount
          FROM exam_results r
         WHERE r.student_id IS NOT NULL
           AND r.created_at >= date_trunc('month', now())
         GROUP BY r.student_id
         ORDER BY AVG(r.total_score) DESC, COUNT(*) DESC
         LIMIT :limit
        """, nativeQuery = true)
    List<TopStudentAggregateRow> findTopStudentsForCurrentMonth(@Param("limit") int limit);

    interface TopStudentAggregateRow {
        Long getStudentId();
        Double getAvgScore();
        Integer getAttemptCount();
    }
}
