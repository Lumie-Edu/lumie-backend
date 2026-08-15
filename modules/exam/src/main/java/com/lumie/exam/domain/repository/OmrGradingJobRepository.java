package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.vo.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OmrGradingJobRepository extends JpaRepository<OmrGradingJob, Long> {

    List<OmrGradingJob> findByExamIdOrderByCreatedAtDesc(Long examId);

    /**
     * Dashboard-sized OMR completion summaries. The RLS policy on every joined
     * table scopes this native aggregate to the active tenant.
     */
    @Query(value = """
        WITH recent_exams AS (
            SELECT DISTINCT ON (j.exam_id)
                   j.exam_id,
                   j.updated_at
              FROM omr_grading_jobs j
              JOIN exams e ON e.id = j.exam_id
             WHERE j.status = 'COMPLETED'
               AND e.source = 'EXAM_MANAGEMENT'
             ORDER BY j.exam_id, j.updated_at DESC
        )
        SELECT recent_exams.exam_id AS "examId",
               e.name AS "examName",
               recent_exams.updated_at AS "completedAt",
               COUNT(r.id) FILTER (WHERE r.current_submission_id IS NOT NULL) AS "participantCount",
               COALESCE(
                   AVG(r.total_score) FILTER (WHERE r.current_submission_id IS NOT NULL),
                   0
               ) AS "averageScore"
          FROM recent_exams
          JOIN exams e ON e.id = recent_exams.exam_id
          LEFT JOIN exam_results r ON r.exam_id = recent_exams.exam_id
         GROUP BY recent_exams.exam_id, e.name, recent_exams.updated_at
         ORDER BY recent_exams.updated_at DESC
         LIMIT 4
        """, nativeQuery = true)
    List<RecentCompletedExamRow> findRecentCompletedExams();

    boolean existsByIdAndStatusIn(Long id, Collection<JobStatus> statuses);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET processed_images = processed_images + 1, " +
            "success_count = success_count + 1 " +
            "WHERE id = :jobId", nativeQuery = true)
    void incrementSuccess(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET processed_images = processed_images + 1, " +
            "fail_count = fail_count + 1 WHERE id = :jobId", nativeQuery = true)
    void incrementFail(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET status = 'PROCESSING' " +
            "WHERE id = :jobId AND status = 'PENDING'", nativeQuery = true)
    int markProcessingIfPending(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET status = 'COMPLETED', updated_at = NOW() " +
            "WHERE id = :jobId AND processed_images >= :totalImages " +
            "  AND success_count > 0 AND status = 'PROCESSING'", nativeQuery = true)
    int markCompletedIfAllProcessed(@Param("jobId") Long jobId, @Param("totalImages") int totalImages);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET status = 'FAILED', updated_at = NOW() " +
            "WHERE id = :jobId AND processed_images >= :totalImages " +
            "  AND success_count = 0 AND status = 'PROCESSING'", nativeQuery = true)
    int markFailedIfAllProcessed(@Param("jobId") Long jobId, @Param("totalImages") int totalImages);

    default boolean startProcessingIfPending(Long jobId) {
        return markProcessingIfPending(jobId) > 0;
    }

    interface RecentCompletedExamRow {
        Long getExamId();

        String getExamName();

        Instant getCompletedAt();

        long getParticipantCount();

        Double getAverageScore();
    }

    /**
     * Idempotently transition to a terminal state once every image's callback has
     * landed. COMPLETED if any image succeeded, FAILED if all failed. Returns true
     * on transition. Outer {@code @Transactional} required by the {@code @Modifying}
     * native queries.
     */
    default boolean tryFinalize(Long jobId, int totalImages) {
        return markCompletedIfAllProcessed(jobId, totalImages) > 0
                || markFailedIfAllProcessed(jobId, totalImages) > 0;
    }

    /**
     * Mark every open job whose row was created before {@code cutoff} as
     * {@code FAILED}. Covers both worker crashes after the first callback
     * ({@code PROCESSING}) and request messages that never reached a worker
     * ({@code PENDING}).
     */
    @Modifying
    @Query(value = "UPDATE omr_grading_jobs " +
            "SET status = 'FAILED', updated_at = NOW() " +
            "WHERE status IN ('PENDING', 'PROCESSING') AND created_at < :cutoff", nativeQuery = true)
    int markStaleAsFailed(@Param("cutoff") Instant cutoff);
}
