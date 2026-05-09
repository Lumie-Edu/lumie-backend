package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.OmrGradingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OmrGradingJobRepository extends JpaRepository<OmrGradingJob, Long> {

    List<OmrGradingJob> findByExamIdOrderByCreatedAtDesc(Long examId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET processed_images = processed_images + 1, " +
            "success_count = success_count + 1, saved_count = saved_count + 1 " +
            "WHERE id = :jobId", nativeQuery = true)
    void incrementSuccess(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET processed_images = processed_images + 1, " +
            "fail_count = fail_count + 1 WHERE id = :jobId", nativeQuery = true)
    void incrementFail(@Param("jobId") Long jobId);

    /** Append a serialized JSON payload to the job's results JSONB column. Caller serializes. */
    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET results = COALESCE(results, CAST('[]' AS jsonb)) || CAST(:resultJson AS jsonb) " +
            "WHERE id = :jobId", nativeQuery = true)
    void appendResult(@Param("jobId") Long jobId, @Param("resultJson") String resultJson);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET status = 'PROCESSING' " +
            "WHERE id = :jobId AND status = 'PENDING'", nativeQuery = true)
    int markProcessingIfPending(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET status = 'COMPLETED' " +
            "WHERE id = :jobId AND processed_images >= :totalImages AND status = 'PROCESSING'", nativeQuery = true)
    int markCompletedIfAllProcessed(@Param("jobId") Long jobId, @Param("totalImages") int totalImages);

    default boolean startProcessingIfPending(Long jobId) {
        return markProcessingIfPending(jobId) > 0;
    }

    default boolean tryComplete(Long jobId, int totalImages) {
        return markCompletedIfAllProcessed(jobId, totalImages) > 0;
    }
}
