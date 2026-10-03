package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.ExamResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    @EntityGraph(attributePaths = {"exam"})
    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    Optional<ExamResult> findByCurrentSubmissionId(Long currentSubmissionId);

    @Query("""
        select count(r)
          from ExamResult r
         where r.omrGradingJobId = :jobId
           and r.currentSubmissionId is not null
        """)
    int countOfficializedByJobId(@Param("jobId") Long jobId);

    @Query("""
        select r.omrGradingJobId as jobId, count(r) as savedCount
          from ExamResult r
         where r.omrGradingJobId in :jobIds
           and r.currentSubmissionId is not null
         group by r.omrGradingJobId
        """)
    List<JobSavedCountRow> countOfficializedByJobIds(@Param("jobIds") Collection<Long> jobIds);

    @EntityGraph(attributePaths = {"exam", "questionResults"})
    List<ExamResult> findByExamId(Long examId);

    @EntityGraph(attributePaths = {"exam", "questionResults"})
    List<ExamResult> findByOmrGradingJobId(Long omrGradingJobId);

    /**
     * Batch-fetch all results across multiple exams. Used to avoid N+1 queries
     * when computing per-exam statistics for a student's history.
     */
    @EntityGraph(attributePaths = {"exam"})
    List<ExamResult> findByExamIdIn(Collection<Long> examIds);

    List<ExamResult> findByExamIdAndStudentIdIsNotNull(Long examId);

    @EntityGraph(attributePaths = {"exam", "questionResults"})
    List<ExamResult> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"exam"})
    List<ExamResult> findByStudentIdIn(Collection<Long> studentIds);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    @EntityGraph(attributePaths = {"exam"})
    List<ExamResult> findByPhoneNumberAndStudentIdIsNullAndStudentDeletedFalse(String phoneNumber);

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
     * <p>Known edge: student-registration backfill can bump updated_at, so a
     * prior-month result linked to a student who registers this month can surface
     * in the window. Accepted over an {@code omr_grading_job_id IS NOT NULL}
     * guard, which would silently drop manually-graded results. Native query —
     * studentName is left joined later in the service via StudentService.
     */
    @Query(value = """
        SELECT r.student_id AS studentId,
               AVG(r.total_score) AS avgScore,
               COUNT(*) AS attemptCount
          FROM exam_results r
          JOIN exams e ON e.id = r.exam_id
         WHERE r.student_id IS NOT NULL
           AND e.source = 'EXAM_MANAGEMENT'
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

    interface JobSavedCountRow {
        Long getJobId();
        long getSavedCount();
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
          JOIN exams e ON e.id = r.exam_id
         WHERE e.source = 'EXAM_MANAGEMENT'
        """, nativeQuery = true)
    ResultAggregateRow aggregateResults();

    /** Current academic-grade buckets, derived from raw scores rather than historical stored grades. */
    @Query(value = """
        WITH ranked AS (
            SELECT COALESCE(e.grade_scale, 'NINE_GRADE') AS grade_scale,
                   e.grading_type,
                   r.total_score,
                   (RANK() OVER (PARTITION BY r.exam_id ORDER BY r.total_score DESC) - 1)
                       * 100.0 / COUNT(*) OVER (PARTITION BY r.exam_id) AS percentile
              FROM exam_results r
              JOIN exams e ON e.id = r.exam_id
             WHERE e.category = 'GRADED'
               AND e.source = 'EXAM_MANAGEMENT'
        ), graded AS (
            SELECT grade_scale,
                   CASE
                     WHEN grading_type = 'RELATIVE' AND grade_scale = 'FIVE_GRADE' THEN
                       CASE WHEN percentile < 10 THEN 1 WHEN percentile < 35 THEN 2
                            WHEN percentile < 65 THEN 3 WHEN percentile < 90 THEN 4 ELSE 5 END
                     WHEN grading_type = 'RELATIVE' THEN
                       CASE WHEN percentile < 4 THEN 1 WHEN percentile < 11 THEN 2
                            WHEN percentile < 23 THEN 3 WHEN percentile < 40 THEN 4
                            WHEN percentile < 60 THEN 5 WHEN percentile < 77 THEN 6
                            WHEN percentile < 89 THEN 7 WHEN percentile < 96 THEN 8 ELSE 9 END
                     WHEN grade_scale = 'FIVE_GRADE' THEN
                       CASE WHEN total_score >= 90 THEN 1 WHEN total_score >= 80 THEN 2
                            WHEN total_score >= 60 THEN 3 WHEN total_score >= 40 THEN 4 ELSE 5 END
                     ELSE
                       CASE WHEN total_score >= 90 THEN 1 WHEN total_score >= 80 THEN 2
                            WHEN total_score >= 70 THEN 3 WHEN total_score >= 60 THEN 4
                            WHEN total_score >= 50 THEN 5 WHEN total_score >= 40 THEN 6
                            WHEN total_score >= 30 THEN 7 WHEN total_score >= 20 THEN 8 ELSE 9 END
                   END AS grade
              FROM ranked
        )
        SELECT grade_scale AS "gradeScale", grade AS grade, COUNT(*)::integer AS cnt
          FROM graded
         GROUP BY grade_scale, grade
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
          JOIN exams e ON e.id = r.exam_id
         WHERE e.source = 'EXAM_MANAGEMENT'
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
