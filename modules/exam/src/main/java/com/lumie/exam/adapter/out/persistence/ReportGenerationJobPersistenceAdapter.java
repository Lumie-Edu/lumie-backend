package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.ReportGenerationJobPersistencePort;
import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.vo.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportGenerationJobPersistenceAdapter implements ReportGenerationJobPersistencePort {

    private final ReportGenerationJobJpaRepository jpaRepository;

    @Override public Optional<ReportGenerationJob> findByIdForUpdate(Long jobId) {
        return jpaRepository.findByIdForUpdate(jobId);
    }
    @Override public List<ReportGenerationJob> findByExamIdOrderByCreatedAtDesc(Long examId) {
        return jpaRepository.findByExamIdOrderByCreatedAtDesc(examId);
    }
    @Override public void incrementSuccess(Long jobId) { jpaRepository.incrementSuccess(jobId); }
    @Override public void incrementFail(Long jobId) { jpaRepository.incrementFail(jobId); }
    @Override public void setZipFileKey(Long jobId, String key, Instant expiresAt) {
        jpaRepository.setZipFileKey(jobId, key, expiresAt);
    }
    @Override public int markProcessingIfPending(Long jobId) { return jpaRepository.markProcessingIfPending(jobId); }
    @Override public int markCompletedIfAllProcessed(Long jobId, int totalReports) {
        return jpaRepository.markCompletedIfAllProcessed(jobId, totalReports);
    }
    @Override public int markFailedIfAllProcessed(Long jobId, int totalReports) {
        return jpaRepository.markFailedIfAllProcessed(jobId, totalReports);
    }
    @Override public List<ReportGenerationJob> findStaleOpenJobsForUpdate(Collection<JobStatus> statuses,
            Instant cutoff) {
        return jpaRepository.findStaleOpenJobsForUpdate(statuses, cutoff);
    }
    @Override public int failStaleJob(Long jobId, int failedItems) {
        return jpaRepository.failStaleJob(jobId, failedItems);
    }
    @Override public Optional<ReportGenerationJob> findById(Long id) { return jpaRepository.findById(id); }
    @Override public ReportGenerationJob save(ReportGenerationJob job) { return jpaRepository.save(job); }
}
