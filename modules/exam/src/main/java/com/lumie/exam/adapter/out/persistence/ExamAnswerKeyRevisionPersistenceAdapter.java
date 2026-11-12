package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.ExamAnswerKeyRevisionPersistencePort;
import com.lumie.exam.domain.entity.ExamAnswerKeyRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExamAnswerKeyRevisionPersistenceAdapter implements ExamAnswerKeyRevisionPersistencePort {

    private final ExamAnswerKeyRevisionJpaRepository jpaRepository;

    @Override
    public ExamAnswerKeyRevision save(ExamAnswerKeyRevision revision) {
        return jpaRepository.save(revision);
    }

    @Override
    public ExamAnswerKeyRevision saveAndFlush(ExamAnswerKeyRevision revision) {
        return jpaRepository.saveAndFlush(revision);
    }

    @Override
    public Page<ExamAnswerKeyRevision> findByExamId(Long examId, Pageable pageable) {
        return jpaRepository.findByExamId(examId, pageable);
    }
}
