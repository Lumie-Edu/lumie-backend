package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.ExamAnswerKeyRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExamAnswerKeyRevisionPersistencePort {

    ExamAnswerKeyRevision save(ExamAnswerKeyRevision revision);

    ExamAnswerKeyRevision saveAndFlush(ExamAnswerKeyRevision revision);

    Page<ExamAnswerKeyRevision> findByExamId(Long examId, Pageable pageable);
}
