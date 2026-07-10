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

    @EntityGraph(attributePaths = {"exam"})
    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    @EntityGraph(attributePaths = {"exam"})
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

    @EntityGraph(attributePaths = {"exam"})
    List<ExamResult> findByStudentIdIn(Collection<Long> studentIds);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ExamResult r
           SET r.studentId = :studentId
         WHERE r.phoneNumber = :phoneNumber
           AND r.studentId IS NULL
           AND r.studentDeleted = false
        """)
    int linkUnregisteredByPhone(@Param("phoneNumber") String phoneNumber,
                                @Param("studentId") Long studentId);

    @EntityGraph(attributePaths = {"exam", "questionResults"})
    @Query("SELECT r FROM ExamResult r JOIN r.questionResults qr WHERE qr.id = :questionResultId")
    Optional<ExamResult> findByQuestionResultId(@Param("questionResultId") Long questionResultId);

    @EntityGraph(attributePaths = {"exam", "questionResults"})
    @Query("SELECT r FROM ExamResult r WHERE r.id = :id")
    Optional<ExamResult> findWithQuestionResultsById(@Param("id") Long id);

    /**
     * Top students by average total_score over results graded this month.
     * "This month" is the Asia/Seoul calendar month keyed on updated_at — the
     * grade / re-grade time. created_at is updatable=false, so a result first
     * inserted in a prior month but (re)graded this month must still count;
     * filtering on created_at silently drops every re-graded sheet. The month
     * boundary is computed in Asia/Seoul so it matches the Korean calendar
     * rather than UTC (which lags by 9h at the month edge).
     * <p>Known edge: {@code linkUnregisteredByPhone} also bumps updated_at, so a
     * prior-month result back-filled to a student who registers this month can
     * surface in the window. Accepted over an {@code omr_grading_job_id IS NOT
     * NULL} guard, which would silently drop manually-graded results. Native
     * query — studentName is left joined later in the service via StudentService.
     */
    @Query(value = """
        SELECT r.student_id AS studentId,
               AVG(r.total_score) AS avgScore,
               COUNT(*) AS attemptCount
          FROM exam_results r
         WHERE r.student_id IS NOT NULL
           AND r.updated_at >= date_trunc('month', now() AT TIME ZONE 'Asia/Seoul') AT TIME ZONE 'Asia/Seoul'
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

    // ---------------------------------------------------------------------
    // Dashboard statistics — compact SQL aggregates over the whole tenant.
    // Aggregation is pushed into Postgres (COUNT / SUM / GROUP BY) so the
    // dashboard never materialises ExamResult / QuestionResult entities into
    // heap. All queries are RLS-scoped to the current tenant, so each one only
    // ever scans that tenant's rows. camelCase aliases are quoted so the column
    // labels survive Postgres' lower-case folding for the projection mapper.
    // ---------------------------------------------------------------------

    /** Result count + total-score sum across all of the tenant's exams. */
    @Query(value = """
        SELECT COUNT(*)                       AS "resultCount",
               COALESCE(SUM(r.total_score), 0) AS "scoreSum"
          FROM exam_results r
        """, nativeQuery = true)
    ResultAggregateRow aggregateResults();

    /** (gradeScale, grade) -> count across the tenant. Scale comes from the exam. */
    @Query(value = """
        SELECT e.grade_scale AS "gradeScale",
               r.grade       AS grade,
               COUNT(*)      AS cnt
          FROM exam_results r
          JOIN exams e ON e.id = r.exam_id
         WHERE r.grade IS NOT NULL
           AND r.grade > 0
         GROUP BY e.grade_scale, r.grade
        """, nativeQuery = true)
    List<GradeBucketRow> aggregateGradeBuckets();

    /**
     * Per (examId, questionNumber, selectedChoice, isCorrect) counts across the
     * tenant. Row cardinality is bounded by exams × questions × distinct choices
     * × 2 — a few thousand rows at most, independent of the number of results.
     */
    @Query(value = """
        SELECT r.exam_id        AS "examId",
               qr.question_number AS "questionNumber",
               qr.selected_choice AS "selectedChoice",
               qr.is_correct      AS correct,
               COUNT(*)           AS cnt
          FROM question_results qr
          JOIN exam_results r ON r.id = qr.exam_result_id
         GROUP BY r.exam_id, qr.question_number, qr.selected_choice, qr.is_correct
        """, nativeQuery = true)
    List<QuestionChoiceCountRow> aggregateQuestionChoices();

    interface ResultAggregateRow {
        long getResultCount();
        long getScoreSum();
    }

    interface GradeBucketRow {
        String getGradeScale();
        Integer getGrade();
        Integer getCnt();
    }

    interface QuestionChoiceCountRow {
        Long getExamId();
        Integer getQuestionNumber();
        String getSelectedChoice();
        Boolean getCorrect();
        Integer getCnt();
    }
}
