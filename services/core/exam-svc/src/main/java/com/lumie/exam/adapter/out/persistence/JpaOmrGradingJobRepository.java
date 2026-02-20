package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.OmrGradingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaOmrGradingJobRepository extends JpaRepository<OmrGradingJob, Long> {

    List<OmrGradingJob> findByExamIdOrderByCreatedAtDesc(Long examId);
}
