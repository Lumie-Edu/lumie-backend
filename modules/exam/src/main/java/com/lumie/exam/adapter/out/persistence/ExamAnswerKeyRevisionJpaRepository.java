package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamAnswerKeyRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAnswerKeyRevisionJpaRepository extends JpaRepository<ExamAnswerKeyRevision, Long> {

    @EntityGraph(attributePaths = "exam")
    Page<ExamAnswerKeyRevision> findByExamId(Long examId, Pageable pageable);
}
