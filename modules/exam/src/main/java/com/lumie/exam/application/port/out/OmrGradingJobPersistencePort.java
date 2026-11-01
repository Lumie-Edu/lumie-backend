package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.vo.JobStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Application-owned OMR grading-job persistence operations. */
public interface OmrGradingJobPersistencePort {

    List<OmrGradingJob> findByExamIdOrderByCreatedAtDesc(Long examId);
    boolean existsByExamIdAndStatusIn(Long examId, Collection<JobStatus> statuses);
    int deleteTerminalJobsByExamId(Long examId);
    List<RecentCompletedExamRow> findRecentCompletedExams();
    boolean existsByIdAndStatusIn(Long id, Collection<JobStatus> statuses);
    void incrementSuccess(Long jobId);
    void incrementFail(Long jobId);
    int markProcessingIfPending(Long jobId);
    int markCompletedIfAllProcessed(Long jobId, int totalImages);
    int markFailedIfAllProcessed(Long jobId, int totalImages);
    int markStaleAsFailed(Instant cutoff);
    Optional<OmrGradingJob> findById(Long id);
    OmrGradingJob save(OmrGradingJob job);

    default boolean startProcessingIfPending(Long jobId) {
        return markProcessingIfPending(jobId) > 0;
    }

    default boolean tryFinalize(Long jobId, int totalImages) {
        return markCompletedIfAllProcessed(jobId, totalImages) > 0
                || markFailedIfAllProcessed(jobId, totalImages) > 0;
    }

    interface RecentCompletedExamRow {
        Long getExamId();
        String getExamName();
        Instant getCompletedAt();
        long getParticipantCount();
        Double getAverageScore();
    }
}
