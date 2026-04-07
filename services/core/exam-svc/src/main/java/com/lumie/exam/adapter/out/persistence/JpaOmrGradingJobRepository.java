package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.OmrGradingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaOmrGradingJobRepository extends JpaRepository<OmrGradingJob, Long> {

    List<OmrGradingJob> findByExamIdOrderByCreatedAtDesc(Long examId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET processed_images = processed_images + 1, " +
            "success_count = success_count + 1, saved_count = saved_count + 1 " +
            "WHERE id = :jobId", nativeQuery = true)
    void incrementSuccess(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET processed_images = processed_images + 1, " +
            "fail_count = fail_count + 1 " +
            "WHERE id = :jobId", nativeQuery = true)
    void incrementFail(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET results = COALESCE(results, '[]'::jsonb) || :resultJson::jsonb " +
            "WHERE id = :jobId", nativeQuery = true)
    void appendResult(@Param("jobId") Long jobId, @Param("resultJson") String resultJson);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET status = 'PROCESSING' " +
            "WHERE id = :jobId AND status = 'PENDING'", nativeQuery = true)
    int startProcessingIfPending(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE omr_grading_jobs SET status = 'COMPLETED' " +
            "WHERE id = :jobId AND processed_images >= :totalImages AND status = 'PROCESSING'", nativeQuery = true)
    int completeIfAllProcessed(@Param("jobId") Long jobId, @Param("totalImages") int totalImages);
}
