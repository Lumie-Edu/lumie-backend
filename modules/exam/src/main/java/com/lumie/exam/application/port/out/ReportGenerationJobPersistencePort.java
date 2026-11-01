package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.vo.JobStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Application-owned report job persistence operations. */
public interface ReportGenerationJobPersistencePort {

    Optional<ReportGenerationJob> findByIdForUpdate(Long jobId);
    List<ReportGenerationJob> findByExamIdOrderByCreatedAtDesc(Long examId);
    void incrementSuccess(Long jobId);
    void incrementFail(Long jobId);
    void setZipFileKey(Long jobId, String zipFileKey, Instant zipExpiresAt);
    int markProcessingIfPending(Long jobId);
    int markCompletedIfAllProcessed(Long jobId, int totalReports);
    int markFailedIfAllProcessed(Long jobId, int totalReports);
    List<ReportGenerationJob> findStaleOpenJobsForUpdate(Collection<JobStatus> statuses, Instant cutoff);
    int failStaleJob(Long jobId, int failedItems);
    Optional<ReportGenerationJob> findById(Long id);
    ReportGenerationJob save(ReportGenerationJob job);

    default boolean startProcessingIfPending(Long jobId) {
        return markProcessingIfPending(jobId) > 0;
    }

    default boolean tryFinalize(Long jobId, int totalReports) {
        return markCompletedIfAllProcessed(jobId, totalReports) > 0
                || markFailedIfAllProcessed(jobId, totalReports) > 0;
    }
}
