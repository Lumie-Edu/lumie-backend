package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    List<ExamResult> findByExamId(Long examId);

    List<ExamResult> findByStudentId(Long studentId);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);
}
