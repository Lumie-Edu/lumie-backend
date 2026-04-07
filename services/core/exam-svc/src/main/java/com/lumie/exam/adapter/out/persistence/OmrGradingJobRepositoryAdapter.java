package com.lumie.exam.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OmrGradingJobRepositoryAdapter implements OmrGradingJobRepository {

    private final JpaOmrGradingJobRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public OmrGradingJob save(OmrGradingJob job) {
        return jpaRepository.save(job);
    }

    @Override
    public Optional<OmrGradingJob> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<OmrGradingJob> findByExamIdOrderByCreatedAtDesc(Long examId) {
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
    public void appendResult(Long jobId, Map<String, Object> result) {
        try {
            String json = objectMapper.writeValueAsString(List.of(result));
            jpaRepository.appendResult(jobId, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize result to JSON", e);
        }
    }

    @Override
    @Transactional
    public boolean startProcessingIfPending(Long jobId) {
        return jpaRepository.startProcessingIfPending(jobId) > 0;
    }

    @Override
    @Transactional
    public boolean tryComplete(Long jobId, int totalImages) {
        return jpaRepository.completeIfAllProcessed(jobId, totalImages) > 0;
    }
}
