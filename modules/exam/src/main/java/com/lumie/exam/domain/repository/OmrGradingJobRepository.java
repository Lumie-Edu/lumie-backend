package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.OmrGradingJob;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OmrGradingJobRepository {

    OmrGradingJob save(OmrGradingJob job);

    Optional<OmrGradingJob> findById(Long id);

    List<OmrGradingJob> findByExamIdOrderByCreatedAtDesc(Long examId);

    void incrementSuccess(Long jobId);

    void incrementFail(Long jobId);

    void appendResult(Long jobId, Map<String, Object> result);

    boolean startProcessingIfPending(Long jobId);

    boolean tryComplete(Long jobId, int totalImages);
}
