package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OmrGradingJobRepositoryAdapter implements OmrGradingJobRepository {

    private final JpaOmrGradingJobRepository jpaRepository;

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
}
