package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.OmrGradingJobPersistencePort;
import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.vo.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OmrGradingJobPersistenceAdapter implements OmrGradingJobPersistencePort {

    private final OmrGradingJobJpaRepository jpaRepository;

    @Override public List<OmrGradingJob> findByExamIdOrderByCreatedAtDesc(Long examId) {
        return jpaRepository.findByExamIdOrderByCreatedAtDesc(examId);
    }
    @Override public boolean existsByExamIdAndStatusIn(Long examId, Collection<JobStatus> statuses) {
        return jpaRepository.existsByExamIdAndStatusIn(examId, statuses);
    }
    @Override public int deleteTerminalJobsByExamId(Long examId) {
        return jpaRepository.deleteTerminalJobsByExamId(examId);
    }
    @Override public List<RecentCompletedExamRow> findRecentCompletedExams() {
        return jpaRepository.findRecentCompletedExams();
    }
    @Override public boolean existsByIdAndStatusIn(Long id, Collection<JobStatus> statuses) {
        return jpaRepository.existsByIdAndStatusIn(id, statuses);
    }
    @Override public void incrementSuccess(Long jobId) { jpaRepository.incrementSuccess(jobId); }
    @Override public void incrementFail(Long jobId) { jpaRepository.incrementFail(jobId); }
    @Override public int markProcessingIfPending(Long jobId) { return jpaRepository.markProcessingIfPending(jobId); }
    @Override public int markCompletedIfAllProcessed(Long jobId, int totalImages) {
        return jpaRepository.markCompletedIfAllProcessed(jobId, totalImages);
    }
    @Override public int markFailedIfAllProcessed(Long jobId, int totalImages) {
        return jpaRepository.markFailedIfAllProcessed(jobId, totalImages);
    }
    @Override public int markStaleAsFailed(Instant cutoff) { return jpaRepository.markStaleAsFailed(cutoff); }
    @Override public Optional<OmrGradingJob> findById(Long id) { return jpaRepository.findById(id); }
    @Override public OmrGradingJob save(OmrGradingJob job) { return jpaRepository.save(job); }
}
