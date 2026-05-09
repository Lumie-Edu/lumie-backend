package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.ReportGenerationJob;

import java.util.List;
import java.util.Optional;

public interface ReportGenerationJobRepository {

    ReportGenerationJob save(ReportGenerationJob job);

    Optional<ReportGenerationJob> findById(Long id);

    List<ReportGenerationJob> findByExamIdOrderByCreatedAtDesc(Long examId);

    void incrementSuccess(Long jobId);

    void incrementFail(Long jobId);

    boolean startProcessingIfPending(Long jobId);

    boolean tryComplete(Long jobId, int totalReports);

    void setZipFileKey(Long jobId, String zipFileKey);
}
