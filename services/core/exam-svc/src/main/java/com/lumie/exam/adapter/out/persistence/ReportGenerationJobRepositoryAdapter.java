package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportGenerationJobRepositoryAdapter implements ReportGenerationJobRepository {

    private final JpaReportGenerationJobRepository jpaRepository;

    @Override
    public ReportGenerationJob save(ReportGenerationJob job) {
        return jpaRepository.save(job);
    }

    @Override
    public Optional<ReportGenerationJob> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ReportGenerationJob> findByExamIdOrderByCreatedAtDesc(Long examId) {
        return jpaRepository.findByExamIdOrderByCreatedAtDesc(examId);
    }

    @Override
    @Transactional
    public void incrementSuccess(Long jobId) {
        jpaRepository.incrementSuccess(jobId);
    }

    @Override
    @Transactional
    public void incrementFail(Long jobId) {
        jpaRepository.incrementFail(jobId);
    }

    @Override
    @Transactional
    public boolean startProcessingIfPending(Long jobId) {
        return jpaRepository.startProcessingIfPending(jobId) > 0;
    }

    @Override
    @Transactional
    public boolean tryComplete(Long jobId, int totalReports) {
        return jpaRepository.completeIfAllProcessed(jobId, totalReports) > 0;
    }

    @Override
    @Transactional
    public void setZipFileKey(Long jobId, String zipFileKey) {
        jpaRepository.setZipFileKey(jobId, zipFileKey);
    }
}
