package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ReportGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaReportGenerationJobRepository extends JpaRepository<ReportGenerationJob, Long> {

    List<ReportGenerationJob> findByExamIdOrderByCreatedAtDesc(Long examId);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET processed_reports = processed_reports + 1, " +
            "success_count = success_count + 1 WHERE id = :jobId", nativeQuery = true)
    void incrementSuccess(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET processed_reports = processed_reports + 1, " +
            "fail_count = fail_count + 1 WHERE id = :jobId", nativeQuery = true)
    void incrementFail(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET status = 'PROCESSING' " +
            "WHERE id = :jobId AND status = 'PENDING'", nativeQuery = true)
    int startProcessingIfPending(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET status = 'COMPLETED' " +
            "WHERE id = :jobId AND processed_reports >= :totalReports AND status = 'PROCESSING'", nativeQuery = true)
    int completeIfAllProcessed(@Param("jobId") Long jobId, @Param("totalReports") int totalReports);

    @Modifying
    @Query(value = "UPDATE report_generation_jobs SET zip_file_key = :zipFileKey " +
            "WHERE id = :jobId", nativeQuery = true)
    void setZipFileKey(@Param("jobId") Long jobId, @Param("zipFileKey") String zipFileKey);
}
