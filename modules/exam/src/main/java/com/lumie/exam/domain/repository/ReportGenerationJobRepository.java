package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.vo.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ReportGenerationJobRepository extends JpaRepository<ReportGenerationJob, Long> {

    List<ReportGenerationJob> findByExamIdOrderByCreatedAtDesc(Long examId);

    boolean existsByIdAndStatusIn(Long id, Collection<JobStatus> statuses);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET processed_reports = processed_reports + 1, " +
            "success_count = success_count + 1 WHERE id = :jobId", nativeQuery = true)
    void incrementSuccess(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET processed_reports = processed_reports + 1, " +
            "fail_count = fail_count + 1 WHERE id = :jobId", nativeQuery = true)
    void incrementFail(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET zip_file_key = :zipFileKey " +
            "WHERE id = :jobId", nativeQuery = true)
    void setZipFileKey(@Param("jobId") Long jobId, @Param("zipFileKey") String zipFileKey);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET status = 'PROCESSING' " +
            "WHERE id = :jobId AND status = 'PENDING'", nativeQuery = true)
    int markProcessingIfPending(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET status = 'COMPLETED', updated_at = NOW() " +
            "WHERE id = :jobId AND processed_reports >= :totalReports " +
            "  AND success_count > 0 AND status = 'PROCESSING'", nativeQuery = true)
    int markCompletedIfAllProcessed(@Param("jobId") Long jobId, @Param("totalReports") int totalReports);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET status = 'FAILED', updated_at = NOW() " +
            "WHERE id = :jobId AND processed_reports >= :totalReports " +
            "  AND success_count = 0 AND status = 'PROCESSING'", nativeQuery = true)
    int markFailedIfAllProcessed(@Param("jobId") Long jobId, @Param("totalReports") int totalReports);

    default boolean startProcessingIfPending(Long jobId) {
        return markProcessingIfPending(jobId) > 0;
    }

    /**
     * Idempotently transition to a terminal state once every report's callback has
     * landed. COMPLETED if any succeeded, FAILED if all failed. Returns true on
     * transition. Outer {@code @Transactional} required by the {@code @Modifying}
     * native queries.
     */
    default boolean tryFinalize(Long jobId, int totalReports) {
        return markCompletedIfAllProcessed(jobId, totalReports) > 0
                || markFailedIfAllProcessed(jobId, totalReports) > 0;
    }

    /**
     * Mark every open report job whose row was created before {@code cutoff} as
     * {@code FAILED}. Covers both worker crashes after the first callback
     * ({@code PROCESSING}) and request messages that never reached a worker
     * ({@code PENDING}).
     */
    @Modifying
    @Query(value = "UPDATE report_generation_jobs " +
            "SET status = 'FAILED', updated_at = NOW() " +
            "WHERE status IN ('PENDING', 'PROCESSING') AND created_at < :cutoff", nativeQuery = true)
    int markStaleAsFailed(@Param("cutoff") Instant cutoff);
}
